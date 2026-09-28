-- DMS 种子数据（幂等：INSERT 失败忽略由 continue-on-error 保证，仅首次生效）
-- ============ 经销商：2 家授权经销商 + 1 家直营店 ============
INSERT INTO dms_dealer(code,name,type,level,region,province,city,address,contact,phone,status,contract_start,contract_end,credit_limit,labor_rate,tax_no,bank_account)
SELECT u.* FROM (
SELECT 'D001' AS code,'上海申联汽车4S店' AS name,'DEALER' AS type,'A' AS level,'华东' AS region,'上海市' AS province,'上海市' AS city,'浦东新区申江路100号' AS address,'王经理' AS contact,'021-58880001' AS phone,'ACTIVE' AS status,'2023-01-01' AS contract_start,'2026-12-31' AS contract_end,5000000.00 AS credit_limit,180.00 AS labor_rate,'91310000MA1K00001A' AS tax_no,'中国银行上海分行 10010001' AS bank_account
UNION ALL
SELECT 'D002','杭州宏达汽车4S店','DEALER','B','华东','浙江省','杭州市','西湖区文三路200号','李经理','0571-88880002','ACTIVE','2023-06-01','2026-05-31',3000000.00,160.00,'91330100MA2K00002B','工商银行杭州分行 20020002'
UNION ALL
SELECT 'S001','品牌直营上海旗舰店','DIRECT','A','华东','上海市','上海市','静安区南京西路300号','直营店总','021-58880003','ACTIVE',NULL,NULL,NULL,200.00,'91310000MA1K00003C','建设银行上海分行 30030003'
) u
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM dms_dealer) t WHERE t.code = u.code);

INSERT INTO dms_dealer_target(dealer_code,`year_month`,sales_target,service_target,revenue_target)
SELECT u.* FROM (
SELECT 'D001' AS dealer_code,'2024-01' AS `year_month`,40 AS sales_target,300 AS service_target,8000000.00 AS revenue_target
UNION ALL
SELECT 'D002','2024-01',25,200,5000000.00
UNION ALL
SELECT 'S001','2024-01',50,350,10000000.00
) u
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM dms_dealer_target) t WHERE t.dealer_code = u.dealer_code AND t.`year_month` = u.`year_month`);

INSERT INTO dms_technician(dealer_code,code,name,level,skills,status)
SELECT u.* FROM (
SELECT 'D001' AS dealer_code,'T001' AS code,'张建国' AS name,'技师长' AS level,'发动机,变速箱,电控' AS skills,'IDLE' AS status
UNION ALL
SELECT 'D001','T002','刘伟','高级','机修,底盘','IDLE'
UNION ALL
SELECT 'D001','T003','陈强','中级','钣金,喷漆','IDLE'
UNION ALL
SELECT 'S001','T101','赵敏','高级','电控,新能源','IDLE'
UNION ALL
SELECT 'S001','T102','孙磊','中级','快保,轮胎','IDLE'
) u
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM dms_technician) t WHERE t.dealer_code = u.dealer_code AND t.code = u.code);

INSERT INTO dms_bay(dealer_code,code,type,status)
SELECT u.* FROM (
SELECT 'D001' AS dealer_code,'B01' AS code,'机修' AS type,'IDLE' AS status
UNION ALL
SELECT 'D001','B02','机修','IDLE'
UNION ALL
SELECT 'D001','B03','钣金','IDLE'
UNION ALL
SELECT 'D001','B04','喷漆','IDLE'
UNION ALL
SELECT 'D001','B05','快保','IDLE'
UNION ALL
SELECT 'S001','B01','机修','IDLE'
UNION ALL
SELECT 'S001','B02','快保','IDLE'
UNION ALL
SELECT 'S001','B03','喷漆','IDLE'
) u
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM dms_bay) t WHERE t.dealer_code = u.dealer_code AND t.code = u.code);

-- ============ 车型 ============
INSERT INTO dms_vehicle_model(code,name,brand,series,warranty_months,warranty_km,maintenance_interval_km)
SELECT u.* FROM (
SELECT 'M001' AS code,'星越L 2.0T 两驱尊贵型' AS name,'自主品牌' AS brand,'星越' AS series,36 AS warranty_months,100000 AS warranty_km,10000 AS maintenance_interval_km
UNION ALL
SELECT 'M002','帝豪GL 1.5L CVT','自主品牌','帝豪',36,100000,7500
UNION ALL
SELECT 'M003','远景SUV 1.4T','自主品牌','远景',36,100000,7500
UNION ALL
SELECT 'M004','博越Pro 1.8T 四驱','自主品牌','博越',48,120000,10000
UNION ALL
SELECT 'M005','极氪001 WE版','新能源','极氪',48,120000,15000
) u
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM dms_vehicle_model) t WHERE t.code = u.code);

