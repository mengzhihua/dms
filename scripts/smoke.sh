#!/usr/bin/env bash
set -euo pipefail
BASE="${1:-http://localhost:8080}/api"
J='Content-Type: application/json'
need(){ command -v "$1" >/dev/null || { echo "missing $1"; exit 1; }; }
need curl; need jq
login(){ curl -s -X POST "$BASE/auth/login" -H "$J" -d "{\"username\":\"$1\",\"password\":\"$2\"}" | jq -r '.data.token'; }
TOKEN=$(login admin 123456); [ "$TOKEN" != "null" ] && [ -n "$TOKEN" ] || { echo "admin login failed"; exit 1; }
call(){ local out; out=$(curl -sf -X "$1" "$BASE$2" -H "$J" -H "Authorization: Bearer $TOKEN" ${3:+-d "$3"}); [ "$(echo "$out"|jq -r .code)" = "0" ] || { echo "FAIL $1 $2 -> $out"; exit 1; }; echo "$out"|jq -c .data; }
fail(){ local out; out=$(curl -s -X "$1" "$BASE$2" -H "$J" -H "Authorization: Bearer $TOKEN" ${3:+-d "$3"}); [ "$(echo "$out"|jq -r .code)" != "0" ] || { echo "EXPECTED-FAIL but ok $1 $2 -> $out"; exit 1; }; echo "$out"|jq -c '{code,msg}'; }
callt(){ local out; out=$(curl -sf -X "$2" "$BASE$3" -H "$J" -H "Authorization: Bearer $1" ${4:+-d "$4"}); [ "$(echo "$out"|jq -r .code)" = "0" ] || { echo "FAIL(as $1) $2 $3 -> $out"; exit 1; }; echo "$out"|jq -c .data; }
raw(){ curl -s -X "$2" "$BASE$3" -H "$J" ${1:+-H "Authorization: Bearer $1"} ${4:+-d "$4"}; }

DC=D001
echo "== 0 auth negative checks"
test "$(raw "" GET /dashboard | jq -r .code)" = "401"
test "$(curl -s -o /dev/null -w '%{http_code}' "$BASE/dashboard")" = "401"
TOK_SA=$(login d001sa 123456); TOK_D002=$(login d002mgr 123456); TOK_TECH=$(login d001tech 123456)
test "$(callt "$TOK_SA" GET '/network/dealer/page?size=50' | jq -c '[.records[].code]')" = '["D001"]'
test "$(raw "$TOK_TECH" POST /customer/customer '{"name":"x","phone":"1"}' | jq -r .code)" = "403"
echo "== 1 create customer + vehicle"
C=$(call POST /customer/customer "{\"name\":\"冒烟测试客户\",\"phone\":\"13900000000\",\"dealerCode\":\"$DC\"}")
CID=$(echo "$C"|jq -r .id)
V=$(call POST /customer/vehicle "{\"vin\":\"LSMOKE$(date +%s)\",\"plateNo\":\"沪T99999\",\"modelCode\":\"M001\",\"customerId\":$CID,\"dealerCode\":\"$DC\",\"mileage\":12000,\"purchaseDate\":\"2024-01-01\",\"warrantyStart\":\"2024-01-01\",\"warrantyEnd\":\"2027-01-01\"}")
VID=$(echo "$V"|jq -r .id); VIN=$(echo "$V"|jq -r .vin)
WC=$(call GET "/customer/vehicle/$VID/warranty-check?mileage=12000"); test "$(echo "$WC"|jq -r .inWarranty)" = "true"

echo "== 2 appointment -> work order"
A=$(call POST /workshop/appointment "{\"dealerCode\":\"$DC\",\"customerId\":$CID,\"vehicleId\":$VID,\"appointmentTime\":\"2025-01-01 10:00:00\",\"serviceType\":\"维修\",\"status\":\"BOOKED\"}")
AID=$(echo "$A"|jq -r .id)
WO=$(call POST /workshop/order "{\"appointmentId\":$AID,\"mileageIn\":12000,\"fuelLevel\":\"1/2\",\"orderType\":\"REGULAR\",\"complaint\":\"刹车异响\",\"advisorName\":\"服务顾问小王\"}")
OID=$(echo "$WO"|jq -r .id); ONO=$(echo "$WO"|jq -r .orderNo)
test "$(echo "$WO"|jq -r .status)" = "CHECKED_IN"; [[ "$ONO" == WO* ]]
# 跨店访问工单应被拒
test "$(raw "$TOK_D002" GET "/workshop/order/$OID" | jq -rc '[.code, (.msg|test("无权访问"))]')" = '[400,true]'

