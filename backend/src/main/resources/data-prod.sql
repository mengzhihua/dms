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

-- 通知模板
INSERT INTO dms_notify_template(code,name,channel,content,enabled)
SELECT u.* FROM (
SELECT 'MAINT_REMIND' AS code,'保养提醒短信' AS name,'SMS' AS channel,'【{{dealer}}】尊敬的{{name}}，您的爱车{{plate}}已临近保养期，请于{{date}}前回店保养。' AS content,TRUE AS enabled
UNION ALL
SELECT 'MAINT_REMIND_WX','保养提醒微信','WECHAT','{{name}}您好，{{plate}} 保养到期（{{date}}），请预约{{dealer}}。',TRUE
UNION ALL
SELECT 'SERVICE_FOLLOWUP','售后回访短信','SMS','【{{dealer}}】{{name}}您好，感谢您到店服务（{{plate}}），如有任何问题请随时联系我们。',TRUE
UNION ALL
SELECT 'COMPLAINT_FOLLOWUP','投诉跟进短信','SMS','【{{dealer}}】{{name}}您好，您的反馈我们已收到，服务顾问将在{{date}}前与您联系。',TRUE
UNION ALL
SELECT 'BIRTHDAY','生日关怀短信','SMS','【{{dealer}}】亲爱的{{name}}，生日快乐！本月到店可享专属礼遇。',TRUE
) u
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM dms_notify_template) t WHERE t.code = u.code);