-- ============ 客户 ============
INSERT INTO dms_customer(name,phone,id_no,gender,level,dealer_code)
SELECT u.* FROM (
SELECT '王小明' AS name,'13800000001' AS phone,'310104198801010011' AS id_no,'男' AS gender,'VIP' AS level,'D001' AS dealer_code
UNION ALL
SELECT '李芳','13800000002','310104199002020022','女','普通','D001'
UNION ALL
SELECT '张伟','13800000003','330106197503030033','男','普通','D001'
UNION ALL
SELECT '刘洋','13800000004','330106199104040044','男','VIP','D002'
UNION ALL
SELECT '陈静','13800000005','310110198505050055','女','普通','D002'
UNION ALL
SELECT '杨帆','13800000006','310110199206060066','男','普通','S001'
UNION ALL
SELECT '黄磊','13800000007','310115198707070077','男','VIP','S001'
UNION ALL
SELECT '周敏','13800000008','310115199308080088','女','普通','S001'
UNION ALL
SELECT '吴刚','13800000009','310104198009090099','男','普通','D001'
UNION ALL
SELECT '郑爽','13800000010','310104199510100100','女','VIP','D002'
) u
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM dms_customer) t WHERE t.dealer_code = u.dealer_code AND t.phone = u.phone);

-- ============ 车辆 ============
INSERT INTO dms_vehicle(vin,plate_no,model_code,customer_id,dealer_code,mileage,purchase_date,warranty_start,warranty_end,last_service_date,next_service_mileage)
SELECT u.* FROM (
SELECT 'L6T7824Z1NN000001' AS vin,'沪A10001' AS plate_no,'M001' AS model_code,1 AS customer_id,'D001' AS dealer_code,15000 AS mileage,'2023-03-10' AS purchase_date,'2023-03-10' AS warranty_start,'2026-03-10' AS warranty_end,'2023-12-01' AS last_service_date,20000 AS next_service_mileage
UNION ALL
SELECT 'L6T7824Z1NN000002','沪B20002','M002',2,'D001',8000,'2023-08-15','2023-08-15','2026-08-15',NULL,15000
UNION ALL
SELECT 'L6T7824Z1NN000003','沪C30003','M004',3,'D001',42000,'2022-05-20','2022-05-20','2026-05-20','2023-11-10',50000
UNION ALL
SELECT 'L6T7824Z1NN000004','浙A40004','M001',4,'D002',30000,'2022-09-01','2022-09-01','2025-09-01','2023-12-20',35000
UNION ALL
SELECT 'L6T7824Z1NN000005','浙B50005','M003',5,'D002',6000,'2023-10-11','2023-10-11','2026-10-11',NULL,13500
UNION ALL
SELECT 'L6T7824Z1NN000006','沪D60006','M005',6,'S001',20000,'2023-01-25','2023-01-25','2027-01-25','2023-12-15',35000
UNION ALL
SELECT 'L6T7824Z1NN000007','沪E70007','M005',7,'S001',5000,'2023-11-30','2023-11-30','2027-11-30',NULL,20000
UNION ALL
SELECT 'L6T7824Z1NN000008','沪F80008','M002',8,'S001',25000,'2022-06-18','2022-06-18','2025-06-18','2023-10-05',30000
UNION ALL
SELECT 'L6T7824Z1NN000009','沪G90009','M001',9,'D001',120000,'2020-04-02','2020-04-02','2023-04-02','2023-12-28',125000
UNION ALL
SELECT 'L6T7824Z1NN000010','浙C10010','M004',10,'D002',55000,'2021-07-07','2021-07-07','2025-07-07','2023-11-22',60000
) u
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM dms_vehicle) t WHERE t.vin = u.vin);

