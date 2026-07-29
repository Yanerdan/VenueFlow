SET NAMES utf8mb4;
SET time_zone = '+00:00';
USE venueflow_resource;
START TRANSACTION;

-- This fixture is synthetic. Reserved identifiers make it safe to reseed without touching user data.
INSERT INTO venueflow_user.user_profile
  (external_user_id, display_name, campus_id, identity_type, department, phone, email,
   account_status, booking_eligibility, version, created_at, updated_at)
SELECT credentials.user_id, '陈安（申请人演示）', 'DEMO-S2026099', 'STUDENT', '计算机学院',
       '13800001999', 'demo.applicant@example.edu.cn', 'ACTIVE', 'ELIGIBLE', 2,
       NOW() - INTERVAL 168 DAY, NOW() - INTERVAL 2 DAY
FROM venueflow_auth.auth_credentials credentials
WHERE credentials.username = 'campus.user'
ON DUPLICATE KEY UPDATE
  display_name = VALUES(display_name), campus_id = VALUES(campus_id),
  identity_type = VALUES(identity_type), department = VALUES(department),
  phone = VALUES(phone), email = VALUES(email), account_status = 'ACTIVE',
  booking_eligibility = 'ELIGIBLE', updated_at = VALUES(updated_at);

INSERT INTO venueflow_user.user_profile
  (external_user_id, display_name, campus_id, identity_type, department, phone, email,
   account_status, booking_eligibility, version, created_at, updated_at)
