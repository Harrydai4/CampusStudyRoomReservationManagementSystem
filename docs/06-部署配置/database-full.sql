-- MySQL dump 10.13  Distrib 8.0.44, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: study_room_reservation
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
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `admin_account`
--

LOCK TABLES `admin_account` WRITE;
/*!40000 ALTER TABLE `admin_account` DISABLE KEYS */;
INSERT INTO `admin_account` (`id`, `account`, `password_hash`, `name`, `role`, `phone`, `status`, `created_at`, `updated_at`) VALUES (1,'admin','$2a$10$tFJDic7cwgZTaVgOg/ygDeIhUDzuAvRRJc5D4xnjPqipTviPT1ii6','张老师','ADMIN','13800138000','NORMAL','2026-05-25 23:40:40','2026-05-26 23:58:53'),(2,'superadmin','$2a$10$YdOTJYJztshRqdx4bSvVcOdEVYWCiaO7mGr78frjiv/pkvKrwyZXC','超级管理员','SUPER_ADMIN','13900139000','NORMAL','2026-05-25 23:40:40','2026-05-26 23:58:53'),(3,'Ladmin','$2a$10$GZwYZBtgmyTTzhFYWgfJ2.QPx45XGu7xgbavt0p2Y9asoJ0aD20Zy','梁老板','ADMIN','11111111111','NORMAL','2026-05-26 22:17:58','2026-05-26 22:21:31'),(4,'live_ext_live20260526230415','$2a$10$FWtN3jJhsy9yMgMpdEQ0JeYjpYUqPyGRxHIQtTMYg0BhjsJpbaXai','【极端测试-LIVE】管理员','ADMIN','13700000000','NORMAL','2026-05-26 23:04:16','2026-05-26 23:04:16');
/*!40000 ALTER TABLE `admin_account` ENABLE KEYS */;
UNLOCK TABLES;

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
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `announcement`
--

LOCK TABLES `announcement` WRITE;
/*!40000 ALTER TABLE `announcement` DISABLE KEYS */;
INSERT INTO `announcement` (`id`, `title`, `content`, `type`, `pinned`, `scope`, `room_id`, `publisher_id`, `status`, `published_at`, `view_count`, `created_at`, `updated_at`) VALUES (1,'期末周开放时间延长','图书馆自习室期末周开放至 22:30，请同学们按预约时段使用','SYSTEM',1,'GLOBAL',NULL,1,'PUBLISHED','2026-05-24 23:40:40',2,'2026-05-24 23:40:40','2026-05-26 21:37:42'),(2,'签到规则提醒','预约开始前后 15 分钟内可由管理员扫码签到，超时未签到将扣除信用积分。','RULE',0,'GLOBAL',NULL,1,'PUBLISHED','2026-05-25 18:40:40',3,'2026-05-25 18:40:40','2026-05-25 23:40:40'),(3,'【极端测试-LIVE】公告-LIVE-20260526-230415','Live 极端测试公告，请管理员与学生端公告页核对。','SYSTEM',1,'GLOBAL',NULL,2,'PUBLISHED','2026-05-26 23:04:16',3,'2026-05-26 23:04:16','2026-05-26 23:04:16');
/*!40000 ALTER TABLE `announcement` ENABLE KEYS */;
UNLOCK TABLES;

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
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `blacklist_record`
--

LOCK TABLES `blacklist_record` WRITE;
/*!40000 ALTER TABLE `blacklist_record` DISABLE KEYS */;
INSERT INTO `blacklist_record` (`id`, `user_id`, `start_time`, `end_time`, `reason`, `status`, `released_at`) VALUES (1,1,'2026-05-26 23:21:40','2026-06-02 23:21:40','信用积分小于等于0','RELEASED','2026-05-26 23:41:29');
/*!40000 ALTER TABLE `blacklist_record` ENABLE KEYS */;
UNLOCK TABLES;

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
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `checkin_record`
--

LOCK TABLES `checkin_record` WRITE;
/*!40000 ALTER TABLE `checkin_record` DISABLE KEYS */;
INSERT INTO `checkin_record` (`id`, `reservation_id`, `user_id`, `admin_id`, `checkin_method`, `checkin_time`, `checkout_time`, `result`, `remark`) VALUES (1,5,1,1,'QR_SCAN','2026-05-26 19:00:09','2026-05-26 19:04:10','ON_TIME',NULL),(2,6,1,1,'QR_SCAN','2026-05-26 19:04:26','2026-05-26 11:00:00','ON_TIME',NULL),(3,8,1,1,'QR_SCAN','2026-05-26 19:14:04','2026-05-26 19:21:27','ON_TIME',NULL),(4,10,1,1,'QR_SCAN','2026-05-26 19:22:48','2026-05-26 19:22:56','ON_TIME',NULL),(5,11,1,1,'QR_SCAN','2026-05-26 19:24:50','2026-05-26 19:24:58','ON_TIME',NULL),(6,12,1,1,'QR_SCAN','2026-05-26 20:24:07','2026-05-26 20:29:38','ON_TIME',NULL),(7,13,1,1,'STUDENT_NO','2026-05-26 21:01:00','2026-05-26 21:02:30','ON_TIME',NULL),(8,14,1,1,'STUDENT_NO','2026-05-26 21:05:33','2026-05-26 21:06:08','ON_TIME',NULL),(9,15,1,1,'STUDENT_NO','2026-05-26 21:06:29','2026-05-26 21:08:17','ON_TIME',NULL),(10,18,6,1,'STUDENT_NO','2026-05-26 21:28:13','2026-05-26 21:28:25','ON_TIME',NULL),(11,21,1,1,'STUDENT_NO','2026-05-26 21:35:10','2026-05-26 21:35:16','ON_TIME',NULL),(12,22,1,1,'STUDENT_NO','2026-05-26 21:36:50','2026-05-26 21:37:03','ON_TIME',NULL),(13,23,7,1,'STUDENT_NO','2026-05-26 21:37:23','2026-05-26 21:40:00','ON_TIME',NULL),(14,24,1,1,'STUDENT_NO','2026-05-26 21:40:43','2026-05-26 21:50:00','ON_TIME',NULL),(15,28,7,2,'STUDENT_NO','2026-05-26 22:27:21','2026-05-26 22:30:00','ON_TIME',NULL);
/*!40000 ALTER TABLE `checkin_record` ENABLE KEYS */;
UNLOCK TABLES;

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
) ENGINE=InnoDB AUTO_INCREMENT=30 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `credit_log`
--

LOCK TABLES `credit_log` WRITE;
/*!40000 ALTER TABLE `credit_log` DISABLE KEYS */;
INSERT INTO `credit_log` (`id`, `user_id`, `before_score`, `change_value`, `after_score`, `change_type`, `reason`, `reservation_id`, `created_at`) VALUES (1,1,300,-20,280,'NO_CHECKIN','历史未按时签到扣分',NULL,'2026-05-22 23:40:40'),(2,1,280,-50,230,'NO_SHOW','预约超时未签到',1,'2026-05-25 23:41:00'),(3,1,230,-50,180,'NO_SHOW','预约超时未签到',3,'2026-05-26 12:23:00'),(4,1,280,5,285,'ON_TIME_CHECKIN','准时签到奖励',5,'2026-05-26 19:00:09'),(5,1,285,5,290,'ON_TIME_CHECKIN','准时签到奖励',6,'2026-05-26 19:04:26'),(6,1,280,5,285,'ON_TIME_CHECKIN','准时签到奖励',8,'2026-05-26 19:14:04'),(7,1,280,5,285,'ON_TIME_CHECKIN','准时签到奖励',10,'2026-05-26 19:22:48'),(8,1,280,5,285,'ON_TIME_CHECKIN','准时签到奖励',11,'2026-05-26 19:24:50'),(9,1,280,5,285,'ON_TIME_CHECKIN','准时签到奖励',12,'2026-05-26 20:24:07'),(10,1,280,5,285,'ON_TIME_CHECKIN','准时签到奖励',13,'2026-05-26 21:01:00'),(11,1,285,5,290,'ON_TIME_CHECKIN','准时签到奖励',14,'2026-05-26 21:05:33'),(12,1,290,5,295,'ON_TIME_CHECKIN','准时签到奖励',15,'2026-05-26 21:06:29'),(13,1,295,-50,245,'NO_SHOW','预约超时未签到',17,'2026-05-26 21:23:00'),(14,1,245,-50,195,'NO_SHOW','预约超时未签到',16,'2026-05-26 21:25:00'),(15,6,300,5,305,'ON_TIME_CHECKIN','准时签到奖励',18,'2026-05-26 21:28:13'),(16,7,300,-50,250,'NO_SHOW','预约超时未签到',19,'2026-05-26 21:33:00'),(17,1,195,5,200,'ON_TIME_CHECKIN','准时签到奖励',21,'2026-05-26 21:35:10'),(18,1,200,5,205,'ON_TIME_CHECKIN','准时签到奖励',22,'2026-05-26 21:36:50'),(19,7,250,5,255,'ON_TIME_CHECKIN','准时签到奖励',23,'2026-05-26 21:37:23'),(20,1,205,5,210,'ON_TIME_CHECKIN','准时签到奖励',24,'2026-05-26 21:40:43'),(21,6,305,-50,255,'NO_SHOW','预约超时未签到',25,'2026-05-26 21:56:00'),(22,1,280,-50,230,'NO_SHOW','预约超时未签到',26,'2026-05-26 22:05:00'),(23,7,255,5,260,'ON_TIME_CHECKIN','准时签到奖励',28,'2026-05-26 22:27:21'),(24,1,280,-50,230,'USER_CANCEL','学生主动取消预约',34,'2026-05-26 23:20:02'),(25,1,230,-50,180,'USER_CANCEL','学生主动取消预约',35,'2026-05-26 23:20:43'),(26,1,180,-50,130,'USER_CANCEL','学生主动取消预约',36,'2026-05-26 23:21:04'),(27,1,130,-50,80,'USER_CANCEL','学生主动取消预约',37,'2026-05-26 23:21:12'),(28,1,80,-50,30,'USER_CANCEL','学生主动取消预约',38,'2026-05-26 23:21:22'),(29,1,30,-50,0,'USER_CANCEL','学生主动取消预约',39,'2026-05-26 23:21:40');
/*!40000 ALTER TABLE `credit_log` ENABLE KEYS */;
UNLOCK TABLES;

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
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `feedback_ticket`
--

