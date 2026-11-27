-- Initial schema for user_service (MySQL 8.0 / Hibernate 7)

CREATE TABLE `profiles` (
  `id` varchar(255) NOT NULL,
  `account_id` varchar(255) NOT NULL,
  `avatar_url` varchar(255) DEFAULT NULL,
  `bio` varchar(500) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `email` varchar(100) NOT NULL,
  `name` varchar(100) NOT NULL,
  `profile_type` enum('ADMIN','INSTRUCTOR','USER') NOT NULL,
  `public_id` varchar(100) DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_profiles_account_id` (`account_id`),
  UNIQUE KEY `uk_profiles_email` (`email`),
  UNIQUE KEY `uk_profiles_public_id` (`public_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `instructor_profiles` (
  `id` varchar(255) NOT NULL,
  `applied_at` datetime(6) DEFAULT NULL,
  `approved_at` datetime(6) DEFAULT NULL,
  `biography` varchar(2000) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `headline` varchar(150) DEFAULT NULL,
  `name` varchar(150) DEFAULT NULL,
  `public_id` varchar(100) NOT NULL,
  `reactivated_at` datetime(6) DEFAULT NULL,
  `rejected_at` datetime(6) DEFAULT NULL,
  `review_comment` varchar(1000) DEFAULT NULL,
  `status` enum('APPROVED','PENDING','REJECTED','SUSPENDED') NOT NULL,
  `suspended_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  `years_of_experience` int DEFAULT NULL,
  `profile_id` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_instructor_profiles_public_id` (`public_id`),
  UNIQUE KEY `uk_instructor_profiles_profile_id` (`profile_id`),
  CONSTRAINT `fk_instructor_profiles_profile` FOREIGN KEY (`profile_id`) REFERENCES `profiles` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `instructor_credentials` (
  `id` varchar(255) NOT NULL,
  `credential_url` varchar(500) DEFAULT NULL,
  `description` varchar(1000) DEFAULT NULL,
  `institution` varchar(255) DEFAULT NULL,
  `issued_date` date DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `type` enum('BACHELOR','CERTIFICATE','DOCTORATE','MASTER') NOT NULL,
  `instructor_profile_id` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_instructor_credentials_profile_id` (`instructor_profile_id`),
  CONSTRAINT `fk_instructor_credentials_profile` FOREIGN KEY (`instructor_profile_id`) REFERENCES `instructor_profiles` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `instructor_expertises` (
  `id` varchar(255) NOT NULL,
  `description` varchar(500) DEFAULT NULL,
  `level` varchar(50) DEFAULT NULL,
  `name` varchar(150) NOT NULL,
  `instructor_profile_id` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_instructor_expertises_profile_id` (`instructor_profile_id`),
  CONSTRAINT `fk_instructor_expertises_profile` FOREIGN KEY (`instructor_profile_id`) REFERENCES `instructor_profiles` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
