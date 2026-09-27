import assert from 'node:assert/strict'
import test from 'node:test'
import {
  buildPaymentIdempotencyKey,
  buildPaymentNo,
  canApplyAfterSale,
  canCancel,
  canComplete,
  canPay,
  orderSubActionTargets,
  resolveOrderItemName,
  resolveOrderItemSpec
} from './order-rules.ts'

const aggregate = {
  id: '10',
  orderNo: 'MAIN-10',
  userId: '20',
  totalAmount: 99,
  subOrders: [
    { subOrderId: '31', subOrderNo: 'SUB-31', merchantId: '41', payAmount: 39, status: 0 },
    { subOrderId: '32', subOrderNo: 'SUB-32', merchantId: '42', payAmount: 60, status: 0 }
  ],
  items: [
    { subOrderId: '31', skuId: '51', skuSnapshot: { skuName: 'Snapshot name' } },
    { subOrderId: '32', skuId: '52' }
  ]
}

test('order rules project an aggregate into actionable sub-orders', () => {
  const targets = orderSubActionTargets(aggregate)

  assert.equal(targets.length, 2)
  assert.equal(targets[0].payAmount, 39)
  assert.deepEqual(targets[0].items?.map((item) => item.skuId), ['51'])
  assert.equal(canPay(targets[0]), true)
  assert.equal(canCancel(aggregate), true)
  assert.equal(canComplete(aggregate), false)
})

test('order rules enforce after-sale metadata and lifecycle', () => {
  assert.equal(
    canApplyAfterSale({ id: '1', subOrderId: '2', merchantId: '3', status: 2, afterSaleStatus: 'NONE' }),
    true
  )
  assert.equal(canApplyAfterSale({ id: '1', status: 2 }), false)
})

test('payment identifiers are deterministic for an attempt and sanitize order numbers', () => {
  const order = { id: '7', orderNo: 'MAIN-7', subOrderNo: 'SUB / 7' }

  assert.equal(buildPaymentNo(order, 'attempt1'), 'PAY-SUB7-attempt1')
  assert.equal(
    buildPaymentIdempotencyKey(order, 'attempt1'),
    'payment:MAIN-7:SUB / 7:attempt1'
  )
})

test('order item display falls back through current and snapshot product data', () => {
  const item = {
    skuId: '51',
    skuSnapshot: { skuName: 'Snapshot name', specJson: '{"Color":"Blue","Size":"L"}' }
  }

  assert.equal(resolveOrderItemName(item), 'Snapshot name')
  assert.equal(resolveOrderItemSpec(item), 'Color: Blue / Size: L')
})