VALUES
  ('showcase-applicant-01', '林晨（演示）', 'DEMO-S2026001', 'STUDENT', '计算机学院', '13800001001', 'demo.s001@example.edu.cn', 'ACTIVE', 'ELIGIBLE', 3, NOW() - INTERVAL 190 DAY, NOW() - INTERVAL 18 DAY),
  ('showcase-applicant-02', '周宁（演示）', 'DEMO-S2026002', 'STUDENT', '商学院', '13800001002', 'demo.s002@example.edu.cn', 'ACTIVE', 'ELIGIBLE', 2, NOW() - INTERVAL 185 DAY, NOW() - INTERVAL 30 DAY),
  ('showcase-applicant-03', '许清（演示）', 'DEMO-S2026003', 'STUDENT', '建筑与艺术学院', '13800001003', 'demo.s003@example.edu.cn', 'ACTIVE', 'ELIGIBLE', 4, NOW() - INTERVAL 180 DAY, NOW() - INTERVAL 8 DAY),
  ('showcase-applicant-04', '陈嘉（演示）', 'DEMO-S2026004', 'STUDENT', '外国语学院', '13800001004', 'demo.s004@example.edu.cn', 'ACTIVE', 'ELIGIBLE', 2, NOW() - INTERVAL 175 DAY, NOW() - INTERVAL 21 DAY),
  ('showcase-applicant-05', '沈悦（演示）', 'DEMO-S2026005', 'STUDENT', '法学院', '13800001005', 'demo.s005@example.edu.cn', 'ACTIVE', 'ELIGIBLE', 1, NOW() - INTERVAL 170 DAY, NOW() - INTERVAL 45 DAY),
  ('showcase-applicant-06', '唐宇（演示）', 'DEMO-S2026006', 'STUDENT', '公共管理学院', '13800001006', 'demo.s006@example.edu.cn', 'ACTIVE', 'ELIGIBLE', 3, NOW() - INTERVAL 165 DAY, NOW() - INTERVAL 12 DAY),
  ('showcase-applicant-07', '蒋怡（演示）', 'DEMO-S2026007', 'STUDENT', '新闻与传播学院', '13800001007', 'demo.s007@example.edu.cn', 'ACTIVE', 'ELIGIBLE', 2, NOW() - INTERVAL 160 DAY, NOW() - INTERVAL 36 DAY),
  ('showcase-applicant-08', '韩卓（演示）', 'DEMO-S2026008', 'STUDENT', '电子信息学院', '13800001008', 'demo.s008@example.edu.cn', 'ACTIVE', 'ELIGIBLE', 4, NOW() - INTERVAL 155 DAY, NOW() - INTERVAL 6 DAY),
  ('showcase-applicant-09', '罗欣（演示）', 'DEMO-S2026009', 'STUDENT', '马克思主义学院', '13800001009', 'demo.s009@example.edu.cn', 'ACTIVE', 'ELIGIBLE', 1, NOW() - INTERVAL 150 DAY, NOW() - INTERVAL 60 DAY),
  ('showcase-applicant-10', '方可（演示）', 'DEMO-S2026010', 'STUDENT', '数学与统计学院', '13800001010', 'demo.s010@example.edu.cn', 'ACTIVE', 'ELIGIBLE', 2, NOW() - INTERVAL 145 DAY, NOW() - INTERVAL 25 DAY),
  ('showcase-applicant-11', '顾言（演示）', 'DEMO-T2026001', 'STAFF', '学生工作部', '13800001011', 'demo.t001@example.edu.cn', 'ACTIVE', 'ELIGIBLE', 3, NOW() - INTERVAL 210 DAY, NOW() - INTERVAL 4 DAY),
  ('showcase-applicant-12', '叶青（演示）', 'DEMO-T2026002', 'STAFF', '校团委', '13800001012', 'demo.t002@example.edu.cn', 'ACTIVE', 'ELIGIBLE', 3, NOW() - INTERVAL 205 DAY, NOW() - INTERVAL 10 DAY),
  ('showcase-approver-01', '王老师（演示审批）', 'DEMO-A2026001', 'STAFF', '学生工作部', '13800001101', 'demo.a001@example.edu.cn', 'ACTIVE', 'ELIGIBLE', 2, NOW() - INTERVAL 260 DAY, NOW() - INTERVAL 3 DAY),
  ('showcase-approver-02', '赵老师（演示审批）', 'DEMO-A2026002', 'STAFF', '教务处', '13800001102', 'demo.a002@example.edu.cn', 'ACTIVE', 'ELIGIBLE', 2, NOW() - INTERVAL 250 DAY, NOW() - INTERVAL 7 DAY),
  ('showcase-approver-03', '孙老师（演示审批）', 'DEMO-A2026003', 'STAFF', '体育部', '13800001103', 'demo.a003@example.edu.cn', 'ACTIVE', 'ELIGIBLE', 1, NOW() - INTERVAL 240 DAY, NOW() - INTERVAL 15 DAY),
  ('showcase-admin-01', '校级管理员（演示）', 'DEMO-ADM-001', 'STAFF', '学校办公室', '13800001199', 'demo.admin@example.edu.cn', 'ACTIVE', 'ELIGIBLE', 5, NOW() - INTERVAL 300 DAY, NOW() - INTERVAL 1 DAY)
ON DUPLICATE KEY UPDATE
  display_name = VALUES(display_name), campus_id = VALUES(campus_id),
  identity_type = VALUES(identity_type), department = VALUES(department),
  phone = VALUES(phone), email = VALUES(email), account_status = 'ACTIVE',
  booking_eligibility = 'ELIGIBLE', updated_at = VALUES(updated_at);

INSERT INTO venueflow_resource.resource_category (code, name)
VALUES
  ('SHOW-TEACHING', '教学与研讨空间'),
  ('SHOW-MEETING', '会议与行政空间'),
  ('SHOW-ACTIVITY', '学生活动空间'),
  ('SHOW-SPORT', '体育与艺术空间'),
  ('SHOW-SERVICE', '公共服务空间')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- Retire only known engineering fixtures from the applicant catalogue.
UPDATE venueflow_resource.resource
SET status = 'ARCHIVED', updated_at = NOW()
WHERE resource_no IN ('ROOM-A-101', 'VF-DEMO-001', 'VF-DEMO-002', 'VF-DEMO-003')
   OR resource_no LIKE 'C04-%'
   OR resource_no LIKE 'SLOT-%'
   OR resource_no LIKE 'MANUAL-%';

