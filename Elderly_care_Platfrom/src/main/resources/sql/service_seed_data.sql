-- =============================================================
-- 种子数据:演示商家与服务(2026-09-07 录入,非建表)
-- 目标:5 个服务分类各 2 家正常(1)商家,每家至少 2 个上架(1)服务
-- 现有:助餐=城志社区食堂(provider 1)、助洁=胜利家政(provider 2),本轮在其上补齐
-- 登录密码统一明文 123456a(密码明文存储为项目已定决策,可用 phone 登录演示)
-- 注意:uk_provider_phone 唯一,重复执行会撞键,勿重复执行本脚本
-- =============================================================

-- ===== 助餐服务(category 1):家和幸福餐厅 =====
INSERT INTO service_provider (category_id, provider_name, intro, phone, password, legal_person, province, city, district, address, status)
VALUES (1, '家和幸福餐厅', '社区便民餐厅，主打适合老年人口味与牙口的软烂营养餐，可送餐上门', '13811001234', '123456a', '王秀兰', '北京市', '北京市', '朝阳区', '朝阳区团结湖北里 12 号楼底商', 1);
SET @p = LAST_INSERT_ID();
INSERT INTO service_item (provider_id, category_id, item_name, price, unit, duration, detail, sort, status) VALUES
(@p, 1, '两荤一素营养套餐', 22.00, '份', '约30分钟', '两荤一素一汤，米饭软硬可选，少油少盐适合老人', 0, 1),
(@p, 1, '软食流食定制餐', 25.00, '份', '约30分钟', '粥羹、肉沫糊等易吞咽餐食，可按医嘱口味定制', 1, 1),
(@p, 1, '无糖杂粮早餐', 8.00, '份', '约20分钟', '杂粮粥配无糖点心与鸡蛋，适合糖尿病老人', 2, 1);

-- ===== 助洁服务(category 2):洁馨家政 =====
INSERT INTO service_provider (category_id, provider_name, intro, phone, password, legal_person, province, city, district, address, status)
VALUES (2, '洁馨家政', '专业持证保洁团队，老人居家环境深度清洁，工具消毒上门', '13901002345', '123456a', '赵国强', '北京市', '北京市', '海淀区', '海淀区知春里小区 3 号楼', 1);
SET @p = LAST_INSERT_ID();
INSERT INTO service_item (provider_id, category_id, item_name, price, unit, duration, detail, sort, status) VALUES
(@p, 2, '全屋日常保洁', 150.00, '次', '约3小时', '全屋除尘拖地、台面整理、厨卫清洁', 0, 1),
(@p, 2, '厨房深度除油', 120.00, '次', '约2小时', '灶台墙面重油污深度清理，油烟机表面除油', 1, 1),
(@p, 2, '换季衣物整理', 100.00, '次', '约2小时', '衣物分类收纳、换季打包、防潮除味', 2, 1);

-- ===== 助浴服务(category 3):安心助浴 =====
INSERT INTO service_provider (category_id, provider_name, intro, phone, password, legal_person, province, city, district, address, status)
VALUES (3, '安心助浴', '持证助浴师上门，为失能半失能老人提供安全沐浴服务', '13701234567', '123456a', '陈丽华', '北京市', '北京市', '东城区', '东城区东直门北小街 8 号院', 1);
SET @p = LAST_INSERT_ID();
INSERT INTO service_item (provider_id, category_id, item_name, price, unit, duration, detail, sort, status) VALUES
(@p, 3, '卧床老人上门助浴', 260.00, '次', '约90分钟', '充气浴槽上门助浴，浴前测血压，两名助浴师全程照护', 0, 1),
(@p, 3, '半自理老人坐浴', 180.00, '次', '约60分钟', '坐浴椅辅助沐浴，含洗头、修剪指甲', 1, 1);

-- ===== 助浴服务(category 3):清泉浴乐助老 =====
INSERT INTO service_provider (category_id, provider_name, intro, phone, password, legal_person, province, city, district, address, status)
VALUES (3, '清泉浴乐助老', '社区助浴服务站，到店与上门洗浴均可预约，设施适老化', '15810335566', '123456a', '张淑芳', '北京市', '北京市', '西城区', '西城区德胜门外大街 45 号', 1);
SET @p = LAST_INSERT_ID();
INSERT INTO service_item (provider_id, category_id, item_name, price, unit, duration, detail, sort, status) VALUES
(@p, 3, '上门洗浴护理', 200.00, '次', '约60分钟', '到店或上门洗浴护理，含全身清洁与洗头', 0, 1),
(@p, 3, '理发加洗浴套餐', 120.00, '次', '约60分钟', '理发修面加洗浴一体，行动不便可预约上门', 1, 1);

