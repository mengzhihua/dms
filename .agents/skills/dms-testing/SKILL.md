---
name: dms-replenishment-runtime-testing
description: UI and local API verification of DMS replenishment, packaged SPA, and Open IR with mock OMS.
---

# Local DMS replenishment testing

- Follow the repository blueprint for Maven backend on 8080 and Vite frontend on 5173, proxying `/api`.
- Current local builds require login. Seed demo users include admin and d001sa, password 123456; do not assume these credentials apply to external deployments. Advisor scope is D001 with a disabled dealer selector.
- Navigate via 备件管理 → OMS补货 (`/parts/replenish`), 库存 (`/parts/stock`), 出入库流水 (`/parts/movement`), and 缺货预警 (`/parts/shortage`).
- Rebuild/restart the backend after Java changes; Vite reload alone does not update backend code.
- Mock OMS status queries advance PUSHED → SHIPPED → COMPLETED. Avoid incidental status queries while testing intermediate states.
- Mock OMS memory/sequence resets when the backend restarts, whereas H2 file data persists. Capture dealer stock totals before/after and movement timestamps rather than assuming each OMS batch number is new.
- Duplicate rows in the draft input table are expected. Open persisted 明细 after 生成补货单 to verify server-side merging.
- Terminal RECEIVED rows hide per-row sync/cancel actions. For explicitly authorized API checks, use the intended authentication mechanism; never extract browser credentials for unrelated backend requests.
- Error responses may use HTTP200 with JSON `code:400`. Assert both layers separately.
- For shortage fixtures, create a QA part with minimum stock10 and one stock unit for the selected dealer; expected draft qty is19. A part with no dealer stock row may not appear as a shortage. Shortage page can show multiple dealers; generation uses the selected dealer. Cancel the draft and reset fixture minStock0 after testing.
- Read native browser console logs in addition to temporary window console wrappers: framework-cached logging functions can bypass wrappers.

## Packaged SPA and Open IR

- Build with `SKIP_TESTS=1 bash scripts/package-release.sh`. The staged portable directory is `release/dms-1.0.0`; its smoke script is named `smoke.sh`, copied from `scripts/release-smoke.sh`.
- Run staged `smoke.sh` with port 8092 free. It starts and stops its own process; afterward separately run `SKIP_BROWSER=1 ./start.sh` from the staged directory for browser testing. The JAR serves SPA and API on the same port, no Vite needed. Check deep-link refresh, not only root HTML.
- Development 8080 and package 8092 use separate working-directory-relative `data/dms` files. Avoid resetting either without authorization. Count customer seed rows before/after restart: unconditional seed inserts can create duplicates even if uniquely keyed tables stay unchanged.
- Open IR uses `X-Api-Key` independently of JWT; local default is `dms-open-key`, override `DMS_OPEN_API_KEY`. Assert transport status and JSON `code` independently for absent/invalid keys, empty objects, and malformed JSON.
- Read snapshots before choosing a shortage fixture. Fresh seeds contain D002/P0008 with available2/minimum8. Action body `{"type":"DMS_REPLENISH_SHORTAGE","targetKey":"D002/P0008","params":{"dealerCode":"D002"}}` should create a DRAFT/SHORTAGE replenishment with quantity14. Verify the returned replenishment number through authenticated OMS补货 UI and its 明细; creation alone does not increase stock.
- Existing development smoke may exhaust its matching D001/M001/珍珠白 vehicle across runs. If it exits at sales allocation, inspect stock and report the missing prerequisite rather than resetting persistent data or treating later OMS/dashboard checks as passed.

## Devin Secrets Needed

None for local mock mode. Authenticated callback tests require a test-only `DMS_OMS_CALLBACK_KEY` configured at backend startup; default unset-key mode should reject all callbacks. Real OMS testing requires separately provisioned integration settings and credentials.
