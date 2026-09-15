---
name: dms-replenishment-runtime-testing
description: UI and local API verification of DMS dealer replenishment with mock OMS.
---

# Local DMS replenishment testing

- Follow the repository blueprint for Maven backend on 8080 and Vite frontend on 5173, proxying `/api`.
- The local demo currently opens directly as admin with dealer D001; do not assume external deployments have the same authentication.
- Navigate via 备件管理 → OMS补货 (`/parts/replenish`), 库存 (`/parts/stock`), 出入库流水 (`/parts/movement`), and 缺货预警 (`/parts/shortage`).
- Rebuild/restart the backend after Java changes; Vite reload alone does not update backend code.
- Mock OMS status queries advance PUSHED → SHIPPED → COMPLETED. Avoid incidental status queries while testing intermediate states.
- Mock OMS memory/sequence resets when the backend restarts, whereas H2 file data persists. Capture dealer stock totals before/after and movement timestamps rather than assuming each OMS batch number is new.
- Duplicate rows in the draft input table are expected. Open persisted 明细 after 生成补货单 to verify server-side merging.
- Terminal RECEIVED rows hide per-row sync/cancel actions. For explicitly authorized API checks, use local unauthenticated endpoints only in this demo setup, not extracted browser credentials.
- Error responses may use HTTP200 with JSON `code:400`. Assert both layers separately.
- For shortage fixtures, create a QA part with minimum stock10 and one stock unit for the selected dealer; expected draft qty is19. A part with no dealer stock row may not appear as a shortage. Shortage page can show multiple dealers; generation uses the selected dealer. Cancel the draft and reset fixture minStock0 after testing.
- Read native browser console logs in addition to temporary window console wrappers: framework-cached logging functions can bypass wrappers.

## Devin Secrets Needed

None for local mock mode. Authenticated callback tests require a test-only `DMS_OMS_CALLBACK_KEY` configured at backend startup; default unset-key mode should reject all callbacks. Real OMS testing requires separately provisioned integration settings and credentials.