-- ============ 整车库存 ============
INSERT INTO dms_vehicle_stock(dealer_code,vin,model_code,color,status)
SELECT u.* FROM (
SELECT 'D001' AS dealer_code,'L6T7824Z1NN100001' AS vin,'M001' AS model_code,'珍珠白' AS color,'IN_STOCK' AS status
UNION ALL
SELECT 'D001','L6T7824Z1NN100002','M001','曜石黑','IN_STOCK'
UNION ALL
SELECT 'D001','L6T7824Z1NN100003','M004','钛金灰','IN_STOCK'
UNION ALL
SELECT 'D002','L6T7824Z1NN200001','M003','珍珠白','IN_STOCK'
UNION ALL
SELECT 'D002','L6T7824Z1NN200002','M004','烈焰红','IN_STOCK'
UNION ALL
SELECT 'S001','L6T7824Z1NN300001','M005','极昼白','IN_STOCK'
UNION ALL
SELECT 'S001','L6T7824Z1NN300002','M005','电光蓝','IN_STOCK'
UNION ALL
SELECT 'S001','L6T7824Z1NN300003','M002','珍珠白','IN_STOCK'
) u
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM dms_vehicle_stock) t WHERE t.dealer_code = u.dealer_code AND t.vin = u.vin);

-- ============ 备件主数据 (~30) ============
INSERT INTO dms_part(part_no,name,category,unit,cost_price,sale_price,tax_rate,min_stock)
SELECT u.* FROM (
SELECT 'P0001' AS part_no,'机油滤芯' AS name,'保养件' AS category,'个' AS unit,15.00 AS cost_price,35.00 AS sale_price,0.13 AS tax_rate,20 AS min_stock
UNION ALL
SELECT 'P0002','空气滤芯','保养件','个',30.00,68.00,0.13,15
UNION ALL
SELECT 'P0003','空调滤芯','保养件','个',45.00,98.00,0.13,15
UNION ALL
SELECT 'P0004','全合成机油 5W-30 4L','油液','桶',180.00,328.00,0.13,30
UNION ALL
SELECT 'P0005','刹车油 DOT4 1L','油液','瓶',35.00,80.00,0.13,10
UNION ALL
SELECT 'P0006','变速箱油 ATF 1L','油液','瓶',60.00,128.00,0.13,10
UNION ALL
SELECT 'P0007','冷却液 -35℃ 4L','油液','桶',40.00,88.00,0.13,10
UNION ALL
SELECT 'P0008','前刹车片','制动系统','套',150.00,320.00,0.13,8
UNION ALL
SELECT 'P0009','后刹车片','制动系统','套',120.00,260.00,0.13,8
UNION ALL
SELECT 'P0010','刹车盘','制动系统','只',200.00,450.00,0.13,4
UNION ALL
SELECT 'P0011','火花塞','发动机','只',25.00,60.00,0.13,30
UNION ALL
SELECT 'P0012','正时皮带','发动机','条',180.00,420.00,0.13,3
UNION ALL
SELECT 'P0013','水泵','发动机','个',220.00,480.00,0.13,2
UNION ALL
SELECT 'P0014','发电机','电器','台',800.00,1600.00,0.13,1
UNION ALL
SELECT 'P0015','蓄电池 60Ah','电器','只',350.00,680.00,0.13,5
UNION ALL
SELECT 'P0016','雨刮片 24寸','车身','只',20.00,55.00,0.13,20
UNION ALL
SELECT 'P0017','前减震器','底盘','只',380.00,780.00,0.13,4
UNION ALL
SELECT 'P0018','轮胎 225/55R18','轮胎','条',550.00,980.00,0.13,8
UNION ALL
SELECT 'P0019','大灯总成 左','车身','个',1200.00,2600.00,0.13,1
UNION ALL
SELECT 'P0020','大灯总成 右','车身','个',1200.00,2600.00,0.13,1
UNION ALL
SELECT 'P0021','前保险杠','车身','个',600.00,1300.00,0.13,2
UNION ALL
SELECT 'P0022','后视镜总成 左','车身','个',280.00,560.00,0.13,2
UNION ALL
SELECT 'P0023','离合器三件套','变速箱','套',900.00,1800.00,0.13,1
UNION ALL
SELECT 'P0024','氧传感器','发动机','只',150.00,320.00,0.13,4
UNION ALL
SELECT 'P0025','节气门总成','发动机','个',450.00,900.00,0.13,2
UNION ALL
SELECT 'P0026','燃油泵','发动机','个',520.00,1050.00,0.13,2
UNION ALL
SELECT 'P0027','空调压缩机','空调','台',1500.00,3200.00,0.13,1
UNION ALL
SELECT 'P0028','冷媒 R134a','空调','瓶',30.00,68.00,0.13,10
UNION ALL
SELECT 'P0029','玻璃水 2L','油液','瓶',8.00,20.00,0.13,30
UNION ALL
SELECT 'P0030','车门密封条','车身','条',60.00,120.00,0.13,5
UNION ALL
SELECT 'P-IR-SHORT','控制塔缺货演示件','保养件','个',10.00,25.00,0.13,10
) u
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM dms_part) t WHERE t.part_no = u.part_no);