INSERT INTO venueflow_resource.resource
  (resource_no, category_id, name, description, location, capacity, owner_department,
   approver_external_user_id, approval_mode, final_approver_external_user_id,
   booking_notice, min_advance_hours, max_advance_days, max_duration_minutes,
   status, version, created_at, updated_at)
VALUES
  ('VF-CAMPUS-001', (SELECT id FROM venueflow_resource.resource_category WHERE code='SHOW-TEACHING'),
   '新校区图书馆研讨室 A', '配备智慧屏和白板，适合课程研讨、课题组例会与小型答辩。',
   '新校区图书馆 3 层 A 区', 18, '图书馆', 'showcase-approver-02', 'DIRECT', NULL,
   '请保持安静；使用智慧屏需自备 Type-C 转接设备。', 2, 30, 180, 'ACTIVE', 6, NOW() - INTERVAL 220 DAY, NOW() - INTERVAL 6 DAY),
  ('VF-CAMPUS-002', (SELECT id FROM venueflow_resource.resource_category WHERE code='SHOW-TEACHING'),
   '信息楼智慧教室 302', '支持录播、无线投屏和分组教学，可用于公开课与教学培训。',
   '信息楼 3 层 302', 64, '教务处', 'showcase-approver-02', 'TWO_STAGE', 'showcase-admin-01',
   '公开课须在备注中说明课程名称；设备使用前联系楼宇管理员。', 24, 45, 240, 'ACTIVE', 8, NOW() - INTERVAL 215 DAY, NOW() - INTERVAL 2 DAY),
  ('VF-CAMPUS-003', (SELECT id FROM venueflow_resource.resource_category WHERE code='SHOW-MEETING'),
   '本部会议中心第一会议室', '校级会议与跨部门协调会议使用，支持视频会议和同声传译接入。',
   '本部办公楼 2 层', 36, '学校办公室', 'showcase-approver-01', 'TWO_STAGE', 'showcase-admin-01',
   '仅限校内公务活动；请提前上传会议议程并预留布场时间。', 48, 60, 240, 'ACTIVE', 11, NOW() - INTERVAL 230 DAY, NOW() - INTERVAL 1 DAY),
  ('VF-CAMPUS-004', (SELECT id FROM venueflow_resource.resource_category WHERE code='SHOW-ACTIVITY'),
   '学生活动中心多功能厅', '适合社团展演、讲座、迎新和中型学生组织活动。',
   '学生活动中心 1 层', 220, '学生工作部', 'showcase-approver-01', 'TWO_STAGE', 'showcase-admin-01',
   '涉及舞台、音响或校外嘉宾时请在申请中说明；活动结束后须恢复场地。', 72, 60, 360, 'ACTIVE', 14, NOW() - INTERVAL 225 DAY, NOW() - INTERVAL 3 DAY),
  ('VF-CAMPUS-005', (SELECT id FROM venueflow_resource.resource_category WHERE code='SHOW-ACTIVITY'),
   '校团委创新工坊', '面向学生团队的共创空间，提供移动桌椅、展示墙和基础直播设备。',
   '大学生活动中心 4 层', 42, '校团委', 'showcase-approver-01', 'DIRECT', NULL,
   '优先支持校级学生组织和创新创业项目；材料制作请使用防护垫。', 12, 30, 240, 'ACTIVE', 9, NOW() - INTERVAL 205 DAY, NOW() - INTERVAL 5 DAY),
  ('VF-CAMPUS-006', (SELECT id FROM venueflow_resource.resource_category WHERE code='SHOW-SPORT'),
   '体育馆羽毛球场 1–2 号', '两片标准羽毛球场地，适合院系训练、教职工活动与小型比赛。',
   '东区体育馆主馆', 24, '体育部', 'showcase-approver-03', 'DIRECT', NULL,
   '请穿着室内运动鞋；比赛用网架和记分牌需提前备注。', 4, 14, 120, 'ACTIVE', 7, NOW() - INTERVAL 200 DAY, NOW() - INTERVAL 4 DAY),
  ('VF-CAMPUS-007', (SELECT id FROM venueflow_resource.resource_category WHERE code='SHOW-SPORT'),
   '艺术楼黑匣子排练厅', '配备基础灯光、镜墙和音响，适用于戏剧、舞蹈与语言类节目排练。',
   '艺术楼 B1 层', 80, '建筑与艺术学院', 'showcase-approver-01', 'DIRECT', NULL,
   '禁止使用明火、喷雾和难以清理的舞美材料。', 24, 21, 240, 'ACTIVE', 5, NOW() - INTERVAL 190 DAY, NOW() - INTERVAL 9 DAY),
  ('VF-CAMPUS-008', (SELECT id FROM venueflow_resource.resource_category WHERE code='SHOW-SERVICE'),
   '心理健康中心团体辅导室', '私密、安静的团体活动空间，支持团辅、沙龙和朋辈培训。',
   '学生服务中心 2 层 205', 20, '学生工作部', 'showcase-approver-01', 'DIRECT', NULL,
   '申请用途须与学生发展或心理健康教育相关，现场禁止拍摄。', 24, 21, 150, 'ACTIVE', 6, NOW() - INTERVAL 185 DAY, NOW() - INTERVAL 11 DAY),
  ('VF-CAMPUS-009', (SELECT id FROM venueflow_resource.resource_category WHERE code='SHOW-SERVICE'),
   '就业指导中心宣讲厅', '支持用人单位宣讲、就业政策解读和职业发展课程。',
   '学生服务中心 3 层', 140, '就业指导中心', 'showcase-approver-01', 'TWO_STAGE', 'showcase-admin-01',
   '校外单位须由校内部门联合申请；请注明单位名称和入校人员规模。', 72, 90, 300, 'ACTIVE', 12, NOW() - INTERVAL 210 DAY, NOW() - INTERVAL 2 DAY),
  ('VF-CAMPUS-010', (SELECT id FROM venueflow_resource.resource_category WHERE code='SHOW-MEETING'),
   '国际交流中心会客厅', '用于国际来访接待、外事会谈和小型文化交流活动。',
   '国际交流中心 1 层', 28, '国际合作处', 'showcase-approver-02', 'TWO_STAGE', 'showcase-admin-01',
   '请提前提交来访单位、人员名单及活动语言需求。', 72, 60, 180, 'SUSPENDED', 4, NOW() - INTERVAL 175 DAY, NOW() - INTERVAL 1 DAY)
