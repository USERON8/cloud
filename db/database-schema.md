# Database Schema (Generated)
Version: 1.1.0
Generated: 2026-03-12

This document is generated from `db/init/**/init.sql` and reflects the current repository state.
If a table definition differs from runtime, update the corresponding init SQL first.

## auth_db

### auth_oauth_account

| Column | Type |
| --- | --- |
| id | BIGINT UNSIGNED |
| user_id | BIGINT UNSIGNED |
| provider | VARCHAR(32) |
| provider_user_id | VARCHAR(100) |
| provider_username | VARCHAR(100) |
| email | VARCHAR(100) |
| avatar_url | VARCHAR(255) |
| created_at | DATETIME |
| updated_at | DATETIME |
| deleted | TINYINT |
| version | INT |

### auth_user

| Column | Type |
| --- | --- |
| id | BIGINT UNSIGNED |
| username | VARCHAR(50) |
| password | VARCHAR(255) |
| status | TINYINT |
| created_at | DATETIME |
| updated_at | DATETIME |
| deleted | TINYINT |
| version | INT |

### sys_permission

| Column | Type |
| --- | --- |
| id | BIGINT UNSIGNED |
| permission_name | VARCHAR(64) |
| permission_code | VARCHAR(128) |
| http_method | VARCHAR(16) |
| api_path | VARCHAR(255) |
| created_at | DATETIME |
| updated_at | DATETIME |
| deleted | TINYINT |
| version | INT |

### sys_role

| Column | Type |
| --- | --- |
| id | BIGINT UNSIGNED |
| role_name | VARCHAR(64) |
| role_code | VARCHAR(64) |
| role_status | TINYINT |
| created_at | DATETIME |
| updated_at | DATETIME |
| deleted | TINYINT |
| version | INT |

### sys_role_permission

| Column | Type |
| --- | --- |
| id | BIGINT UNSIGNED |
| role_id | BIGINT UNSIGNED |
| permission_id | BIGINT UNSIGNED |
| created_at | DATETIME |
| updated_at | DATETIME |
| deleted | TINYINT |
| version | INT |

### sys_user_role

| Column | Type |
| --- | --- |
| id | BIGINT UNSIGNED |
| user_id | BIGINT UNSIGNED |
| role_id | BIGINT UNSIGNED |
| created_at | DATETIME |
| updated_at | DATETIME |
| deleted | TINYINT |
| version | INT |

## nacos_config

## order_db

### after_sale

| Column | Type |
| --- | --- |
| id | BIGINT UNSIGNED |
| after_sale_no | VARCHAR(64) |
| main_order_id | BIGINT UNSIGNED |
| sub_order_id | BIGINT UNSIGNED |
| user_id | BIGINT UNSIGNED |
| merchant_id | BIGINT UNSIGNED |
| after_sale_type | VARCHAR(16) |
| status | VARCHAR(32) |
| reason | VARCHAR(255) |
| description | VARCHAR(1000) |
| apply_amount | DECIMAL(12, 2) |
| approved_amount | DECIMAL(12, 2) |
| return_logistics_company | VARCHAR(64) |
| return_logistics_no | VARCHAR(64) |
| refund_channel | VARCHAR(32) |
| refunded_at | DATETIME |
| closed_at | DATETIME |
| close_reason | VARCHAR(255) |
| created_at | DATETIME |
| updated_at | DATETIME |
| deleted | TINYINT |
| version | INT |

### after_sale_evidence

| Column | Type |
| --- | --- |
| id | BIGINT UNSIGNED |
| after_sale_id | BIGINT UNSIGNED |
| evidence_type | VARCHAR(32) |
| object_key | VARCHAR(255) |
| object_url | VARCHAR(500) |
| uploaded_by | BIGINT UNSIGNED |
| created_at | DATETIME |
| updated_at | DATETIME |
| deleted | TINYINT |
| version | INT |

### after_sale_item

| Column | Type |
| --- | --- |
| id | BIGINT UNSIGNED |
| after_sale_id | BIGINT UNSIGNED |
| order_item_id | BIGINT UNSIGNED |
| sku_id | BIGINT UNSIGNED |
| quantity | INT |
| apply_amount | DECIMAL(12, 2) |
| approved_amount | DECIMAL(12, 2) |
| created_at | DATETIME |
| updated_at | DATETIME |
| deleted | TINYINT |
| version | INT |

