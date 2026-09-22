import assert from 'node:assert/strict'
import test from 'node:test'
import {
  configureHttpFailureHandler,
  reportHttpFailure
} from './failure-events.ts'

test('HTTP failure events keep transport independent from application policy', () => {
  let received
  const error = new Error('gateway unavailable')
  configureHttpFailureHandler((event) => {
    received = event
  })

  reportHttpFailure({ kind: 'response', error, status: 503 })

  assert.deepEqual(received, { kind: 'response', error, status: 503 })
  configureHttpFailureHandler()
})

test('HTTP failure reporting is safe before an application handler is installed', () => {
  configureHttpFailureHandler()
  assert.doesNotThrow(() => {
    reportHttpFailure({ kind: 'network', error: new Error('offline') })
  })
})