ON DUPLICATE KEY UPDATE
  category_id = VALUES(category_id), name = VALUES(name), description = VALUES(description),
  location = VALUES(location), capacity = VALUES(capacity),
  owner_department = VALUES(owner_department),
  approver_external_user_id = VALUES(approver_external_user_id),
  approval_mode = VALUES(approval_mode),
  final_approver_external_user_id = VALUES(final_approver_external_user_id),
  booking_notice = VALUES(booking_notice), min_advance_hours = VALUES(min_advance_hours),
  max_advance_days = VALUES(max_advance_days), max_duration_minutes = VALUES(max_duration_minutes),
  status = VALUES(status), version = VALUES(version), updated_at = VALUES(updated_at);

-- Remove only unreferenced generated slots so advancing the calendar does not accumulate stale choices.
DELETE slot
FROM venueflow_resource.resource_slot slot
JOIN venueflow_resource.resource resource ON resource.id = slot.resource_id
WHERE resource.resource_no LIKE 'VF-CAMPUS-%'
  AND slot.id < 900000
  AND NOT EXISTS (
    SELECT 1 FROM venueflow_booking.booking_reservation booking WHERE booking.slot_id = slot.id
  )
  AND NOT EXISTS (
    SELECT 1 FROM venueflow_resource.resource_slot_allocation allocation WHERE allocation.slot_id = slot.id
  );