### after_sale_timeline

| Column | Type |
| --- | --- |
| id | BIGINT UNSIGNED |
| after_sale_id | BIGINT UNSIGNED |
| from_status | VARCHAR(32) |
| to_status | VARCHAR(32) |
| action | VARCHAR(64) |
| operator_id | BIGINT UNSIGNED |
| operator_role | VARCHAR(32) |
| remark | VARCHAR(500) |
| created_at | DATETIME |
| updated_at | DATETIME |
| deleted | TINYINT |
| version | INT |

### cart

| Column | Type |
| --- | --- |
| id | BIGINT UNSIGNED |
| cart_no | VARCHAR(64) |
| user_id | BIGINT UNSIGNED |
| cart_status | VARCHAR(16) |
| selected_count | INT |
| total_amount | DECIMAL(12, 2) |
| created_at | DATETIME |
| updated_at | DATETIME |
| deleted | TINYINT |
| version | INT |

### cart_item

| Column | Type |
| --- | --- |
| id | BIGINT UNSIGNED |
| cart_id | BIGINT UNSIGNED |
| user_id | BIGINT UNSIGNED |
| spu_id | BIGINT UNSIGNED |
| sku_id | BIGINT UNSIGNED |
| sku_name | VARCHAR(255) |
| quantity | INT |
| unit_price | DECIMAL(12, 2) |
| selected | TINYINT |
| checked_out | TINYINT |
| created_at | DATETIME |
| updated_at | DATETIME |
| deleted | TINYINT |
| version | INT |

### inbox_consume_log

| Column | Type |
| --- | --- |
| id | BIGINT UNSIGNED |
| event_id | VARCHAR(64) |
| consumer_group | VARCHAR(64) |
| event_type | VARCHAR(64) |
| consume_status | VARCHAR(16) |
| error_message | VARCHAR(1000) |
| consumed_at | DATETIME |
| created_at | DATETIME |
| updated_at | DATETIME |
| deleted | TINYINT |
| version | INT |

### order_item

| Column | Type |
| --- | --- |
| id | BIGINT UNSIGNED |
| main_order_id | BIGINT UNSIGNED |
| sub_order_id | BIGINT UNSIGNED |
| spu_id | BIGINT UNSIGNED |
| sku_id | BIGINT UNSIGNED |
| sku_code | VARCHAR(64) |
| sku_name | VARCHAR(255) |
| sku_snapshot | JSON |
| quantity | INT |
| unit_price | DECIMAL(12, 2) |
| total_price | DECIMAL(12, 2) |
| created_at | DATETIME |
| updated_at | DATETIME |
| deleted | TINYINT |
| version | INT |

### order_main

| Column | Type |
| --- | --- |
| id | BIGINT UNSIGNED |
| main_order_no | VARCHAR(64) |
| user_id | BIGINT UNSIGNED |
| order_status | VARCHAR(32) |
| total_amount | DECIMAL(12, 2) |
| payable_amount | DECIMAL(12, 2) |
| pay_channel | VARCHAR(32) |
| paid_at | DATETIME |
| cancelled_at | DATETIME |
| cancel_reason | VARCHAR(255) |
| remark | VARCHAR(255) |
| idempotency_key | VARCHAR(128) |
| created_at | DATETIME |
| updated_at | DATETIME |
| deleted | TINYINT |
| version | INT |

### order_sub

| Column | Type |
| --- | --- |
| id | BIGINT UNSIGNED |
| sub_order_no | VARCHAR(64) |
| main_order_id | BIGINT UNSIGNED |
| merchant_id | BIGINT UNSIGNED |
| order_status | VARCHAR(32) |
| shipping_status | VARCHAR(32) |
| after_sale_status | VARCHAR(32) |
| item_amount | DECIMAL(12, 2) |
| shipping_fee | DECIMAL(12, 2) |
| discount_amount | DECIMAL(12, 2) |
| payable_amount | DECIMAL(12, 2) |
| receiver_name | VARCHAR(64) |
| receiver_phone | VARCHAR(32) |
| receiver_address | VARCHAR(255) |
| shipping_company | VARCHAR(50) |
| tracking_number | VARCHAR(100) |
| shipped_at | DATETIME |
| estimated_arrival | DATE |
| received_at | DATETIME |
| done_at | DATETIME |
| closed_at | DATETIME |
| close_reason | VARCHAR(255) |
| created_at | DATETIME |
| updated_at | DATETIME |
| deleted | TINYINT |
| version | INT |

