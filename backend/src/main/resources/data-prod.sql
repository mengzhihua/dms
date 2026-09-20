-- ============ 满意度模板：1 套售后模板 5 题 ============
INSERT INTO dms_survey_template(code,name,type) VALUES ('SV01','售后服务满意度调研','SERVICE');
INSERT INTO dms_survey_question(template_id,seq,text,type,weight) VALUES
(1,1,'您对本次维修/保养的总体满意度评分（1-10分）','SCORE',0.3),
(1,2,'服务顾问接待与沟通满意度（1-10分）','SCORE',0.2),
(1,3,'维修质量与一次性修复满意度（1-10分）','SCORE',0.3),
(1,4,'交车及时性与车辆清洁满意度（1-10分）','SCORE',0.2),
(1,5,'您愿意向亲友推荐本店吗（0-10分）','NPS',1.0);
INSERT INTO dms_survey_template(code,name,type) VALUES ('SV02','新车销售满意度调研','SALES');
INSERT INTO dms_survey_question(template_id,seq,text,type,weight) VALUES
((SELECT id FROM dms_survey_template WHERE code='SV02'),1,'您对购车顾问服务的满意度（1-10分）','SCORE',0.3),
((SELECT id FROM dms_survey_template WHERE code='SV02'),2,'您对交车流程与PDI检查的满意度（1-10分）','SCORE',0.3),
((SELECT id FROM dms_survey_template WHERE code='SV02'),3,'您对金融/保险服务的满意度（1-10分）','SCORE',0.2),
((SELECT id FROM dms_survey_template WHERE code='SV02'),4,'您愿意向亲友推荐本店购车吗（0-10分）','NPS',1.0);

-- ============ 税率配置 ============
INSERT INTO dms_tax_config(dealer_code,tax_rate,seller_name,seller_tax_no) VALUES
('D001',0.13,'上海申联汽车4S店','91310000MA1K00001A'),
('D002',0.13,'杭州宏达汽车4S店','91330100MA2K00002B'),
('S001',0.13,'品牌直营上海旗舰店','91310000MA1K00003C');