INSERT IGNORE INTO venueflow_resource.resource_slot
  (resource_id, start_at, end_at, status, allocated_quantity, version, created_at, updated_at)
WITH RECURSIVE days(day_offset) AS (
  SELECT 3 UNION ALL SELECT day_offset + 4 FROM days WHERE day_offset < 31
)
SELECT r.id,
       DATE_ADD(DATE_ADD(UTC_DATE(), INTERVAL days.day_offset DAY),
                INTERVAL (CASE WHEN MOD(r.id, 2)=0 THEN 1 ELSE 6 END) HOUR),
       DATE_ADD(DATE_ADD(UTC_DATE(), INTERVAL days.day_offset DAY),
                INTERVAL (CASE WHEN MOD(r.id, 2)=0 THEN 3 ELSE 8 END) HOUR),
       'OPEN', 0, 1, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
FROM venueflow_resource.resource r
CROSS JOIN days
WHERE r.resource_no LIKE 'VF-CAMPUS-%' AND r.status = 'ACTIVE';

-- Materialize the use periods referenced by semester history so applicant cards can resolve them.
INSERT INTO venueflow_resource.resource_slot
  (id, resource_id, start_at, end_at, status, allocated_quantity, version, created_at, updated_at)
WITH RECURSIVE seq(n) AS (
  SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 72
),
resources AS (
  SELECT id, ROW_NUMBER() OVER (ORDER BY resource_no) rn
  FROM venueflow_resource.resource
  WHERE resource_no LIKE 'VF-CAMPUS-%' AND status = 'ACTIVE'
)
SELECT 900000 + n, resource.id,
       CASE WHEN n <= 58
            THEN DATE_ADD(DATE_ADD(DATE_SUB(UTC_DATE(), INTERVAL (122 - FLOOR(n * 116 / 72)) DAY), INTERVAL 2 DAY), INTERVAL 6 HOUR)
            ELSE DATE_ADD(DATE_ADD(UTC_DATE(), INTERVAL (3 + (n - 59) * 2) DAY), INTERVAL 5 HOUR) END,
       CASE WHEN n <= 58
            THEN DATE_ADD(DATE_ADD(DATE_SUB(UTC_DATE(), INTERVAL (122 - FLOOR(n * 116 / 72)) DAY), INTERVAL 2 DAY), INTERVAL 8 HOUR)
            ELSE DATE_ADD(DATE_ADD(UTC_DATE(), INTERVAL (3 + (n - 59) * 2) DAY), INTERVAL 7 HOUR) END,
       'CLOSED', 0,
       1, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
FROM seq
JOIN resources resource ON resource.rn = 1 + MOD(n * n + 2 * n, 9)
ON DUPLICATE KEY UPDATE
  resource_id = VALUES(resource_id), start_at = VALUES(start_at), end_at = VALUES(end_at),
  status = VALUES(status), allocated_quantity = VALUES(allocated_quantity), updated_at = VALUES(updated_at);

-- Recreate only the reserved semester history.
DELETE FROM venueflow_booking.booking_approval_action
WHERE booking_id IN (
  SELECT id FROM venueflow_booking.booking_reservation WHERE booking_no LIKE 'VF-SHOW-%'
);
DELETE FROM venueflow_booking.booking_status_log
WHERE booking_id IN (
  SELECT id FROM venueflow_booking.booking_reservation WHERE booking_no LIKE 'VF-SHOW-%'
);
DELETE FROM venueflow_booking.booking_reservation WHERE booking_no LIKE 'VF-SHOW-%';
DELETE FROM venueflow_notification.notification_record WHERE booking_no LIKE 'VF-SHOW-%';

