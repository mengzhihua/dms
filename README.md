# DMS 经销商管理系统 (Dealer Management System)

面向汽车 4S 店/经销商网络的一体化管理后台，覆盖 **整车销售 → 预约 → 维修工单 → 备件库存 → 结算开票 → 满意度回访 → 经销商考核** 全流程。架构与代码风格与同厂 TMS 项目保持一致：Spring Boot 单体后端 + Vue3 前端 + H2/MySQL。

项目亮点见 [docs/项目亮点.md](docs/项目亮点.md)。

## 功能范围

| 模块 | 功能 |
| --- | --- |
| 网络管理 | 经销商/直营店（4S 流程）、月度目标与达成、经销商考核（自动评分）、技师、工位、整车库存、整车销售订单（新建→分配→开票→交付） |
| 客户车辆 | 客户档案、车型（保修月/里程、保养间隔）、车辆档案、VIN 维修历史、保修校验 |
| 备件管理 | 备件主数据、多库位多批次库存、出入库/预留/释放流水、缺货预警 |
| 维修指导 | 工时项目、维修指导库（故障码+症状+步骤）、技术通报 TSB、智能推荐（DTC/车型/症状打分）、在线估价 |
| 维修工单 | 预约、工单全流程状态机、工时/备件行、一键带入指导、派工、质检、结算、保修索赔、取消释放预留 |
| 满意度 | 调研模板（SCORE/NPS/TEXT）、答卷评分、NPS 统计、低分自动投诉、投诉处理 |
| 发票管理 | 发票申请、开具（幂等）、红冲、报文预览、税控回调、MOCK/HTTP 双通道适配 |
| 工作台 | 状态分布、当日接车、当月产值、技师利用率、缺货数、NPS、投诉、经销商产值排名 |

## 目录结构

```
backend/   Spring Boot 后端（com.dms，按业务模块分包：network/customer/parts/guide/workshop/survey/invoice/dashboard/common）
frontend/  Vue3 + Vite + Element Plus 前端
scripts/   smoke.sh 全链路冒烟脚本
```

## 快速开始

- 后端：JDK 8+（可用更高版本编译，source/target=1.8）+ Maven 3.6+

  ```bash
  cd backend && mvn spring-boot:run   # H2 文件库 ./data/dms，端口 8080
  cd backend && mvn test              # 单元测试
  # MySQL：mvn spring-boot:run -Dspring-boot.run.profiles=mysql
  ```

- 前端：Node 18+

  ```bash
  cd frontend && npm install && npm run dev   # 端口 5173，/api 代理到 8080
  ```

- 冒烟：`bash scripts/smoke.sh`（后端启动后执行，覆盖工单全流程+发票+调研+销售流程，输出 SMOKE OK）

### 生产部署

- 启用 `prod` profile：`java -jar dms-backend.jar --spring.profiles.active=prod`（可叠加 MySQL：`prod,mysql`）。
- `prod` 下不装载演示数据/演示账号：仅初始化税率配置与满意度模板（`data-prod.sql`），并关闭 H2 控制台。
- 首次启动（`sys_user` 为空）自动创建 admin/ADMIN：密码取环境变量 `DMS_ADMIN_PASSWORD`；未配置则随机生成 16 位密码并以 WARN 打印一次日志（`首次启动已创建 admin，初始密码: xxx`），请立即修改。
- 生产必须配置 `DMS_JWT_SECRET`（至少 32 字节）。
- 演示账号（admin/oem/d001* 等，密码 `123456`）仅存在于默认开发 profile 的 `data.sql` 中，prod 下不可用。

## 登录与权限（RBAC）

所有 `/api/**` 接口（除 `/api/auth/login`、`/api/invoice/callback/**`、`/api/open/**`）需携带 `Authorization: Bearer <token>`（JWT，HS256）。前端打开即跳转 `/login`。

演示账号（初始密码均为 `123456`）：

| 账号 | 角色 | 经销商 | 说明 |
|---|---|---|---|
| admin | ADMIN | — | 全部权限，含用户管理 |
| oem | OEM | — | 全网只读；可写网络/指导库/车型/备件主数据/调研模板 |
| d001mgr | DEALER_MANAGER | D001 | 店内全部权限；不可写经销商主数据/目标/考核/指导库/调研模板/用户管理 |
| d001sa | ADVISOR | D001 | 全网只读；可写工单、客户车辆、调研答卷/投诉、备件预留释放/入库、OMS 补货 |
| d001tech | TECHNICIAN | D001 | 只读工单/指导/客户/备件；可写工单开工/完工/质检 |
| d001fin | FINANCE | D001 | 全网只读；可写发票、工单结算、整车销售开票/交车 |
| d002mgr | DEALER_MANAGER | D002 | 同 d001mgr |