LOCK TABLES `feedback_ticket` WRITE;
/*!40000 ALTER TABLE `feedback_ticket` DISABLE KEYS */;
INSERT INTO `feedback_ticket` (`id`, `user_id`, `reservation_id`, `room_id`, `seat_id`, `type`, `severity`, `content`, `status`, `handler_id`, `handle_result`, `created_at`, `handled_at`) VALUES (1,1,NULL,1,12,'SEAT_REPAIR','MEDIUM','A-12 插座接触不良，请安排维修。','DONE',1,'已处理并记录','2026-05-25 21:40:40','2026-05-26 18:00:16'),(2,1,NULL,1,NULL,'SUGGESTION','MEDIUM','不好用','DONE',1,'确实不好用','2026-05-26 18:12:20','2026-05-26 18:24:50'),(3,1,NULL,1,NULL,'SUGGESTION','MEDIUM','网站有小广告','DONE',1,'cgjjjj','2026-05-26 21:23:02','2026-05-26 21:23:30'),(4,6,NULL,1,NULL,'SUGGESTION','MEDIUM','崔兆飞nb','DONE',1,'nb','2026-05-26 21:37:02','2026-05-26 21:37:54'),(5,6,NULL,1,NULL,'SUGGESTION','MEDIUM','很严重，崔兆飞牛逼','DONE',1,'梁增杰也nb','2026-05-26 21:37:51','2026-05-26 21:40:14'),(6,1,NULL,1,NULL,'SUGGESTION','MEDIUM','1','PENDING',NULL,NULL,'2026-05-26 22:02:15',NULL),(7,10,NULL,8,NULL,'SUGGESTION','HIGH','【极端测试-LIVE】反馈-LIVE-20260526-230415','DONE',1,'Live测试已处理-LIVE-20260526-230415','2026-05-26 23:04:16','2026-05-26 23:04:16');
/*!40000 ALTER TABLE `feedback_ticket` ENABLE KEYS */;
UNLOCK TABLES;

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
) ENGINE=InnoDB AUTO_INCREMENT=64 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notification_message`
--

LOCK TABLES `notification_message` WRITE;
/*!40000 ALTER TABLE `notification_message` DISABLE KEYS */;
INSERT INTO `notification_message` (`id`, `user_id`, `title`, `content`, `type`, `read_flag`, `related_id`, `created_at`, `read_at`) VALUES (1,1,'预约即将开始','你的图书馆一楼A区 A-12 座位预约请按时签到。','RESERVATION',0,1,'2026-05-25 23:25:40',NULL),(2,1,'预约违约','你有一条预约因超时未签到被判定违约，已扣除 50 信用分。','VIOLATION',0,1,'2026-05-25 23:41:00',NULL),(3,1,'预约成功','你的座位预约已创建，请按时签到。','RESERVATION',0,3,'2026-05-26 00:35:07',NULL),(4,1,'预约违约','你有一条预约因超时未签到被判定违约，已扣除 50 信用分。','VIOLATION',0,3,'2026-05-26 12:23:00',NULL),(5,1,'预约成功','你的座位预约已创建，请按时签到。','RESERVATION',1,4,'2026-05-26 12:23:42','2026-05-26 16:20:27'),(6,1,'反馈已处理','你提交的问题反馈已由管理员处理：确实不好用','FEEDBACK',1,2,'2026-05-26 18:24:50','2026-05-26 18:32:02'),(7,1,'预约成功','你的座位预约已创建，请按时签到。','RESERVATION',0,5,'2026-05-26 18:32:24',NULL),(8,1,'预约成功','你的座位预约已创建，请按时签到。','RESERVATION',0,6,'2026-05-26 19:04:16',NULL),(9,1,'预约成功','你的座位预约已创建，请按时签到。','RESERVATION',0,7,'2026-05-26 19:07:44',NULL),(10,1,'预约成功','你的座位预约已创建，请按时签到。','RESERVATION',0,8,'2026-05-26 19:13:43',NULL),(11,1,'暂离开始','你已暂离座位，请在 30 分钟内返回，超时将自动签退并扣分。','TEMP_LEAVE',0,8,'2026-05-26 19:14:39',NULL),(12,1,'暂离结束','你已返回座位，请继续学习并在结束时签退。','TEMP_LEAVE',0,8,'2026-05-26 19:14:41',NULL),(13,1,'预约成功','你的座位预约已创建，请按时签到。','RESERVATION',0,9,'2026-05-26 19:21:37',NULL),(14,1,'预约成功','你的座位预约已创建，请按时签到。','RESERVATION',0,10,'2026-05-26 19:22:41',NULL),(15,1,'预约成功','你的座位预约已创建，请按时签到。','RESERVATION',1,11,'2026-05-26 19:24:46','2026-05-26 21:08:00'),(16,1,'预约成功','你的座位预约已创建，请按时签到。','RESERVATION',0,12,'2026-05-26 20:23:02',NULL),(17,1,'预约成功','你的座位预约已创建，请按时签到。','RESERVATION',1,13,'2026-05-26 21:00:52','2026-05-26 21:07:57'),(18,1,'预约成功','你的座位预约已创建，请按时签到。','RESERVATION',1,14,'2026-05-26 21:04:39','2026-05-26 21:07:56'),(19,1,'预约成功','你的座位预约已创建，请按时签到。','RESERVATION',1,15,'2026-05-26 21:06:22','2026-05-26 21:23:51'),(20,1,'预约成功','你的座位预约已创建，请按时签到。','RESERVATION',1,16,'2026-05-26 21:15:57','2026-05-26 21:23:50'),(21,1,'预约成功','你的座位预约已创建，请按时签到。','RESERVATION',1,17,'2026-05-26 21:22:38','2026-05-26 21:23:49'),(22,1,'预约违约','你有一条预约因超时未签到被判定违约，已扣除 50 信用分。','VIOLATION',1,17,'2026-05-26 21:23:00','2026-05-26 21:23:48'),(23,1,'反馈已处理','你提交的问题反馈已由管理员处理：cgjjjj','FEEDBACK',1,3,'2026-05-26 21:23:30','2026-05-26 21:23:43'),(24,1,'预约违约','你有一条预约因超时未签到被判定违约，已扣除 50 信用分。','VIOLATION',0,16,'2026-05-26 21:25:00',NULL),(25,6,'预约成功','你的座位预约已创建，请按时签到。','RESERVATION',0,18,'2026-05-26 21:27:00',NULL),(26,7,'预约成功','你的座位预约已创建，请按时签到。','RESERVATION',0,19,'2026-05-26 21:32:32',NULL),(27,7,'预约违约','你有一条预约因超时未签到被判定违约，已扣除 50 信用分。','VIOLATION',0,19,'2026-05-26 21:33:00',NULL),(28,7,'预约成功','你的座位预约已创建，请按时签到。','RESERVATION',0,20,'2026-05-26 21:33:36',NULL),(29,1,'预约成功','你的座位预约已创建，请按时签到。','RESERVATION',0,21,'2026-05-26 21:34:57',NULL),(30,1,'预约成功','你的座位预约已创建，请按时签到。','RESERVATION',0,22,'2026-05-26 21:35:26',NULL),(31,7,'预约成功','你的座位预约已创建，请按时签到。','RESERVATION',0,23,'2026-05-26 21:36:17',NULL),(32,1,'预约成功','你的座位预约已创建，请按时签到。','RESERVATION',0,24,'2026-05-26 21:37:14',NULL),(33,6,'反馈已处理','你提交的问题反馈已由管理员处理：nb','FEEDBACK',0,4,'2026-05-26 21:37:54',NULL),(34,6,'反馈已处理','你提交的问题反馈已由管理员处理：梁增杰也nb','FEEDBACK',1,5,'2026-05-26 21:40:14','2026-05-26 21:41:37'),(35,7,'自动签退','预约时段结束，系统已自动签退','CHECKOUT',0,23,'2026-05-26 21:41:00',NULL),(36,1,'自动签退','预约时段结束，系统已自动签退','CHECKOUT',0,24,'2026-05-26 21:51:00',NULL),(37,6,'预约成功','你的座位预约已创建，请按时签到。','RESERVATION',0,25,'2026-05-26 21:55:55',NULL),(38,6,'预约违约','你有一条预约因超时未签到被判定违约，已扣除 50 信用分。','VIOLATION',0,25,'2026-05-26 21:56:00',NULL),(39,1,'预约成功','你的座位预约已创建，请按时签到。','RESERVATION',0,26,'2026-05-26 21:56:29',NULL),(40,6,'预约成功','你的座位预约已创建，请按时签到。','RESERVATION',0,27,'2026-05-26 22:02:10',NULL),(41,1,'预约违约','你有一条预约因超时未签到被判定违约，已扣除 50 信用分。','VIOLATION',0,26,'2026-05-26 22:05:00',NULL),(42,7,'预约成功','你的座位预约已创建，请按时签到。','RESERVATION',0,28,'2026-05-26 22:26:41',NULL),(43,7,'暂离开始','你已暂离座位，请在 30 分钟内返回，超时将自动签退并扣分。','TEMP_LEAVE',0,28,'2026-05-26 22:27:26',NULL),(44,7,'暂离结束','你已返回座位，请继续学习并在结束时签退。','TEMP_LEAVE',0,28,'2026-05-26 22:27:28',NULL),(45,7,'自动签退','预约时段结束，系统已自动签退','CHECKOUT',0,28,'2026-05-26 22:31:00',NULL),(46,1,'预约成功','你的座位预约已创建，请按时签到。','RESERVATION',0,29,'2026-05-26 23:00:05',NULL),(47,1,'预约成功','你的座位预约已创建，请按时签到。','RESERVATION',0,30,'2026-05-26 23:04:07',NULL),(48,10,'预约成功','你的座位预约已创建，请按时签到。','RESERVATION',0,31,'2026-05-26 23:04:16',NULL),(49,10,'反馈已处理','你提交的问题反馈已由管理员处理：Live测试已处理-LIVE-20260526-230415','FEEDBACK',0,7,'2026-05-26 23:04:16',NULL),(50,1,'预约成功','你的座位预约已创建，请按时签到。','RESERVATION',0,32,'2026-05-26 23:11:10',NULL),(51,1,'预约成功','你的座位预约已创建，请按时签到。','RESERVATION',0,33,'2026-05-26 23:11:47',NULL),(52,1,'预约成功','你的座位预约已创建，请按时签到。','RESERVATION',0,34,'2026-05-26 23:19:58',NULL),(53,1,'预约已取消','你的预约已取消，已扣除 50 信用分。','RESERVATION',0,34,'2026-05-26 23:20:02',NULL),(54,1,'预约成功','你的座位预约已创建，请按时签到。','RESERVATION',0,35,'2026-05-26 23:20:39',NULL),(55,1,'预约已取消','你的预约已取消，已扣除 50 信用分。','RESERVATION',0,35,'2026-05-26 23:20:43',NULL),(56,1,'预约成功','你的座位预约已创建，请按时签到。','RESERVATION',0,36,'2026-05-26 23:20:56',NULL),(57,1,'预约已取消','你的预约已取消，已扣除 50 信用分。','RESERVATION',0,36,'2026-05-26 23:21:04',NULL),(58,1,'预约成功','你的座位预约已创建，请按时签到。','RESERVATION',0,37,'2026-05-26 23:21:10',NULL),(59,1,'预约已取消','你的预约已取消，已扣除 50 信用分。','RESERVATION',0,37,'2026-05-26 23:21:12',NULL),(60,1,'预约成功','你的座位预约已创建，请按时签到。','RESERVATION',0,38,'2026-05-26 23:21:16',NULL),(61,1,'预约已取消','你的预约已取消，已扣除 50 信用分。','RESERVATION',0,38,'2026-05-26 23:21:22',NULL),(62,1,'预约成功','你的座位预约已创建，请按时签到。','RESERVATION',0,39,'2026-05-26 23:21:36',NULL),(63,1,'预约已取消','你的预约已取消，已扣除 50 信用分。','RESERVATION',0,39,'2026-05-26 23:21:40',NULL);
/*!40000 ALTER TABLE `notification_message` ENABLE KEYS */;
UNLOCK TABLES;

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
) ENGINE=InnoDB AUTO_INCREMENT=60 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `operation_log`
--

LOCK TABLES `operation_log` WRITE;
/*!40000 ALTER TABLE `operation_log` DISABLE KEYS */;
INSERT INTO `operation_log` (`id`, `operator_id`, `operator_name`, `module`, `action`, `target_type`, `target_id`, `detail`, `created_at`) VALUES (1,1,'张三','AUTH','CHANGE_PASSWORD','STUDENT',1,'修改登录密码','2026-05-26 17:51:57'),(2,1,'张三','AUTH','CHANGE_PASSWORD','STUDENT',1,'修改登录密码','2026-05-26 17:52:19'),(3,1,'张老师','USER','APPROVE','STUDENT',1,'','2026-05-26 17:53:06'),(4,1,'张老师','USER','REJECT','STUDENT',1,'1','2026-05-26 17:53:12'),(5,1,'张老师','USER','REJECT','STUDENT',1,'1','2026-05-26 17:53:29'),(6,1,'张老师','ROOM','UPDATE','STUDY_ROOM',1,'图书馆一楼A区','2026-05-26 17:56:03'),(7,1,'张三','AUTH','CHANGE_PASSWORD','STUDENT',1,'修改登录密码','2026-05-26 18:11:32'),(8,1,'张老师','FEEDBACK','HANDLE','FEEDBACK',2,'确实不好用','2026-05-26 18:24:50'),(9,1,'张老师','USER','APPROVE','STUDENT',3,'','2026-05-26 18:25:10'),(10,1,'张老师','ROOM','CREATE','STUDY_ROOM',4,'1','2026-05-26 18:27:35'),(11,1,'张老师','ROOM','UPDATE','STUDY_ROOM',4,'1','2026-05-26 18:27:43'),(12,1,'张老师','ROOM','UPDATE','STUDY_ROOM',2,'图书馆二楼B区','2026-05-26 18:27:54'),(13,1,'张老师','ROOM','UPDATE','STUDY_ROOM',1,'图书馆一楼A区','2026-05-26 18:31:21'),(14,1,'张老师','USER','REJECT','STUDENT',4,'测试','2026-05-26 18:34:54'),(15,1,'张老师','FEEDBACK','HANDLE','FEEDBACK',3,'cgjjjj','2026-05-26 21:23:30'),(16,1,'张老师','USER','APPROVE','STUDENT',5,'','2026-05-26 21:25:31'),(17,1,'张老师','USER','APPROVE','STUDENT',6,'','2026-05-26 21:25:32'),(18,1,'张老师','ROOM','CREATE','STUDY_ROOM',5,'405','2026-05-26 21:25:35'),(19,1,'张老师','USER','APPROVE','STUDENT',7,'','2026-05-26 21:30:08'),(20,1,'张老师','SEAT','DELETE','SEAT',91,'N-01','2026-05-26 21:31:37'),(21,1,'张老师','SEAT','CREATE','SEAT',140,'100-49','2026-05-26 21:31:41'),(22,6,'L','AUTH','CHANGE_PASSWORD','STUDENT',6,'修改登录密码','2026-05-26 21:34:41'),(23,6,'L','AUTH','CHANGE_PASSWORD','STUDENT',6,'修改登录密码','2026-05-26 21:35:01'),(24,1,'张老师','FEEDBACK','HANDLE','FEEDBACK',4,'nb','2026-05-26 21:37:54'),(25,1,'张老师','FEEDBACK','HANDLE','FEEDBACK',5,'梁增杰也nb','2026-05-26 21:40:14'),(26,1,'张老师','USER','APPROVE','STUDENT',8,'','2026-05-26 21:58:18'),(27,2,'超级管理员','ADMIN','UPDATE','ADMIN_ACCOUNT',1,'admin','2026-05-26 22:17:03'),(28,2,'超级管理员','ROOM','UPDATE','STUDY_ROOM',1,'图书馆一楼A区','2026-05-26 22:17:08'),(29,2,'超级管理员','ADMIN','CREATE','ADMIN_ACCOUNT',3,'Ladmin','2026-05-26 22:17:58'),(30,2,'超级管理员','ROOM','UPDATE','STUDY_ROOM',4,'1','2026-05-26 22:18:30'),(31,2,'超级管理员','ADMIN','UPDATE','ADMIN_ACCOUNT',3,'Ladmin','2026-05-26 22:19:25'),(32,3,'梁老板','ROOM','UPDATE','STUDY_ROOM',4,'1','2026-05-26 22:20:23'),(33,3,'梁老板','ROOM','UPDATE','STUDY_ROOM',4,'1','2026-05-26 22:20:33'),(34,2,'超级管理员','ADMIN','UPDATE','ADMIN_ACCOUNT',3,'Ladmin','2026-05-26 22:21:31'),(35,2,'超级管理员','SEAT','CREATE','SEAT',141,'1-25','2026-05-26 22:23:15'),(36,2,'超级管理员','SEAT','CREATE','SEAT',142,'1-26','2026-05-26 22:23:21'),(37,2,'超级管理员','SEAT','DELETE','SEAT',90,'N-24','2026-05-26 22:23:46'),(38,2,'超级管理员','SEAT','DELETE','SEAT',89,'N-23','2026-05-26 22:23:49'),(39,1,'张老师','USER','APPROVE','STUDENT',9,'','2026-05-26 22:32:05'),(40,2,'超级管理员','ROOM','UPDATE','STUDY_ROOM',1,'图书馆一楼A区','2026-05-26 22:34:44'),(41,1,'张老师','ROOM','UPDATE','STUDY_ROOM',1,'图书馆一楼A区','2026-05-26 22:37:13'),(42,2,'超级管理员','ROOM','UPDATE','STUDY_ROOM',4,'1','2026-05-26 22:41:10'),(43,2,'超级管理员','ROOM','CREATE','STUDY_ROOM',6,'2','2026-05-26 22:41:27'),(44,2,'超级管理员','ROOM','CREATE','STUDY_ROOM',7,'4','2026-05-26 22:44:14'),(45,2,'超级管理员','SEAT','CREATE','SEAT',161,'4-07','2026-05-26 22:44:20'),(46,2,'超级管理员','ROOM','UPDATE','STUDY_ROOM',1,'图书馆一楼A区','2026-05-26 22:54:02'),(47,2,'超级管理员','ROOM','UPDATE','STUDY_ROOM',1,'图书馆一楼A区','2026-05-26 22:54:36'),(48,2,'超级管理员','ROOM','UPDATE','STUDY_ROOM',1,'图书馆一楼A区','2026-05-26 22:54:44'),(50,2,'超级管理员','ROOM','UPDATE','STUDY_ROOM',1,'图书馆一楼A区','2026-05-26 22:55:33'),(51,2,'超级管理员','ROOM','UPDATE','STUDY_ROOM',1,'图书馆一楼A区','2026-05-26 22:56:43'),(52,2,'超级管理员','ROOM','CREATE','STUDY_ROOM',8,'【极端测试-LIVE】自习室-LIVE-20260526-230415','2026-05-26 23:04:16'),(53,2,'超级管理员','ROOM','UPDATE','STUDY_ROOM',8,'【极端测试-LIVE】自习室-2x5-LIVE-20260526-230415','2026-05-26 23:04:16'),(54,2,'超级管理员','ADMIN','CREATE','ADMIN_ACCOUNT',4,'live_ext_live20260526230415','2026-05-26 23:04:16'),(55,2,'超级管理员','USER','APPROVE','STUDENT',10,'Live极端测试通过','2026-05-26 23:04:16'),(56,1,'张老师','FEEDBACK','HANDLE','FEEDBACK',7,'Live测试已处理-LIVE-20260526-230415','2026-05-26 23:04:16'),(57,1,'张老师','ROOM','UPDATE','STUDY_ROOM',5,'405','2026-05-26 23:08:47'),(58,1,'张老师','ROOM','UPDATE','STUDY_ROOM',5,'405','2026-05-26 23:09:32'),(59,1,'张老师','ROOM','UPDATE','STUDY_ROOM',5,'405','2026-05-26 23:10:33');
/*!40000 ALTER TABLE `operation_log` ENABLE KEYS */;
UNLOCK TABLES;

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
) ENGINE=InnoDB AUTO_INCREMENT=40 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reservation`
--