INSERT INTO venueflow_booking.booking_reservation
  (booking_no, request_id, user_id, slot_id, resource_id, owner_department,
   assigned_approver_external_user_id, approval_mode,
   final_assigned_approver_external_user_id, current_approval_step, quantity,
   activity_title, application_purpose, contact_name, contact_phone, application_note,
   status, allocation_operation_id, release_operation_id, version, created_at,
   expire_at, confirmed_at, cancelled_at, expired_at, completed_at, terminal_reason,
   review_decision, review_note, reviewer_role, reviewed_at, timeout_state,
   timeout_attempt_count, updated_at)
WITH RECURSIVE seq(n) AS (
  SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 72
),
applicants AS (
  SELECT id, display_name, phone, ROW_NUMBER() OVER (ORDER BY external_user_id) rn
  FROM venueflow_user.user_profile WHERE external_user_id LIKE 'showcase-applicant-%'
),
resources AS (
  SELECT id, owner_department, approver_external_user_id, approval_mode,
         final_approver_external_user_id,
         ROW_NUMBER() OVER (ORDER BY resource_no) rn
  FROM venueflow_resource.resource WHERE resource_no LIKE 'VF-CAMPUS-%'
)
SELECT
  CONCAT('VF-SHOW-2026-', LPAD(n, 4, '0')),
  CONCAT('10000000-0000-4000-8000-', LPAD(n, 12, '0')),
  a.id, 900000 + n, r.id, r.owner_department, r.approver_external_user_id,
  r.approval_mode, r.final_approver_external_user_id,
  CASE WHEN n > 66 AND r.approval_mode='TWO_STAGE' THEN 2 ELSE 1 END,
  8 + MOD(n * 7, 73),
  ELT(1 + MOD(n - 1, 10),
      '学院学术交流沙龙', '学生骨干培训', '课程项目集中研讨', '就业能力提升讲座',
      '社团年度成果展示', '跨学院创新工作坊', '教职工文体活动', '朋辈辅导技能培训',
      '国际文化交流分享会', '毕业设计联合评审'),
  ELT(1 + MOD(n - 1, 6),
      '开展面向师生的主题交流与协作活动', '组织院系学生发展与能力提升项目',
      '完成课程实践和项目阶段成果讨论', '提供就业与职业发展公共服务',
      '支持校园文化建设和学生社团活动', '推进跨部门协同与专题工作研讨'),
  a.display_name, COALESCE(a.phone, '13800001999'),
  CASE WHEN MOD(n, 5)=0 THEN '需要使用投影与无线麦克风，活动后恢复桌椅。' ELSE NULL END,
  CASE WHEN n <= 44 THEN 'COMPLETED' WHEN n <= 52 THEN 'CANCELLED'
       WHEN n <= 58 THEN 'EXPIRED' WHEN n <= 66 THEN 'CONFIRMED'
       ELSE 'PENDING_CONFIRMATION' END,
  CONCAT('showcase-allocate-', LPAD(n, 4, '0')),
  CONCAT('showcase-release-', LPAD(n, 4, '0')),
  CASE WHEN n <= 66 THEN 2 ELSE 0 END,
  DATE_SUB(UTC_TIMESTAMP(6), INTERVAL (122 - FLOOR(n * 116 / 72)) DAY),
  DATE_ADD(DATE_SUB(UTC_TIMESTAMP(6), INTERVAL (122 - FLOOR(n * 116 / 72)) DAY), INTERVAL 15 MINUTE),
  CASE WHEN n <= 52 OR (n BETWEEN 59 AND 66)
       THEN DATE_ADD(DATE_SUB(UTC_TIMESTAMP(6), INTERVAL (122 - FLOOR(n * 116 / 72)) DAY), INTERVAL 6 HOUR) END,
  CASE WHEN n BETWEEN 45 AND 52
       THEN DATE_ADD(DATE_SUB(UTC_TIMESTAMP(6), INTERVAL (122 - FLOOR(n * 116 / 72)) DAY), INTERVAL 8 HOUR) END,
  CASE WHEN n BETWEEN 53 AND 58
       THEN DATE_ADD(DATE_SUB(UTC_TIMESTAMP(6), INTERVAL (122 - FLOOR(n * 116 / 72)) DAY), INTERVAL 16 MINUTE) END,
  CASE WHEN n <= 44
       THEN DATE_ADD(DATE_SUB(UTC_TIMESTAMP(6), INTERVAL (122 - FLOOR(n * 116 / 72)) DAY), INTERVAL 2 DAY) END,
  CASE WHEN n BETWEEN 45 AND 52 THEN CASE WHEN MOD(n,2)=0 THEN 'REJECTED' ELSE 'APPLICANT_CANCELLED' END
       WHEN n BETWEEN 53 AND 58 THEN 'CONFIRMATION_TIMEOUT'
       WHEN n <= 44 THEN 'CHECKED_IN' END,
  CASE WHEN n <= 44 OR (n BETWEEN 59 AND 66) THEN 'APPROVED'
       WHEN n BETWEEN 45 AND 52 AND MOD(n,2)=0 THEN 'REJECTED' END,
  CASE WHEN n <= 44 OR (n BETWEEN 59 AND 66) THEN
         ELT(1 + MOD(n,3), '材料完整，场地安排无冲突。', '活动方案清晰，同意按计划使用。', '已核对用途与安全要求，同意申请。')
       WHEN n BETWEEN 45 AND 52 AND MOD(n,2)=0 THEN
         ELT(1 + MOD(n,3), '申请时段与校级活动冲突。', '用途说明不完整，请补充后重新申请。', '该场地不适合本次活动规模。') END,
  CASE WHEN n <= 66 AND n NOT BETWEEN 53 AND 58 THEN 'APPROVER' END,
  CASE WHEN n <= 52 OR (n BETWEEN 59 AND 66)
       THEN DATE_ADD(DATE_SUB(UTC_TIMESTAMP(6), INTERVAL (122 - FLOOR(n * 116 / 72)) DAY), INTERVAL 6 HOUR) END,
  'COMPLETED', 0,
  CASE WHEN n <= 44 THEN DATE_ADD(DATE_SUB(UTC_TIMESTAMP(6), INTERVAL (122 - FLOOR(n * 116 / 72)) DAY), INTERVAL 2 DAY)
       ELSE DATE_ADD(DATE_SUB(UTC_TIMESTAMP(6), INTERVAL (122 - FLOOR(n * 116 / 72)) DAY), INTERVAL 8 HOUR) END