- 数据范围：非全网角色（ADMIN/OEM 之外）仅能访问 `dealer_code` = 本店的记录；跨店读取/操作返回 400「无权访问其他经销商数据」；新建记录自动归属本店。
- 用户管理：`/api/auth/user` CRUD（仅 ADMIN），支持新建时传 `password` 明文（后端 BCrypt 落库，`passwordHash` 永不回传），不能删除/禁用自己。
- 接口：`POST /api/auth/login`、`GET /api/auth/me`、`POST /api/auth/logout`、`PUT /api/auth/password`。
- 配置：`dms.auth.jwt-secret`（HS256 密钥，经环境变量 `DMS_JWT_SECRET` 注入；未配置时启动生成随机密钥并告警，重启后旧 token 全部失效；配置值不足 32 字节时启动直接报错）、`dms.auth.expire-hours`（默认 12）。
- 登录锁定：同一用户名连续 5 次失败锁定 15 分钟。
- 登录风控：`dms.auth.ip-max-per-minute`（env `DMS_AUTH_IP_MAX_PER_MINUTE`，默认 30，0 关闭）对登录接口按客户端 IP 做固定 60 秒窗口限流，超限返回 429；`dms.auth.captcha-mode`（env `DMS_AUTH_CAPTCHA_MODE`，`OFF|ADAPTIVE|ALWAYS`，默认 ADAPTIVE）在用户名或 IP 近 15 分钟累计 ≥3 次失败后要求图形验证码（`GET /api/auth/captcha` 取图，登录体带 `captchaId`/`captchaCode`，缺失/错误返回业务码 4001）；`dms.auth.trust-proxy`（env `DMS_AUTH_TRUST_PROXY`，默认 false）为 true 时以 `X-Forwarded-For` 首个 IP 作为客户端地址。
- 权限矩阵见 `RolePolicy`（基于 AntPathMatcher 的角色→路径表）。

## 工单状态机

```
DRAFT → CHECKED_IN(接车/环检) → DIAGNOSED(诊断，可一键带入维修指导) → QUOTED(报价)
      → APPROVED(客户确认，预留备件，不足报错列缺口) → DISPATCHED(派工，技师/工位占用)
      → IN_REPAIR(开工) → QC_PENDING(完工，消耗备件释放资源) → QC_PASSED → SETTLED(结算/生成索赔与发票草稿)
      → DELIVERED(交车，更新里程并自动创建调研) → CLOSED
QC_FAILED → IN_REPAIR(返工)；DISPATCHED 之前任意状态可 CANCELLED(释放预留)
```

非法流转一律返回 400（`BizException`）。

## 金额与税

- 金额均为 `BigDecimal` scale 2，HALF_UP。
- 报价/结算：`总额 = 工时 + 备件`；保修项计入 `warrantyAmount`（厂家承担，不向客户收）；`customerPayable = 总额 - 保修 - 优惠`；`税额 = customerPayable / (1 + 0.13) × 0.13`。
- 保修金额 > 0 结算时自动生成 `WarrantyClaim`（SUBMITTED→APPROVED→PAID / REJECTED）。

## 发票对接（HTTP 税控网关）

- 接口 `TaxInvoiceGateway { issue / query / redFlush }`，默认 `MockTaxAdapter`（本地模拟；`dms.tax.mock-fail-rate` 模拟失败率）。`dms.tax.provider=HTTP` 启用 `HttpTaxAdapter`。
- 请求报文（`TaxRequest`，不再透传 Invoice 实体）：
  `POST {endpoint}/issue | /query | /red-flush`，JSON `{requestId(=invoiceNo), invoiceType, buyer{name,taxNo,address,bank}, seller{name,taxNo}, amount, taxAmount, netAmount, taxRate, lines[{name,unit,qty,unitPrice,amount,taxRate,taxAmount,taxCategoryCode}], redOf{code,number}(仅红冲), callbackUrl, remark}`。
