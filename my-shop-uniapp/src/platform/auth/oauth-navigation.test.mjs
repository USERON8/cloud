import assert from 'node:assert/strict'
import test from 'node:test'
import { buildOidcLogoutPath } from './oauth-navigation.ts'

test('buildOidcLogoutPath carries the ID token and exact browser return URI', () => {
  const path = buildOidcLogoutPath({
    idToken: 'header.payload.signature',
    clientId: 'web-client',
    postLogoutRedirectUri: 'http://127.0.0.1:5173/login?logout=true'
  })
  const url = new URL(path, 'http://gateway.test')

  assert.equal(url.pathname, '/connect/logout')
  assert.equal(url.searchParams.get('id_token_hint'), 'header.payload.signature')
  assert.equal(url.searchParams.get('client_id'), 'web-client')
  assert.equal(
    url.searchParams.get('post_logout_redirect_uri'),
    'http://127.0.0.1:5173/login?logout=true'
  )
})