-- ===== 助医服务(category 4):康宁陪诊 =====
INSERT INTO service_provider (category_id, provider_name, intro, phone, password, legal_person, province, city, district, address, status)
VALUES (4, '康宁陪诊', '专业陪诊团队，陪同挂号取药、代跑医院流程，子女不在身边更安心', '13612004567', '123456a', '刘志远', '北京市', '北京市', '丰台区', '丰台区方庄芳群园小区', 1);
SET @p = LAST_INSERT_ID();
INSERT INTO service_item (provider_id, category_id, item_name, price, unit, duration, detail, sort, status) VALUES
(@p, 4, '医院全程陪诊', 150.00, '次', '半天', '挂号缴费取报告全程陪同，帮助记录医嘱', 0, 1),
(@p, 4, '代取药送药上门', 50.00, '次', '约1小时', '凭处方代取药、代开慢病常用药并送药上门', 1, 1),
(@p, 4, '就医专车接送', 80.00, '次', '单程', '无障碍车辆预约接送，陪同下车进院', 2, 1);

-- ===== 助医服务(category 4):仁和社区健康站 =====
INSERT INTO service_provider (category_id, provider_name, intro, phone, password, legal_person, province, city, district, address, status)
VALUES (4, '仁和社区健康站', '社区卫生服务延伸，注册护士上门提供基础护理', '18601331234', '123456a', '周玉梅', '北京市', '北京市', '大兴区', '大兴区黄村西里社区服务站', 1);
SET @p = LAST_INSERT_ID();
INSERT INTO service_item (provider_id, category_id, item_name, price, unit, duration, detail, sort, status) VALUES
(@p, 4, '上门换药护理', 120.00, '次', '约40分钟', '护士上门伤口消毒换药，压疮护理指导', 0, 1),
(@p, 4, '血压血糖上门监测', 60.00, '次', '约30分钟', '上门测量血压血糖，记录数值并口头反馈健康建议', 1, 1);

-- ===== 康复护理(category 5):颐康康复理疗馆 =====
INSERT INTO service_provider (category_id, provider_name, intro, phone, password, legal_person, province, city, district, address, status)
VALUES (5, '颐康康复理疗馆', '康复理疗师上门服务，缓解老人颈肩腰腿痛，调理慢病', '13522116677', '123456a', '孙建国', '北京市', '北京市', '通州区', '通州区玉桥中路 6 号', 1);
SET @p = LAST_INSERT_ID();
INSERT INTO service_item (provider_id, category_id, item_name, price, unit, duration, detail, sort, status) VALUES
(@p, 5, '上门推拿理疗', 180.00, '次', '约60分钟', '颈肩腰腿部手法推拿，缓解肌肉酸痛僵硬', 0, 1),
(@p, 5, '艾灸温养理疗', 150.00, '次', '约60分钟', '关节怕冷、体虚体质的艾灸调理，含施灸环境评估', 1, 1),
(@p, 5, '关节功能康复训练', 220.00, '次', '约60分钟', '康复师上门评估后一对一制定训练方案并指导', 2, 1);

-- ===== 既有商家补服务(助洁 category 2:胜利家政 provider_id=2,原仅 1 个服务,补齐至 2 个) =====
INSERT INTO service_item (provider_id, category_id, item_name, price, unit, duration, detail, sort, status) VALUES
(2, 2, '卫生间除垢消毒', 90.00, '次', '约1.5小时', '马桶浴缸水垢清理、瓷砖除霉、整体消毒', 1, 1);

-- ===== 康复护理(category 5):舒心运动康复 =====
INSERT INTO service_provider (category_id, provider_name, intro, phone, password, legal_person, province, city, district, address, status)
VALUES (5, '舒心运动康复', '运动康复指导，帮助术后与慢病老人恢复日常活动能力', '17788110099', '123456a', '李秀英', '北京市', '北京市', '房山区', '房山区良乡拱辰大街 28 号', 1);
SET @p = LAST_INSERT_ID();
INSERT INTO service_item (provider_id, category_id, item_name, price, unit, duration, detail, sort, status) VALUES
(@p, 5, '居家康复锻炼指导', 160.00, '次', '约60分钟', '评估活动能力后制定居家锻炼计划并现场示范', 0, 1),
(@p, 5, '术后恢复评估', 100.00, '次', '约40分钟', '术后活动能力评估，恢复进度跟踪建档', 1, 1);