echo "== 3 guide recommend + apply"
R=$(call POST /guide/recommend "{\"modelCode\":\"M001\",\"dtcCodes\":[],\"symptom\":\"刹车异响\",\"dealerCode\":\"$DC\"}")
test "$(echo "$R"|jq length)" -gt 0
GC=$(echo "$R"|jq -r '.[0].guide.code')
# 补齐该指导涉及的备件库存（入库 50 件）
for PN in $(echo "$R"|jq -r '.[0].parts[].partNo'); do
  call POST /parts/stock/inbound "{\"dealerCode\":\"$DC\",\"partNo\":\"$PN\",\"location\":\"A-99-01\",\"batchNo\":\"B-SMOKE\",\"qty\":50}" >/dev/null
done
call POST "/workshop/order/$OID/apply-guide/$GC" >/dev/null

echo "== 4 diagnose -> quote"
call POST "/workshop/order/$OID/diagnose" '{"diagnosis":"前刹车片磨损"}' >/dev/null
WO=$(call POST "/workshop/order/$OID/quote" '{}')
test "$(echo "$WO"|jq -r .status)" = "QUOTED"
test "$(echo "$WO"|jq -r .totalAmount)" != "0"

echo "== 5 approve (parts reserved)"
fail POST "/workshop/order/$OID/start" '{}' | grep -q 非法 || { echo "illegal transition not blocked"; exit 1; }
WO=$(call POST "/workshop/order/$OID/approve" '{}')
ST=$(call GET "/parts/stock/page?dealerCode=$DC&size=200")
test "$(echo "$ST"|jq '[.records[]|select(.reservedQty>0)]|length')" -gt 0

echo "== 6 dispatch -> start -> finish -> qc pass"
call POST "/workshop/order/$OID/dispatch" '{"technicianCode":"T002","bayCode":"B01"}' >/dev/null
call POST "/workshop/order/$OID/start" '{}' >/dev/null
call POST "/workshop/order/$OID/finish" '{}' >/dev/null
WO=$(call POST "/workshop/order/$OID/qc" '{"pass":true,"remark":"OK"}')
test "$(echo "$WO"|jq -r .status)" = "QC_PASSED"

echo "== 7 settle + auto invoice"
WO=$(call POST "/workshop/order/$OID/settle" '{"discountAmount":0,"needInvoice":true,"invoiceType":"ELECTRONIC","buyerName":"冒烟测试客户"}')
test "$(echo "$WO"|jq -r .status)" = "SETTLED"
IID=$(echo "$WO"|jq -r .invoiceId); test "$IID" != "null"
INV=$(call POST "/invoice/$IID/issue")
test "$(echo "$INV"|jq -r .status)" = "ISSUED"
test "$(echo "$INV"|jq -r '.taxInvoiceNumber|length')" = "8"
call GET "/invoice/$IID/preview" >/dev/null
INV2=$(call POST "/invoice/$IID/issue"); test "$(echo "$INV2"|jq -r .taxInvoiceNumber)" = "$(echo "$INV"|jq -r .taxInvoiceNumber)"

echo "== 8 deliver -> survey created"
WO=$(call POST "/workshop/order/$OID/deliver" '{}')
SID=$(echo "$WO"|jq -r .surveyId); test "$SID" != "null"
V2=$(call GET "/customer/vehicle/$VID"); test "$(echo "$V2"|jq -r .lastServiceDate)" != "null"

echo "== 9 answer survey low score -> complaint"
Q=$(call GET "/survey/question/page?size=50")
QIDS=$(echo "$Q"|jq -r '[.records[].id]|join(" ")')
ANS="["; i=0
for qid in $QIDS; do [ $i -gt 0 ] && ANS="$ANS,"; ANS="$ANS{\"questionId\":$qid,\"score\":2}"; i=$((i+1)); done
ANS="$ANS]"
SV=$(call POST "/survey/$SID/answer" "{\"answers\":$ANS}")
test "$(echo "$SV"|jq -r .status)" = "ANSWERED"
CMP=$(call GET "/survey/complaint/page?size=50")
test "$(echo "$CMP"|jq "[.records[]|select(.surveyId==$SID)]|length")" -ge 1
CPC=$(echo "$CMP"|jq -r ".records[]|select(.surveyId==$SID)|.id"|head -1)
call POST "/survey/complaint/$CPC/handle" '{"status":"PROCESSING","handler":"客服小李","resolution":"回电安抚"}' >/dev/null
STATS=$(call GET "/survey/stats?dealerCode=$DC"); test "$(echo "$STATS"|jq -r .count)" -ge 1