### outbox_event

| Column | Type |
| --- | --- |
| id | BIGINT UNSIGNED |
| event_id | VARCHAR(64) |
| aggregate_type | VARCHAR(64) |
| aggregate_id | VARCHAR(64) |
| event_type | VARCHAR(64) |
| payload | JSON |
| status | VARCHAR(16) |
| retry_count | INT |
| next_retry_at | DATETIME |
| created_at | DATETIME |
| updated_at | DATETIME |
| deleted | TINYINT |
| version | INT |

## payment_db

### inbox_consume_log

| Column | Type |
| --- | --- |
| id | BIGINT UNSIGNED |
| event_id | VARCHAR(64) |
| consumer_group | VARCHAR(64) |
| event_type | VARCHAR(64) |
| consume_status | VARCHAR(16) |
| error_message | VARCHAR(1000) |
| consumed_at | DATETIME |
| created_at | DATETIME |
| updated_at | DATETIME |
| deleted | TINYINT |
| version | INT |

### outbox_event

| Column | Type |
| --- | --- |
| id | BIGINT UNSIGNED |
| event_id | VARCHAR(64) |
| aggregate_type | VARCHAR(64) |
| aggregate_id | VARCHAR(64) |
| event_type | VARCHAR(64) |
| payload | JSON |
| status | VARCHAR(16) |
| retry_count | INT |
| next_retry_at | DATETIME |
| created_at | DATETIME |
| updated_at | DATETIME |
| deleted | TINYINT |
| version | INT |

### payment_callback_log

| Column | Type |
| --- | --- |
| id | BIGINT UNSIGNED |
| payment_no | VARCHAR(64) |
| callback_no | VARCHAR(64) |
| callback_status | VARCHAR(32) |
| provider_txn_no | VARCHAR(128) |
| payload | TEXT |
| idempotency_key | VARCHAR(128) |
| created_at | DATETIME |
| updated_at | DATETIME |
| deleted | TINYINT |
| version | INT |

### payment_order

| Column | Type |
| --- | --- |
| id | BIGINT UNSIGNED |
| payment_no | VARCHAR(64) |
| main_order_no | VARCHAR(64) |
| sub_order_no | VARCHAR(64) |
| user_id | BIGINT UNSIGNED |
| amount | DECIMAL(12, 2) |
| channel | VARCHAR(32) |
| status | VARCHAR(32) |
| provider_txn_no | VARCHAR(128) |
| idempotency_key | VARCHAR(128) |
| paid_at | DATETIME |
| poll_count | INT |
| next_poll_at | DATETIME |
| last_polled_at | DATETIME |
| last_poll_error | VARCHAR(255) |
| created_at | DATETIME |
| updated_at | DATETIME |
| deleted | TINYINT |
| version | INT |

### payment_refund

| Column | Type |
| --- | --- |
| id | BIGINT UNSIGNED |
| refund_no | VARCHAR(64) |
| payment_no | VARCHAR(64) |
| after_sale_no | VARCHAR(64) |
| refund_amount | DECIMAL(12, 2) |
| status | VARCHAR(32) |
| reason | VARCHAR(255) |
| idempotency_key | VARCHAR(128) |
| refunded_at | DATETIME |
| retry_count | INT |
| next_retry_at | DATETIME |
| last_retry_at | DATETIME |
| last_error | VARCHAR(255) |
| created_at | DATETIME |
| updated_at | DATETIME |
| deleted | TINYINT |
| version | INT |

## product_db

### category

| Column | Type |
| --- | --- |
| id | BIGINT UNSIGNED |
| parent_id | BIGINT UNSIGNED |
| name | VARCHAR(100) |
| level | TINYINT |
| path | VARCHAR(255) |
| sort_order | INT |
| status | TINYINT |
| created_at | DATETIME |
| updated_at | DATETIME |
| deleted | TINYINT |
| version | INT |

### inbox_consume_log

| Column | Type |
| --- | --- |
| id | BIGINT UNSIGNED |
| event_id | VARCHAR(64) |
| consumer_group | VARCHAR(64) |
| event_type | VARCHAR(64) |
| consume_status | VARCHAR(16) |
| error_message | VARCHAR(1000) |
| consumed_at | DATETIME |
| created_at | DATETIME |
| updated_at | DATETIME |
| deleted | TINYINT |
| version | INT |

