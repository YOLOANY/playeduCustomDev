CREATE TABLE IF NOT EXISTS courses (
    id INT NOT NULL PRIMARY KEY,
    title VARCHAR(255) NOT NULL DEFAULT '',
    thumb INT NOT NULL DEFAULT 0,
    charge INT NOT NULL DEFAULT 0,
    short_desc VARCHAR(255) NOT NULL DEFAULT '',
    is_required TINYINT NOT NULL DEFAULT 0,
    class_hour INT NOT NULL DEFAULT 0,
    is_show TINYINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NULL DEFAULT NULL,
    sort_at TIMESTAMP NULL DEFAULT NULL,
    extra TEXT NULL,
    admin_id INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NULL DEFAULT NULL,
    deleted_at TIMESTAMP NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS course_hour (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    course_id INT NOT NULL DEFAULT 0,
    chapter_id INT NOT NULL DEFAULT 0,
    sort INT NOT NULL DEFAULT 0,
    title VARCHAR(255) NOT NULL DEFAULT '',
    type VARCHAR(32) NOT NULL DEFAULT '',
    rid INT NOT NULL DEFAULT 0,
    duration INT NOT NULL DEFAULT 0,
    credit INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NULL DEFAULT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY course_id (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_course_hour_records (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL DEFAULT 0,
    course_id INT NOT NULL DEFAULT 0,
    hour_id INT NOT NULL DEFAULT 0,
    total_duration INT NOT NULL DEFAULT 0,
    finished_duration INT NOT NULL DEFAULT 0,
    real_duration INT NOT NULL DEFAULT 0,
    is_finished TINYINT NOT NULL DEFAULT 0,
    finished_at TIMESTAMP NULL DEFAULT NULL,
    created_at TIMESTAMP NULL DEFAULT NULL,
    updated_at TIMESTAMP NULL DEFAULT NULL,
    PRIMARY KEY (id),
    KEY user_id (user_id),
    KEY hour_id (hour_id),
    KEY course_id (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
