CREATE TABLE IF NOT EXISTS space (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(32) NOT NULL,
  name VARCHAR(100) NOT NULL,
  open_start_time TIME NOT NULL,
  open_end_time TIME NOT NULL,
  wifi_ssid VARCHAR(64) NULL,
  wifi_password VARCHAR(128) NULL,
  rules TEXT NULL,
  price_hourly DECIMAL(10,2) NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1,
  deleted TINYINT NOT NULL DEFAULT 0,
  deleted_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  CONSTRAINT uk_space_code UNIQUE (code),
  INDEX idx_space_status_deleted (status, deleted),
  INDEX idx_space_name_deleted (name, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS seat (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  space_id BIGINT UNSIGNED NOT NULL,
  seat_number VARCHAR(20) NOT NULL,
  has_socket TINYINT NOT NULL DEFAULT 0,
  is_quiet TINYINT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1,
  deleted TINYINT NOT NULL DEFAULT 0,
  deleted_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  CONSTRAINT uk_seat_space_no_deleted UNIQUE (space_id, seat_number, deleted),
  CONSTRAINT fk_seat_space FOREIGN KEY (space_id) REFERENCES space(id) ON UPDATE CASCADE ON DELETE RESTRICT,
  INDEX idx_seat_space_status_deleted (space_id, status, deleted),
  INDEX idx_seat_feature_deleted (has_socket, is_quiet, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS app_user (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  phone VARCHAR(20) NOT NULL,
  nickname VARCHAR(50) NULL,
  role TINYINT NOT NULL DEFAULT 1,
  status TINYINT NOT NULL DEFAULT 1,
  last_login_at DATETIME(3) NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  deleted_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  CONSTRAINT uk_user_phone_deleted UNIQUE (phone, deleted),
  INDEX idx_user_role_status_deleted (role, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS booking (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  booking_no VARCHAR(32) NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  space_id BIGINT UNSIGNED NOT NULL,
  seat_id BIGINT UNSIGNED NOT NULL,
  start_at DATETIME(3) NOT NULL,
  end_at DATETIME(3) NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  confirm_code VARCHAR(20) NULL,
  cancel_reason VARCHAR(255) NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  deleted_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  CONSTRAINT uk_booking_no UNIQUE (booking_no),
  CONSTRAINT uk_booking_confirm_code UNIQUE (confirm_code),
  CONSTRAINT fk_booking_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_booking_space FOREIGN KEY (space_id) REFERENCES space(id) ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_booking_seat FOREIGN KEY (seat_id) REFERENCES seat(id) ON UPDATE CASCADE ON DELETE RESTRICT,
  INDEX idx_booking_user_start_status_del (user_id, start_at, status, deleted),
  INDEX idx_booking_seat_time_status_del (seat_id, start_at, end_at, status, deleted),
  INDEX idx_booking_space_date_del (space_id, start_at, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE IF NOT EXISTS space_comment (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  space_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  parent_id BIGINT UNSIGNED NOT NULL DEFAULT 0,
  root_id BIGINT UNSIGNED NOT NULL DEFAULT 0,
  content VARCHAR(500) NOT NULL,
  like_count BIGINT UNSIGNED NOT NULL DEFAULT 0,
  reply_count BIGINT UNSIGNED NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1,
  deleted TINYINT NOT NULL DEFAULT 0,
  deleted_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  CONSTRAINT fk_space_comment_space FOREIGN KEY (space_id) REFERENCES space(id) ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_space_comment_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON UPDATE CASCADE ON DELETE RESTRICT,
  INDEX idx_space_comment_root_page (space_id, parent_id, status, deleted, created_at, id),
  INDEX idx_space_comment_reply_tree (space_id, root_id, status, deleted, created_at, id),
  INDEX idx_space_comment_user (user_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS space_comment_like (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  comment_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  liked TINYINT NOT NULL DEFAULT 1,
  deleted TINYINT NOT NULL DEFAULT 0,
  deleted_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  CONSTRAINT uk_space_comment_like_user UNIQUE (comment_id, user_id, deleted),
  CONSTRAINT fk_space_comment_like_comment FOREIGN KEY (comment_id) REFERENCES space_comment(id) ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_space_comment_like_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON UPDATE CASCADE ON DELETE RESTRICT,
  INDEX idx_space_comment_like_user_liked (user_id, liked, deleted),
  INDEX idx_space_comment_like_comment_liked (comment_id, liked, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
