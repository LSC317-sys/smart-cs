-- MySQL dump 10.13  Distrib 8.0.44, for Win64 (x86_64)
--
-- Host: localhost    Database: smart_cs_user
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
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `nickname` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `role` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'USER',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'ACTIVE',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  KEY `idx_username` (`username`),
  KEY `idx_email` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=29 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'testuser1','$2a$10$J9n2Vd2FjsCgJnv2Q/UvH.q/paXVX7EsTi8tMdxt8Kq5y0cEkGOuu','test@test.com','testuser1','USER','ACTIVE','2026-05-09 13:24:45','2026-05-09 13:24:45'),(2,'user132515','$2a$10$G9yCrlXq8Xb/YpA4rxYycOi/IdUzEPIVOGRcKD.g0Crp0sroi20Ei','132515@test.com','user132515','USER','ACTIVE','2026-05-09 13:25:16','2026-05-09 13:25:16'),(3,'u132844','$2a$10$ihohSsMMz4BwzLlRi/WUVeBmD1vviPPrplJV7Ssd2yvpzg/1doCp2','132844@test.com','u132844','USER','ACTIVE','2026-05-09 13:28:45','2026-05-09 13:28:45'),(4,'test','$2a$10$unQhHd.ZCTV.wULSMRFZ3.T7s8Yn1kcGo5tG/d/D9lGbAlLWyHc.2','test@test.com','test','USER','ACTIVE','2026-05-09 14:43:48','2026-05-09 14:43:48'),(5,'testuser','$2a$10$Nd.TTOqKEB1n0D8IKpiNnuAu77RSjCkyKXkZSf3se15O0P0BBSMAG','test@test.com','testuser','USER','ACTIVE','2026-05-09 14:44:01','2026-05-09 14:44:01'),(6,'test2','$2a$10$q3abZ9Q2pVbBNP9.xdfajuXBPJoA.Yegjt7zte58ST8HncybQ9M86','test2@test.com','test2','USER','ACTIVE','2026-05-09 14:44:41','2026-05-09 14:44:41'),(7,'lsc','$2a$10$pK4BR.eOU.6uI5qykVyT9.cBY8qgCBoMrk0xpkywa3RTLLYWX4Dj6','3502648906@qq.com','lsc','USER','ACTIVE','2026-05-09 15:02:35','2026-05-09 15:02:35'),(8,'testuser2','$2a$10$ErtMF.e4p0kJ4ZS4ufM7wOL2TFEpMp1qLciStoxEQtVIMFESIj9UC','test2@test.com','testuser2','USER','ACTIVE','2026-05-09 15:05:32','2026-05-09 15:05:32'),(9,'testuser4','$2a$10$.ldxxVrq2slvvNFhiDXuCuCqUXJ0ai9Lpk8fA4hkZrjq5zCIQjMNu',NULL,'????4','USER','ACTIVE','2026-05-09 15:54:26','2026-05-09 15:54:26'),(10,'testuser5','$2a$10$oJiMQTqvI0.dUCfuH7lwWuHc5vc44j8X/S4TxSVhue/kecgvONlli',NULL,'????5','USER','ACTIVE','2026-05-09 15:54:35','2026-05-09 15:54:35'),(11,'admin2','$2a$10$wM57.5ViNl.vuwIP5nPNEuv86bV7t.gpgBG4hwWPLZAJSrfknv0y6','admin2@test.com','admin2','ADMIN','ACTIVE','2026-05-11 14:13:35','2026-05-11 14:13:35'),(12,'admin3','$2a$10$KAQlDSpn.5Q74Xplljj62u4uEwSORIwRMq/NQ9egriatDJ4NiHIku','admin3@test.com','admin3','ADMIN','ACTIVE','2026-05-11 14:15:07','2026-05-11 14:15:07'),(13,'admin5','$2a$10$ngbSFU7RMNYfTJJfj1OjOeVBW0dJw.UWcMTrKo9px8bW6KA3o/QMG','admin5@test.com','admin5','ADMIN','ACTIVE','2026-05-11 14:19:11','2026-05-11 14:19:11'),(14,'admin6','$2a$10$KYhl/W3voZsTmhgTIRXEp.mDdc70E81tWPMNxFj8s.C4sWLBA/QrW','admin6@test.com','admin6','ADMIN','ACTIVE','2026-05-11 14:19:30','2026-05-11 14:19:30'),(15,'admint10','$2a$10$JvOMiAKDgF0w8PWJoVZPLOntxmYdsJ3r3usM3S7LmERYni2y3PQnW','admint10@test.com','admint10','ADMIN','ACTIVE','2026-05-11 14:21:47','2026-05-11 14:21:47'),(16,'finaltest','$2a$10$.JJfF0fnIv0DxA32uIMyeOKJl1VtMgWDi/J0FyGE0TjMZXKuhnqBq','final@test.com','finaltest','ADMIN','ACTIVE','2026-05-11 14:22:27','2026-05-11 14:22:27'),(17,'testlogin001','$2a$10$LZS7tsAu/OZvdePevx.XOeC19HrxiXkAJwt6jNrLlvjs5AQNkjFLa','test@test.com','testlogin001','USER','ACTIVE','2026-05-11 14:37:46','2026-05-11 14:37:46'),(18,'3502648906@qq.com','$2a$10$e5D4nOz/BoWgouXWDS8ziuUv6Mbm8/TSNgvo/3uO0/s2nvfjt3z1.','3502648906@qq.com','3502648906@qq.com','ADMIN','ACTIVE','2026-05-11 16:29:38','2026-05-11 16:29:38'),(19,'e2etest','$2a$10$wOzo3W3itTUQuYekSeEdf.o0B.HFYaKgybR1sOfCt9mK6KlSdQdby','e2e@smartcs.com','e2etest','USER','ACTIVE','2026-05-14 21:33:20','2026-05-14 21:33:20'),(20,'testuser_1778766318','$2a$10$mDQF/Agl8BiBFh1Bva5LzuzZlVMWbow1GTqTMRioXK8O9KggsrEzi','testuser_1778766318@test.com','testuser_1778766318','USER','ACTIVE','2026-05-14 21:45:18','2026-05-14 21:45:18'),(21,'e2e_1778766345','$2a$10$BvLV1oIrBlwHmqYvdaWKgOokcmxInwx6Vp/04A3bnaTqJQ6CSCXXa','e2e_1778766345@test.com','e2e_1778766345','USER','ACTIVE','2026-05-14 21:45:45','2026-05-14 21:45:45'),(22,'e2e_1778766388','$2a$10$GGdO7k6OvL76nuWOpURB2ulfBo.e39jmV6D27VMKLGkvSzsXUufPG','e2e_1778766388@test.com','e2e_1778766388','USER','ACTIVE','2026-05-14 21:46:29','2026-05-14 21:46:29'),(23,'e2e_1778766591','$2a$10$CWnkKQH7rg2/kUGjowgqD.Vvyr4hJPqJrhAo.x9PFgSS08XRmHsOq','e2e_1778766591@test.com','e2e_1778766591','USER','ACTIVE','2026-05-14 21:49:51','2026-05-14 21:49:51'),(24,'testuser_5448','$2a$10$htobQMrlK9fPhlkbcIZcoeN1swFRxAyXfVgPwu/luhZu8NejtnqnG','testuser_5448@test.com','testuser_5448','USER','ACTIVE','2026-05-14 22:55:31','2026-05-14 22:55:31'),(25,'testuser_15164','$2a$10$46elcCgZ3TDLCRkJ4PXXJOZQcD5St038ADleZefaYXMtNyg9k2DQq','testuser_15164@test.com','testuser_15164','USER','ACTIVE','2026-05-14 22:55:51','2026-05-14 22:55:51'),(26,'testuser_25688','$2a$10$V2mBBelksJDxnMeay2wUkuL8y7OZRJxhcdtThsQ3rJzs7lE89IMCq','testuser_25688@test.com','testuser_25688','USER','ACTIVE','2026-05-14 22:56:09','2026-05-14 22:56:09'),(27,'testuser_8832','$2a$10$NBCZD95fzd2BVVacIXrHCOlpj9SeOkoM6M5l0WGpeTe54v9QQ7W1u','testuser_8832@test.com','testuser_8832','USER','ACTIVE','2026-05-14 22:56:25','2026-05-14 22:56:25'),(28,'testuser_6104','$2a$10$NUUyMLBMFDzDgspDw5cN0u7pikbcxzVeO/cKhhj0VDavn1d0kIHZu','testuser_6104@test.com','testuser_6104','USER','ACTIVE','2026-05-14 22:58:13','2026-05-14 22:58:13');
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'smart_cs_user'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-05-24  2:13:46