-- ============ 备件库存（库位 A-01-01 风格，批次） ============
INSERT INTO dms_part_stock(dealer_code,part_no,location,batch_no,qty,reserved_qty)
SELECT u.* FROM (
SELECT 'D001' AS dealer_code,'P-IR-SHORT' AS part_no,'A-09-01' AS location,'B20260101' AS batch_no,1 AS qty,0 AS reserved_qty
UNION ALL
SELECT 'D001','P0001','A-01-01','B20231201',50,0
UNION ALL
SELECT 'D001','P0001','A-01-02','B20240101',30,0
UNION ALL
SELECT 'D001','P0002','A-01-03','B20231201',40,0
UNION ALL
SELECT 'D001','P0003','A-01-04','B20231201',35,0
UNION ALL
SELECT 'D001','P0004','B-01-01','B20231201',80,0
UNION ALL
SELECT 'D001','P0005','B-01-02','B20231201',20,0
UNION ALL
SELECT 'D001','P0006','B-01-03','B20231201',15,0
UNION ALL
SELECT 'D001','P0008','C-01-01','B20231201',12,0
UNION ALL
SELECT 'D001','P0009','C-01-02','B20231201',10,0
UNION ALL
SELECT 'D001','P0011','A-02-01','B20231201',60,0
UNION ALL
SELECT 'D001','P0015','D-01-01','B20231201',8,0
UNION ALL
SELECT 'D001','P0016','A-02-02','B20231201',40,0
UNION ALL
SELECT 'D001','P0018','E-01-01','B20231201',16,0
UNION ALL
SELECT 'S001','P0001','A-01-01','B20231201',60,0
UNION ALL
SELECT 'S001','P0004','B-01-01','B20231201',100,0
UNION ALL
SELECT 'S001','P0008','C-01-01','B20231201',15,0
UNION ALL
SELECT 'S001','P0015','D-01-01','B20231201',10,0
UNION ALL
SELECT 'D002','P0001','A-01-01','B20231201',40,0
UNION ALL
SELECT 'D002','P0004','B-01-01','B20231201',60,0
UNION ALL
SELECT 'D002','P0008','C-01-01','B20231201',2,0
) u
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM dms_part_stock) t WHERE t.dealer_code = u.dealer_code AND t.part_no = u.part_no AND t.location = u.location AND t.batch_no = u.batch_no);

-- ============ 工时项目 (~15) ============
INSERT INTO dms_labor_item(code,name,standard_hours,category)
SELECT u.* FROM (
SELECT 'L001' AS code,'更换机油机滤（小保养）' AS name,0.5 AS standard_hours,'保养' AS category
UNION ALL
SELECT 'L002','更换空气滤芯',0.2,'保养'
UNION ALL
SELECT 'L003','更换空调滤芯',0.3,'保养'
UNION ALL
SELECT 'L004','全车安全检查',0.5,'保养'
UNION ALL
SELECT 'L005','更换前刹车片',1.0,'制动'
UNION ALL
SELECT 'L006','更换后刹车片',0.8,'制动'
UNION ALL
SELECT 'L007','更换刹车盘',1.2,'制动'
UNION ALL
SELECT 'L008','更换火花塞',0.8,'发动机'
UNION ALL
SELECT 'L009','正时皮带更换',3.5,'发动机'
UNION ALL
SELECT 'L010','水泵更换',2.5,'发动机'
UNION ALL
SELECT 'L011','前减震器更换(单侧)',1.5,'底盘'
UNION ALL
SELECT 'L012','四轮定位',0.8,'底盘'
UNION ALL
SELECT 'L013','轮胎更换+动平衡(每条)',0.4,'轮胎'
UNION ALL
SELECT 'L014','钣金修复(每面板)',2.0,'钣喷'
UNION ALL
SELECT 'L015','喷漆(每面板)',2.5,'钣喷'
) u
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM dms_labor_item) t WHERE t.code = u.code);