FROM seq
JOIN applicants a ON a.rn = 1 + MOD(n - 1, 12)
JOIN resources r ON r.rn = 1 + MOD(n * n + 2 * n, 9);

-- Bind one persona's representative history to the real login-ready applicant account.
UPDATE venueflow_booking.booking_reservation booking
JOIN venueflow_user.user_profile source_profile
  ON source_profile.id = booking.user_id
 AND source_profile.external_user_id = 'showcase-applicant-01'
JOIN venueflow_auth.auth_credentials credentials
  ON credentials.username = 'campus.user'
JOIN venueflow_user.user_profile demo_profile
  ON demo_profile.external_user_id = credentials.user_id
SET booking.user_id = demo_profile.id,
    booking.contact_name = demo_profile.display_name,
    booking.contact_phone = demo_profile.phone
WHERE booking.booking_no LIKE 'VF-SHOW-%';

INSERT INTO venueflow_booking.booking_approval_action
  (booking_id, approval_step, actor_external_user_id, actor_role, decision, review_note, created_at)
SELECT b.id, 1, b.assigned_approver_external_user_id, 'APPROVER',
       CASE WHEN b.review_decision='REJECTED' THEN 'REJECTED' ELSE 'APPROVED' END,
       b.review_note, COALESCE(b.reviewed_at, b.updated_at)