- 签名（`dms.tax.sign-mode=HMAC`，默认）：头 `X-App-Id`、`X-Timestamp`(毫秒)、`X-Nonce`(UUID)、`X-Sign = hex(HMAC-SHA256(appSecret, appId + "\n" + timestamp + "\n" + nonce + "\n" + body))`；`sign-mode=SECRET_HEADER` 时仅携带 `X-App-Secret`。
- 响应 JSON：`{success, pending?, code, number, checkCode, pdfUrl, providerRef, errorMsg}`。`pending:true` 表示平台受理中：本侧发票保持 `ISSUING` 并记录 `providerRef`，用 `POST /api/invoice/{id}/sync`（内部调 `/query`）或异步回调收敛终态。
- 配置：`dms.tax.endpoint/app-id/app-secret/sign-mode/timeout-ms(默认10000)/callback-url`；回调 `POST /api/invoice/callback/{provider}`，`dms.tax.callback-token` 非空时需 `X-Tax-Callback-Token` 头。
- 红冲：`POST /api/invoice/{id}/red-flush` 生成负数红字发票并把原票置为 `RED_FLUSHED`；红票 ISSUED 时原票→RED_FLUSHED，红票 FAILED 时原票→ISSUED（回调与 sync 同样联动）。
- 发票状态机：`DRAFT → ISSUING → ISSUED → RED_FLUSHING → RED_FLUSHED`（失败落 FAILED，红冲失败回退 ISSUED）；开具/红冲均为原子状态抢占，防并发重复。
- 税收分类编码：工时（修理修配服务）`3040502000000000000`，备件/整车 `1090511010000000000`。
- `POST /api/invoice/{id}/issue` 幂等：已 ISSUED 直接返回原票；`POST /api/invoice/{id}/sync` 同步 ISSUING 发票。

### 联调（内置模拟税控）

```bash
cd backend && mvn -q spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=tax-sim \
  --dms.tax.provider=HTTP --dms.tax.endpoint=http://127.0.0.1:8080/sim/tax \
  --dms.tax.app-id=dms-app --dms.tax.app-secret=dev-tax-secret-padded-to-32-bytes"
bash scripts/tax-http-smoke.sh   # 期望输出 TAX HTTP SMOKE OK
```

`tax-sim` profile 开启 `TaxSimController`（`/sim/tax/issue|query|red-flush`，校验同一套 HMAC 签名）：remark 含 `PENDING` → 受理中（query 后返回已开）、含 `FAIL` → 拒绝、其余直接成功。

## OMS 备件补货对接

经销商备件从 OMS 中心仓补货：DMS 作为 OMS 的一个渠道店铺（`shopCode=SHOP-DMS01`，OMS 侧 `data.sql` 已预置渠道 `DMS`、店铺与 `P0001~P0030` 备件 SKU/`WH-SH` 库存）。

- 单据：`dms_replenish_order`，状态机 `DRAFT → PUSHING(建单中) → PUSHED → SHIPPED → RECEIVED`，`DRAFT/PUSHED → CANCELLED`（`PUSHING` 不可取消，建单失败回退 `DRAFT`；进程中断遗留的 `PUSHING` 超过 2 分钟后会在启动时及「全部同步」时按 `shopCode+replenishNo` 反查 OMS 恢复：已建单转 `PUSHED`，未建单回退 `DRAFT`；若恢复任务回退期间原下单请求在 OMS 建单成功，该请求会把单据从 `DRAFT` 直接确认为 `PUSHED`，不会静默返回草稿）；`channelOrderNo = replenishNo`，OMS 按 `shopCode+channelOrderNo` 幂等，重复下单不会产生第二张 OMS 订单。
- 下单：`POST /api/oms/replenish/draft {dealerCode, items:[{partNo,qty}]}` 或 `POST /api/oms/replenish/from-shortage?dealerCode=`（按缺货预警补到 2×minStock），再 `POST /api/oms/replenish/{id}/push` → OMS `POST /api/open/channel/orders`（收货人取经销商名称/电话/地址，SKU 直接使用备件号）。
- 状态同步（双通道）：
  - 拉：`POST /api/oms/replenish/{id}/sync`、`POST /api/oms/replenish/sync-all` → OMS `GET /api/open/channel/orders/{shopCode}/{channelOrderNo}`；
  - 推：OMS 发货/签收/取消时回调 `POST /api/open/oms/orders/status`（必须配置 `dms.oms.callback-key` 并携带匹配的 `X-Api-Key`，未配置时拒绝所有回推）。