-- ============ 维修指导库 (~10) ============
INSERT INTO dms_repair_guide(code,title,model_codes,dtc_codes,symptoms,diagnosis_steps,repair_steps,labor_item_codes,part_nos,difficulty,safety_notes)
SELECT u.* FROM (
SELECT 'G001' AS code,'发动机抖动且故障灯亮（缺火）' AS title,'ALL' AS model_codes,'P0300,P0301,P0302,P0303,P0304' AS dtc_codes,'抖动,故障灯,怠速不稳' AS symptoms,'1.读取故障码确认缺火气缸\n2.对调火花塞/点火线圈确认故障件\n3.检查缸压' AS diagnosis_steps,'1.更换火花塞\n2.如仍缺火更换点火线圈\n3.清除故障码路试' AS repair_steps,'L008' AS labor_item_codes,'P0011' AS part_nos,'低' AS difficulty,'断开电瓶负极后操作' AS safety_notes
UNION ALL
SELECT 'G002','刹车异响/制动距离变长','ALL','C1234','异响,刹车软,抖动','1.检查刹车片厚度\n2.检查刹车盘磨损\n3.路试确认','1.更换前/后刹车片\n2.必要时更换刹车盘\n3.排空气','L005,L006,L007','P0008,P0009,P0010','中','举升车辆必须使用安全支架'
UNION ALL
SELECT 'G003','常规小保养','ALL',NULL,'保养,到期','1.核对保养里程\n2.全车检查','1.更换机油机滤\n2.检查空气/空调滤芯\n3.复位保养灯','L001,L002,L003,L004','P0001,P0002,P0003,P0004','低','使用规定标号机油'
UNION ALL
SELECT 'G004','空调不制冷','ALL','B10A0','空调,不制冷,无冷风','1.检查冷媒压力\n2.检漏\n3.检查压缩机','1.回收冷媒\n2.修复泄漏点\n3.更换压缩机或补冷媒','L001','P0027,P0028','中','冷媒回收需专用设备'
UNION ALL
SELECT 'G005','正时皮带更换周期到','M001,M003,M004',NULL,'里程到期,正时','1.核对里程≥8万km\n2.检查皮带裂纹','1.拆下附件\n2.更换正时皮带及张紧轮\n3.校对正时标记','L009,L010','P0012,P0013','高','必须校对正时，错位顶气门'
UNION ALL
SELECT 'G006','底盘异响/方向跑偏','ALL',NULL,'异响,跑偏,吃胎','1.检查减震器漏油\n2.路试听诊\n3.四轮定位数据','1.更换减震器\n2.四轮定位','L011,L012','P0017','中','拆装弹簧使用专用工具'
UNION ALL
SELECT 'G007','蓄电池亏电无法启动','ALL','P0562,U0121','无法启动,亏电,仪表暗','1.测电瓶电压\n2.测发电电压\n3.漏电检查','1.更换蓄电池\n2.如发电异常更换发电机','L001','P0015,P0014','低','先断负极再断正极'
UNION ALL
SELECT 'G008','事故车钣金喷漆','ALL',NULL,'事故,剐蹭,凹陷','1.定损拍照\n2.拆检内部损伤','1.钣金整形\n2.刮腻子打磨\n3.喷漆抛光','L014,L015','P0021,P0019,P0020','高','喷漆房防火防爆'
UNION ALL
SELECT 'G009','变速箱换挡顿挫','M001,M002','P0700,P0750','顿挫,换挡,冲击','1.读取变速箱故障码\n2.检查油位油质','1.更换变速箱油\n2.升级TCU程序','L001','P0006','中','使用循环机换油'
UNION ALL
SELECT 'G010','轮胎更换与动平衡','ALL',NULL,'扎胎,磨损,偏磨','1.检查轮胎磨损标记\n2.检查胎侧损伤','1.更换轮胎\n2.动平衡\n3.四轮换位','L013,L012','P0018','低','扭矩按标准上紧轮毂螺栓'
) u
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM dms_repair_guide) t WHERE t.code = u.code);

-- ============ 技术通报 TSB ============
INSERT INTO dms_technical_bulletin(code,title,model_codes,content,issue_date)
SELECT u.* FROM (
SELECT 'TSB001' AS code,'关于部分星越L偶发P0300缺火的技术通报' AS title,'M001' AS model_codes,'部分车辆冷启动偶发缺火，请优先更换改进型火花塞并升级ECU至V2.3。' AS content,'2023-10-15' AS issue_date
UNION ALL
SELECT 'TSB002','帝豪GL CVT变速箱油更换周期调整','M002','CVT变速箱油更换周期由6万公里调整为4万公里。','2023-08-01'
UNION ALL
SELECT 'TSB003','极氪001 充电口盖板卡滞处理方案','M005','低温环境充电口盖板卡滞，更换改进型执行器。','2023-12-01'
) u
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM dms_technical_bulletin) t WHERE t.code = u.code);