echo "== 10 warranty order -> claim"
VW=$(call POST /customer/vehicle "{\"vin\":\"LSMOKW$(date +%s)\",\"plateNo\":\"沪T88888\",\"modelCode\":\"M001\",\"customerId\":$CID,\"dealerCode\":\"$DC\",\"mileage\":5000,\"purchaseDate\":\"2024-06-01\",\"warrantyStart\":\"2024-06-01\",\"warrantyEnd\":\"2027-06-01\"}")
VWID=$(echo "$VW"|jq -r .id)
WO2=$(call POST /workshop/order "{\"vehicleId\":$VWID,\"mileageIn\":5000,\"orderType\":\"WARRANTY\",\"complaint\":\"保修索赔测试\",\"serviceType\":\"维修\"}")
OID2=$(echo "$WO2"|jq -r .id)
call POST "/workshop/order/$OID2/apply-guide/G001" >/dev/null
call POST "/workshop/order/$OID2/diagnose" '{"diagnosis":"缺火"}' >/dev/null
call POST "/workshop/order/$OID2/quote" '{}' >/dev/null
call POST "/workshop/order/$OID2/approve" '{}' >/dev/null
call POST "/workshop/order/$OID2/dispatch" '{"technicianCode":"T001","bayCode":"B02"}' >/dev/null
call POST "/workshop/order/$OID2/start" '{}' >/dev/null
call POST "/workshop/order/$OID2/finish" '{}' >/dev/null
call POST "/workshop/order/$OID2/qc" '{"pass":true}' >/dev/null
WO2=$(call POST "/workshop/order/$OID2/settle" '{}')
test "$(echo "$WO2"|jq -r .warrantyAmount)" != "0"
CL=$(call GET "/workshop/claim/page?size=50")
CLID=$(echo "$CL"|jq -r ".records[]|select(.orderId==$OID2)|.id")
test -n "$CLID"
call POST "/workshop/claim/$CLID/approve" >/dev/null
call POST "/workshop/claim/$CLID/pay" >/dev/null

echo "== 10b qc fail rework does not double-consume stock"
WO3=$(call POST /workshop/order "{\"vehicleId\":$VID,\"mileageIn\":13000,\"orderType\":\"REGULAR\",\"complaint\":\"刹车异响返工测试\",\"serviceType\":\"维修\"}")
OID3=$(echo "$WO3"|jq -r .id); ONO3=$(echo "$WO3"|jq -r .orderNo)
call POST "/workshop/order/$OID3/apply-guide/G002" >/dev/null
call POST "/workshop/order/$OID3/diagnose" '{"diagnosis":"刹车片"}' >/dev/null
call POST "/workshop/order/$OID3/quote" '{}' >/dev/null
call POST "/workshop/order/$OID3/approve" '{}' >/dev/null
call POST "/workshop/order/$OID3/dispatch" '{"technicianCode":"T001","bayCode":"B01"}' >/dev/null
call POST "/workshop/order/$OID3/start" '{}' >/dev/null
call POST "/workshop/order/$OID3/finish" '{}' >/dev/null
OUT1=$(call GET "/parts/movement/page?size=500" | jq "[.records[]|select(.refNo==\"$ONO3\" and .type==\"OUT\")]|length")
test "$OUT1" -gt 0
WO3=$(call POST "/workshop/order/$OID3/qc" '{"pass":false,"remark":"返工"}')
test "$(echo "$WO3"|jq -r .status)" = "IN_REPAIR"
call POST "/workshop/order/$OID3/finish" '{}' >/dev/null
OUT2=$(call GET "/parts/movement/page?size=500" | jq "[.records[]|select(.refNo==\"$ONO3\" and .type==\"OUT\")]|length")
test "$OUT1" = "$OUT2"   # 返工后不得重复出库
call POST "/workshop/order/$OID3/qc" '{"pass":true}' >/dev/null
call POST "/workshop/order/$OID3/settle" '{}' >/dev/null