- 入库：OMS `COMPLETED`（签收）时按实发数量 `shippedQty` 入库到经销商 `dms.oms.receive-location` 库位、批次号=OMS 单号；`SHIPPED→RECEIVED` 为条件更新，回推与轮询并发/重复也只入库一次；同一备件多行会在建草稿时合并（溢出拒绝）；OMS 元数据（`omsStatus`/物流）仅在本地单在途且快照不早于已记录 OMS 状态时更新，旧轮询不会覆盖新回推。补货单不支持通用 `POST/PUT/DELETE`，只能经 `draft/push/sync/cancel` 变更。
- 取消：`POST /api/oms/replenish/{id}/cancel` 仅 `DRAFT/PUSHED` 可取消（OMS 已发货不可取消）。
- 库存参考：`GET /api/oms/replenish/oms-inventory?partNos=P0001,P0002` → OMS 渠道可售量。
- 配置（`dms.oms.*`）：`DMS_OMS_MOCK`（默认 `true`，内存模拟 OMS，每次查询状态前进一步；为 `false` 时必须配置 `DMS_OMS_URL`，否则启动失败）、`DMS_OMS_URL`、`DMS_OMS_API_KEY`（OMS 的 `oms.open.api-key`）、`DMS_OMS_CALLBACK_KEY`、`shop-code`、`receive-location`、`timeout-ms`。
- 本地联调：OMS `SERVER_PORT=8081 OMS_DMS_URL=http://localhost:8080 OMS_DMS_KEY=cb`，DMS `DMS_OMS_MOCK=false DMS_OMS_URL=http://localhost:8081 DMS_OMS_API_KEY=oms-open-key DMS_OMS_CALLBACK_KEY=cb`。

## 满意度与投诉

- 答卷提交后计算加权总分（0-100）与 NPS 得分；**总分 < 60 或任一题 ≤ 3 分**自动生成投诉（总分<40 为 HIGH，否则 MEDIUM）。
- NPS = %推荐者(9-10) − %贬损者(0-6)；`/api/survey/stats` 返回份数、均分、NPS、分数段分布。
- 交车（DELIVERED）自动创建 SERVICE 类型调研（PENDING，SMS 渠道）。

## 经销商考核公式

`POST /api/network/assessment/generate?dealerCode=&yearMonth=`：

```
总分 = 销售达成率(封顶100)×30% + 工单达成率(封顶100)×30%
     + 当月已答卷均分(0-100，无答卷按80基准)×30%
     + (100 - 未关闭投诉数×10，最低0)×10%
等级: ≥90 A / ≥80 B / ≥70 C / ≥60 D / 其余 E
```

## 库存模型（对标说明）

备件库存借鉴 **富勒 WMS 与 SAP EWM** 的思路：库位（location）+ 批次（batchNo）+ 预留（reservedQty）三维度；预留按批次号 FIFO；所有扣减用带条件的原子 UPDATE（`WHERE qty - reserved_qty >= ?`）保证并发安全；出入库全部落 `StockMovement` 流水。整体流程对标主流汽车 DMS 的 4S 流程（销售-售后-配件-满意度闭环）。

单号（工单 WO/销售 SO/发票 INV/索赔 WC/调研 SV/投诉 CP）由 `seq_no` 表 `UPDATE ... SET seq_value=seq_value+1` 原子自增生成，并发安全。

## 数据表清单

`seq_no`（单号序列）；`dms_dealer`、`dms_dealer_target`、`dms_dealer_assessment`、`dms_technician`、`dms_bay`、`dms_vehicle_sales_order`、`dms_vehicle_stock`；`dms_customer`、`dms_vehicle_model`、`dms_vehicle`；`dms_part`、`dms_part_stock`、`dms_stock_movement`；`dms_labor_item`、`dms_repair_guide`、`dms_technical_bulletin`；`dms_appointment`、`dms_work_order`、`dms_work_order_labor`、`dms_work_order_part`、`dms_work_order_log`、`dms_warranty_claim`；`dms_survey_template`、`dms_survey_question`、`dms_survey`、`dms_survey_answer`、`dms_complaint`；`dms_invoice`、`dms_invoice_line`、`dms_tax_config`；`dms_replenish_order`（OMS 备件补货）。

## API 概览（统一前缀 /api，返回 {code,msg,data}）