-- ============ 满意度模板：1 套售后模板 5 题 ============
INSERT INTO dms_survey_template(code,name,type)
SELECT u.* FROM (
SELECT 'SV01' AS code,'售后服务满意度调研' AS name,'SERVICE' AS type
) u
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM dms_survey_template) t WHERE t.code = u.code);
INSERT INTO dms_survey_question(template_id,seq,text,type,weight)
SELECT u.* FROM (
SELECT 1 AS template_id,1 AS seq,'您对本次维修/保养的总体满意度评分（1-10分）' AS text,'SCORE' AS type,0.3 AS weight
UNION ALL
SELECT 1,2,'服务顾问接待与沟通满意度（1-10分）','SCORE',0.2
UNION ALL
SELECT 1,3,'维修质量与一次性修复满意度（1-10分）','SCORE',0.3
UNION ALL
SELECT 1,4,'交车及时性与车辆清洁满意度（1-10分）','SCORE',0.2
UNION ALL
SELECT 1,5,'您愿意向亲友推荐本店吗（0-10分）','NPS',1.0
) u
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM dms_survey_question) t WHERE t.template_id = u.template_id AND t.seq = u.seq);
INSERT INTO dms_survey_template(code,name,type)
SELECT u.* FROM (
SELECT 'SV02' AS code,'新车销售满意度调研' AS name,'SALES' AS type
) u
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM dms_survey_template) t WHERE t.code = u.code);
INSERT INTO dms_survey_question(template_id,seq,text,type,weight)
SELECT u.* FROM (
SELECT (SELECT id FROM dms_survey_template WHERE code='SV02') AS template_id,1 AS seq,'您对购车顾问服务的满意度（1-10分）' AS text,'SCORE' AS type,0.3 AS weight
UNION ALL
SELECT (SELECT id FROM dms_survey_template WHERE code='SV02'),2,'您对交车流程与PDI检查的满意度（1-10分）','SCORE',0.3
UNION ALL
SELECT (SELECT id FROM dms_survey_template WHERE code='SV02'),3,'您对金融/保险服务的满意度（1-10分）','SCORE',0.2
UNION ALL
SELECT (SELECT id FROM dms_survey_template WHERE code='SV02'),4,'您愿意向亲友推荐本店购车吗（0-10分）','NPS',1.0
) u
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM dms_survey_question) t WHERE t.template_id = u.template_id AND t.seq = u.seq);

-- ============ 税率配置 ============
INSERT INTO dms_tax_config(dealer_code,tax_rate,seller_name,seller_tax_no)
SELECT u.* FROM (
SELECT 'D001' AS dealer_code,0.13 AS tax_rate,'上海申联汽车4S店' AS seller_name,'91310000MA1K00001A' AS seller_tax_no
UNION ALL
SELECT 'D002',0.13,'杭州宏达汽车4S店','91330100MA2K00002B'
UNION ALL
SELECT 'S001',0.13,'品牌直营上海旗舰店','91310000MA1K00003C'
) u
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM dms_tax_config) t WHERE t.dealer_code = u.dealer_code);

-- ============ 系统用户（初始密码均为 123456，BCrypt） ============
INSERT INTO sys_user(username,password_hash,real_name,role,dealer_code,enabled)
SELECT u.* FROM (
SELECT 'admin' AS username,'$2a$10$C9iArSMtcAXl9h0wqSrG7u4RR9kBcSkoANit.wBKa/vHhu3sAzzU.' AS password_hash,'系统管理员' AS real_name,'ADMIN' AS role,NULL AS dealer_code,TRUE AS enabled
UNION ALL
SELECT 'oem','$2a$10$d0a8j/7gjzq5zTlatOHsI.nxcOFAcQOP5aCpNbX1d7OPV2vomz.se','厂家管理员','OEM',NULL,TRUE
UNION ALL
SELECT 'd001mgr','$2a$10$j65dKjZSUblsakkGMnWUh.oL/cylZnw19QXaK52AuXLg.OapmEogK','上海申联-店总','DEALER_MANAGER','D001',TRUE
UNION ALL
SELECT 'd001sa','$2a$10$Y.YD/se9tPtbRqgev6M/su5tCkNZwIP.2.uPz5sVz69OYcD/tgTCG','服务顾问小王','ADVISOR','D001',TRUE
UNION ALL
SELECT 'd001tech','$2a$10$3a0NQRntg9qzQcMbh/uObOsUNnXQsKlhues.grTUqa6cdtuNwR2bK','技师小李','TECHNICIAN','D001',TRUE
UNION ALL
SELECT 'd001fin','$2a$10$pSIaQNaeCZdmXbo1WfXhGuV1fFa9X1qJ9BGTFYUiWeX4ak83LcdHa','财务小赵','FINANCE','D001',TRUE
UNION ALL
SELECT 'd002mgr','$2a$10$0NkIZUTrB4qQ7H8QaVanyelSzpZLJkdXayMRz6tPjElzqsrZdc.We','杭州宏达-店总','DEALER_MANAGER','D002',TRUE
) u
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_user) t WHERE t.username = u.username);