### outbox_event

| Column | Type |
| --- | --- |
| id | BIGINT UNSIGNED |
| event_id | VARCHAR(64) |
| aggregate_type | VARCHAR(64) |
| aggregate_id | VARCHAR(64) |
| event_type | VARCHAR(64) |
| payload | JSON |
| status | VARCHAR(16) |
| retry_count | INT |
| next_retry_at | DATETIME |
| created_at | DATETIME |
| updated_at | DATETIME |
| deleted | TINYINT |
| version | INT |

### product_review

| Column | Type |
| --- | --- |
| id | BIGINT UNSIGNED |
| spu_id | BIGINT UNSIGNED |
| sku_id | BIGINT UNSIGNED |
| order_sub_no | VARCHAR(64) |
| user_id | BIGINT UNSIGNED |
| rating | TINYINT |
| content | TEXT |
| images | JSON |
| tags | VARCHAR(500) |
| is_anonymous | TINYINT |
| audit_status | VARCHAR(32) |
| merchant_reply | TEXT |
| reply_time | DATETIME |
| like_count | INT |
| is_visible | TINYINT |
| created_at | DATETIME |
| updated_at | DATETIME |
| deleted | TINYINT |
| version | INT |

### sku

| Column | Type |
| --- | --- |
| id | BIGINT UNSIGNED |
| spu_id | BIGINT UNSIGNED |
| sku_code | VARCHAR(100) |
| sku_name | VARCHAR(200) |
| spec_json | JSON |
| sale_price | DECIMAL(12, 2) |
| market_price | DECIMAL(12, 2) |
| cost_price | DECIMAL(12, 2) |
| status | TINYINT |
| image_url | VARCHAR(500) |
| created_at | DATETIME |
| updated_at | DATETIME |
| deleted | TINYINT |
| version | INT |

### spu

| Column | Type |
| --- | --- |
| id | BIGINT UNSIGNED |
| spu_name | VARCHAR(200) |
| subtitle | VARCHAR(255) |
| category_id | BIGINT UNSIGNED |
| brand_id | BIGINT UNSIGNED |
| merchant_id | BIGINT UNSIGNED |
| status | TINYINT |
| description | TEXT |
| main_image | VARCHAR(500) |
| created_at | DATETIME |
| updated_at | DATETIME |
| deleted | TINYINT |
| version | INT |

## skywalking

## stock_db

### inbox_consume_log

| Column | Type |
| --- | --- |
| id | BIGINT UNSIGNED |
| event_id | VARCHAR(64) |
| consumer_group | VARCHAR(64) |
| event_type | VARCHAR(64) |
| consume_status | VARCHAR(16) |
| error_message | VARCHAR(1000) |
| consumed_at | DATETIME |
| created_at | DATETIME |
| updated_at | DATETIME |
| deleted | TINYINT |
| version | INT |

### outbox_event

| Column | Type |
| --- | --- |
| id | BIGINT UNSIGNED |
| event_id | VARCHAR(64) |
| aggregate_type | VARCHAR(64) |
| aggregate_id | VARCHAR(64) |
| event_type | VARCHAR(64) |
| payload | JSON |
| status | VARCHAR(16) |
| retry_count | INT |
| next_retry_at | DATETIME |
| created_at | DATETIME |
| updated_at | DATETIME |
| deleted | TINYINT |
| version | INT |

### stock_segment

| Column | Type |
| --- | --- |
| id | BIGINT UNSIGNED |
| sku_id | BIGINT UNSIGNED |
| segment_id | INT |
| available_qty | INT |
| locked_qty | INT |
| sold_qty | INT |
| alert_threshold | INT |
| status | TINYINT |
| created_at | DATETIME |
| updated_at | DATETIME |
| deleted | TINYINT |
| version | INT |

### stock_reservation

| Column | Type |
| --- | --- |
| id | BIGINT UNSIGNED |
| main_order_no | VARCHAR(64) |
| sub_order_no | VARCHAR(64) |
| sku_id | BIGINT UNSIGNED |
| segment_id | INT |
| quantity | INT |
| status | VARCHAR(32) |
| idempotency_key | VARCHAR(128) |
| created_at | DATETIME |
| updated_at | DATETIME |
| deleted | TINYINT |
| version | INT |

