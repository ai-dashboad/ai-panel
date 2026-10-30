# AI Panel

> A self-hosted **AI gateway and API key / model management panel**. Put every LLM provider behind one OpenAI-compatible endpoint, hand out your own API keys, and keep routing, rate limits, quotas and usage in one place.

[English](#overview) · [中文简介](#中文简介) · [License](#license)

[![License: MIT](https://img.shields.io/badge/license-MIT-A78BFA?style=flat-square)](LICENSE) [![Release](https://img.shields.io/github/v/release/ai-dashboad/ai-panel?style=flat-square&color=EC4899)](https://github.com/ai-dashboad/ai-panel/releases) ![Java 17](https://img.shields.io/badge/Java-17-ED8B00?style=flat-square&logo=openjdk&logoColor=white) ![Spring AI](https://img.shields.io/badge/Spring_AI-6DB33F?style=flat-square&logo=spring&logoColor=white) ![Vue 3](https://img.shields.io/badge/Vue-3-4FC08D?style=flat-square&logo=vuedotjs&logoColor=white) ![OpenAI-compatible](https://img.shields.io/badge/API-OpenAI--compatible-7C3AED?style=flat-square)

---

## Overview

AI Panel is a Java / Spring Cloud microservice platform with a Vue 3 admin console. It sits between your apps (IDEs, SDKs, internal tools) and upstream model providers such as OpenAI, Anthropic, DeepSeek and Moonshot. It lets you:

- expose **one OpenAI-compatible API** (`/v1/chat/completions`, `/v1/models`) to every client,
- issue and revoke **your own API keys** (`sk-…`) per user, team or channel, with per-key model allow-lists and expiry,
- manage a **pool of upstream provider keys** with weighted round-robin and automatic failover,
- route requests across models with **fallback chains and circuit breakers**,
- enforce **rate limits and quotas** (Redis token bucket), and
- track **usage, cost and model health** on a dashboard.

> **Status:** early stage (`1.0.0-SNAPSHOT`). Some services are still being built; see [Known limitations](#known-limitations) before running it in production.

## Features

| Module | What it does (from the code) |
|---|---|
| **ai-panel-gateway** | Spring Cloud Gateway entry point. `ApiKeyAuthFilter` validates `Authorization: Bearer sk-…` keys, `JwtAuthFilter` protects console APIs, and `RateLimitFilter` applies global, per-user (by plan) and per-key limits using a Redis + Lua token bucket. Returns `X-RateLimit-*` headers. Routes to services through Eureka (`lb://…`). |
| **ai-panel-user** | Registration, login and refresh with JWT; user profile (`/api/v1/auth/**`, `/api/v1/users/**`). It also has an optional **order auto-delivery** feature: admins configure external sales platforms and product SKUs, and a signed webhook (`/api/v1/webhook/{platform}`, HMAC-SHA256) creates orders and delivers an API key automatically. Admins can view, retry and cancel orders and see delivery logs. |
| **ai-panel-key** | API key lifecycle: create, list, update, delete, regenerate, and view per-key usage. Keys are stored only as **SHA-256 hashes** with a display prefix. Validation results are cached in Redis, and an internal validation endpoint serves the gateway. |
| **ai-panel-dispatch** | Core model router. `ModelRouterService` uses Resilience4j circuit breakers and per-model fallback chains (for example `gpt-4o → gpt-4-turbo → claude-3-5-sonnet → gpt-3.5-turbo`). Has OpenAI and Anthropic clients, sync and SSE-streaming chat completions, model groups, and stats (daily per-user usage, per-model stats, provider errors, model health). Checks quota against the billing service before each call. |
| **ai-panel-channel** | OpenAI-compatible API (`POST /v1/chat/completions` with JSON or SSE streaming, `GET /v1/models`). Routes **any model name** registered on a provider (`Qwen/Qwen3-32B`, `glm-4.5`, `deepseek-r1:32b`, …) to hosted providers or **self-hosted vLLM / Ollama / any OpenAI-compatible server** (custom base URL, optional key, several providers per type, priority failover). **Tool calling** is passed through end to end (`tools`, `tool_choice`, `parallel_tool_calls`, `tool_calls` / `tool` messages, streaming `tool_calls` deltas, real `finish_reason`), with no injected sampling defaults. The Anthropic adapter maps tools to `tool_use` / `tool_result`. Manages **provider key pools** (`ApiKeyPool`: weighted round-robin, failover, reset), sales/distribution *channels* with bulk key generation, and usage logging. See [docs/self-hosted-models.md](docs/self-hosted-models.md). |
| **ai-panel-model** | Model catalog: CRUD for models (context window, max output, input/output price, features, fallback chain) and providers, plus provider sync. |
| **ai-panel-billing** | Usage recording, usage summaries, usage records, user balance and credit top-ups. |
| **ai-panel-analytics** | Dashboard metrics, plus usage broken down by time range, channel and API key. |
| **ai-panel-eureka** | Service registry, protected with HTTP basic auth. |
| **ai-panel-common** | Shared `R<T>` response envelope, `PageResult`, error codes, exceptions, plan types (FREE / PRO / ENTERPRISE with RPM, daily and monthly limits), a catalog of 30+ provider types (OpenAI, Anthropic, Google, Azure OpenAI, Bedrock, Mistral, Groq, DeepSeek, Moonshot, Qwen, Zhipu, Doubao, Ollama, vLLM, LM Studio, any OpenAI-compatible endpoint, and more), and the API key generator. |
| **ai-panel-ui** | Vue 3 + Element Plus admin console: login and register, first-run **setup wizard**, dashboard, API keys, providers, model catalog, usage and billing, subscription requests, settings, and delivery (platforms / SKUs / orders). Playwright E2E tests are in `e2e/`. |
| **workers/** *(experimental)* | A Cloudflare Workers port (Hono + Drizzle + D1/KV) of the **key**, **model**, **billing** (with a monthly token quota check) and **subscriptions** (employees request access to a service and admins review it) services. All four verify the same JWT that ai-panel-user issues (shared `JWT_SECRET`). Service-to-service endpoints require an `X-Internal-Token`. |

> Note: the channel service (`/v1/chat/completions` on port `8086`) has native adapters for **OpenAI**, **Anthropic** and **any OpenAI-compatible server** (vLLM, Ollama, SGLang, LM Studio, DeepSeek, Moonshot, Zhipu, Qwen, SiliconFlow, OpenRouter, …). The gateway path `/api/v1/chat/completions` goes to ai-panel-dispatch, which still only has basic OpenAI and Anthropic clients (see [Known limitations](#known-limitations)).

## Architecture

```mermaid
flowchart LR
    subgraph Clients
        IDE["IDEs / SDKs / apps<br/>(OpenAI-compatible)"]
        UI["ai-panel-ui<br/>Vue 3 console"]
    end

    IDE -->|"Bearer sk-…"| GW
    UI -->|"JWT"| GW

    subgraph Platform["AI Panel (Spring Cloud)"]
        GW["ai-panel-gateway<br/>API key auth · JWT · rate limit"]
        EU[("ai-panel-eureka<br/>service registry")]
        USER["ai-panel-user<br/>auth · users · order delivery"]
        KEY["ai-panel-key<br/>API keys (SHA-256)"]
        DIS["ai-panel-dispatch<br/>model router · fallback · circuit breaker"]
        CH["ai-panel-channel<br/>OpenAI-compatible API · provider key pool"]
        MOD["ai-panel-model<br/>model catalog"]
        BILL["ai-panel-billing<br/>usage · balance"]
        AN["ai-panel-analytics<br/>dashboards"]
    end

    GW --> USER & KEY & DIS & BILL
    GW -. validate key .-> KEY
    DIS -. quota check .-> BILL
    DIS & CH --> P["Upstream providers<br/>OpenAI · Anthropic · DeepSeek · Moonshot · …"]

    USER & KEY & DIS & CH & MOD & BILL & AN --> DB[("MySQL 8")]
    GW & KEY & DIS & CH --> R[("Redis 7")]
    Platform -.register.-> EU
```

**Request path:** client → gateway (validate API key → rate limit → inject `X-User-Id` / API key headers) → dispatch/channel (quota check → choose model and provider key → circuit breaker → fallback) → upstream provider → usage logged for billing and analytics.

Each backend service uses a **4-layer layout**:

```
<service>-api    DTOs, Feign clients, OpenAPI annotations
<service>-core   business logic / service interfaces
<service>-data   persistence (MyBatis-Plus mappers, Redis cache)
<service>-web    Spring Boot app, controllers, configuration
```

## Tech stack

| Layer | Technologies |
|---|---|
| Backend | Java 17, Spring Boot 3.2, Spring Cloud 2023.0 (Gateway, Netflix Eureka, OpenFeign), Spring AI 1.0.0-M6, MyBatis-Plus 3.5, ShardingSphere-JDBC 5.4 (optional usage-record sharding), Resilience4j, JJWT, springdoc-openapi |
| Data | MySQL 8, Redis 7 |
| Config (optional) | Apollo |
| Frontend | Vue 3, TypeScript, Vite 5, Pinia, Element Plus, ECharts, Tailwind CSS |
| Edge (experimental) | Cloudflare Workers, Hono, Drizzle ORM, D1, KV |
| Testing | Playwright (UI E2E), Vitest (workers); `mvn test` is wired up for backend tests |
| Ops | Docker, Docker Compose, Nginx |

## Quick start (Docker Compose)

**Prerequisites:** Docker with Compose v2, JDK 17, and Maven 3.9+. The backend Dockerfiles copy pre-built jars, so build them first.

```bash
git clone https://github.com/ai-dashboad/ai-panel.git
cd ai-panel

# 1. Configure environment
cp .env.example .env
#    then edit .env and set at least:
#      MYSQL_PASSWORD   – MySQL root password
#      JWT_SECRET       – long random string, e.g. `openssl rand -base64 48`
#      EUREKA_PASSWORD  – registry basic-auth password
#      OPENAI_API_KEY / ANTHROPIC_API_KEY / DEEPSEEK_API_KEY (optional)

# 2. Build backend jars
mvn clean package -DskipTests

# 3. Start everything (MySQL, Redis, Eureka, gateway, services, UI)
docker compose -f docker-compose.dev.yml up -d --build
```

Once the containers are healthy:

| Service | URL |
|---|---|
| Web console | http://localhost:3000 |
| API gateway | http://localhost:8080 |
| Eureka dashboard | http://localhost:8761 (basic auth: `EUREKA_USER` / `EUREKA_PASSWORD`) |
| Dispatch Swagger UI | http://localhost:8082/swagger-ui.html |

On first start, MySQL runs the scripts in `scripts/sql/`, which create the databases and tables. They also seed a default admin account (`role = ADMIN`):

| Username | Password |
|---|---|
| `admin` | `admin123` |

> ⚠️ **This password is public. Change it immediately after first login** (console → *Settings → Security*, or `PUT /api/v1/users/me` with `currentPassword` / `newPassword`). Better still, replace the bcrypt hash in `scripts/sql/01-user-service-init.sql` before the first start of any deployment reachable from a network.

The setup wizard then walks you through adding a provider key and creating your first API key.

`make help` lists other shortcuts (`make infra`, `make build`, `make dev`, `make logs`, …). `deploy/one-click-deploy.sh` builds and starts everything in one step and creates a `.env` with random secrets if none exists.

### Call the API

```bash
curl http://localhost:8080/api/v1/chat/completions \
  -H "Authorization: Bearer sk-YOUR_AI_PANEL_KEY" \
  -H "Content-Type: application/json" \
  -d '{"model":"gpt-4o","messages":[{"role":"user","content":"Hello"}]}'
```

The channel service also serves the standard OpenAI paths (`/v1/chat/completions`, `/v1/models`) on port `8086`. Point any OpenAI SDK's `base_url` at it (`http://localhost:8086/v1`) and use a channel API key.

**Self-hosted models and tool calling:** to serve Qwen, DeepSeek, GLM and other models from your own vLLM or Ollama servers, and to use function/tool calling through ai-panel, see **[docs/self-hosted-models.md](docs/self-hosted-models.md)**.

## Local development

```bash
# Infrastructure only
docker compose -f docker-compose.dev.yml up -d mysql redis

# Registry (requires EUREKA_PASSWORD in the environment)
export EUREKA_PASSWORD=change_me JWT_SECRET="$(openssl rand -base64 48)"
mvn -pl ai-panel-eureka spring-boot:run

# Services (one terminal each)
mvn -pl ai-panel-gateway/gateway-web spring-boot:run
mvn -pl ai-panel-user/user-web       spring-boot:run
mvn -pl ai-panel-key/key-web         spring-boot:run
mvn -pl ai-panel-dispatch/dispatch-web spring-boot:run
# … channel-web, model-web, billing-web, analytics-web

# Frontend (http://localhost:5173, proxies /api to the gateway – see vite.config.ts)
npm --prefix ai-panel-ui install
npm --prefix ai-panel-ui run dev
```

Experimental Cloudflare Workers (each in `workers/<name>`):

```bash
cd workers/model && npm install && npm run db:migrate:local && npm run dev   # :8787
# workers/key :8788 · workers/billing :8789 · workers/subscriptions :8793
```

For local secrets, copy `workers/<name>/.dev.vars.example` to `.dev.vars` in each worker. In production, set them with `npx wrangler secret put JWT_SECRET` (and `INTERNAL_API_TOKEN` for `key` and `billing`), and replace the `REPLACE_WITH_*` IDs in each `wrangler.toml`.

Worker auth model:

| Worker | User endpoints (Bearer JWT from ai-panel-user) | Admin-only (`role=ADMIN` claim or `ADMIN_USERNAMES`) | Internal (`X-Internal-Token: $INTERNAL_API_TOKEN`) |
|---|---|---|---|
| `key` | all `/api/v1/keys/**` (the user id comes from the token's `sub`) | — | `GET /validate`, `POST /internal/validate` |
| `model` | all reads | `POST`/`PUT`/`DELETE` on models, provider sync | — |
| `billing` | `/usage/summary`, `/records`, `/balance` (own data; other users' data is admin-only) | `POST /credits` | `GET /quota-check`, `POST /usage` |
| `subscriptions` | all | reviewing requests | — |

All four workers fail closed: if `JWT_SECRET` / `INTERNAL_API_TOKEN` are not configured, every request is rejected.

### Tests

```bash
npm --prefix ai-panel-ui test              # Playwright E2E
(cd workers/key && npm test)               # workers: Vitest auth tests (key, model, billing, subscriptions)
mvn test                                   # backend unit tests (gateway JWT/admin filter, CORS, user-service admin guard,
                                           #   channel adapters/routing/tool calling against a mock upstream)
```

## Configuration

All services read their settings from environment variables (Spring `${VAR:default}` placeholders). The most important ones:

| Variable | Used by | Description |
|---|---|---|
| `MYSQL_HOST`, `MYSQL_PORT`, `MYSQL_USER`, `MYSQL_PASSWORD`, `MYSQL_DATABASE` | all DB-backed services | MySQL connection (one database per service: `ai_panel_user`, `ai_panel_key`, `ai_panel_dispatch`, `ai_panel_model`, `ai_panel_billing`, …) |
| `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` | gateway, key, dispatch, channel, billing, … | Redis connection |
| `JWT_SECRET` | user, gateway, channel, all workers | HMAC secret for console JWTs. **Required; must be identical everywhere.** Without it the channel service's admin APIs (`/api/v1/providers`, `/api/v1/channels`) are disabled. |
| `INTERNAL_API_TOKEN` | dispatch, workers/key, workers/billing | Shared secret sent as `X-Internal-Token` on service-to-service calls (e.g. the dispatch → billing quota check) |
| `CORS_ALLOWED_ORIGINS` | gateway, dispatch | Comma-separated browser origins allowed to call the API, e.g. `https://panel.example.com`. Default (dev only): `http://localhost:5173,http://127.0.0.1:5173,http://localhost:3000`. **Required by `docker-compose.prod.yml`.** See [CORS](#cors). |
| `JWT_ACCESS_EXPIRATION`, `JWT_REFRESH_EXPIRATION` | user | Token lifetimes in seconds (default 3600 / 604800) |
| `EUREKA_URL` | all clients | Registry URL, e.g. `http://admin:<pwd>@eureka:8761/eureka/` |
| `EUREKA_USER`, `EUREKA_PASSWORD` | eureka | Registry basic-auth credentials (**password is required**) |
| `OPENAI_API_KEY`, `OPENAI_BASE_URL` | dispatch, channel | OpenAI credentials and endpoint (channel: used as a fallback only when `OPENAI_ENABLED=true`; providers configured in the console take precedence) |
| `ANTHROPIC_API_KEY`, `ANTHROPIC_BASE_URL` | dispatch, channel | Anthropic credentials and endpoint (channel: fallback when `ANTHROPIC_ENABLED=true`) |
| `GOOGLE_API_KEY`, `DEEPSEEK_API_KEY`, `MOONSHOT_API_KEY` (+ `*_BASE_URL`) | dispatch, channel | Other providers |
| `BILLING_QUOTA_CHECK_URL` | dispatch | Quota check endpoint (default: billing worker on `:8789`) |
| `APOLLO_ENABLED`, `APOLLO_META` | services | Optional Apollo config center (disabled by default) |
| `SERVER_PORT`, `SPRING_PROFILES_ACTIVE` | services | Standard Spring Boot settings |
| `DOCKER_REGISTRY`, `VERSION` | `docker-compose.prod.yml` | Image registry and tag for production deployments |

### Roles and admin APIs

Each user has a `role` (`USER` or `ADMIN`, column `t_user.role`). ai-panel-user puts it in the access token as the `role` claim. The `/api/v1/admin/**` routes (delivery platforms, SKUs, orders) are admin-only, and this is enforced twice:

1. **Gateway** (`JwtAuthFilter`): no valid access token → `401`, token without `role=ADMIN` → `403`. The gateway also strips client-supplied `X-User-ID` / `X-User-Role` / `X-Plan-Type` headers on every request and does not expose `/api/v1/**/internal/**` endpoints.
2. **User service** (`AdminAuthInterceptor`): verifies the bearer token itself (it ignores `X-User-ID`) and checks `t_user.role` in the database, so demoting a user takes effect immediately even though their token is valid for up to an hour.

The seeded `admin` account has `role = 'ADMIN'`; new registrations get `USER`. To promote someone: `UPDATE ai_panel_user.t_user SET role = 'ADMIN' WHERE username = '…';` (they need to sign in again to get a token with the new claim). Upgrading a database created before roles existed: run `scripts/sql/migrations/V006__user_role.sql`. The console hides the delivery pages for non-admins.

The channel service's management APIs (`/api/v1/providers/**`: upstream providers, keys and models; `/api/v1/channels/**`: sales channels and bulk key generation) are admin-only too. The gateway applies the same 401/403 rules, and ai-panel-channel verifies the admin JWT itself (`AdminJwtInterceptor`), because the console's nginx proxies `/api/v1/providers` straight to it. Without `JWT_SECRET` these APIs answer `503`.

The Workers accept either the `role=ADMIN` claim or a username listed in `ADMIN_USERNAMES`.

### CORS

The gateway (and dispatch) only send CORS headers to origins listed in `CORS_ALLOWED_ORIGINS` (comma-separated, `scheme://host[:port]`, no paths; Spring origin patterns such as `https://*.example.com` work too). Preflight and cross-origin requests from any other origin get `403`. Credentials (cookies) are never allowed, because the API uses bearer tokens.

- **Dev:** the default covers the Vite dev server (`:5173`) and the Docker UI (`:3000`). Both proxy `/api` to the gateway, but they forward the browser's `Origin`, so the origin still has to be allowed.
- **Production:** `docker-compose.prod.yml` refuses to start unless `CORS_ALLOWED_ORIGINS` is set. An empty value rejects every cross-origin request. `*` is accepted only as the sole entry, as an explicit opt-in, and logs a warning.

The Workers send no CORS headers, so browsers can only reach them through a same-origin proxy (as the Vite config does).

You can also add provider keys at runtime from the **Providers** page or the setup wizard. Those keys are stored in the database and served from the key pool.

Default service ports: gateway `8080`, user `8081`, dispatch `8082`, model `8083`, billing `8084`, key `8085`, channel `8086`, analytics `8087`, eureka `8761`, UI `3000` (Docker) / `5173` (Vite dev).

## Project structure

```
ai-panel/
├── ai-panel-common/        # shared DTOs, enums, exceptions, utils
├── ai-panel-eureka/        # service registry
├── ai-panel-gateway/       # API gateway (auth, rate limiting, routing)
├── ai-panel-user/          # users, JWT auth, order auto-delivery
├── ai-panel-key/           # API key management
├── ai-panel-dispatch/      # model routing, fallback, provider clients, stats
├── ai-panel-channel/       # OpenAI-compatible API, provider key pools, channels
├── ai-panel-model/         # model catalog
├── ai-panel-billing/       # usage & balance
├── ai-panel-analytics/     # analytics dashboards
├── ai-panel-ui/            # Vue 3 admin console (+ Playwright e2e)
├── workers/                # experimental Cloudflare Workers (key, model, billing, subscriptions)
├── AGENTS.md               # contributor / AI coding-agent guide
├── deploy/                 # Dockerfiles, nginx config, one-click deploy script
├── scripts/sql/            # database init scripts & migrations
├── scripts/sharding/       # ShardingSphere config for billing usage records
├── docker-compose.dev.yml  # full local stack
├── docker-compose.prod.yml # production-style stack (pre-built images)
├── Makefile
└── pom.xml                 # Maven multi-module root
```

## Known limitations

This is an early-stage project (`1.0.0-SNAPSHOT`). Known gaps, which are good first contributions:

- `docker-compose.dev.yml` does not start `ai-panel-model` yet, and there is no `Dockerfile.model`.
- `docker-compose.prod.yml` mounts `deploy/nginx/nginx.conf` and `deploy/nginx/ssl/`. Neither is in the repo, so provide your own.
- Two chat entry points exist. The channel service (`:8086/v1/chat/completions`, channel API keys) has the full provider support: self-hosted/OpenAI-compatible providers, arbitrary model names and tool calling. The gateway route `/api/v1/chat/completions` goes to ai-panel-dispatch, whose OpenAI/Anthropic clients don't support tools or custom providers yet, and the gateway has no route to the channel's `/v1/**` API. Merging the two paths is the next step.
- Azure OpenAI (deployment-based URLs) has no adapter yet. Gemini works through its OpenAI-compatible endpoint (type `google`).
- The Anthropic adapter covers text, images, tools and thinking deltas. Prompt caching, citations, PDFs and server tools are not mapped.
- Provider API keys are stored in plain text in `provider_api_key.api_key`. Restrict database access, or contribute encryption at rest.
- Billing in the channel service uses a flat placeholder rate per token. Per-model prices on `provider_model` are not applied yet.
- Upgrading an existing database: run `scripts/sql/migrations/V007__provider_multi_instance.sql` to allow several providers of the same type.
- The Spring services trust the `X-User-ID` header set by the gateway (the gateway strips any client-supplied value). Only expose the gateway, never the individual services. `docker-compose.dev.yml` publishes service ports for debugging; don't use it on an untrusted network.
- Actuator health details are public. Tighten them for production.
- Backend unit tests cover the security filters and the channel service's adapters/routing. Other services have none yet.

## Security

- Never commit `.env` or `.dev.vars`. Only `.env.example` belongs in git.
- Always set strong values for `JWT_SECRET`, `EUREKA_PASSWORD` and `MYSQL_PASSWORD`. The services intentionally refuse to start without a JWT secret.
- Change the seeded admin password (`admin` / `admin123`) right after the first login.
- Set `CORS_ALLOWED_ORIGINS` to the exact origin(s) your console is served from (see [CORS](#cors)).
- Give each Cloudflare Worker its own `JWT_SECRET` (identical to ai-panel-user's) and `INTERNAL_API_TOKEN` via `wrangler secret put`.
- To report a vulnerability, please open a private security advisory on GitHub rather than a public issue.

## Contributing

Issues and pull requests are welcome. Code style: Google Java Style (Lombok, Jakarta Validation) for Java, and Prettier + ESLint (Vue Composition API) for TypeScript. Use Conventional Commits. Please run the Playwright suite (and `mvn test` once you add backend tests) before you open a PR.

## License

Released under the [MIT License](LICENSE). Copyright (c) 2026 ai-dashboad.

---

## 中文简介

**AI Panel** 是一个可私有化部署的 **企业级 AI 网关 / API Key 与模型管理面板**。它把 OpenAI、Anthropic、DeepSeek、Moonshot 等多家模型服务统一到一个 **OpenAI 兼容接口** 下，提供:

- **统一接入**:`/v1/chat/completions`、`/v1/models`,支持同步与 SSE 流式响应;
- **API Key 管理**:为用户、团队或渠道签发与吊销自有 `sk-` Key,只存 SHA-256 哈希,可设置模型白名单和过期时间;
- **上游 Key 池**:加权轮询、故障自动切换,支持一键或批量配置供应商;
- **自部署模型**:接入 vLLM、Ollama 或任意 OpenAI 兼容服务(自定义 Base URL、API Key 可选),支持任意模型名(如 `Qwen/Qwen3-32B`、`glm-4.5`、`deepseek-r1`)及 Tool Calling 透传(含流式 `tool_calls`),详见 [docs/self-hosted-models.md](docs/self-hosted-models.md);
- **智能路由**:基于 Resilience4j 熔断与模型降级链(fallback chain);
- **限流与配额**:Redis + Lua 令牌桶,分全局、用户套餐、单 Key 三级限流;
- **用量与计费**:用量记录、余额、充值、看板统计与模型健康度;
- **订单自动发货(可选)**:配置外部销售平台与 SKU,通过签名 Webhook 自动生成并发放 API Key;
- **管理后台**:Vue 3 + Element Plus,内置首次安装向导。

**技术栈**:Java 17 · Spring Boot 3.2 · Spring Cloud(Gateway / Eureka / OpenFeign)· Spring AI · MyBatis-Plus · MySQL 8 · Redis 7 · Vue 3 · Vite · Element Plus;另有实验性的 Cloudflare Workers(Hono + D1)实现。

**快速开始**:

```bash
cp .env.example .env        # 填写 MYSQL_PASSWORD、JWT_SECRET、EUREKA_PASSWORD 及模型 API Key
mvn clean package -DskipTests
docker compose -f docker-compose.dev.yml up -d --build
# 控制台: http://localhost:3000    网关: http://localhost:8080
```

默认管理员账号为 `admin` / `admin123`(公开密码),**首次登录后请立即在 设置 → 安全 中修改**。详细配置见上文 [Configuration](#configuration)。

本项目基于 [MIT 许可证](LICENSE) 开源。