FROM venueflow_booking.booking_reservation b
WHERE b.booking_no LIKE 'VF-SHOW-%'
  AND b.status IN ('COMPLETED', 'CONFIRMED', 'CANCELLED')
  AND (b.status <> 'CANCELLED' OR b.review_decision='REJECTED');

INSERT INTO venueflow_booking.booking_approval_action
  (booking_id, approval_step, actor_external_user_id, actor_role, decision, review_note, created_at)
SELECT b.id, 2, b.final_assigned_approver_external_user_id, 'SYSTEM_ADMIN', 'APPROVED',
       '终审已核对校级活动安排与场地使用要求。',
       DATE_ADD(b.reviewed_at, INTERVAL 30 MINUTE)
FROM venueflow_booking.booking_reservation b
WHERE b.booking_no LIKE 'VF-SHOW-%'
  AND b.approval_mode='TWO_STAGE'
  AND b.status IN ('COMPLETED', 'CONFIRMED');

INSERT INTO venueflow_notification.notification_record
  (consumer_name, event_id, user_id, booking_no, notification_type, title, body, created_at)
SELECT 'showcase-semester',
       CONCAT('20000000-0000-4000-8000-', LPAD(SUBSTRING(b.booking_no, 14), 12, '0')),
       b.user_id, b.booking_no,
       CASE b.status WHEN 'COMPLETED' THEN 'BOOKING_COMPLETED'
                     WHEN 'CONFIRMED' THEN 'BOOKING_CONFIRMED'
                     WHEN 'EXPIRED' THEN 'BOOKING_EXPIRED'
                     ELSE 'BOOKING_CANCELLED' END,
       CASE b.status WHEN 'COMPLETED' THEN '场地使用已完成'
                     WHEN 'CONFIRMED' THEN '预约申请已通过'
                     WHEN 'EXPIRED' THEN '预约申请已超时'
                     ELSE '预约申请已结束' END,
       CONCAT('申请 ', b.booking_no, '（', b.activity_title, '）状态已更新，请查看申请详情。'),
       b.updated_at
FROM venueflow_booking.booking_reservation b
WHERE b.booking_no LIKE 'VF-SHOW-%' AND b.status <> 'PENDING_CONFIRMATION';

COMMIT;

SELECT 'showcase_profiles' AS metric, COUNT(*) AS total
FROM venueflow_user.user_profile WHERE external_user_id LIKE 'showcase-%'
UNION ALL
SELECT 'showcase_resources', COUNT(*)
FROM venueflow_resource.resource WHERE resource_no LIKE 'VF-CAMPUS-%'
UNION ALL
SELECT 'showcase_future_slots', COUNT(*)
FROM venueflow_resource.resource_slot s
JOIN venueflow_resource.resource r ON r.id=s.resource_id
WHERE r.resource_no LIKE 'VF-CAMPUS-%' AND s.start_at >= UTC_TIMESTAMP()
UNION ALL
SELECT 'showcase_bookings', COUNT(*)
FROM venueflow_booking.booking_reservation WHERE booking_no LIKE 'VF-SHOW-%'
UNION ALL
SELECT 'showcase_approval_actions', COUNT(*)
FROM venueflow_booking.booking_approval_action a
JOIN venueflow_booking.booking_reservation b ON b.id=a.booking_id
WHERE b.booking_no LIKE 'VF-SHOW-%'
UNION ALL
SELECT 'showcase_notifications', COUNT(*)
FROM venueflow_notification.notification_record WHERE booking_no LIKE 'VF-SHOW-%'
UNION ALL
SELECT 'demo_applicant_history', COUNT(*)
FROM venueflow_booking.booking_reservation booking
JOIN venueflow_user.user_profile profile ON profile.id = booking.user_id
JOIN venueflow_auth.auth_credentials credentials ON credentials.user_id = profile.external_user_id
WHERE credentials.username = 'campus.user' AND booking.booking_no LIKE 'VF-SHOW-%';