- 主数据：`/{模块}/{资源}/page|list|/{id}` GET/POST/PUT/DELETE，如 `/api/network/dealer/page?page=1&size=20&keyword=`
- 网络：`GET /api/network/target/achievement?dealerCode&yearMonth`；`POST /api/network/assessment/generate`；销售订单 `POST /api/network/sales-order/{id}/allocate|invoice|deliver|cancel`
- 车辆：`GET /api/customer/vehicle/vin/{vin}/history`；`GET /api/customer/vehicle/{id}/warranty-check?mileage=`
- 备件：`POST /api/parts/stock/inbound`；`GET /api/parts/stock/shortage`；`GET /api/parts/stock/available`
- 指导：`POST /api/guide/recommend` `{modelCode,dtcCodes[],symptom,dealerCode}`；`GET /api/guide/estimate?guideCode&dealerCode`
- 工单：`POST /api/workshop/order`；`POST /api/workshop/order/{id}/labor|part`（DELETE 移除）；`POST /api/workshop/order/{id}/apply-guide/{guideCode}`；动作 `diagnose|quote|approve|dispatch|start|finish|qc|settle|deliver|close|cancel`；`GET /api/workshop/order/{id}`；索赔 `POST /api/workshop/claim/{id}/approve|reject|pay`
- 满意度：`POST /api/survey/{id}/answer`；`GET /api/survey/stats`；`POST /api/survey/complaint/{id}/handle`
- 发票：`POST /api/invoice`；`POST /api/invoice/{id}/issue|red-flush`；`GET /api/invoice/{id}/preview`；`POST /api/invoice/callback/{provider}`
- OMS 补货：`POST /api/oms/replenish/draft|from-shortage|sync-all`；`POST /api/oms/replenish/{id}/push|sync|cancel`；`GET /api/oms/replenish/{id}/lines`、`/oms-inventory?partNos=`；OMS 回推 `POST /api/open/oms/orders/status`
- 工作台：`GET /api/dashboard?dealerCode=`

## 前端

左侧菜单按业务分组；头部有经销商选择器（持久化 localStorage，各页面默认按所选经销商过滤）。自定义页面：工单详情（步骤条+工时/备件/日志 Tab+状态机动作按钮）、智能推荐、答卷、满意度统计、发票开具/红冲/预览、工作台卡片与排名。

## 控制塔对接

缺货和补货单快照，以及按缺货生成、按单号下发，见 [技术方案](docs/技术方案.md)。

有 API Key 时走 `/api/open/ir/snapshots` 和 `/actions`，并回退 `/replenish-shortage`、`/push-replenish`。没有 Key 时，控制塔登录后打 `/api/oms/replenish/from-shortage`，再按补货单号查出 id 后 `push`。

## 发布包（开箱即用）

前端生产构建打进 Spring Boot 可执行 JAR。三种用法：

### 1. 服务端（任意已装 JDK 17 的机器）

```bash
java -jar dms-backend-1.0.0.jar --server.port=8092
```

Linux systemd 示例见发布包 `README.txt`。

### 2. 便携包（需本机已装 Java）

```bash
bash scripts/package-release.sh
unzip release/dms-1.0.0.zip
cd dms-1.0.0
```

| 系统 | 怎么用 |
| --- | --- |
| Linux | `./start.sh` |
| macOS | 双击 `start.command`，或 `./start.sh` |
| Windows | 双击 `start.bat` |

### 3. 原生包（捆绑 JRE，不必装 Java）

合并到默认分支且便携包冒烟通过后，GitHub Actions 自动发布 GitHub Release（也可在 Actions 里手动 `workflow_dispatch`）。分别在 Ubuntu / Windows / macOS 生成：

- `dms-1.0.0-linux-x64.zip` → `bin/dms`
- `dms-1.0.0-windows-x64.zip` → 双击 `dms.exe`
- `dms-1.0.0-macos-arm64.zip` → Apple Silicon（M 系列），双击 `dms.app`
- `dms-1.0.0-macos-x64.zip` → Intel Mac，双击 `dms.app`

浏览器访问 `http://127.0.0.1:8092`。默认账号 `admin / 123456`。

十二套系统可同时启动：OMS 8081 / WMS 8082 / TMS 8083 / BMS 8084 / SAP 8085 / OA 8086 / SRM 8087 / BOM 8088 / INV 8089 / IR 8090 / CRM 8091 / DMS 8092。

