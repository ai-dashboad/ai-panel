#!/usr/bin/env node

/**
 * 配置官方API Keys到AI Panel系统
 * 只使用官方API提供商，禁止第三方代理
 */

const axios = require('axios');

// 官方API配置
const OFFICIAL_APIS = {
    deepseek: {
        name: 'DeepSeek',
        baseURL: 'https://api.deepseek.com',
        official: true,
        models: ['deepseek-chat', 'deepseek-coder'],
        docs: 'https://platform.deepseek.com/api-docs'
    },
    openrouter: {
        name: 'OpenRouter',
        baseURL: 'https://openrouter.ai/api/v1',
        official: true,
        models: ['openai/gpt-4', 'anthropic/claude-3.5-sonnet', 'deepseek/deepseek-chat'],
        docs: 'https://openrouter.ai/docs'
    },
    anthropic: {
        name: 'Anthropic',
        baseURL: 'https://api.anthropic.com',
        official: true,
        models: ['claude-3-5-sonnet-20241022', 'claude-3-haiku-20240307'],
        docs: 'https://docs.anthropic.com/claude/reference'
    }
};

// 读取官方API Keys从环境变量
function getOfficialAPIKeys() {
    const keys = {
        deepseek: process.env.DEEPSEEK_API_KEY,
        openrouter: process.env.OPENROUTER_API_KEY,
        anthropic: process.env.ANTHROPIC_API_KEY
    };

    console.log('🔍 检查官方API Keys...');

    for (const [provider, key] of Object.entries(keys)) {
        if (key) {
            console.log(`✅ ${OFFICIAL_APIS[provider].name}: 已配置`);
        } else {
            console.log(`⏰ ${OFFICIAL_APIS[provider].name}: 未配置`);
        }
    }

    return keys;
}

// 配置API Keys到AI Panel
async function configureToAIPanel(provider, apiKey) {
    const AI_PANEL_URL = 'http://localhost:8080';

    try {
        console.log(`🔧 配置${OFFICIAL_APIS[provider].name}到AI Panel...`);

        // 使用AI Panel的API添加官方API Key
        const response = await axios.post(`${AI_PANEL_URL}/api/v1/providers`, {
            name: OFFICIAL_APIS[provider].name,
            type: provider,
            baseURL: OFFICIAL_APIS[provider].baseURL,
            apiKey: apiKey,
            official: true,
            models: OFFICIAL_APIS[provider].models
        }, {
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'Bearer your-ai-panel-admin-key'
            }
        });

        console.log(`✅ ${OFFICIAL_APIS[provider].name} 配置成功`);
        return true;

    } catch (error) {
        console.error(`❌ ${OFFICIAL_APIS[provider].name} 配置失败:`, error.message);
        return false;
    }
}

// 测试官方API连接
async function testOfficialAPI(provider, apiKey) {
    const config = OFFICIAL_APIS[provider];

    try {
        console.log(`🧪 测试${config.name}官方API...`);

        let testEndpoint;
        let testData;

        switch(provider) {
            case 'deepseek':
                testEndpoint = `${config.baseURL}/v1/chat/completions`;
                testData = {
                    model: 'deepseek-chat',
                    messages: [{ role: 'user', content: '测试消息' }],
                    max_tokens: 10
                };
                break;

            case 'openrouter':
                testEndpoint = `${config.baseURL}/chat/completions`;
                testData = {
                    model: 'deepseek/deepseek-chat',
                    messages: [{ role: 'user', content: '测试消息' }],
                    max_tokens: 10
                };
                break;

            case 'anthropic':
                testEndpoint = `${config.baseURL}/v1/messages`;
                testData = {
                    model: 'claude-3-haiku-20240307',
                    max_tokens: 10,
                    messages: [{ role: 'user', content: '测试消息' }]
                };
                break;
        }

        const response = await axios.post(testEndpoint, testData, {
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${apiKey}`
            },
            timeout: 10000
        });

        console.log(`✅ ${config.name} API测试成功`);
        return true;

    } catch (error) {
        console.error(`❌ ${config.name} API测试失败:`, error.message);
        return false;
    }
}

// 主函数
async function main() {
    console.log('🔐 AI Panel官方API Keys配置工具');
    console.log('==================================');

    console.log('\n📋 支持的官方API提供商:');
    for (const [key, config] of Object.entries(OFFICIAL_APIS)) {
        console.log(`   ${config.name}: ${config.baseURL}`);
    }

    console.log('\n🚫 严格禁止: 第三方代理、非官方API服务');

    const apiKeys = getOfficialAPIKeys();

    console.log('\n🧪 开始测试官方API连接...');

    for (const [provider, apiKey] of Object.entries(apiKeys)) {
        if (apiKey) {
            const testResult = await testOfficialAPI(provider, apiKey);

            if (testResult) {
                await configureToAIPanel(provider, apiKey);
            }
        }
    }

    console.log('\n✅ 官方API配置完成!');
}

if (require.main === module) {
    main().catch(console.error);
}

module.exports = { getOfficialAPIKeys, configureToAIPanel, testOfficialAPI, OFFICIAL_APIS };