LOCK TABLES `reservation` WRITE;
/*!40000 ALTER TABLE `reservation` DISABLE KEYS */;
INSERT INTO `reservation` (`id`, `reservation_no`, `user_id`, `room_id`, `seat_id`, `reserve_date`, `start_time`, `end_time`, `status`, `sign_in_time`, `sign_out_time`, `actual_minutes`, `cancel_reason`, `created_at`, `updated_at`) VALUES (1,'R202605250001',1,1,12,'2026-05-25','09:00:00','11:00:00','VIOLATED',NULL,NULL,0,'超时未签到','2026-05-25 22:40:40','2026-05-25 23:41:00'),(2,'R202605240002',1,2,27,'2026-05-24','14:00:00','16:00:00','COMPLETED','2026-05-24 14:40:40','2026-05-24 16:40:40',120,NULL,'2026-05-23 23:40:40','2026-05-24 23:40:40'),(3,'R20260526003507001',1,1,13,'2026-05-26','09:00:00','11:00:00','VIOLATED',NULL,NULL,0,'超时未签到','2026-05-26 00:35:07','2026-05-26 12:23:00'),(4,'R20260526122341001',1,1,14,'2026-05-26','08:00:00','10:00:00','CANCELLED',NULL,NULL,0,'学生主动取消','2026-05-26 12:23:42','2026-05-26 12:23:57'),(5,'R20260526183223001',1,1,13,'2026-05-26','19:00:00','21:00:00','COMPLETED','2026-05-26 19:00:09','2026-05-26 19:04:10',4,NULL,'2026-05-26 18:32:24','2026-05-26 19:04:10'),(6,'R20260526190416001',1,1,13,'2026-05-26','09:00:00','11:00:00','AUTO_CHECKOUT','2026-05-26 19:04:26','2026-05-26 11:00:00',0,NULL,'2026-05-26 19:04:16','2026-05-26 19:05:00'),(7,'R20260526190744001',1,1,14,'2026-05-26','09:00:00','11:00:00','VIOLATED',NULL,NULL,0,'超时未签到','2026-05-26 19:07:44','2026-05-26 19:08:00'),(8,'R20260526191343001',1,1,13,'2026-05-26','19:00:00','21:00:00','COMPLETED','2026-05-26 19:14:04','2026-05-26 19:21:27',7,NULL,'2026-05-26 19:13:43','2026-05-26 19:21:27'),(9,'R20260526192136001',1,1,13,'2026-05-26','19:00:00','21:00:00','CANCELLED',NULL,NULL,0,'学生主动取消','2026-05-26 19:21:37','2026-05-26 19:21:48'),(10,'R20260526192240001',1,1,13,'2026-05-26','19:20:00','21:00:00','COMPLETED','2026-05-26 19:22:48','2026-05-26 19:22:56',0,NULL,'2026-05-26 19:22:41','2026-05-26 19:22:56'),(11,'R20260526192445001',1,1,13,'2026-05-26','19:20:00','21:00:00','COMPLETED','2026-05-26 19:24:50','2026-05-26 19:24:58',0,NULL,'2026-05-26 19:24:46','2026-05-26 19:24:58'),(12,'R20260526202301001',1,1,13,'2026-05-26','20:20:00','21:00:00','COMPLETED','2026-05-26 20:24:07','2026-05-26 20:29:38',5,NULL,'2026-05-26 20:23:02','2026-05-26 20:29:38'),(13,'R20260526210051001',1,1,13,'2026-05-26','21:00:00','21:10:00','COMPLETED','2026-05-26 21:01:00','2026-05-26 21:02:30',1,NULL,'2026-05-26 21:00:52','2026-05-26 21:02:30'),(14,'R20260526210439001',1,1,13,'2026-05-26','21:10:00','21:20:00','COMPLETED','2026-05-26 21:05:33','2026-05-26 21:06:08',0,NULL,'2026-05-26 21:04:39','2026-05-26 21:06:08'),(15,'R20260526210622001',1,1,13,'2026-05-26','21:00:00','21:10:00','COMPLETED','2026-05-26 21:06:29','2026-05-26 21:08:17',1,NULL,'2026-05-26 21:06:22','2026-05-26 21:08:17'),(16,'R20260526211556001',1,1,13,'2026-05-26','21:10:00','21:20:00','VIOLATED',NULL,NULL,0,'超时未签到','2026-05-26 21:15:57','2026-05-26 21:25:00'),(17,'R20260526212238001',1,1,24,'2026-05-26','08:00:00','10:00:00','VIOLATED',NULL,NULL,0,'超时未签到','2026-05-26 21:22:38','2026-05-26 21:23:00'),(18,'R20260526212659006',6,1,13,'2026-05-26','21:40:00','22:10:00','COMPLETED','2026-05-26 21:28:13','2026-05-26 21:28:25',0,NULL,'2026-05-26 21:27:00','2026-05-26 21:28:25'),(19,'R20260526213231007',7,5,105,'2026-05-26','09:00:00','11:00:00','VIOLATED',NULL,NULL,0,'超时未签到','2026-05-26 21:32:32','2026-05-26 21:33:00'),(20,'R20260526213335007',7,5,105,'2026-05-27','09:00:00','11:00:00','CANCELLED',NULL,NULL,0,'学生主动取消','2026-05-26 21:33:36','2026-05-26 21:36:50'),(21,'R20260526213457001',1,1,13,'2026-05-26','21:30:00','21:40:00','COMPLETED','2026-05-26 21:35:10','2026-05-26 21:35:16',0,NULL,'2026-05-26 21:34:57','2026-05-26 21:35:16'),(22,'R20260526213526001',1,1,14,'2026-05-26','21:30:00','21:40:00','COMPLETED','2026-05-26 21:36:50','2026-05-26 21:37:03',0,NULL,'2026-05-26 21:35:26','2026-05-26 21:37:03'),(23,'R20260526213617007',7,5,105,'2026-05-26','21:30:00','21:40:00','AUTO_CHECKOUT','2026-05-26 21:37:23','2026-05-26 21:40:00',2,NULL,'2026-05-26 21:36:17','2026-05-26 21:41:00'),(24,'R20260526213714001',1,1,13,'2026-05-26','21:40:00','21:50:00','AUTO_CHECKOUT','2026-05-26 21:40:43','2026-05-26 21:50:00',9,NULL,'2026-05-26 21:37:14','2026-05-26 21:51:00'),(25,'R20260526215555006',6,1,11,'2026-05-26','08:50:00','11:10:00','VIOLATED',NULL,NULL,0,'超时未签到','2026-05-26 21:55:55','2026-05-26 21:56:00'),(26,'R20260526215629001',1,1,13,'2026-05-26','21:50:00','22:10:00','VIOLATED',NULL,NULL,0,'超时未签到','2026-05-26 21:56:29','2026-05-26 22:05:00'),(27,'R20260526220210006',6,2,28,'2026-05-27','08:00:00','11:00:00','PENDING',NULL,NULL,0,NULL,'2026-05-26 22:02:10','2026-05-26 22:02:10'),(28,'R20260526222640007',7,4,79,'2026-05-26','22:20:00','22:30:00','AUTO_CHECKOUT','2026-05-26 22:27:21','2026-05-26 22:30:00',2,NULL,'2026-05-26 22:26:41','2026-05-26 22:31:00'),(29,'R20260526230004001',1,1,1,'2026-05-27','09:00:00','11:00:00','CANCELLED',NULL,NULL,0,'学生主动取消','2026-05-26 23:00:05','2026-05-26 23:02:01'),(30,'R20260526230406001',1,1,3,'2026-05-27','07:00:00','07:10:00','CANCELLED',NULL,NULL,0,'学生主动取消','2026-05-26 23:04:07','2026-05-26 23:04:14'),(31,'R20260526230415010',10,8,162,'2026-05-27','14:00:00','16:00:00','PENDING',NULL,NULL,0,NULL,'2026-05-26 23:04:16','2026-05-26 23:04:16'),(32,'R20260526231110001',1,1,4,'2026-05-27','07:00:00','07:10:00','CANCELLED',NULL,NULL,0,'学生主动取消','2026-05-26 23:11:10','2026-05-26 23:11:17'),(33,'R20260526231147001',1,1,15,'2026-05-26','23:00:00','23:10:00','CANCELLED',NULL,NULL,0,'学生主动取消','2026-05-26 23:11:47','2026-05-26 23:11:57'),(34,'R20260526231958001',1,1,21,'2026-05-27','20:50:00','22:20:00','CANCELLED',NULL,NULL,0,'学生主动取消','2026-05-26 23:19:58','2026-05-26 23:20:02'),(35,'R20260526232039001',1,1,21,'2026-05-26','23:10:00','23:20:00','CANCELLED',NULL,NULL,0,'学生主动取消','2026-05-26 23:20:39','2026-05-26 23:20:43'),(36,'R20260526232055001',1,1,21,'2026-05-26','23:10:00','23:20:00','CANCELLED',NULL,NULL,0,'学生主动取消','2026-05-26 23:20:56','2026-05-26 23:21:04'),(37,'R20260526232109001',1,1,4,'2026-05-26','23:10:00','23:20:00','CANCELLED',NULL,NULL,0,'学生主动取消','2026-05-26 23:21:10','2026-05-26 23:21:12'),(38,'R20260526232116001',1,1,1,'2026-05-26','23:10:00','23:20:00','CANCELLED',NULL,NULL,0,'学生主动取消','2026-05-26 23:21:16','2026-05-26 23:21:22'),(39,'R20260526232136001',1,1,21,'2026-05-26','23:10:00','23:20:00','CANCELLED',NULL,NULL,0,'学生主动取消','2026-05-26 23:21:36','2026-05-26 23:21:40');
/*!40000 ALTER TABLE `reservation` ENABLE KEYS */;
UNLOCK TABLES;

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
) ENGINE=InnoDB AUTO_INCREMENT=232 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reservation_slot`
--

LOCK TABLES `reservation_slot` WRITE;
/*!40000 ALTER TABLE `reservation_slot` DISABLE KEYS */;
INSERT INTO `reservation_slot` (`id`, `reservation_id`, `seat_id`, `slot_start`, `slot_end`, `status`) VALUES (37,6,13,'2026-05-26 09:00:00','2026-05-26 09:10:00','ACTIVE'),(38,6,13,'2026-05-26 09:10:00','2026-05-26 09:20:00','ACTIVE'),(39,6,13,'2026-05-26 09:20:00','2026-05-26 09:30:00','ACTIVE'),(40,6,13,'2026-05-26 09:30:00','2026-05-26 09:40:00','ACTIVE'),(41,6,13,'2026-05-26 09:40:00','2026-05-26 09:50:00','ACTIVE'),(42,6,13,'2026-05-26 09:50:00','2026-05-26 10:00:00','ACTIVE'),(43,6,13,'2026-05-26 10:00:00','2026-05-26 10:10:00','ACTIVE'),(44,6,13,'2026-05-26 10:10:00','2026-05-26 10:20:00','ACTIVE'),(45,6,13,'2026-05-26 10:20:00','2026-05-26 10:30:00','ACTIVE'),(46,6,13,'2026-05-26 10:30:00','2026-05-26 10:40:00','ACTIVE'),(47,6,13,'2026-05-26 10:40:00','2026-05-26 10:50:00','ACTIVE'),(48,6,13,'2026-05-26 10:50:00','2026-05-26 11:00:00','ACTIVE'),(49,7,14,'2026-05-26 09:00:00','2026-05-26 09:10:00','ACTIVE'),(50,7,14,'2026-05-26 09:10:00','2026-05-26 09:20:00','ACTIVE'),(51,7,14,'2026-05-26 09:20:00','2026-05-26 09:30:00','ACTIVE'),(52,7,14,'2026-05-26 09:30:00','2026-05-26 09:40:00','ACTIVE'),(53,7,14,'2026-05-26 09:40:00','2026-05-26 09:50:00','ACTIVE'),(54,7,14,'2026-05-26 09:50:00','2026-05-26 10:00:00','ACTIVE'),(55,7,14,'2026-05-26 10:00:00','2026-05-26 10:10:00','ACTIVE'),(56,7,14,'2026-05-26 10:10:00','2026-05-26 10:20:00','ACTIVE'),(57,7,14,'2026-05-26 10:20:00','2026-05-26 10:30:00','ACTIVE'),(58,7,14,'2026-05-26 10:30:00','2026-05-26 10:40:00','ACTIVE'),(59,7,14,'2026-05-26 10:40:00','2026-05-26 10:50:00','ACTIVE'),(60,7,14,'2026-05-26 10:50:00','2026-05-26 11:00:00','ACTIVE'),(172,27,28,'2026-05-27 08:00:00','2026-05-27 08:10:00','ACTIVE'),(173,27,28,'2026-05-27 08:10:00','2026-05-27 08:20:00','ACTIVE'),(174,27,28,'2026-05-27 08:20:00','2026-05-27 08:30:00','ACTIVE'),(175,27,28,'2026-05-27 08:30:00','2026-05-27 08:40:00','ACTIVE'),(176,27,28,'2026-05-27 08:40:00','2026-05-27 08:50:00','ACTIVE'),(177,27,28,'2026-05-27 08:50:00','2026-05-27 09:00:00','ACTIVE'),(178,27,28,'2026-05-27 09:00:00','2026-05-27 09:10:00','ACTIVE'),(179,27,28,'2026-05-27 09:10:00','2026-05-27 09:20:00','ACTIVE'),(180,27,28,'2026-05-27 09:20:00','2026-05-27 09:30:00','ACTIVE'),(181,27,28,'2026-05-27 09:30:00','2026-05-27 09:40:00','ACTIVE'),(182,27,28,'2026-05-27 09:40:00','2026-05-27 09:50:00','ACTIVE'),(183,27,28,'2026-05-27 09:50:00','2026-05-27 10:00:00','ACTIVE'),(184,27,28,'2026-05-27 10:00:00','2026-05-27 10:10:00','ACTIVE'),(185,27,28,'2026-05-27 10:10:00','2026-05-27 10:20:00','ACTIVE'),(186,27,28,'2026-05-27 10:20:00','2026-05-27 10:30:00','ACTIVE'),(187,27,28,'2026-05-27 10:30:00','2026-05-27 10:40:00','ACTIVE'),(188,27,28,'2026-05-27 10:40:00','2026-05-27 10:50:00','ACTIVE'),(189,27,28,'2026-05-27 10:50:00','2026-05-27 11:00:00','ACTIVE'),(204,31,162,'2026-05-27 14:00:00','2026-05-27 14:10:00','ACTIVE'),(205,31,162,'2026-05-27 14:10:00','2026-05-27 14:20:00','ACTIVE'),(206,31,162,'2026-05-27 14:20:00','2026-05-27 14:30:00','ACTIVE'),(207,31,162,'2026-05-27 14:30:00','2026-05-27 14:40:00','ACTIVE'),(208,31,162,'2026-05-27 14:40:00','2026-05-27 14:50:00','ACTIVE'),(209,31,162,'2026-05-27 14:50:00','2026-05-27 15:00:00','ACTIVE'),(210,31,162,'2026-05-27 15:00:00','2026-05-27 15:10:00','ACTIVE'),(211,31,162,'2026-05-27 15:10:00','2026-05-27 15:20:00','ACTIVE'),(212,31,162,'2026-05-27 15:20:00','2026-05-27 15:30:00','ACTIVE'),(213,31,162,'2026-05-27 15:30:00','2026-05-27 15:40:00','ACTIVE'),(214,31,162,'2026-05-27 15:40:00','2026-05-27 15:50:00','ACTIVE'),(215,31,162,'2026-05-27 15:50:00','2026-05-27 16:00:00','ACTIVE');
/*!40000 ALTER TABLE `reservation_slot` ENABLE KEYS */;
UNLOCK TABLES;

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
) ENGINE=InnoDB AUTO_INCREMENT=176 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `seat`
--

LOCK TABLES `seat` WRITE;
/*!40000 ALTER TABLE `seat` DISABLE KEYS */;
INSERT INTO `seat` (`id`, `room_id`, `seat_no`, `row_no`, `col_no`, `is_seat`, `cell_category`, `seat_type`, `has_power`, `near_window`, `quiet_zone`, `hot_seat`, `status`, `created_at`, `updated_at`) VALUES (1,1,'A-01',1,1,1,'SEAT','普通座位',0,1,1,0,'NORMAL','2026-05-25 23:40:41','2026-05-25 23:40:41'),(2,1,'A-02',1,2,1,'SEAT','普通座位',1,0,1,0,'NORMAL','2026-05-25 23:40:41','2026-05-25 23:40:41'),(3,1,'A-03',1,3,1,'SEAT','普通座位',0,0,1,1,'NORMAL','2026-05-25 23:40:41','2026-05-25 23:40:41'),(4,1,'A-04',1,4,1,'SEAT','普通座位',1,0,1,0,'NORMAL','2026-05-25 23:40:41','2026-05-25 23:40:41'),(5,1,'A-05',1,5,1,'SEAT','普通座位',0,0,1,0,'NORMAL','2026-05-25 23:40:41','2026-05-25 23:40:41'),(6,1,'A-06',1,6,1,'SEAT','普通座位',1,1,1,0,'NORMAL','2026-05-25 23:40:41','2026-05-25 23:40:41'),(7,1,'A-07',2,1,1,'SEAT','普通座位',0,1,1,0,'NORMAL','2026-05-25 23:40:41','2026-05-26 17:56:20'),(8,1,'A-08',2,2,1,'SEAT','普通座位',1,0,1,0,'NORMAL','2026-05-25 23:40:41','2026-05-25 23:40:41'),(9,1,'A-09',2,3,1,'SEAT','普通座位',0,0,1,1,'NORMAL','2026-05-25 23:40:41','2026-05-25 23:40:41'),(10,1,'A-10',2,4,1,'SEAT','普通座位',1,0,1,0,'NORMAL','2026-05-25 23:40:41','2026-05-25 23:40:41'),(11,1,'A-11',2,5,1,'SEAT','普通座位',0,0,1,0,'NORMAL','2026-05-25 23:40:41','2026-05-25 23:40:41'),(12,1,'A-12',2,6,1,'SEAT','普通座位',1,1,1,0,'NORMAL','2026-05-25 23:40:41','2026-05-25 23:40:41'),(13,1,'A-13',3,1,1,'SEAT','普通座位',0,1,0,0,'NORMAL','2026-05-25 23:40:41','2026-05-26 17:56:35'),(14,1,'A-14',3,2,1,'SEAT','普通座位',1,0,0,0,'NORMAL','2026-05-25 23:40:41','2026-05-25 23:40:41'),(15,1,'A-15',3,3,1,'SEAT','普通座位',0,0,0,1,'NORMAL','2026-05-25 23:40:41','2026-05-25 23:40:41'),(16,1,'A-16',3,4,1,'SEAT','普通座位',1,0,0,0,'NORMAL','2026-05-25 23:40:41','2026-05-25 23:40:41'),(17,1,'A-17',3,5,1,'SEAT','普通座位',0,0,0,0,'NORMAL','2026-05-25 23:40:41','2026-05-25 23:40:41'),(18,1,'A-18',3,6,1,'SEAT','普通座位',1,1,0,0,'NORMAL','2026-05-25 23:40:41','2026-05-25 23:40:41'),(19,1,'A-19',4,1,1,'SEAT','普通座位',0,1,0,0,'NORMAL','2026-05-25 23:40:41','2026-05-25 23:40:41'),(20,1,'A-20',4,2,0,'NON_SEAT','过道',1,0,0,0,'DISABLED','2026-05-25 23:40:41','2026-05-25 23:40:41'),(21,1,'A-21',4,3,1,'SEAT','普通座位',0,0,0,1,'NORMAL','2026-05-25 23:40:41','2026-05-25 23:40:41'),(22,1,'A-22',4,4,1,'SEAT','普通座位',1,0,0,0,'NORMAL','2026-05-25 23:40:41','2026-05-25 23:40:41'),(23,1,'A-23',4,5,0,'NON_SEAT','过道',0,0,0,0,'DISABLED','2026-05-25 23:40:41','2026-05-25 23:40:41'),(24,1,'A-24',4,6,1,'SEAT','普通座位',1,1,0,0,'NORMAL','2026-05-25 23:40:41','2026-05-25 23:40:41'),(25,2,'B-01',1,1,1,'SEAT','普通座位',0,1,1,0,'NORMAL','2026-05-25 23:40:41','2026-05-25 23:40:41'),(26,2,'B-02',1,2,1,'SEAT','普通座位',1,0,1,0,'NORMAL','2026-05-25 23:40:41','2026-05-25 23:40:41'),(27,2,'B-03',1,3,1,'SEAT','普通座位',0,0,1,1,'NORMAL','2026-05-25 23:40:41','2026-05-25 23:40:41'),(28,2,'B-04',1,4,1,'SEAT','普通座位',1,0,1,0,'NORMAL','2026-05-25 23:40:41','2026-05-25 23:40:41'),(29,2,'B-05',1,5,1,'SEAT','普通座位',0,0,1,0,'NORMAL','2026-05-25 23:40:41','2026-05-25 23:40:41'),(30,2,'B-06',1,6,1,'SEAT','普通座位',1,1,1,0,'NORMAL','2026-05-25 23:40:41','2026-05-25 23:40:41'),(31,2,'B-07',2,1,1,'SEAT','普通座位',0,1,1,0,'NORMAL','2026-05-25 23:40:41','2026-05-25 23:40:41'),(32,2,'B-08',2,2,1,'SEAT','普通座位',1,0,1,0,'NORMAL','2026-05-25 23:40:41','2026-05-25 23:40:41'),(33,2,'B-09',2,3,1,'SEAT','普通座位',0,0,1,1,'NORMAL','2026-05-25 23:40:41','2026-05-25 23:40:41'),(34,2,'B-10',2,4,1,'SEAT','普通座位',1,0,1,0,'NORMAL','2026-05-25 23:40:41','2026-05-25 23:40:41'),(35,2,'B-11',2,5,1,'SEAT','普通座位',0,0,1,0,'NORMAL','2026-05-25 23:40:41','2026-05-25 23:40:41'),(36,2,'B-12',2,6,1,'SEAT','普通座位',1,1,1,0,'NORMAL','2026-05-25 23:40:41','2026-05-25 23:40:41'),(37,2,'B-13',3,1,1,'SEAT','普通座位',0,1,0,0,'NORMAL','2026-05-25 23:40:41','2026-05-25 23:40:41'),(38,2,'B-14',3,2,1,'SEAT','普通座位',1,0,0,0,'NORMAL','2026-05-25 23:40:41','2026-05-25 23:40:41'),(39,2,'B-15',3,3,1,'SEAT','普通座位',0,0,0,1,'NORMAL','2026-05-25 23:40:41','2026-05-25 23:40:41'),(40,2,'B-16',3,4,1,'SEAT','普通座位',1,0,0,0,'NORMAL','2026-05-25 23:40:41','2026-05-25 23:40:41'),(41,2,'B-17',3,5,1,'SEAT','普通座位',0,0,0,0,'NORMAL','2026-05-25 23:40:41','2026-05-25 23:40:41'),(42,2,'B-18',3,6,1,'SEAT','普通座位',1,1,0,0,'NORMAL','2026-05-25 23:40:41','2026-05-25 23:40:41'),(43,2,'B-19',4,1,1,'SEAT','普通座位',0,1,0,0,'NORMAL','2026-05-25 23:40:41','2026-05-25 23:40:41'),(44,2,'B-20',4,2,0,'NON_SEAT','过道',1,0,0,0,'DISABLED','2026-05-25 23:40:41','2026-05-25 23:40:41'),(45,2,'B-21',4,3,1,'SEAT','普通座位',0,0,0,1,'NORMAL','2026-05-25 23:40:41','2026-05-25 23:40:41'),(46,2,'B-22',4,4,1,'SEAT','普通座位',1,0,0,0,'NORMAL','2026-05-25 23:40:41','2026-05-25 23:40:41'),(47,2,'B-23',4,5,0,'NON_SEAT','过道',0,0,0,0,'DISABLED','2026-05-25 23:40:41','2026-05-25 23:40:41'),(48,2,'B-24',4,6,1,'SEAT','普通座位',1,1,0,0,'NORMAL','2026-05-25 23:40:41','2026-05-25 23:40:41'),(49,3,'C-01',1,1,1,'SEAT','普通座位',0,1,1,0,'NORMAL','2026-05-25 23:40:41','2026-05-25 23:40:41'),(50,3,'C-02',1,2,1,'SEAT','普通座位',1,0,1,0,'NORMAL','2026-05-25 23:40:41','2026-05-25 23:40:41'),(51,3,'C-03',1,3,1,'SEAT','普通座位',0,0,1,1,'NORMAL','2026-05-25 23:40:41','2026-05-25 23:40:41'),(52,3,'C-04',1,4,1,'SEAT','普通座位',1,0,1,0,'NORMAL','2026-05-25 23:40:41','2026-05-25 23:40:41'),(53,3,'C-05',1,5,1,'SEAT','普通座位',0,0,1,0,'NORMAL','2026-05-25 23:40:41','2026-05-25 23:40:41'),(54,3,'C-06',1,6,1,'SEAT','普通座位',1,1,1,0,'NORMAL','2026-05-25 23:40:41','2026-05-25 23:40:41'),(55,3,'C-07',2,1,1,'SEAT','普通座位',0,1,1,0,'NORMAL','2026-05-25 23:40:41','2026-05-25 23:40:41'),(56,3,'C-08',2,2,1,'SEAT','普通座位',1,0,1,0,'NORMAL','2026-05-25 23:40:41','2026-05-25 23:40:41'),(57,3,'C-09',2,3,1,'SEAT','普通座位',0,0,1,1,'NORMAL','2026-05-25 23:40:41','2026-05-25 23:40:41'),(58,3,'C-10',2,4,1,'SEAT','普通座位',1,0,1,0,'NORMAL','2026-05-25 23:40:41','2026-05-25 23:40:41'),(59,3,'C-11',2,5,1,'SEAT','普通座位',0,0,1,0,'NORMAL','2026-05-25 23:40:41','2026-05-25 23:40:41'),(60,3,'C-12',2,6,1,'SEAT','普通座位',1,1,1,0,'NORMAL','2026-05-25 23:40:41','2026-05-25 23:40:41'),(61,3,'C-13',3,1,1,'SEAT','普通座位',0,1,0,0,'NORMAL','2026-05-25 23:40:41','2026-05-25 23:40:41'),(62,3,'C-14',3,2,0,'NON_SEAT','过道',1,0,0,0,'DISABLED','2026-05-25 23:40:41','2026-05-25 23:40:41'),(63,3,'C-15',3,3,1,'SEAT','普通座位',0,0,0,1,'NORMAL','2026-05-25 23:40:41','2026-05-25 23:40:41'),(64,3,'C-16',3,4,1,'SEAT','普通座位',1,0,0,0,'NORMAL','2026-05-25 23:40:41','2026-05-25 23:40:41'),(65,3,'C-17',3,5,0,'NON_SEAT','过道',0,0,0,0,'DISABLED','2026-05-25 23:40:41','2026-05-25 23:40:41'),(66,3,'C-18',3,6,1,'SEAT','普通座位',1,1,0,0,'NORMAL','2026-05-25 23:40:41','2026-05-25 23:40:41'),(67,4,'N-01',1,1,1,'SEAT','普通座位',0,1,1,0,'NORMAL','2026-05-26 18:27:35','2026-05-26 18:27:35'),(68,4,'N-02',1,2,1,'SEAT','普通座位',1,0,1,0,'NORMAL','2026-05-26 18:27:35','2026-05-26 18:27:35'),(69,4,'N-03',1,3,1,'SEAT','普通座位',0,0,1,0,'NORMAL','2026-05-26 18:27:35','2026-05-26 18:27:35'),(70,4,'N-04',1,4,1,'SEAT','普通座位',1,0,1,0,'NORMAL','2026-05-26 18:27:35','2026-05-26 18:27:35'),(71,4,'N-05',1,5,1,'SEAT','普通座位',0,0,1,0,'NORMAL','2026-05-26 18:27:35','2026-05-26 18:27:35'),(72,4,'N-06',1,6,1,'SEAT','普通座位',1,1,1,0,'NORMAL','2026-05-26 18:27:35','2026-05-26 18:27:35'),(73,4,'N-07',2,1,1,'SEAT','普通座位',0,1,1,0,'NORMAL','2026-05-26 18:27:35','2026-05-26 18:27:35'),(74,4,'N-08',2,2,1,'SEAT','普通座位',1,0,1,0,'NORMAL','2026-05-26 18:27:35','2026-05-26 18:27:35'),(75,4,'N-09',2,3,1,'SEAT','普通座位',0,0,1,0,'NORMAL','2026-05-26 18:27:35','2026-05-26 18:27:35'),(76,4,'N-10',2,4,1,'SEAT','普通座位',1,0,1,0,'NORMAL','2026-05-26 18:27:35','2026-05-26 18:27:35'),(77,4,'N-11',2,5,1,'SEAT','普通座位',0,0,1,0,'NORMAL','2026-05-26 18:27:35','2026-05-26 18:27:35'),(78,4,'N-12',2,6,1,'SEAT','普通座位',1,1,1,0,'NORMAL','2026-05-26 18:27:35','2026-05-26 18:27:35'),(79,4,'N-13',3,1,1,'SEAT','普通座位',0,1,0,0,'NORMAL','2026-05-26 18:27:35','2026-05-26 18:27:35'),(80,4,'N-14',3,2,1,'SEAT','普通座位',1,0,0,0,'NORMAL','2026-05-26 18:27:35','2026-05-26 18:27:35'),(81,4,'N-15',3,3,1,'SEAT','普通座位',0,0,0,0,'NORMAL','2026-05-26 18:27:35','2026-05-26 18:27:35'),(82,4,'N-16',3,4,1,'SEAT','普通座位',1,0,0,0,'NORMAL','2026-05-26 18:27:35','2026-05-26 18:27:35'),(83,4,'N-17',3,5,1,'SEAT','普通座位',0,0,0,0,'NORMAL','2026-05-26 18:27:35','2026-05-26 18:27:35'),(84,4,'N-18',3,6,1,'SEAT','普通座位',1,1,0,0,'NORMAL','2026-05-26 18:27:35','2026-05-26 18:27:35'),(85,4,'N-19',4,1,1,'SEAT','普通座位',0,1,0,0,'NORMAL','2026-05-26 18:27:35','2026-05-26 18:27:35'),(86,4,'N-20',4,2,1,'SEAT','普通座位',1,0,0,0,'NORMAL','2026-05-26 18:27:35','2026-05-26 18:27:35'),(87,4,'N-21',4,3,1,'SEAT','普通座位',0,0,0,0,'NORMAL','2026-05-26 18:27:35','2026-05-26 18:27:35'),(88,4,'N-22',4,4,1,'SEAT','普通座位',1,0,0,0,'NORMAL','2026-05-26 18:27:35','2026-05-26 18:27:35'),(92,5,'N-02',1,2,1,'SEAT','普通座位',1,0,1,0,'NORMAL','2026-05-26 21:25:35','2026-05-26 21:25:35'),(93,5,'N-03',1,3,1,'SEAT','普通座位',0,0,1,0,'NORMAL','2026-05-26 21:25:35','2026-05-26 21:25:35'),(94,5,'N-04',1,4,1,'SEAT','普通座位',1,0,1,0,'NORMAL','2026-05-26 21:25:35','2026-05-26 21:25:35'),(95,5,'N-05',1,5,1,'SEAT','普通座位',0,0,1,0,'NORMAL','2026-05-26 21:25:35','2026-05-26 21:25:35'),(96,5,'N-06',1,6,1,'SEAT','普通座位',1,0,1,0,'NORMAL','2026-05-26 21:25:35','2026-05-26 21:25:35'),(97,5,'N-07',1,7,1,'SEAT','普通座位',1,1,1,0,'NORMAL','2026-05-26 21:25:35','2026-05-26 21:31:23'),(98,5,'N-08',2,1,1,'SEAT','普通座位',0,1,1,0,'NORMAL','2026-05-26 21:25:35','2026-05-26 21:25:35'),(99,5,'N-09',2,2,1,'SEAT','普通座位',1,0,1,0,'NORMAL','2026-05-26 21:25:35','2026-05-26 21:25:35'),(100,5,'N-10',2,3,1,'SEAT','普通座位',0,0,1,0,'NORMAL','2026-05-26 21:25:35','2026-05-26 21:25:35'),(101,5,'N-11',2,4,1,'SEAT','普通座位',1,0,1,0,'NORMAL','2026-05-26 21:25:35','2026-05-26 21:25:35'),(102,5,'N-12',2,5,1,'SEAT','普通座位',0,0,1,0,'NORMAL','2026-05-26 21:25:35','2026-05-26 21:25:35'),(103,5,'N-13',2,6,1,'SEAT','普通座位',1,0,1,0,'NORMAL','2026-05-26 21:25:35','2026-05-26 21:25:35'),(104,5,'N-14',2,7,1,'SEAT','普通座位',0,1,1,0,'NORMAL','2026-05-26 21:25:35','2026-05-26 21:25:35'),(105,5,'N-15',3,1,1,'SEAT','普通座位',0,1,0,0,'NORMAL','2026-05-26 21:25:35','2026-05-26 21:25:35'),(106,5,'N-16',3,2,1,'SEAT','普通座位',1,0,0,0,'NORMAL','2026-05-26 21:25:35','2026-05-26 21:25:35'),(107,5,'N-17',3,3,1,'SEAT','普通座位',0,0,0,0,'NORMAL','2026-05-26 21:25:35','2026-05-26 21:25:35'),(108,5,'N-18',3,4,1,'SEAT','普通座位',1,0,0,0,'NORMAL','2026-05-26 21:25:35','2026-05-26 21:25:35'),(109,5,'N-19',3,5,1,'SEAT','普通座位',0,0,0,0,'NORMAL','2026-05-26 21:25:35','2026-05-26 21:25:35'),(110,5,'N-20',3,6,1,'SEAT','普通座位',1,0,0,0,'NORMAL','2026-05-26 21:25:35','2026-05-26 21:25:35'),(111,5,'N-21',3,7,1,'SEAT','普通座位',0,1,0,0,'NORMAL','2026-05-26 21:25:35','2026-05-26 21:25:35'),(112,5,'N-22',4,1,1,'SEAT','普通座位',0,1,0,0,'NORMAL','2026-05-26 21:25:35','2026-05-26 21:25:35'),(113,5,'N-23',4,2,1,'SEAT','普通座位',1,0,0,0,'NORMAL','2026-05-26 21:25:35','2026-05-26 21:25:35'),(114,5,'N-24',4,3,1,'SEAT','普通座位',0,0,0,0,'NORMAL','2026-05-26 21:25:35','2026-05-26 21:25:35'),(115,5,'N-25',4,4,1,'SEAT','普通座位',1,0,0,0,'NORMAL','2026-05-26 21:25:35','2026-05-26 21:25:35'),(116,5,'N-26',4,5,1,'SEAT','普通座位',0,0,0,0,'NORMAL','2026-05-26 21:25:35','2026-05-26 21:25:35'),(117,5,'N-27',4,6,1,'SEAT','普通座位',1,0,0,0,'NORMAL','2026-05-26 21:25:35','2026-05-26 21:25:35'),(118,5,'N-28',4,7,1,'SEAT','普通座位',0,1,0,0,'NORMAL','2026-05-26 21:25:35','2026-05-26 21:25:35'),(119,5,'N-29',5,1,1,'SEAT','普通座位',0,1,0,0,'NORMAL','2026-05-26 21:25:35','2026-05-26 21:25:35'),(120,5,'N-30',5,2,1,'SEAT','普通座位',1,0,0,0,'NORMAL','2026-05-26 21:25:35','2026-05-26 21:25:35'),(121,5,'N-31',5,3,1,'SEAT','普通座位',0,0,0,0,'NORMAL','2026-05-26 21:25:35','2026-05-26 21:25:35'),(122,5,'N-32',5,4,1,'SEAT','普通座位',1,0,0,0,'NORMAL','2026-05-26 21:25:35','2026-05-26 21:25:35'),(123,5,'N-33',5,5,1,'SEAT','普通座位',0,0,0,0,'NORMAL','2026-05-26 21:25:35','2026-05-26 21:25:35'),(124,5,'N-34',5,6,1,'SEAT','普通座位',1,0,0,0,'NORMAL','2026-05-26 21:25:35','2026-05-26 21:25:35'),(125,5,'N-35',5,7,1,'SEAT','普通座位',0,1,0,0,'NORMAL','2026-05-26 21:25:35','2026-05-26 21:25:35'),(126,5,'N-36',6,1,1,'SEAT','普通座位',0,1,0,0,'NORMAL','2026-05-26 21:25:35','2026-05-26 21:25:35'),(127,5,'N-37',6,2,1,'SEAT','普通座位',1,0,0,0,'NORMAL','2026-05-26 21:25:35','2026-05-26 21:25:35'),(128,5,'N-38',6,3,1,'SEAT','普通座位',0,0,0,0,'NORMAL','2026-05-26 21:25:35','2026-05-26 21:25:35'),(129,5,'N-39',6,4,1,'SEAT','普通座位',1,0,0,0,'NORMAL','2026-05-26 21:25:35','2026-05-26 21:25:35'),(130,5,'N-40',6,5,1,'SEAT','普通座位',0,0,0,0,'NORMAL','2026-05-26 21:25:35','2026-05-26 21:25:35'),(131,5,'N-41',6,6,1,'SEAT','普通座位',1,0,0,0,'NORMAL','2026-05-26 21:25:35','2026-05-26 21:25:35'),(132,5,'N-42',6,7,1,'SEAT','普通座位',0,1,0,0,'NORMAL','2026-05-26 21:25:35','2026-05-26 21:25:35'),(133,5,'N-43',7,1,1,'SEAT','普通座位',0,1,0,0,'NORMAL','2026-05-26 21:25:35','2026-05-26 21:25:35'),(134,5,'N-44',7,2,1,'SEAT','普通座位',1,0,0,0,'NORMAL','2026-05-26 21:25:35','2026-05-26 21:25:35'),(135,5,'N-45',7,3,1,'SEAT','普通座位',0,0,0,0,'NORMAL','2026-05-26 21:25:35','2026-05-26 21:25:35'),(136,5,'N-46',7,4,1,'SEAT','普通座位',1,0,0,0,'NORMAL','2026-05-26 21:25:35','2026-05-26 21:25:35'),(137,5,'N-47',7,5,1,'SEAT','普通座位',0,0,0,0,'NORMAL','2026-05-26 21:25:35','2026-05-26 21:25:35'),(138,5,'N-48',7,6,1,'SEAT','普通座位',1,0,0,0,'NORMAL','2026-05-26 21:25:35','2026-05-26 21:25:35'),(139,5,'N-49',7,7,1,'SEAT','普通座位',0,1,0,0,'NORMAL','2026-05-26 21:25:35','2026-05-26 21:25:35'),(140,5,'100-49',7,7,1,'SEAT','普通座位',0,1,0,0,'NORMAL','2026-05-26 21:31:41','2026-05-26 21:31:41'),(141,4,'1-25',4,4,1,'SEAT','普通座位',1,0,0,0,'NORMAL','2026-05-26 22:23:15','2026-05-26 22:23:15'),(142,4,'1-26',4,2,1,'SEAT','普通座位',1,0,0,0,'NORMAL','2026-05-26 22:23:21','2026-05-26 22:23:21'),(143,6,'N-01',1,1,1,'SEAT','普通座位',0,1,1,0,'NORMAL','2026-05-26 22:41:27','2026-05-26 22:41:27'),(144,6,'N-02',1,2,1,'SEAT','普通座位',1,0,1,0,'NORMAL','2026-05-26 22:41:27','2026-05-26 22:41:27'),(145,6,'N-03',1,3,1,'SEAT','普通座位',0,0,1,0,'NORMAL','2026-05-26 22:41:27','2026-05-26 22:41:27'),(146,6,'N-04',1,4,1,'SEAT','普通座位',1,0,1,0,'NORMAL','2026-05-26 22:41:27','2026-05-26 22:41:27'),(147,6,'N-05',1,5,1,'SEAT','普通座位',0,0,1,0,'NORMAL','2026-05-26 22:41:27','2026-05-26 22:41:27'),(148,6,'N-06',1,6,1,'SEAT','普通座位',1,1,1,0,'NORMAL','2026-05-26 22:41:27','2026-05-26 22:41:27'),(149,6,'N-07',2,1,1,'SEAT','普通座位',0,1,1,0,'NORMAL','2026-05-26 22:41:27','2026-05-26 22:41:27'),(150,6,'N-08',2,2,1,'SEAT','普通座位',1,0,1,0,'NORMAL','2026-05-26 22:41:27','2026-05-26 22:41:27'),(151,6,'N-09',2,3,1,'SEAT','普通座位',0,0,1,0,'NORMAL','2026-05-26 22:41:27','2026-05-26 22:41:27'),(152,6,'N-10',2,4,1,'SEAT','普通座位',1,0,1,0,'NORMAL','2026-05-26 22:41:27','2026-05-26 22:41:27'),(153,6,'N-11',2,5,1,'SEAT','普通座位',0,0,1,0,'NORMAL','2026-05-26 22:41:27','2026-05-26 22:41:27'),(154,6,'N-12',2,6,1,'SEAT','普通座位',1,1,1,0,'NORMAL','2026-05-26 22:41:27','2026-05-26 22:41:27'),(155,7,'N-01',1,1,1,'SEAT','普通座位',0,1,1,0,'NORMAL','2026-05-26 22:44:14','2026-05-26 22:44:14'),(156,7,'N-02',1,2,1,'SEAT','普通座位',1,0,1,0,'NORMAL','2026-05-26 22:44:14','2026-05-26 22:44:14'),(157,7,'N-03',1,3,1,'SEAT','普通座位',0,0,1,0,'NORMAL','2026-05-26 22:44:14','2026-05-26 22:44:14'),(158,7,'N-04',1,4,1,'SEAT','普通座位',1,0,1,0,'NORMAL','2026-05-26 22:44:14','2026-05-26 22:44:14'),(159,7,'N-05',1,5,1,'SEAT','普通座位',0,0,1,0,'NORMAL','2026-05-26 22:44:14','2026-05-26 22:44:14'),(160,7,'N-06',1,6,1,'SEAT','普通座位',1,1,1,0,'NORMAL','2026-05-26 22:44:14','2026-05-26 22:44:14'),(161,7,'4-07',1,7,1,'SEAT','普通座位',0,1,1,0,'NORMAL','2026-05-26 22:44:20','2026-05-26 22:44:20'),(162,8,'L0526230415-01',1,1,1,'SEAT','普通座位',0,1,1,0,'NORMAL','2026-05-26 23:04:16','2026-05-26 23:04:16'),(163,8,'L0526230415-02',1,2,1,'SEAT','普通座位',1,0,1,0,'NORMAL','2026-05-26 23:04:16','2026-05-26 23:04:16'),(164,8,'L0526230415-03',1,3,1,'SEAT','普通座位',0,0,1,0,'NORMAL','2026-05-26 23:04:16','2026-05-26 23:04:16'),(165,8,'L0526230415-04',1,4,1,'SEAT','普通座位',1,1,1,0,'NORMAL','2026-05-26 23:04:16','2026-05-26 23:04:16'),(166,8,'L0526230415-06',2,1,1,'SEAT','普通座位',0,1,1,0,'NORMAL','2026-05-26 23:04:16','2026-05-26 23:04:16'),(167,8,'L0526230415-07',2,2,1,'SEAT','普通座位',1,0,1,0,'NORMAL','2026-05-26 23:04:16','2026-05-26 23:04:16'),(168,8,'L0526230415-08',2,3,1,'SEAT','普通座位',0,0,1,0,'NORMAL','2026-05-26 23:04:16','2026-05-26 23:04:16'),(169,8,'L0526230415-09',2,4,1,'SEAT','普通座位',1,1,1,0,'NORMAL','2026-05-26 23:04:16','2026-05-26 23:04:16'),(174,8,'L0526230415-05',1,5,1,'SEAT','普通座位',0,1,1,0,'NORMAL','2026-05-26 23:04:16','2026-05-26 23:04:16'),(175,8,'L0526230415-10',2,5,1,'SEAT','普通座位',0,1,1,0,'NORMAL','2026-05-26 23:04:16','2026-05-26 23:04:16');
/*!40000 ALTER TABLE `seat` ENABLE KEYS */;
UNLOCK TABLES;

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
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `student_profile`
--

LOCK TABLES `student_profile` WRITE;
/*!40000 ALTER TABLE `student_profile` DISABLE KEYS */;
INSERT INTO `student_profile` (`id`, `user_id`, `student_no`, `name`, `gender`, `college`, `major`, `grade`, `phone`, `email`, `material_url`, `audit_status`, `audit_remark`, `credit_score`, `created_at`, `updated_at`) VALUES (1,1,'202301010101','张三','男','计算机科学与技术学院','软件工程','2023级','13152940518','202301010101@campus.edu.cn','/uploads/material/demo.pdf','APPROVED','1',280,'2026-05-25 23:40:41','2026-05-26 23:58:53'),(2,2,'202301010102','李四','男','计算机科学与技术学院','软件工程','2023级','13152940518','202301010102@campus.edu.cn','/uploads/material/demo.pdf','APPROVED',NULL,320,'2026-05-25 23:40:41','2026-05-26 23:58:53'),(3,3,'202301010199','待审同学','男','计算机科学与技术学院','软件工程','2023级','13152940518','202301010199@campus.edu.cn','/uploads/material/demo.pdf','APPROVED','',300,'2026-05-25 23:40:41','2026-05-26 18:25:10'),(4,4,'202411111111','李隆基','男','计算机科学与技术学院','软件工程','2024级','11111111111','359149330@qq.com','/uploads/material/6fa1f642-f81c-4be8-afc0-7f3db832a311.png','REJECTED','测试',300,'2026-05-26 18:34:05','2026-05-26 18:34:54'),(5,5,'123456789000','李四','男','计算机科学与技术学院','软件工程','2023级','12345678900','123456@qq.com','/uploads/material/7febb20c-1478-48d0-aece-99beb3c54b6c.jpg','APPROVED','',300,'2026-05-26 21:19:59','2026-05-26 21:25:31'),(6,6,'111111111111','L','男','计算机科学与技术学院','软件工程','2024级','11111111111','1@qq.com','/uploads/material/38a8f029-fcf3-4bc7-911c-b7d2a237345e.png','APPROVED','',255,'2026-05-26 21:25:20','2026-05-26 21:56:00'),(7,7,'202400088880','梁增杰','男','数学与信息学院、软件学院','软件工程','2024级','13928912147','3899026789@qq.com','/uploads/material/1e7c15ad-4241-454c-abf2-53be2c588102.jpg','APPROVED','',260,'2026-05-26 21:29:56','2026-05-26 22:27:21'),(8,8,'000000000000','D','男','计算机科学与技术学院','软件工程','2024级','19900000000','1@gmail.com','/uploads/material/17bb7918-4a7a-4e2f-aa32-fb2addef7068.png','APPROVED','',300,'2026-05-26 21:58:10','2026-05-26 21:58:18'),(9,9,'222222222222','W','男','计算机科学与技术学院','软件工程','2024级','19902900000','1@gmail.com','/uploads/material/343d7471-b225-443a-ad11-d1818f2e8804.png','APPROVED','',300,'2026-05-26 22:00:57','2026-05-26 22:32:05'),(10,10,'LIVE2026052623041599','【极端测试-LIVE】学生','男','Live学院','测试','2025','','','/uploads/material/register.pdf','APPROVED','Live极端测试通过',300,'2026-05-26 23:04:16','2026-05-26 23:04:16');
/*!40000 ALTER TABLE `student_profile` ENABLE KEYS */;
UNLOCK TABLES;

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
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `study_room`
--

LOCK TABLES `study_room` WRITE;
/*!40000 ALTER TABLE `study_room` DISABLE KEYS */;
INSERT INTO `study_room` (`id`, `room_code`, `name`, `location`, `floor`, `open_time`, `close_time`, `status`, `manager_id`, `row_count`, `col_count`, `cell_count`, `seat_count`, `facilities`, `layout_image_url`, `created_at`, `updated_at`) VALUES (1,'LIB-01-A','图书馆一楼A区','图书馆1楼东侧','1楼','07:00:00','23:30:00','OPEN',1,3,6,18,18,'空调,WiFi,饮水机,充电区','/uploads/layout/82fc309f-a68e-4f49-a040-d8808c780d1a.png','2026-05-25 23:40:41','2026-05-26 22:56:43'),(2,'LIB-02-B','图书馆二楼B区','图书馆2楼西侧','2楼','07:00:00','22:30:00','OPEN',1,4,6,24,24,'空调,WiFi,打印区','','2026-05-25 23:40:41','2026-05-26 18:27:54'),(3,'TEA-C201','教学楼C201','教学楼C栋2楼','2楼','08:00:00','21:00:00','MAINTAINING',2,3,6,18,16,'空调,WiFi,投影仪',NULL,'2026-05-25 23:40:41','2026-05-25 23:40:41'),(4,'1','1','1','1楼','07:00:00','22:30:00','OPEN',3,2,8,16,16,'空调,WiFi','1','2026-05-26 18:27:35','2026-05-26 22:41:10'),(5,'100','405','教四教学楼','4楼','07:00:00','23:20:00','OPEN',1,7,7,49,49,'空调,WiFi','','2026-05-26 21:25:35','2026-05-26 23:10:33'),(6,'2','2','2','1楼','07:00:00','22:30:00','OPEN',3,2,6,12,12,'空调,WiFi','2','2026-05-26 22:41:27','2026-05-26 22:41:27'),(7,'4','4','4','1楼','07:00:00','22:30:00','OPEN',3,1,7,7,7,'空调,WiFi','4','2026-05-26 22:44:14','2026-05-26 22:44:20'),(8,'L0526230415','【极端测试-LIVE】自习室-2x5-LIVE-20260526-230415','Live测试区A栋','8楼','07:00:00','22:30:00','OPEN',1,2,5,10,10,'空调,WiFi,Live测试','','2026-05-26 23:04:16','2026-05-26 23:04:16');
/*!40000 ALTER TABLE `study_room` ENABLE KEYS */;
UNLOCK TABLES;

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
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `temp_leave`
--

LOCK TABLES `temp_leave` WRITE;
/*!40000 ALTER TABLE `temp_leave` DISABLE KEYS */;
INSERT INTO `temp_leave` (`id`, `reservation_id`, `user_id`, `leave_time`, `return_time`, `leave_status`, `max_leave_minutes`, `created_at`) VALUES (1,8,1,'2026-05-26 19:14:39','2026-05-26 19:14:41','RETURNED',30,'2026-05-26 19:14:39'),(2,28,7,'2026-05-26 22:27:26','2026-05-26 22:27:28','RETURNED',30,'2026-05-26 22:27:26');
/*!40000 ALTER TABLE `temp_leave` ENABLE KEYS */;
UNLOCK TABLES;

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
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_account`
--