-- IR 控制塔：草稿补货单可直接下发 OMS
INSERT INTO dms_replenish_order(replenish_no,dealer_code,status,source,items,remark,created_at,updated_at)
SELECT 'RPL-IR-DRAFT','D001','DRAFT','MANUAL',
       '[{"partNo":"P-IR-SHORT","name":"控制塔缺货演示件","qty":9,"price":25.00}]',
       'IR 控制塔草稿补货单',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM dms_replenish_order WHERE replenish_no='RPL-IR-DRAFT');

-- 保修索赔演示：一单待 OEM 审核，一单已核准待结算（含明细行）
INSERT INTO dms_warranty_claim(claim_no,order_id,dealer_code,vin,plate_no,mileage,repair_date,fault_code,fault_desc,amount,labor_amount,part_amount,parts_return_required,submitted_at,status,remark,created_at,updated_at)
SELECT 'WC-SEED-0001',NULL,'D001','LFV3A28K7J3000001','沪A12345',8000,DATE '2025-06-10','P0301','发动机缺火，更换点火线圈',680.00,180.00,500.00,FALSE,CURRENT_TIMESTAMP,'SUBMITTED','演示索赔单-待审核',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM dms_warranty_claim WHERE claim_no='WC-SEED-0001');
INSERT INTO dms_warranty_claim(claim_no,order_id,dealer_code,vin,plate_no,mileage,repair_date,fault_code,fault_desc,amount,labor_amount,part_amount,approved_amount,parts_return_required,submitted_at,approved_at,status,remark,created_at,updated_at)
SELECT 'WC-SEED-0002',NULL,'D001','LFV3A28K7J3000002','沪A67890',12000,DATE '2025-06-02','B1234','空调压缩机异响，更换压缩机',2200.00,400.00,1800.00,2100.00,FALSE,TIMESTAMPADD(DAY,-3,CURRENT_TIMESTAMP),TIMESTAMPADD(DAY,-1,CURRENT_TIMESTAMP),'APPROVED','演示索赔单-已核准',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM dms_warranty_claim WHERE claim_no='WC-SEED-0002');
INSERT INTO dms_warranty_claim_line(claim_id,line_type,code,name,qty,unit_price,amount,remark,created_at,updated_at)
SELECT c.id,'LABOR','L001','发动机诊断工时',1.0,180.00,180.00,'演示',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
FROM dms_warranty_claim c
WHERE c.claim_no='WC-SEED-0001'
  AND NOT EXISTS (SELECT 1 FROM dms_warranty_claim_line l WHERE l.claim_id=c.id AND l.line_type='LABOR');
INSERT INTO dms_warranty_claim_line(claim_id,line_type,code,name,qty,unit_price,amount,remark,created_at,updated_at)
SELECT c.id,'PART','P0001','点火线圈',1.0,500.00,500.00,'演示',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
FROM dms_warranty_claim c
WHERE c.claim_no='WC-SEED-0001'
  AND NOT EXISTS (SELECT 1 FROM dms_warranty_claim_line l WHERE l.claim_id=c.id AND l.line_type='PART');
INSERT INTO dms_warranty_claim_line(claim_id,line_type,code,name,qty,unit_price,amount,approved_amount,remark,created_at,updated_at)
SELECT c.id,'PART','P-AC-01','空调压缩机',1.0,1800.00,1800.00,1700.00,'演示',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
FROM dms_warranty_claim c
WHERE c.claim_no='WC-SEED-0002'
  AND NOT EXISTS (SELECT 1 FROM dms_warranty_claim_line l WHERE l.claim_id=c.id AND l.line_type='PART');
