-- 多人共用 MySQL 初始化脚本
-- 用法：
--   1) Docker：随 docker-compose.shared.yml 自动执行（首次启动容器时）
--   2) 本机 MySQL：mysql -u root -p < init-shared-mysql.sql
--
-- 默认创建用户 study / 密码 csrrm_shared_123（部署后请修改密码）

CREATE DATABASE IF NOT EXISTS study_room_reservation
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

-- 删除旧用户后重建（便于重复执行脚本）
DROP USER IF EXISTS 'study'@'%';
DROP USER IF EXISTS 'study'@'localhost';

CREATE USER 'study'@'%' IDENTIFIED BY 'csrrm_shared_123';
CREATE USER 'study'@'localhost' IDENTIFIED BY 'csrrm_shared_123';

GRANT ALL PRIVILEGES ON study_room_reservation.* TO 'study'@'%';
GRANT ALL PRIVILEGES ON study_room_reservation.* TO 'study'@'localhost';

FLUSH PRIVILEGES;