echo "== 11 red-flush invoice"
RED=$(call POST "/invoice/$IID/red-flush")
test "$(echo "$RED"|jq -r .status)" = "ISSUED"
test "$(echo "$RED"|jq -r '(.amount|tonumber) < 0')" = "true"
test "$(call GET "/invoice/$IID"|jq -r .status)" = "RED_FLUSHED"

echo "== 12 target achievement + assessment"
call GET "/network/target/achievement?dealerCode=$DC&yearMonth=$(date +%Y-%m)" >/dev/null
AS=$(call POST "/network/assessment/generate?dealerCode=$DC&yearMonth=$(date +%Y-%m)")
test "$(echo "$AS"|jq -r .grade)" != "null"

echo "== 13 sales order flow"
SO=$(call POST /network/sales-order "{\"dealerCode\":\"$DC\",\"customerId\":$CID,\"modelCode\":\"M001\",\"color\":\"珍珠白\",\"price\":150000,\"deposit\":5000}")
SOID=$(echo "$SO"|jq -r .id)
# 冒烟可重复：先补一台在库整车（库存可能已被历史运行售罄）
call POST /network/vehicle-stock "{\"dealerCode\":\"$DC\",\"vin\":\"VSMOKE$(date +%s)\",\"modelCode\":\"M001\",\"color\":\"珍珠白\",\"status\":\"IN_STOCK\"}" >/dev/null
SO=$(call POST "/network/sales-order/$SOID/allocate"); test "$(echo "$SO"|jq -r .vin)" != "null"
call POST "/network/sales-order/$SOID/finance" "{\"loanProvider\":\"上汽通用金融\",\"loanAmount\":100000,\"loanTermMonths\":24}" >/dev/null
SO=$(call POST "/network/sales-order/$SOID/finance/decision" "{\"approved\":true}"); test "$(echo "$SO"|jq -r .loanStatus)" = "APPROVED"
call POST "/network/sales-order/$SOID/insurance" "{\"company\":\"人保财险\",\"policyNo\":\"PICC-001\",\"amount\":6000}" >/dev/null
call POST "/network/sales-order/$SOID/payment" "{\"payType\":\"BALANCE\",\"amount\":45000,\"method\":\"TRANSFER\"}" >/dev/null
call POST "/network/sales-order/$SOID/invoice" "{\"invoiceType\":\"ELECTRONIC\",\"buyerTaxNo\":\"91310000TEST001\"}" >/dev/null
SO=$(call POST "/network/sales-order/$SOID/deliver" "{\"pdiPassed\":true,\"remark\":\"冒烟交车\"}"); test "$(echo "$SO"|jq -r .status)" = "DELIVERED"
test "$(echo "$SO"|jq -r .surveyId)" != "null"
VNEW=$(call GET "/customer/vehicle/page?vin=$(echo "$SO"|jq -r .vin)")
test "$(echo "$VNEW"|jq '.records|length')" = "1"

echo "== 14 OMS replenish (mock OMS: PUSHED -> SHIPPED -> COMPLETED)"
AV0=$(call GET "/parts/stock/available?dealerCode=$DC&partNo=P0002")
RP=$(call POST /oms/replenish/draft "{\"dealerCode\":\"$DC\",\"items\":[{\"partNo\":\"P0002\",\"qty\":6}]}")
RPID=$(echo "$RP"|jq -r .id); test "$(echo "$RP"|jq -r .status)" = "DRAFT"
RP=$(call POST "/oms/replenish/$RPID/push"); test "$(echo "$RP"|jq -r .status)" = "PUSHED"; test "$(echo "$RP"|jq -r .omsOrderNo)" != "null"
RP=$(call POST "/oms/replenish/$RPID/sync"); test "$(echo "$RP"|jq -r .status)" = "SHIPPED"
RP=$(call POST "/oms/replenish/$RPID/sync"); test "$(echo "$RP"|jq -r .status)" = "RECEIVED"
test "$(call GET "/parts/stock/available?dealerCode=$DC&partNo=P0002")" = "$((AV0+6))"
call POST "/oms/replenish/$RPID/sync" >/dev/null
test "$(call GET "/parts/stock/available?dealerCode=$DC&partNo=P0002")" = "$((AV0+6))"
test "$(call GET "/oms/replenish/oms-inventory?partNos=P0001,P0002"|jq length)" = "2"

echo "== 15 dashboard"
call GET "/dashboard?dealerCode=$DC" | jq -c '{workOrderStatusCounts,todayCheckIns,monthRevenue,openComplaints,dealerRanking}'

echo "SMOKE OK"