INSERT INTO dms_warranty_claim_line(claim_id,line_type,code,name,qty,unit_price,amount,approved_amount,remark,created_at,updated_at)
SELECT c.id,'LABOR','L-AC-01','压缩机拆装工时',2.0,200.00,400.00,400.00,'演示',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
FROM dms_warranty_claim c
WHERE c.claim_no='WC-SEED-0002'
  AND NOT EXISTS (SELECT 1 FROM dms_warranty_claim_line l WHERE l.claim_id=c.id AND l.line_type='LABOR');

-- 采购演示：一单厂家已确认，可直接演示到货入库
INSERT INTO dms_purchase_order(po_no,dealer_code,inquiry_id,source,status,total_amount,received_amount,expect_date,oem_order_no,submitted_at,confirmed_at,remark,created_at,updated_at)
SELECT 'PO-SEED-0001','D001',NULL,'MANUAL','CONFIRMED',2500.00,0.00,TIMESTAMPADD(DAY,14,CURRENT_DATE),'OEM-PO-90001',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'演示采购单-已确认',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM dms_purchase_order WHERE po_no='PO-SEED-0001');
INSERT INTO dms_purchase_order_line(order_id,part_no,name,qty,received_qty,unit_price,amount,remark,created_at,updated_at)
SELECT o.id,'P0001','点火线圈',4,0,500.00,2000.00,'演示',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
FROM dms_purchase_order o
WHERE o.po_no='PO-SEED-0001'
  AND NOT EXISTS (SELECT 1 FROM dms_purchase_order_line l WHERE l.order_id=o.id AND l.part_no='P0001');
INSERT INTO dms_purchase_order_line(order_id,part_no,name,qty,received_qty,unit_price,amount,remark,created_at,updated_at)
SELECT o.id,'P0002','机油滤清器',2,0,250.00,500.00,'演示',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
FROM dms_purchase_order o
WHERE o.po_no='PO-SEED-0001'
  AND NOT EXISTS (SELECT 1 FROM dms_purchase_order_line l WHERE l.order_id=o.id AND l.part_no='P0002');

-- ============ 通知模板（幂等） ============
INSERT INTO dms_notify_template(code,name,channel,content,enabled,created_at,updated_at)
SELECT 'MAINT_REMIND','保养提醒短信','SMS','【{{dealer}}】尊敬的{{name}}，您的爱车{{plate}}已临近保养期，请于{{date}}前回店保养。',TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM dms_notify_template WHERE code='MAINT_REMIND');
INSERT INTO dms_notify_template(code,name,channel,content,enabled,created_at,updated_at)
SELECT 'MAINT_REMIND_WX','保养提醒微信','WECHAT','{{name}}您好，{{plate}} 保养到期（{{date}}），请预约{{dealer}}。',TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM dms_notify_template WHERE code='MAINT_REMIND_WX');
INSERT INTO dms_notify_template(code,name,channel,content,enabled,created_at,updated_at)
SELECT 'SERVICE_FOLLOWUP','售后回访短信','SMS','【{{dealer}}】{{name}}您好，感谢您到店服务（{{plate}}），如有任何问题请随时联系我们。',TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM dms_notify_template WHERE code='SERVICE_FOLLOWUP');
INSERT INTO dms_notify_template(code,name,channel,content,enabled,created_at,updated_at)
SELECT 'COMPLAINT_FOLLOWUP','投诉跟进短信','SMS','【{{dealer}}】{{name}}您好，您的反馈我们已收到，服务顾问将在{{date}}前与您联系。',TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM dms_notify_template WHERE code='COMPLAINT_FOLLOWUP');
INSERT INTO dms_notify_template(code,name,channel,content,enabled,created_at,updated_at)
SELECT 'BIRTHDAY','生日关怀短信','SMS','【{{dealer}}】亲爱的{{name}}，生日快乐！本月到店可享专属礼遇。',TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM dms_notify_template WHERE code='BIRTHDAY');
INSERT INTO dms_notify_template(code,name,channel,content,enabled,created_at,updated_at)
SELECT 'SALES_FOLLOWUP','销售回访短信','SMS','【{{dealer}}】{{name}}您好，感谢您选购{{plate}}，用车如有任何问题请随时联系我们。',TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM dms_notify_template WHERE code='SALES_FOLLOWUP');
INSERT INTO dms_notify_template(code,name,channel,content,enabled,created_at,updated_at)
SELECT 'RENEWAL','续保提醒短信','SMS','【{{dealer}}】{{name}}您好，您的爱车{{plate}}保险将于{{date}}到期，欢迎联系我们办理续保。',TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM dms_notify_template WHERE code='RENEWAL');
