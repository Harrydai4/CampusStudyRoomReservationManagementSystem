-- MySQL dump 10.13  Distrib 8.0.44, for Win64 (x86_64)
--
-- Host: localhost    Database: study_room_reservation
-- ------------------------------------------------------
-- Server version	8.0.44

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `admin_account`
--

DROP TABLE IF EXISTS `admin_account`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `announcement`
--

DROP TABLE IF EXISTS `announcement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
  KEY `idx_announcement_pinned` (`pinned`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blacklist_record`
--

DROP TABLE IF EXISTS `blacklist_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
  KEY `idx_blacklist_end_status` (`end_time`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `checkin_record`
--

DROP TABLE IF EXISTS `checkin_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
  KEY `idx_checkin_user_time` (`user_id`,`checkin_time`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `credit_log`
--

DROP TABLE IF EXISTS `credit_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
  KEY `idx_credit_type` (`change_type`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `feedback_ticket`
--

DROP TABLE IF EXISTS `feedback_ticket`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
  KEY `idx_feedback_user_time` (`user_id`,`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `notification_message`
--

DROP TABLE IF EXISTS `notification_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
  KEY `idx_notification_user_time` (`user_id`,`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `operation_log`
--

DROP TABLE IF EXISTS `operation_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `reservation`
--

DROP TABLE IF EXISTS `reservation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reservation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `reservation_no` varchar(30) NOT NULL,
  `user_id` bigint NOT NULL,
  `room_id` bigint NOT NULL,
  `seat_id` bigint NOT NULL,
  `reserve_date` date NOT NULL,
  `start_time` time NOT NULL,
  `end_time` time NOT NULL,
  `status` varchar(20) NOT NULL,
  `sign_in_time` datetime DEFAULT NULL,
  `sign_out_time` datetime DEFAULT NULL,
  `actual_minutes` int NOT NULL DEFAULT '0',
  `cancel_reason` varchar(200) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `reservation_no` (`reservation_no`),
  KEY `idx_res_user_date` (`user_id`,`reserve_date`),
  KEY `idx_res_room_date` (`room_id`,`reserve_date`),
  KEY `idx_res_seat_date` (`seat_id`,`reserve_date`),
  KEY `idx_res_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `reservation_slot`
--

DROP TABLE IF EXISTS `reservation_slot`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reservation_slot` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `reservation_id` bigint NOT NULL,
  `seat_id` bigint NOT NULL,
  `slot_start` datetime NOT NULL,
  `slot_end` datetime NOT NULL,
  `status` varchar(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_seat_slot` (`seat_id`,`slot_start`),
  KEY `idx_slot_reservation` (`reservation_id`)
) ENGINE=InnoDB AUTO_INCREMENT=105 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `seat`
--

DROP TABLE IF EXISTS `seat`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `seat` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `room_id` bigint NOT NULL,
  `seat_no` varchar(20) NOT NULL,
  `row_no` int NOT NULL,
  `col_no` int NOT NULL,
  `is_seat` tinyint NOT NULL DEFAULT '1',
  `cell_category` varchar(20) NOT NULL,
  `seat_type` varchar(80) NOT NULL,
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
  KEY `idx_seat_feature` (`room_id`,`has_power`,`near_window`,`quiet_zone`)
) ENGINE=InnoDB AUTO_INCREMENT=91 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `student_profile`
--

DROP TABLE IF EXISTS `student_profile`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
  KEY `idx_student_college_major` (`college`,`major`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `study_room`
--

DROP TABLE IF EXISTS `study_room`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `study_room` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `room_code` varchar(20) NOT NULL,
  `name` varchar(50) NOT NULL,
  `location` varchar(100) NOT NULL,
  `floor` varchar(20) NOT NULL,
  `open_time` time NOT NULL,
  `close_time` time NOT NULL,
  `status` varchar(20) NOT NULL,
  `manager_id` bigint NOT NULL,
  `row_count` int NOT NULL,
  `col_count` int NOT NULL,
  `cell_count` int NOT NULL,
  `seat_count` int NOT NULL,
  `facilities` varchar(200) DEFAULT NULL,
  `layout_image_url` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `room_code` (`room_code`),
  KEY `idx_room_manager` (`manager_id`),
  KEY `idx_room_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `temp_leave`
--

DROP TABLE IF EXISTS `temp_leave`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `temp_leave` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `reservation_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `leave_time` datetime NOT NULL,
  `return_time` datetime DEFAULT NULL,
  `leave_status` varchar(20) NOT NULL,
  `max_leave_minutes` int NOT NULL DEFAULT '30',
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_temp_leave_res` (`reservation_id`,`leave_status`),
  KEY `idx_temp_leave_user` (`user_id`,`leave_time`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_account`
--

DROP TABLE IF EXISTS `user_account`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Temporary view structure for view `v_room_daily_usage`
--

DROP TABLE IF EXISTS `v_room_daily_usage`;
/*!50001 DROP VIEW IF EXISTS `v_room_daily_usage`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `v_room_daily_usage` AS SELECT 
 1 AS `room_id`,
 1 AS `room_name`,
 1 AS `seat_count`,
 1 AS `reservation_count`,
 1 AS `used_count`,
 1 AS `usage_rate`*/;
SET character_set_client = @saved_cs_client;

--
-- Dumping routines for database 'study_room_reservation'
--

--
-- Final view structure for view `v_room_daily_usage`
--

/*!50001 DROP VIEW IF EXISTS `v_room_daily_usage`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`study`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `v_room_daily_usage` AS select `sr`.`id` AS `room_id`,`sr`.`name` AS `room_name`,`sr`.`seat_count` AS `seat_count`,count(`r`.`id`) AS `reservation_count`,sum((case when (`r`.`status` in ('USING','COMPLETED','AUTO_CHECKOUT','TEMP_LEAVE')) then 1 else 0 end)) AS `used_count`,round(if((`sr`.`seat_count` = 0),0,((count(`r`.`id`) / `sr`.`seat_count`) * 100)),1) AS `usage_rate` from (`study_room` `sr` left join `reservation` `r` on(((`r`.`room_id` = `sr`.`id`) and (`r`.`reserve_date` = curdate())))) group by `sr`.`id`,`sr`.`name`,`sr`.`seat_count` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-05-26 20:08:47