LOCK TABLES `user_account` WRITE;
/*!40000 ALTER TABLE `user_account` DISABLE KEYS */;
INSERT INTO `user_account` (`id`, `username`, `password_hash`, `role`, `status`, `last_login_at`, `created_at`, `updated_at`) VALUES (1,'202301010101','$2a$10$kt7ExBB9gtSMRgQStg0L.ems1OwyS28Fv.JI9gJVYRdyjhV1Gknge','STUDENT','NORMAL','2026-05-26 23:43:42','2026-05-25 23:40:40','2026-05-26 23:58:53'),(2,'202301010102','$2a$10$kt7ExBB9gtSMRgQStg0L.ems1OwyS28Fv.JI9gJVYRdyjhV1Gknge','STUDENT','NORMAL','2026-05-26 18:09:34','2026-05-25 23:40:40','2026-05-26 23:58:53'),(3,'202301010199','$2a$10$kt7ExBB9gtSMRgQStg0L.ems1OwyS28Fv.JI9gJVYRdyjhV1Gknge','STUDENT','PENDING',NULL,'2026-05-25 23:40:40','2026-05-26 23:58:53'),(4,'202411111111','$2a$10$cxpcUDtzBJ1I7v7/eugNSeK.nqkxWJ6i7tnBGftIkSm.GmvUkeNHu','STUDENT','DISABLED',NULL,'2026-05-26 18:34:05','2026-05-26 18:34:54'),(5,'123456789000','$2a$10$bg6HTVFm2kP8ZLNHSMivwuUh5vK9SLnflOdlljMKaXsKQA1J7w/c6','STUDENT','NORMAL',NULL,'2026-05-26 21:19:59','2026-05-26 21:25:31'),(6,'111111111111','$2a$10$gYuSVIYpscLfrQ1H3cgbU.Sb3qE7nBDVUMvPfhSY.KsNalRYUXfEG','STUDENT','NORMAL','2026-05-26 21:41:26','2026-05-26 21:25:20','2026-05-26 21:41:26'),(7,'202400088880','$2a$10$VN0cmFhIhNYJ3pu.NNdDF.OXg8jQ4D8l1VJN2u57PRUCyAfra.IDK','STUDENT','NORMAL','2026-05-26 21:32:19','2026-05-26 21:29:56','2026-05-26 21:32:19'),(8,'000000000000','$2a$10$2gnJRd/bKupaXD6tox34WudoG5NfKbIL0xiinznDhKAISC7iFos0K','STUDENT','NORMAL','2026-05-26 21:59:02','2026-05-26 21:58:10','2026-05-26 21:59:02'),(9,'222222222222','$2a$10$w0c.X7J0ZJlGwHavYZEqeukcWRxofhMYlh5gB1YbNmQjopU7BRJgC','STUDENT','NORMAL',NULL,'2026-05-26 22:00:57','2026-05-26 22:32:05'),(10,'LIVE2026052623041599','$2a$10$TzYAPEuxtkuNlg9uxiUjTedZ5hZCIN3mi2ksWtwMHGi3nmTxK1z1C','STUDENT','NORMAL','2026-05-26 23:04:16','2026-05-26 23:04:16','2026-05-26 23:04:16');
/*!40000 ALTER TABLE `user_account` ENABLE KEYS */;
UNLOCK TABLES;

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

-- Dump completed on 2026-05-27 19:17:01
