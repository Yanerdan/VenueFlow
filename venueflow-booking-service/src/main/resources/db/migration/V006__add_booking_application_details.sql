ALTER TABLE booking_reservation
  ADD COLUMN activity_title VARCHAR(160) NULL AFTER quantity,
  ADD COLUMN application_purpose VARCHAR(500) NULL AFTER activity_title,
  ADD COLUMN contact_name VARCHAR(120) NULL AFTER application_purpose,
  ADD COLUMN contact_phone VARCHAR(32) NULL AFTER contact_name,
  ADD COLUMN application_note VARCHAR(1000) NULL AFTER contact_phone,
  ADD COLUMN review_decision VARCHAR(16) NULL AFTER terminal_reason,
  ADD COLUMN review_note VARCHAR(1000) NULL AFTER review_decision,
  ADD COLUMN reviewer_role VARCHAR(32) NULL AFTER review_note,
  ADD COLUMN reviewed_at DATETIME(6) NULL AFTER reviewer_role;
