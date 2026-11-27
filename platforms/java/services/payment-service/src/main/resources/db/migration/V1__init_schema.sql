-- Initial schema for payment_service (MySQL 8.0 / Hibernate 7)

CREATE TABLE `payments` (
  `id` varchar(255) NOT NULL,
  `account_id` varchar(36) NOT NULL,
  `amount` bigint NOT NULL,
  `bank_code` varchar(50) DEFAULT NULL,
  `course_id` varchar(36) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `currency` varchar(10) NOT NULL,
  `pay_date` varchar(50) DEFAULT NULL,
  `status` enum('CANCELLED','FAILED','PENDING','SUCCESS') NOT NULL,
  `txn_ref` varchar(100) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `vnpay_transaction_no` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payments_txn_ref` (`txn_ref`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
