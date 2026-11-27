-- Initial schema for course_service (MySQL 8.0 / Hibernate 7)

CREATE TABLE `price_tiers` (
  `id` varchar(36) NOT NULL,
  `course_count` int NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `currency` varchar(10) NOT NULL,
  `price_amount` decimal(15,2) NOT NULL,
  `tier_name` varchar(255) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `hashtags` (
  `id` varchar(255) NOT NULL,
  `course_count` int NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `is_verified` bit(1) NOT NULL,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_hashtags_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `course_sync_queue` (
  `course_id` varchar(36) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  PRIMARY KEY (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `courses` (
  `id` varchar(36) NOT NULL,
  `average_rating` double NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `creator_id` varchar(255) DEFAULT NULL,
  `description` text,
  `instructor_avatar_url` varchar(2048) DEFAULT NULL,
  `instructor_name` varchar(100) DEFAULT NULL,
  `instructor_public_id` varchar(100) DEFAULT NULL,
  `is_active` bit(1) NOT NULL,
  `public_id` varchar(16) NOT NULL,
  `status` enum('ARCHIVED','DRAFT','PUBLISHED') NOT NULL,
  `student_count` int NOT NULL,
  `thumbnail_name` varchar(255) DEFAULT NULL,
  `thumbnail_url` varchar(255) DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `total_lessons` int NOT NULL,
  `total_reviews` int NOT NULL,
  `total_sections` int NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `price_tier_id` varchar(36) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_courses_public_id` (`public_id`),
  KEY `idx_courses_price_tier_id` (`price_tier_id`),
  CONSTRAINT `fk_courses_price_tier` FOREIGN KEY (`price_tier_id`) REFERENCES `price_tiers` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `sections` (
  `id` varchar(36) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `public_id` varchar(16) NOT NULL,
  `sort_order` int NOT NULL,
  `title` varchar(255) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `course_id` varchar(36) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sections_public_id` (`public_id`),
  KEY `idx_sections_course_id` (`course_id`),
  CONSTRAINT `fk_sections_course` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `lessons` (
  `id` varchar(36) NOT NULL,
  `content_reference` varchar(255) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `duration_ms` bigint DEFAULT NULL,
  `is_preview` bit(1) NOT NULL,
  `lesson_type` enum('QUIZ','VIDEO') NOT NULL,
  `public_id` varchar(16) NOT NULL,
  `sort_order` int NOT NULL,
  `status` enum('FAILED','PROCESSING','READY') NOT NULL,
  `title` varchar(255) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `section_id` varchar(36) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_lessons_public_id` (`public_id`),
  KEY `idx_lessons_section_id` (`section_id`),
  CONSTRAINT `fk_lessons_section` FOREIGN KEY (`section_id`) REFERENCES `sections` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `course_hashtags` (
  `course_id` varchar(255) NOT NULL,
  `hashtag_id` varchar(255) NOT NULL,
  PRIMARY KEY (`course_id`, `hashtag_id`),
  KEY `idx_course_hashtags_hashtag_id` (`hashtag_id`),
  CONSTRAINT `fk_course_hashtags_hashtag` FOREIGN KEY (`hashtag_id`) REFERENCES `hashtags` (`id`),
  CONSTRAINT `fk_course_hashtags_course` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `user_courses` (
  `account_id` varchar(36) NOT NULL,
  `course_id` varchar(255) NOT NULL,
  `enrolled_at` datetime(6) NOT NULL,
  `is_paid` bit(1) NOT NULL,
  PRIMARY KEY (`account_id`, `course_id`),
  KEY `idx_user_courses_course_id` (`course_id`),
  CONSTRAINT `fk_user_courses_course` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `user_lessons` (
  `account_id` varchar(36) NOT NULL,
  `lesson_id` varchar(255) NOT NULL,
  `is_completed` bit(1) NOT NULL,
  `last_watched_position` int DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`account_id`, `lesson_id`),
  KEY `idx_user_lessons_lesson_id` (`lesson_id`),
  CONSTRAINT `fk_user_lessons_lesson` FOREIGN KEY (`lesson_id`) REFERENCES `lessons` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
