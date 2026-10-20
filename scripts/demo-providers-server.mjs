#!/usr/bin/env node
/**
 * Local providers API for offline demos when channel-web + MySQL are unavailable.
 * Serves the same /api/v1/providers shape as ai-panel-channel ProviderConfigController.
 */
import http from 'node:http'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const DATA_FILE = path.join(__dirname, 'demo-data', 'providers.json')
const PORT = Number(process.env.DEMO_PROVIDERS_PORT || 8086)

function load() {
  return JSON.parse(fs.readFileSync(DATA_FILE, 'utf8'))
}

function save(data) {
  fs.writeFileSync(DATA_FILE, JSON.stringify(data, null, 2))
}

function envelope(data, message = 'Success') {
  return JSON.stringify({ code: 200, message, data, timestamp: Date.now() })
}

function readBody(req) {
  return new Promise((resolve, reject) => {
    const chunks = []
    req.on('data', (c) => chunks.push(c))
    req.on('end', () => {
      try {
        const raw = Buffer.concat(chunks).toString('utf8')
        resolve(raw ? JSON.parse(raw) : {})
      } catch (e) {
        reject(e)
      }
    })
  })
}

const server = http.createServer(async (req, res) => {
  res.setHeader('Access-Control-Allow-Origin', '*')
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization, X-User-ID')
  res.setHeader('Access-Control-Allow-Methods', 'GET,POST,PUT,DELETE,OPTIONS')
  if (req.method === 'OPTIONS') {
    res.writeHead(204)
    res.end()
    return
  }

  const url = new URL(req.url || '/', `http://localhost:${PORT}`)
  const pathname = url.pathname.replace(/\/$/, '') || '/'

  try {
    const store = load()

    if (req.method === 'GET' && pathname === '/api/v1/providers') {
      res.writeHead(200, { 'Content-Type': 'application/json' })
      res.end(envelope(store.providers))
      return
    }

    if (req.method === 'GET' && pathname === '/api/v1/providers/keys') {
      let keys = store.keys
      const providerId = url.searchParams.get('providerId')
      if (providerId) keys = keys.filter((k) => String(k.providerId) === String(providerId))
      res.writeHead(200, { 'Content-Type': 'application/json' })
      res.end(envelope(keys))
      return
    }

    if (req.method === 'GET' && pathname === '/api/v1/providers/status') {
      res.writeHead(200, { 'Content-Type': 'application/json' })
      res.end(
        envelope({
          healthy: store.providers.filter((p) => p.enabled).length,
          total: store.providers.length,
          providers: store.providers.map((p) => ({
            id: p.id,
            name: p.name,
            type: p.type,
            enabled: p.enabled,
            activeKeyCount: p.activeKeyCount,
          })),
        }),
      )
      return
    }

    if (req.method === 'POST' && pathname === '/api/v1/providers/reload') {
      res.writeHead(200, { 'Content-Type': 'application/json' })
      res.end(envelope(true, 'Key pool reloaded'))
      return
    }

    if (req.method === 'PUT' && pathname.match(/^\/api\/v1\/providers\/\d+$/)) {
      const id = Number(pathname.split('/').pop())
      const body = await readBody(req)
      const idx = store.providers.findIndex((p) => p.id === id)
      if (idx >= 0) {
        store.providers[idx] = { ...store.providers[idx], ...body, id, updatedAt: new Date().toISOString() }
        save(store)
        res.writeHead(200, { 'Content-Type': 'application/json' })
        res.end(envelope(store.providers[idx]))
        return
      }
    }

    res.writeHead(404, { 'Content-Type': 'application/json' })
    res.end(JSON.stringify({ code: 404, message: 'Not found', data: null }))
  } catch (err) {
    res.writeHead(500, { 'Content-Type': 'application/json' })
    res.end(JSON.stringify({ code: 500, message: err.message, data: null }))
  }
})

server.listen(PORT, () => {
  console.log(`Demo providers API listening on http://localhost:${PORT}`)
})
