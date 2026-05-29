-- MySQL dump 10.13  Distrib 8.0.44, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: study_room_reservation
-- Third-version normalized schema
-- ------------------------------------------------------

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+08:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

DROP VIEW IF EXISTS `v_room_daily_usage`;
DROP TABLE IF EXISTS `temp_leave`;
DROP TABLE IF EXISTS `study_room_facility`;
DROP TABLE IF EXISTS `feedback_ticket`;
DROP TABLE IF EXISTS `notification_message`;
DROP TABLE IF EXISTS `announcement`;
DROP TABLE IF EXISTS `blacklist_record`;
DROP TABLE IF EXISTS `credit_log`;
DROP TABLE IF EXISTS `checkin_record`;
DROP TABLE IF EXISTS `reservation_slot`;
DROP TABLE IF EXISTS `reservation`;
DROP TABLE IF EXISTS `seat`;
DROP TABLE IF EXISTS `facility`;
DROP TABLE IF EXISTS `study_room`;
DROP TABLE IF EXISTS `student_profile`;
DROP TABLE IF EXISTS `operation_log`;
DROP TABLE IF EXISTS `admin_account`;
DROP TABLE IF EXISTS `user_account`;

CREATE TABLE `user_account` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(30) NOT NULL,
  `password_hash` varchar(100) NOT NULL,
  `role` varchar(20) NOT NULL,
  `status` varchar(20) NOT NULL,
  `last_login_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  KEY `idx_user_role_status` (`role`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=51 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `admin_account` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account` varchar(30) NOT NULL,
  `password_hash` varchar(100) NOT NULL,
  `name` varchar(20) NOT NULL,
  `role` varchar(20) NOT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `status` varchar(20) NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `account` (`account`),
  KEY `idx_admin_role` (`role`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `student_profile` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `student_no` varchar(20) NOT NULL,
  `name` varchar(50) NOT NULL,
  `gender` varchar(10) NOT NULL,
  `college` varchar(50) NOT NULL,
  `major` varchar(50) NOT NULL,
  `grade` varchar(10) NOT NULL,
  `phone` varchar(20) NOT NULL,
  `email` varchar(100) NOT NULL,
  `material_url` varchar(255) DEFAULT NULL,
  `audit_status` varchar(20) NOT NULL,
  `audit_remark` varchar(255) DEFAULT NULL,
  `credit_score` int NOT NULL DEFAULT '300',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_id` (`user_id`),
  UNIQUE KEY `student_no` (`student_no`),
  KEY `idx_student_audit_status` (`audit_status`),
  KEY `idx_student_college_major` (`college`,`major`),
  CONSTRAINT `fk_student_profile_user` FOREIGN KEY (`user_id`) REFERENCES `user_account` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=51 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `study_room` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `room_code` varchar(10) NOT NULL,
  `name` varchar(50) NOT NULL,
  `room_type` varchar(10) NOT NULL DEFAULT '普通',
  `location` varchar(100) NOT NULL,
  `floor` varchar(20) NOT NULL,
  `open_time` time NOT NULL,
  `close_time` time NOT NULL,
  `status` varchar(20) NOT NULL,
  `manager_id` bigint NOT NULL,
  `row_count` int NOT NULL,
  `col_count` int NOT NULL,
  `layout_image_url` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `room_code` (`room_code`),
  KEY `idx_room_manager` (`manager_id`),
  KEY `idx_room_status` (`status`),
  CONSTRAINT `fk_study_room_manager` FOREIGN KEY (`manager_id`) REFERENCES `admin_account` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `facility` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(20) NOT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `study_room_facility` (
  `room_id` bigint NOT NULL,
  `facility_id` bigint NOT NULL,
  PRIMARY KEY (`room_id`,`facility_id`),
  KEY `idx_room_facility_facility` (`facility_id`),
  CONSTRAINT `fk_room_facility_room` FOREIGN KEY (`room_id`) REFERENCES `study_room` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_room_facility_facility` FOREIGN KEY (`facility_id`) REFERENCES `facility` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `seat` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `room_id` bigint NOT NULL,
  `seat_no` varchar(10) NOT NULL,
  `row_no` int NOT NULL,
  `col_no` int NOT NULL,
  `is_seat` tinyint NOT NULL DEFAULT '1',
  `cell_category` varchar(20) NOT NULL,
  `seat_type` varchar(10) NOT NULL,
  `has_power` tinyint NOT NULL DEFAULT '0',
  `near_window` tinyint NOT NULL DEFAULT '0',
  `quiet_zone` tinyint NOT NULL DEFAULT '0',
  `hot_seat` tinyint NOT NULL DEFAULT '0',
  `status` varchar(20) NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_room_seat` (`room_id`,`seat_no`),
  KEY `idx_seat_room_status` (`room_id`,`status`),
  KEY `idx_seat_feature` (`room_id`,`has_power`,`near_window`,`quiet_zone`),
  CONSTRAINT `fk_seat_room` FOREIGN KEY (`room_id`) REFERENCES `study_room` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=415 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `reservation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `reservation_no` varchar(16) NOT NULL,
  `user_id` bigint NOT NULL,
  `room_id` bigint NOT NULL,
  `seat_id` bigint NOT NULL,
  `reserve_date` date NOT NULL,
  `start_time` time NOT NULL,
  `end_time` time NOT NULL,
  `status` varchar(20) NOT NULL,
  `sign_in_time` datetime DEFAULT NULL,
  `sign_out_time` datetime DEFAULT NULL,
  `cancel_reason` varchar(200) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `reservation_no` (`reservation_no`),
  KEY `idx_res_user_date` (`user_id`,`reserve_date`),
  KEY `idx_res_room_date` (`room_id`,`reserve_date`),
  KEY `idx_res_seat_date` (`seat_id`,`reserve_date`),
  KEY `idx_res_status` (`status`),
  CONSTRAINT `fk_reservation_user` FOREIGN KEY (`user_id`) REFERENCES `user_account` (`id`),
  CONSTRAINT `fk_reservation_room` FOREIGN KEY (`room_id`) REFERENCES `study_room` (`id`),
  CONSTRAINT `fk_reservation_seat` FOREIGN KEY (`seat_id`) REFERENCES `seat` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1851 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `reservation_slot` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `reservation_id` bigint NOT NULL,
  `seat_id` bigint NOT NULL,
  `slot_start` datetime NOT NULL,
  `slot_end` datetime NOT NULL,
  `status` varchar(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_seat_slot` (`seat_id`,`slot_start`),
  KEY `idx_slot_reservation` (`reservation_id`),
  CONSTRAINT `fk_slot_reservation` FOREIGN KEY (`reservation_id`) REFERENCES `reservation` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_slot_seat` FOREIGN KEY (`seat_id`) REFERENCES `seat` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=20380 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `checkin_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `reservation_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `admin_id` bigint NOT NULL,
  `checkin_method` varchar(20) NOT NULL,
  `checkin_time` datetime NOT NULL,
  `checkout_time` datetime DEFAULT NULL,
  `result` varchar(20) NOT NULL,
  `remark` varchar(200) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `reservation_id` (`reservation_id`),
  KEY `idx_checkin_admin_time` (`admin_id`,`checkin_time`),
  KEY `idx_checkin_user_time` (`user_id`,`checkin_time`),
  CONSTRAINT `fk_checkin_reservation` FOREIGN KEY (`reservation_id`) REFERENCES `reservation` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_checkin_user` FOREIGN KEY (`user_id`) REFERENCES `user_account` (`id`),
  CONSTRAINT `fk_checkin_admin` FOREIGN KEY (`admin_id`) REFERENCES `admin_account` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1562 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `credit_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `before_score` int NOT NULL,
  `change_value` int NOT NULL,
  `after_score` int NOT NULL,
  `change_type` varchar(30) NOT NULL,
  `reason` varchar(100) NOT NULL,
  `reservation_id` bigint DEFAULT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_credit_user_time` (`user_id`,`created_at`),
  KEY `idx_credit_type` (`change_type`),
  KEY `idx_credit_reservation` (`reservation_id`),
  CONSTRAINT `fk_credit_user` FOREIGN KEY (`user_id`) REFERENCES `user_account` (`id`),
  CONSTRAINT `fk_credit_reservation` FOREIGN KEY (`reservation_id`) REFERENCES `reservation` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=451 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `blacklist_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `start_time` datetime NOT NULL,
  `end_time` datetime NOT NULL,
  `reason` varchar(200) NOT NULL,
  `status` varchar(20) NOT NULL,
  `released_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_blacklist_user_status` (`user_id`,`status`),
  KEY `idx_blacklist_end_status` (`end_time`,`status`),
  CONSTRAINT `fk_blacklist_user` FOREIGN KEY (`user_id`) REFERENCES `user_account` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `announcement` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(100) NOT NULL,
  `content` text NOT NULL,
  `type` varchar(20) NOT NULL,
  `pinned` tinyint NOT NULL DEFAULT '0',
  `scope` varchar(20) NOT NULL,
  `room_id` bigint DEFAULT NULL,
  `publisher_id` bigint NOT NULL,
  `status` varchar(20) NOT NULL,
  `published_at` datetime DEFAULT NULL,
  `view_count` int NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_announcement_status_time` (`status`,`published_at`),
  KEY `idx_announcement_room` (`room_id`),
  KEY `idx_announcement_pinned` (`pinned`),
  KEY `idx_announcement_publisher` (`publisher_id`),
  CONSTRAINT `fk_announcement_room` FOREIGN KEY (`room_id`) REFERENCES `study_room` (`id`),
  CONSTRAINT `fk_announcement_publisher` FOREIGN KEY (`publisher_id`) REFERENCES `admin_account` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `notification_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `title` varchar(100) NOT NULL,
  `content` varchar(500) NOT NULL,
  `type` varchar(20) NOT NULL,
  `read_flag` tinyint NOT NULL DEFAULT '0',
  `related_id` bigint DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `read_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_notification_user_read` (`user_id`,`read_flag`),
  KEY `idx_notification_user_time` (`user_id`,`created_at`),
  CONSTRAINT `fk_notification_user` FOREIGN KEY (`user_id`) REFERENCES `user_account` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2385 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `feedback_ticket` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `reservation_id` bigint DEFAULT NULL,
  `room_id` bigint DEFAULT NULL,
  `seat_id` bigint DEFAULT NULL,
  `type` varchar(30) NOT NULL,
  `severity` varchar(20) NOT NULL,
  `content` varchar(1000) NOT NULL,
  `status` varchar(20) NOT NULL,
  `handler_id` bigint DEFAULT NULL,
  `handle_result` varchar(1000) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `handled_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_feedback_status` (`status`),
  KEY `idx_feedback_room_status` (`room_id`,`status`),
  KEY `idx_feedback_user_time` (`user_id`,`created_at`),
  KEY `idx_feedback_reservation` (`reservation_id`),
  KEY `idx_feedback_seat` (`seat_id`),
  KEY `idx_feedback_handler` (`handler_id`),
  CONSTRAINT `fk_feedback_user` FOREIGN KEY (`user_id`) REFERENCES `user_account` (`id`),
  CONSTRAINT `fk_feedback_reservation` FOREIGN KEY (`reservation_id`) REFERENCES `reservation` (`id`),
  CONSTRAINT `fk_feedback_room` FOREIGN KEY (`room_id`) REFERENCES `study_room` (`id`),
  CONSTRAINT `fk_feedback_seat` FOREIGN KEY (`seat_id`) REFERENCES `seat` (`id`),
  CONSTRAINT `fk_feedback_handler` FOREIGN KEY (`handler_id`) REFERENCES `admin_account` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=37 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `operation_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `operator_id` bigint NOT NULL,
  `operator_name` varchar(50) NOT NULL,
  `module` varchar(30) NOT NULL,
  `action` varchar(30) NOT NULL,
  `target_type` varchar(30) DEFAULT NULL,
  `target_id` bigint DEFAULT NULL,
  `detail` varchar(500) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_op_log_operator` (`operator_id`,`created_at`),
  KEY `idx_op_log_module` (`module`,`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=161 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE OR REPLACE VIEW `v_room_daily_usage` AS
select `sr`.`id` AS `room_id`,
       `sr`.`name` AS `room_name`,
       count(distinct case when `s`.`is_seat` = 1 then `s`.`id` end) AS `seat_count`,
       count(distinct `r`.`id`) AS `reservation_count`,
       count(distinct case when `r`.`status` in ('使用中','已完成') then `r`.`id` end) AS `used_count`,
       round(if(count(distinct case when `s`.`is_seat` = 1 then `s`.`id` end)=0,0,
         count(distinct `r`.`id`) / count(distinct case when `s`.`is_seat` = 1 then `s`.`id` end) * 100),1) AS `usage_rate`
from `study_room` `sr`
left join `seat` `s` on `s`.`room_id` = `sr`.`id`
left join `reservation` `r` on `r`.`room_id` = `sr`.`id` and `r`.`reserve_date` = curdate()
group by `sr`.`id`,`sr`.`name`;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
