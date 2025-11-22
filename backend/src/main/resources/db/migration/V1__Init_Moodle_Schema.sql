CREATE TABLE classes (
                         id   BIGINT AUTO_INCREMENT PRIMARY KEY,
                         name VARCHAR(10) NOT NULL UNIQUE
);

CREATE TABLE users (
                       id           BIGINT AUTO_INCREMENT PRIMARY KEY,
                       email        VARCHAR(255) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       first_name   VARCHAR(100) NOT NULL,
                       last_name    VARCHAR(100) NOT NULL,

                       class_id     BIGINT NULL,

                       active       BOOLEAN NOT NULL DEFAULT TRUE,
                       created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE users
    ADD CONSTRAINT fk_users_class
        FOREIGN KEY (class_id) REFERENCES classes(id);


CREATE TABLE user_roles (
                            user_id BIGINT NOT NULL,
                            role    VARCHAR(50) NOT NULL,

                            CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role),

                            CONSTRAINT fk_user_roles_user
                                FOREIGN KEY (user_id) REFERENCES users(id)
                                    ON DELETE CASCADE
);
