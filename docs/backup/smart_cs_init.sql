-- MySQL dump 10.13  Distrib 8.0.44, for Win64 (x86_64)
--
-- Host: localhost    Database: smart_cs
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
-- Table structure for table `chat_history`
--

DROP TABLE IF EXISTS `chat_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `chat_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `session_id` varchar(128) NOT NULL,
  `question` text NOT NULL,
  `answer` text,
  `model` varchar(128) DEFAULT NULL,
  `tokens_used` int DEFAULT '0',
  `latency_ms` int DEFAULT '0',
  `rating` tinyint DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `chat_history`
--

LOCK TABLES `chat_history` WRITE;
/*!40000 ALTER TABLE `chat_history` DISABLE KEYS */;
INSERT INTO `chat_history` VALUES (3,1,'test-session-1','什么是RAG？','RAG是检索增强生成技术','Qwen2.5-7B',NULL,NULL,5,'2026-05-16 19:34:01','2026-05-16 19:45:52');
/*!40000 ALTER TABLE `chat_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `document_versions`
--

DROP TABLE IF EXISTS `document_versions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `document_versions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `doc_id` bigint NOT NULL,
  `version` int NOT NULL,
  `doc_id_version` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `file_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `file_size` bigint DEFAULT '0',
  `change_summary` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `doc_id_version` (`doc_id_version`),
  KEY `idx_doc_id` (`doc_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `document_versions`
--

LOCK TABLES `document_versions` WRITE;
/*!40000 ALTER TABLE `document_versions` DISABLE KEYS */;
/*!40000 ALTER TABLE `document_versions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `documents`
--

DROP TABLE IF EXISTS `documents`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `documents` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文档标题',
  `file_name` varchar(256) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '原始文件名',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小(字节)',
  `file_type` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `file_path` varchar(1024) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'MinIO存储路径',
  `status` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT 'pending' COMMENT '处理状态:pending/processing/completed/failed',
  `error_msg` text COLLATE utf8mb4_unicode_ci,
  `chunk_count` int DEFAULT '0' COMMENT '分块数量',
  `text_len` bigint DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `kb_id` bigint DEFAULT '1',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_created` (`created_at`),
  KEY `kb_id` (`kb_id`),
  CONSTRAINT `documents_ibfk_1` FOREIGN KEY (`kb_id`) REFERENCES `knowledge_base` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=46 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `documents`
--

LOCK TABLES `documents` WRITE;
/*!40000 ALTER TABLE `documents` DISABLE KEYS */;
INSERT INTO `documents` VALUES (36,'MySQL主从复制原理.md','MySQL主从复制原理.md',4124,'text/markdown','doc_36.md','completed',NULL,6,2596,'2026-05-14 14:58:13','2026-05-14 14:58:13',1),(37,'MySQL备份恢复与高可用.md','MySQL备份恢复与高可用.md',5663,'text/markdown','doc_37.md','completed',NULL,10,4330,'2026-05-14 14:58:14','2026-05-14 14:58:14',1),(38,'MySQL慢查询分析与优化.md','MySQL慢查询分析与优化.md',4445,'text/markdown','doc_38.md','completed',NULL,8,3136,'2026-05-14 14:58:14','2026-05-14 14:58:14',1),(39,'个人简历_Java开发_STAR版.docx','个人简历_Java开发_STAR版.docx',105923,'application/vnd.openxmlformats-officedocument.wordprocessingml.document','doc_39.docx','completed',NULL,7,2742,'2026-05-15 13:12:38','2026-05-15 13:12:38',1),(40,'??????','个人简历_Java开发_STAR版.docx',105923,'application/vnd.openxmlformats-officedocument.wordprocessingml.document','doc_40.docx','completed',NULL,7,2742,'2026-05-15 13:12:44','2026-05-15 13:12:44',1),(41,'2刘仕成','2刘仕成.docx',17113,'application/vnd.openxmlformats-officedocument.wordprocessingml.document','doc_41.docx','completed',NULL,10,4099,'2026-05-16 07:21:14','2026-05-16 07:21:14',1),(42,'2刘仕成','2刘仕成.docx',17113,'application/vnd.openxmlformats-officedocument.wordprocessingml.document','doc_42.docx','completed',NULL,10,4099,'2026-05-16 16:13:14','2026-05-16 16:13:14',2),(43,'软工二班 刘仕成','软工二班 刘仕成.doc',13146,'application/msword','local:软工二班 刘仕成.doc','completed',NULL,4,1615,'2026-05-17 06:15:56','2026-05-17 06:15:56',2),(44,'第1章 需求工程导论','第1章 需求工程导论.docx',25123,'application/vnd.openxmlformats-officedocument.wordprocessingml.document','local:第1章 需求工程导论.docx','completed',NULL,11,4534,'2026-05-17 12:06:23','2026-05-17 12:06:23',1),(45,'minio-test.txt','minio-test.txt',237,'text/plain','doc_45.txt','completed',NULL,1,112,'2026-05-18 05:39:20','2026-05-18 05:39:20',1);
/*!40000 ALTER TABLE `documents` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `knowledge_base`
--

DROP TABLE IF EXISTS `knowledge_base`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `knowledge_base` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `description` text,
  `user_id` bigint DEFAULT '1',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `knowledge_base`
--

LOCK TABLES `knowledge_base` WRITE;
/*!40000 ALTER TABLE `knowledge_base` DISABLE KEYS */;
INSERT INTO `knowledge_base` VALUES (1,'默认知识库','系统默认知识库',1,'2026-05-16 23:35:09','2026-05-16 23:35:09'),(2,'updated-kb','这是一个测试用的知识库',1,'2026-05-16 23:52:45','2026-05-16 23:52:45');
/*!40000 ALTER TABLE `knowledge_base` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'smart_cs'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-05-24  2:12:46
