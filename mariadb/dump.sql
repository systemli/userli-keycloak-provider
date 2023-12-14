/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
SET NAMES utf8mb4;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE='NO_AUTO_VALUE_ON_ZERO', SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

DROP TABLE IF EXISTS `virtual_domains`;

CREATE TABLE `virtual_domains` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `creation_time` datetime NOT NULL,
  `updated_time` datetime NOT NULL,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UNIQ_BA0C6C525E237E06` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

LOCK TABLES `virtual_domains` WRITE;
/*!40000 ALTER TABLE `virtual_domains` DISABLE KEYS */;

INSERT INTO `virtual_domains` (`id`, `creation_time`, `updated_time`, `name`)
VALUES
	(1,'2023-11-17 17:44:28','2023-11-17 17:44:28','example.org'),
	(2,'2023-11-17 17:44:28','2023-11-17 17:44:28','example.com');

/*!40000 ALTER TABLE `virtual_domains` ENABLE KEYS */;
UNLOCK TABLES;

DROP TABLE IF EXISTS `virtual_users`;

CREATE TABLE `virtual_users` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `invitation_voucher_id` int(11) DEFAULT NULL,
  `domain_id` int(11) NOT NULL,
  `creation_time` datetime NOT NULL,
  `updated_time` datetime NOT NULL,
  `email` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `roles` longtext NOT NULL COMMENT '(DC2Type:array)',
  `quota` int(11) DEFAULT NULL,
  `deleted` tinyint(1) NOT NULL DEFAULT 0,
  `last_login_time` datetime DEFAULT NULL,
  `password_version` int(11) NOT NULL,
  `recovery_secret_box` longtext DEFAULT NULL,
  `recovery_start_time` datetime DEFAULT NULL,
  `mail_crypt` tinyint(1) NOT NULL DEFAULT 0,
  `mail_crypt_secret_box` longtext DEFAULT NULL,
  `mail_crypt_public_key` longtext DEFAULT NULL,
  `totp_secret` varchar(255) DEFAULT NULL,
  `totp_confirmed` tinyint(1) NOT NULL DEFAULT 0,
  `totp_backup_codes` longtext NOT NULL COMMENT '(DC2Type:array)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UNIQ_3C68956AE7927C74` (`email`),
  UNIQUE KEY `UNIQ_3C68956AB29B3622` (`invitation_voucher_id`),
  KEY `IDX_3C68956A115F0EE5` (`domain_id`),
  KEY `email_idx` (`email`),
  CONSTRAINT `FK_3C68956A115F0EE5` FOREIGN KEY (`domain_id`) REFERENCES `virtual_domains` (`id`),
  CONSTRAINT `FK_3C68956AB29B3622` FOREIGN KEY (`invitation_voucher_id`) REFERENCES `virtual_vouchers` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

LOCK TABLES `virtual_users` WRITE;
/*!40000 ALTER TABLE `virtual_users` DISABLE KEYS */;

INSERT INTO `virtual_users` (`id`, `invitation_voucher_id`, `domain_id`, `creation_time`, `updated_time`, `email`, `password`, `roles`, `quota`, `deleted`, `last_login_time`, `password_version`, `recovery_secret_box`, `recovery_start_time`, `mail_crypt`, `mail_crypt_secret_box`, `mail_crypt_public_key`, `totp_secret`, `totp_confirmed`, `totp_backup_codes`)
VALUES
	(1,968,1,'2023-11-23 19:04:56','2023-11-23 19:04:56','admin@example.org','$argon2id$v=19$m=65536,t=4,p=1$Exe5ShJPEy6Ar4tFMTmIzg$exP1vf3XaWSXGkboeGfEGhbfly9kMrdl4BIzPKQWY/E','a:1:{i:0;s:10:\"ROLE_ADMIN\";}',NULL,0,NULL,2,NULL,NULL,0,NULL,NULL,NULL,0,'a:0:{}'),
	(2,927,1,'2023-11-23 19:04:56','2023-11-23 19:04:56','user@example.org','$argon2id$v=19$m=65536,t=4,p=1$s2BIwpR8GgagLBPOKhBAvQ$T0WVVMUex6/bW1/Rxn1CSwXDAvBQ8RajhlkZqZHCohM','a:1:{i:0;s:9:\"ROLE_USER\";}',NULL,0,NULL,2,NULL,NULL,0,NULL,NULL,NULL,0,'a:0:{}'),
	(3,771,1,'2023-11-23 19:04:56','2023-11-23 19:04:56','support@example.org','$argon2id$v=19$m=65536,t=4,p=1$1rLp0KaJQzc5l8JMY21IZg$OT0zGa9yiKhkb8hRGb93J6pVPDZc4YZOnsPjEo+0R0M','a:1:{i:0;s:15:\"ROLE_MULTIPLIER\";}',NULL,0,NULL,2,NULL,NULL,0,NULL,NULL,NULL,0,'a:0:{}'),
	(4,939,1,'2023-11-23 19:04:56','2023-11-23 19:04:57','suspicious@example.org','$argon2id$v=19$m=65536,t=4,p=1$Z2qO6yefhZTwSSw5doPU3A$0SbT3ceh+smCFapUjVbpl+fU6YiNH1tWjG/OFC6Mze0','a:1:{i:0;s:15:\"ROLE_SUSPICIOUS\";}',NULL,0,NULL,2,NULL,NULL,0,NULL,NULL,NULL,0,'a:0:{}'),
	(5,914,1,'2023-11-23 19:04:56','2023-11-23 19:04:57','spam@example.org','$argon2i$v=19$m=4096,t=3,p=1$UGhhZThlcGhhaQ$JVVK56YAd///MWtFXUcLk799FbT7P8E3TnwnFhJBE8M','a:1:{i:0;s:9:\"ROLE_SPAM\";}',NULL,0,NULL,2,NULL,NULL,0,NULL,NULL,NULL,0,'a:0:{}');

/*!40000 ALTER TABLE `virtual_users` ENABLE KEYS */;
UNLOCK TABLES;

/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
