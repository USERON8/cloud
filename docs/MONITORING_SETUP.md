# Prometheus + Grafana 鎺ュ叆璇存槑

## 1. 鍚姩鐩戞帶鏍?
```bash
docker compose -f docker/monitoring-compose.yml up -d prometheus grafana
```

璁块棶鍦板潃锛?
- Prometheus: `http://localhost:9099`
- Grafana: `http://localhost:3000`锛堥粯璁?`admin/admin`锛?
## 2. 鎸囨爣鎶撳彇鐩爣

Prometheus 宸查厤缃姄鍙栦互涓嬫湇鍔＄殑 `/actuator/prometheus`锛?
- `host.docker.internal:18080`锛坓ateway锛?- `host.docker.internal:8081`锛坅uth-service锛?- `host.docker.internal:8082`锛坲ser-service锛?- `host.docker.internal:8083`锛坥rder-service锛?- `host.docker.internal:8084`锛坧roduct-service锛?- `host.docker.internal:8085`锛坰tock-service锛?- `host.docker.internal:8086`锛坧ayment-service锛?- `host.docker.internal:8087`锛坰earch-service锛?
## 3. Grafana 鑷姩瀵煎叆

宸查€氳繃 provisioning 鑷姩瀵煎叆锛?
- 鏁版嵁婧愶細`Prometheus`锛坲id=`prometheus`锛?- 鐪嬫澘锛歚Cloud Trade Chain`
- 鐪嬫澘锛歚Cloud Service Overview`
- 鐪嬫澘锛歚Cloud Acceptance Load`

閰嶇疆鏂囦欢浣嶇疆锛?
- `docker/monitor/grafana/provisioning/datasources/prometheus.yml`
- `docker/monitor/grafana/provisioning/dashboards/dashboards.yml`
- `docker/monitor/grafana/provisioning/dashboards/trade-chain.json`
- `docker/monitor/grafana/provisioning/dashboards/cloud-overview.json`
- `docker/monitor/grafana/provisioning/dashboards/acceptance-load.json`

## 4. 鏍稿績涓氬姟鎸囨爣

- `trade_order_total{service,result}`
- `trade_payment_total{service,result}`
- `trade_stock_freeze_total{service,result}`
- `trade_refund_total{service,result}`
- `trade_message_consume_total{service,eventType,result}`

## 5. 楠屾敹鍘嬫祴鎸囨爣锛坘6 -> Prometheus锛?
`docker/monitoring-compose.yml` 宸插紑鍚細

- Prometheus `remote-write receiver`锛坄--web.enable-remote-write-receiver`锛?- `k6` 鍘嬫祴瀹瑰櫒锛坧rofile: `loadtest`锛?
鍘嬫祴鑴氭湰浣嶇疆锛?
- `tests/perf/k6/acceptance-cases.js`
- `tests/perf/k6/run-acceptance.ps1`
- `tests/perf/k6/run-acceptance.sh`

榛樿浼氭寜 8 涓獙鏀跺満鏅€愪釜鎵ц锛屽苟鍐欏叆 Prometheus锛屾牳蹇冩寚鏍囷細

- `k6_acceptance_case_total{case_id,case_name,result}`
- `k6_acceptance_case_failed_total{case_id,case_name}`
- `k6_acceptance_case_skipped_total{case_id,case_name}`
- `k6_acceptance_case_duration_ms_*`
- `k6_http_reqs_total{scenario}`
- `k6_checks_rate{scenario}`

鍚姩鍛戒护绀轰緥锛圥owerShell锛夛細

```powershell
docker compose -f docker/monitoring-compose.yml up -d prometheus grafana
$env:K6_BASE_URL = "http://host.docker.internal:18080"
.\tests\perf\k6\run-acceptance.ps1
```

甯哥敤鐜鍙橀噺锛堝帇娴嬫椂娉ㄥ叆锛夛細

- `AUTH_TOKEN` 鎴?`AUTH_USERNAME` + `AUTH_PASSWORD`
- `USER_ID`銆乣SHOP_ID`銆乣PRODUCT_ID`
- `ORDER_ID`銆乣ORDER_NO`銆乣PAYMENT_ID`
- `CASE_VUS`銆乣CASE_DURATION`銆乣CASE_STAGE_SECONDS`

## 6. 楠岃瘉姝ラ

1. 鎵撳紑 `http://localhost:9099/targets`锛岀‘璁?`spring-boot` 浠诲姟鐩爣涓?`UP`銆?2. 璁块棶浠讳竴鏈嶅姟 `http://localhost:{port}/actuator/prometheus`锛岀‘璁よ繑鍥炴枃鏈寚鏍囥€?3. 鎵撳紑 Grafana 鐪嬫澘锛屾鏌ヤ氦鏄撴垚鍔熺巼鍜屾秷鎭噸璇曟洸绾挎湁鏁版嵁銆?4. 鎵ц `k6` 鍚庯紝鎵撳紑 `Cloud Acceptance Load` 鐪嬫澘锛岀‘璁?8 鍦烘櫙鍚炲悙/鎴愬姛鐜?澶辫触涓庤烦杩囩粺璁℃湁鏁版嵁銆?

