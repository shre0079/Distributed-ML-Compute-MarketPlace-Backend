CREATE TABLE users (
                       user_id VARCHAR(255) PRIMARY KEY,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       wallet_balance NUMERIC(12, 8) DEFAULT 0
);

CREATE TABLE worker_info (
                             worker_id VARCHAR(255) PRIMARY KEY,
                             cpu_cores INTEGER NOT NULL,
                             memorymb BIGINT NOT NULL,
                             os VARCHAR(255) NOT NULL,
                             has_gpu BOOLEAN NOT NULL DEFAULT FALSE,
                             last_seen BIGINT NOT NULL DEFAULT 0,
                             worker_secret VARCHAR(255) NOT NULL,
                             wallet_balance NUMERIC(12, 8) DEFAULT 0,
                             total_earned NUMERIC(12, 8) DEFAULT 0
);

CREATE TABLE job (
                     job_id VARCHAR(255) PRIMARY KEY,
                     user_id VARCHAR(255) NOT NULL,
                     docker_image VARCHAR(255),
                     file_url VARCHAR(255),
                     status VARCHAR(50),
                     retry_count INTEGER DEFAULT 0,
                     max_retries INTEGER DEFAULT 3,
                     worker_id VARCHAR(255),
                     required_cpu INTEGER NOT NULL DEFAULT 1,
                     required_memory_mb INTEGER NOT NULL DEFAULT 512,
                     gpu_required BOOLEAN NOT NULL DEFAULT FALSE,
                     max_runtime_seconds INTEGER NOT NULL DEFAULT 60,
                     duration_ms BIGINT,
                     estimated_cost NUMERIC(12, 8),
                     cost NUMERIC(12, 8),
                     worker_reward NUMERIC(12, 8),
                     platform_fee NUMERIC(12, 8)
);

CREATE TABLE transactions (
                              transaction_id VARCHAR(255) PRIMARY KEY,
                              user_id VARCHAR(255),
                              worker_id VARCHAR(255),
                              job_id VARCHAR(255),
                              type VARCHAR(50),
                              amount NUMERIC(12, 8),
                              timestamp BIGINT
);