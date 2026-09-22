import assert from 'node:assert/strict'
import test from 'node:test'
import { resolveApiErrorCategory } from './error-category.ts'

test('resolveApiErrorCategory prioritizes HTTP authentication and permission statuses', () => {
  assert.equal(resolveApiErrorCategory(200, 401), 'auth')
  assert.equal(resolveApiErrorCategory(200, 403), 'permission')
})

test('resolveApiErrorCategory maps gateway and downstream failures', () => {
  assert.equal(resolveApiErrorCategory(18002, 504), 'remote')
  assert.equal(resolveApiErrorCategory(14002, 500), 'system')
  assert.equal(resolveApiErrorCategory(18003, 429), 'rateLimit')
})

test('resolveApiErrorCategory preserves business fallback', () => {
  assert.equal(resolveApiErrorCategory(29999, 200), 'business')
})
