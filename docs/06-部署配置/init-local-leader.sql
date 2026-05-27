-- 组长本机一键导入专用：只建库，不创建 study 用户（避免权限不足）
-- 完整数据见 database-full.sql

CREATE DATABASE IF NOT EXISTS study_room_reservation
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