### stock_txn

| Column | Type |
| --- | --- |
| id | BIGINT UNSIGNED |
| sku_id | BIGINT UNSIGNED |
| segment_id | INT |
| sub_order_no | VARCHAR(64) |
| txn_type | VARCHAR(32) |
| quantity | INT |
| before_available | INT |
| after_available | INT |
| before_locked | INT |
| after_locked | INT |
| before_sold | INT |
| after_sold | INT |
| remark | VARCHAR(1000) |
| created_at | DATETIME |
| updated_at | DATETIME |
| deleted | TINYINT |
| version | INT |

## user_db

### admin

| Column | Type |
| --- | --- |
| id | BIGINT UNSIGNED |
| username | VARCHAR(50) |
| real_name | VARCHAR(50) |
| phone | VARCHAR(20) |
| role | VARCHAR(20) |
| status | TINYINT |
| created_at | DATETIME |
| updated_at | DATETIME |
| deleted | TINYINT |
| version | INT |

### inbox_consume_log

| Column | Type |
| --- | --- |
| id | BIGINT UNSIGNED |
| event_id | VARCHAR(64) |
| consumer_group | VARCHAR(64) |
| event_type | VARCHAR(64) |
| consume_status | VARCHAR(16) |
| error_message | VARCHAR(1000) |
| consumed_at | DATETIME |
| created_at | DATETIME |
| updated_at | DATETIME |
| deleted | TINYINT |
| version | INT |

### merchant

| Column | Type |
| --- | --- |
| id | BIGINT UNSIGNED |
| username | VARCHAR(50) |
| merchant_name | VARCHAR(100) |
| phone | VARCHAR(20) |
| status | TINYINT |
| audit_status | TINYINT |
| created_at | DATETIME |
| updated_at | DATETIME |
| deleted | TINYINT |
| version | INT |

### merchant_auth

| Column | Type |
| --- | --- |
| id | BIGINT UNSIGNED |
| merchant_id | BIGINT UNSIGNED |
| business_license_number | VARCHAR(50) |
| business_license_url | VARCHAR(255) |
| id_card_front_url | VARCHAR(255) |
| id_card_back_url | VARCHAR(255) |
| contact_phone | VARCHAR(20) |
| contact_address | VARCHAR(255) |
| auth_status | TINYINT |
| auth_remark | VARCHAR(255) |
| created_at | DATETIME |
| updated_at | DATETIME |
| deleted | TINYINT |
| version | INT |

### operation_audit_log

| Column | Type |
| --- | --- |
| id | BIGINT UNSIGNED |
| operator_id | BIGINT UNSIGNED |
| operator_role | VARCHAR(32) |
| action | VARCHAR(128) |
| target_type | VARCHAR(64) |
| target_id | VARCHAR(64) |
| trace_id | VARCHAR(64) |
| request_uri | VARCHAR(255) |
| request_method | VARCHAR(16) |
| request_payload | TEXT |
| result_code | INT |
| created_at | DATETIME |
| updated_at | DATETIME |
| deleted | TINYINT |
| version | INT |

### outbox_event

| Column | Type |
| --- | --- |
| id | BIGINT UNSIGNED |
| event_id | VARCHAR(64) |
| aggregate_type | VARCHAR(64) |
| aggregate_id | VARCHAR(64) |
| event_type | VARCHAR(64) |
| payload | JSON |
| status | VARCHAR(16) |
| retry_count | INT |
| next_retry_at | DATETIME |
| created_at | DATETIME |
| updated_at | DATETIME |
| deleted | TINYINT |
| version | INT |

### test_access_token

| Column | Type |
| --- | --- |
| id | BIGINT UNSIGNED |
| token_value | VARCHAR(255) |
| token_owner | VARCHAR(64) |
| expires_at | DATETIME |
| is_active | TINYINT |
| created_at | DATETIME |
| updated_at | DATETIME |

### user_address

