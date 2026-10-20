# AI Panel - Contributor & AI Agent Guide

## Project Overview

AI Panel is a multi-model AI distribution platform that provides a unified API gateway for accessing various AI providers (OpenAI, Anthropic, Google, DeepSeek, Moonshot, etc.).

## Architecture

### Backend (Java Spring Boot 3.2)

```
ai-panel/
├── ai-panel-common/          # Shared utilities, DTOs, exceptions
├── ai-panel-eureka/          # Service registry
├── ai-panel-gateway/         # API Gateway (rate limiting, auth)
│   ├── gateway-api/
│   ├── gateway-core/
│   ├── gateway-data/
│   └── gateway-web/
├── ai-panel-user/            # User management
├── ai-panel-key/             # API Key management
├── ai-panel-dispatch/        # Model routing (CORE)
├── ai-panel-model/           # Model metadata
├── ai-panel-billing/         # Usage & billing
└── ai-panel-ui/              # Vue 3 frontend
```

### 4-Layer Architecture

Each service follows:
1. **API** - DTOs, Feign clients, OpenAPI annotations
2. **Core** - Business logic, service interfaces
3. **Data** - Persistence, MyBatis mappers, cache
4. **Web** - Controllers, configuration

### Key Technologies

- **Spring Boot 3.2** with Java 17
- **MyBatis-Plus** for ORM
- **ShardingSphere-JDBC** for optional sharding of billing usage records
- **Resilience4j** for circuit breakers
- **Redis** for caching and rate limiting
- **Eureka** for service discovery
- **Apollo** for configuration (optional)

### Frontend (Vue 3)

- **Vue 3** with Composition API
- **Vite** for build
- **Pinia** for state management
- **Element Plus** for UI components
- **TypeScript** for type safety

## Key Patterns

### API Key Flow
```
Request → Gateway → Validate API Key → Rate Limit → Route to Service
```

### Model Routing
```
1. Parse requested model
2. Check fallback chain
3. Try primary provider
4. Circuit breaker check
5. Fallback to next provider if failed
```

### Response Format
```java
R<T> {
    int code;      // 0 = success
    String message;
    T data;
}
```

## Common Tasks

### Add New AI Provider

1. Create client in `dispatch-core/client/impl/`
2. Implement `AiClient` interface
3. Add to `SUPPORTED_MODELS` set
4. Update fallback chains in `ModelRouterService`
5. Add provider to database init script

### Add New API Endpoint

1. Create DTO in `*-api` module
2. Add Feign method to API interface
3. Implement service in `*-core`
4. Create controller in `*-web`
5. Add OpenAPI annotations

### Database Changes

1. Create migration in `scripts/sql/migrations/`
2. Update PO class if needed
3. Update mapper if needed
4. Apply the migration to your local MySQL (e.g. `docker compose exec mysql mysql ...`)

## Testing

```bash
# Backend tests
mvn test

# Frontend unit tests
npm --prefix ai-panel-ui test

# Frontend E2E tests (Playwright)
npm --prefix ai-panel-ui run test:e2e

# Integration tests
mvn verify -Pit
```

### Frontend UI Testing Rules

**IMPORTANT: Always use Playwright for UI testing after making frontend changes.**

1. **Run Playwright tests after any UI changes:**
   ```bash
   cd ai-panel-ui && npx playwright test
   ```

2. **Create tests for new pages/features:**
   - Add test file in `ai-panel-ui/e2e/` directory
   - Use the auth fixture for authenticated pages:
     ```typescript
     import { test, expect, setupAuthState } from './fixtures/auth'

     test.beforeEach(async ({ page }) => {
       await setupAuthState(page)
       await page.goto('/your-page')
     })
     ```
   - Add mock API responses in `e2e/fixtures/auth.ts`

3. **Test coverage requirements:**
   - Page header and main elements visibility
   - Button clicks and form interactions
   - Dialog open/close behavior
   - Form validation
   - Navigation between pages

4. **Before deploying UI to Docker:**
   - Run `npx playwright test` to verify all tests pass
   - Fix any failing tests before deployment

## Local Development

```bash
# Start infrastructure
docker-compose -f docker-compose.dev.yml up -d mysql redis

# Start Eureka
mvn -pl ai-panel-eureka spring-boot:run

# Start services
mvn -pl ai-panel-gateway/gateway-web spring-boot:run
mvn -pl ai-panel-user/user-web spring-boot:run
# ... etc

# Start frontend
npm --prefix ai-panel-ui run dev
```

## Important Files

- `pom.xml` - Root Maven configuration
- `scripts/sql/` - Database initialization
- `docker-compose.*.yml` - Container orchestration
- `.env.example` - Environment variable template (copy to `.env`, never commit `.env`)
- `workers/` - Experimental Cloudflare Workers ports (key, model, billing, subscriptions)

## Code Style

- Java: Google Java Style (with Lombok, Jakarta Validation)
- TypeScript: Prettier + ESLint (Vue Composition API)
- Commits: Conventional Commits (with scope)
- **Documentation: All documentation must be written in English**

## Git Hooks

```bash
# Pre-commit: Check code style
mvn spotless:check && npm --prefix ai-panel-ui run lint

# Pre-push: Run tests
mvn test -DskipITs
```


## Context Paths

**Include:**
- `ai-panel-common/src/**/*.java`
- `ai-panel-*/*/src/**/*.java`
- `ai-panel-ui/src/**/*.{ts,vue}`
- `scripts/**/*.sql`
- `docker-compose*.yml`

**Exclude:**
- `**/target/**`
- `**/node_modules/**`
- `**/dist/**`
- `**/*.log`

## Security Notes

- API keys stored as SHA-256 hashes
- JWT for user authentication
- Rate limiting with Redis Lua scripts
- No secrets in code - use environment variables (`JWT_SECRET`, `EUREKA_PASSWORD`, `MYSQL_PASSWORD`, provider keys)
- Workers verify the same HMAC-signed JWT as ai-panel-user (shared `JWT_SECRET`); internal endpoints require `INTERNAL_API_TOKEN`
- `/api/v1/admin/**` requires `role=ADMIN` (JWT `role` claim from `t_user.role`), enforced in the gateway `JwtAuthFilter` and the user-service `AdminAuthInterceptor`; the gateway strips client-supplied `X-User-ID`/`X-User-Role`
- CORS is an allowlist (`CORS_ALLOWED_ORIGINS`); never reintroduce `allowedOrigins("*")`
