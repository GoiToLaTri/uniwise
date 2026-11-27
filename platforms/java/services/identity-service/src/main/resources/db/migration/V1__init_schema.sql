-- Initial schema for identity_service (MySQL 8.0 / Hibernate 7)

CREATE TABLE `accounts` (
  `id` varchar(255) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `email` varchar(100) NOT NULL,
  `is_active` bit(1) NOT NULL,
  `password` varchar(255) NOT NULL,
  `provider` varchar(50) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_accounts_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `roles` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `display_name` varchar(50) NOT NULL,
  `is_active` bit(1) NOT NULL,
  `name` varchar(50) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `user_count` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_roles_display_name` (`display_name`),
  UNIQUE KEY `uk_roles_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `permissions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `name` varchar(50) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_permissions_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `account_roles` (
  `account_id` varchar(255) NOT NULL,
  `role_id` bigint NOT NULL,
  PRIMARY KEY (`account_id`, `role_id`),
  KEY `idx_account_roles_role_id` (`role_id`),
  CONSTRAINT `fk_account_roles_account` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`),
  CONSTRAINT `fk_account_roles_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `role_permissions` (
  `role_id` bigint NOT NULL,
  `permission_id` bigint NOT NULL,
  PRIMARY KEY (`role_id`, `permission_id`),
  KEY `idx_role_permissions_permission_id` (`permission_id`),
  CONSTRAINT `fk_role_permissions_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`),
  CONSTRAINT `fk_role_permissions_permission` FOREIGN KEY (`permission_id`) REFERENCES `permissions` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `sessions` (
  `id` varchar(255) NOT NULL,
  `access_token_expires_at` datetime(6) DEFAULT NULL,
  `browser` varchar(255) DEFAULT NULL,
  `device_type` varchar(255) DEFAULT NULL,
  `expires_at` datetime(6) DEFAULT NULL,
  `ip_address` varchar(255) DEFAULT NULL,
  `is_revoked` bit(1) DEFAULT NULL,
  `last_activity` datetime(6) DEFAULT NULL,
  `os` varchar(255) DEFAULT NULL,
  `refresh_token_id` varchar(255) DEFAULT NULL,
  `token` varchar(255) NOT NULL,
  `account_id` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sessions_token` (`token`),
  KEY `idx_sessions_account_id` (`account_id`),
  CONSTRAINT `fk_sessions_account` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `refresh_tokens` (
  `id` varchar(255) NOT NULL,
  `compromised` bit(1) NOT NULL,
  `expiry_date` datetime(6) NOT NULL,
  `token_hash` varchar(255) NOT NULL,
  `used` bit(1) NOT NULL,
  `session_id` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refresh_tokens_token_hash` (`token_hash`),
  KEY `idx_refresh_tokens_session_id` (`session_id`),
  CONSTRAINT `fk_refresh_tokens_session` FOREIGN KEY (`session_id`) REFERENCES `sessions` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
