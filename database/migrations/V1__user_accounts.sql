CREATE TABLE user_accounts (
 id UUID PRIMARY KEY,
 name VARCHAR(100) NOT NULL,
 email VARCHAR(254) NOT NULL,
 password_hash VARCHAR(100) NOT NULL,
 role VARCHAR(20) NOT NULL DEFAULT 'USER',
 created_at TIMESTAMP WITH TIME ZONE NOT NULL,
 CONSTRAINT uk_user_accounts_email UNIQUE(email),
 CONSTRAINT ck_user_accounts_email_normalized CHECK(email=lower(email)),
 CONSTRAINT ck_user_accounts_role CHECK(role IN ('USER','ADMIN'))
);