| Column | Type |
| --- | --- |
| id | BIGINT UNSIGNED |
| user_id | BIGINT UNSIGNED |
| address_tag | VARCHAR(32) |
| receiver_name | VARCHAR(50) |
| receiver_phone | VARCHAR(20) |
| country | VARCHAR(64) |
| province | VARCHAR(64) |
| city | VARCHAR(64) |
| district | VARCHAR(64) |
| street | VARCHAR(100) |
| detail_address | VARCHAR(255) |
| postal_code | VARCHAR(16) |
| longitude | DECIMAL(11, 7) |
| latitude | DECIMAL(11, 7) |
| is_default | TINYINT |
| created_at | DATETIME |
| updated_at | DATETIME |
| deleted | TINYINT |
| version | INT |

### user_favorite

| Column | Type |
| --- | --- |
| id | BIGINT UNSIGNED |
| user_id | BIGINT UNSIGNED |
| spu_id | BIGINT UNSIGNED |
| sku_id | BIGINT UNSIGNED |
| favorite_status | VARCHAR(16) |
| created_at | DATETIME |
| updated_at | DATETIME |
| deleted | TINYINT |
| version | INT |

### user_profile_ext

| Column | Type |
| --- | --- |
| id | BIGINT UNSIGNED |
| user_id | BIGINT UNSIGNED |
| gender | VARCHAR(16) |
| birthday | DATE |
| bio | VARCHAR(500) |
| country | VARCHAR(64) |
| province | VARCHAR(64) |
| city | VARCHAR(64) |
| personal_tags | JSON |
| preferences | JSON |
| last_login_at | DATETIME |
| created_at | DATETIME |
| updated_at | DATETIME |
| deleted | TINYINT |
| version | INT |

### users

| Column | Type |
| --- | --- |
| id | BIGINT UNSIGNED |
| username | VARCHAR(50) |
| phone | VARCHAR(20) |
| nickname | VARCHAR(50) |
| avatar_url | VARCHAR(255) |
| email | VARCHAR(100) |
| status | TINYINT |
| created_at | DATETIME |
| updated_at | DATETIME |
| deleted | TINYINT |
| version | INT |

## xxl_job

### xxl_job_group

| Column | Type |
| --- | --- |
| id | INT |
| app_name | VARCHAR(64) |
| title | VARCHAR(64) |
| address_type | TINYINT |
| address_list | TEXT |
| update_time | DATETIME |

### xxl_job_info

| Column | Type |
| --- | --- |
| id | INT |
| job_group | INT |
| job_desc | VARCHAR(255) |
| add_time | DATETIME |
| update_time | DATETIME |
| author | VARCHAR(64) |
| schedule_type | VARCHAR(50) |
| schedule_conf | VARCHAR(128) |
| misfire_strategy | VARCHAR(50) |
| executor_route_strategy | VARCHAR(50) |
| executor_handler | VARCHAR(255) |
| executor_param | VARCHAR(512) |
| executor_block_strategy | VARCHAR(50) |
| executor_timeout | INT |
| executor_fail_retry_count | INT |
| glue_type | VARCHAR(50) |
| glue_source | MEDIUMTEXT |
| glue_remark | VARCHAR(128) |
| glue_updatetime | DATETIME |
| child_jobid | VARCHAR(255) |
| trigger_status | TINYINT |
| trigger_last_time | BIGINT |
| trigger_next_time | BIGINT |

### xxl_job_log

| Column | Type |
| --- | --- |
| id | BIGINT |
| job_group | INT |
| job_id | INT |
| executor_address | VARCHAR(255) |
| executor_handler | VARCHAR(255) |
| executor_param | VARCHAR(512) |
| executor_sharding_param | VARCHAR(20) |
| executor_fail_retry_count | INT |
| trigger_time | DATETIME |
| trigger_code | INT |
| trigger_msg | TEXT |
| handle_time | DATETIME |
| handle_code | INT |
| handle_msg | TEXT |
| alarm_status | TINYINT |

### xxl_job_log_report

| Column | Type |
| --- | --- |
| id | INT |
| trigger_day | DATETIME |
| running_count | INT |
| suc_count | INT |
| fail_count | INT |

### xxl_job_registry

| Column | Type |
| --- | --- |
| id | INT |
| registry_group | VARCHAR(50) |
| registry_key | VARCHAR(255) |
| registry_value | VARCHAR(255) |
| update_time | DATETIME |

### xxl_job_user

| Column | Type |
| --- | --- |
| id | INT |
| username | VARCHAR(50) |
| password | VARCHAR(255) |
| role | TINYINT |
| permission | VARCHAR(255) |
