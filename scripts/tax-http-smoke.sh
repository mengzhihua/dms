#!/usr/bin/env bash
# 税控 HTTP 网关 + 模拟税控平台联调冒烟。
# 启动方式（在 backend/ 下）：
#   mvn -q spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=tax-sim \
#     --dms.tax.provider=HTTP --dms.tax.endpoint=http://127.0.0.1:8080/sim/tax \
#     --dms.tax.app-id=dms-app --dms.tax.app-secret=dev-tax-secret-padded-to-32-bytes"
set -euo pipefail
BASE="${1:-http://localhost:8080}/api"
J='Content-Type: application/json'
need(){ command -v "$1" >/dev/null || { echo "missing $1"; exit 1; }; }
need curl; need jq
TOKEN=$(curl -s -X POST "$BASE/auth/login" -H "$J" -d '{"username":"admin","password":"123456"}' | jq -r '.data.token')
[ "$TOKEN" != "null" ] && [ -n "$TOKEN" ] || { echo "admin login failed"; exit 1; }
call(){ local out; out=$(curl -s -X "$1" "$BASE$2" -H "$J" -H "Authorization: Bearer $TOKEN" ${3:+-d "$3"}); [ "$(echo "$out"|jq -r .code)" = "0" ] || { echo "FAIL $1 $2 -> $out"; exit 1; }; echo "$out"|jq -c .data; }

echo "== 1 issue (immediate success)"
INV=$(call POST /invoice '{"dealerCode":"D001","invoiceType":"ELECTRONIC","buyerName":"联调买家","amount":113,"remark":""}')
ID1=$(echo "$INV"|jq -r .id)
R=$(call POST "/invoice/$ID1/issue"); test "$(echo "$R"|jq -r .status)" = "ISSUED"; test "$(echo "$R"|jq -r .taxInvoiceNumber)" != "null"

echo "== 2 issue pending -> sync"
INV=$(call POST /invoice '{"dealerCode":"D001","invoiceType":"ELECTRONIC","buyerName":"联调买家","amount":226,"remark":"PENDING"}')
ID2=$(echo "$INV"|jq -r .id)
R=$(call POST "/invoice/$ID2/issue"); test "$(echo "$R"|jq -r .status)" = "ISSUING"; test "$(echo "$R"|jq -r .providerRef)" != "null"
R=$(call POST "/invoice/$ID2/sync"); test "$(echo "$R"|jq -r .status)" = "ISSUED"

echo "== 3 issue fail"
INV=$(call POST /invoice '{"dealerCode":"D001","invoiceType":"ELECTRONIC","buyerName":"联调买家","amount":50,"remark":"FAIL"}')
ID3=$(echo "$INV"|jq -r .id)
R=$(call POST "/invoice/$ID3/issue"); test "$(echo "$R"|jq -r .status)" = "FAILED"; test "$(echo "$R"|jq -r .errorMsg)" = "模拟税控拒绝"

echo "== 4 red-flush"
R=$(call POST "/invoice/$ID1/red-flush"); test "$(echo "$R"|jq -r .status)" = "ISSUED"
ORIG=$(call GET "/invoice/$ID1"); test "$(echo "$ORIG"|jq -r .status)" = "RED_FLUSHED"

echo "TAX HTTP SMOKE OK"
