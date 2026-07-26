ALTER TABLE notification_record
  DROP CHECK ck_notification_record_type,
  ADD CONSTRAINT ck_notification_record_type CHECK (
    notification_type IN ('BOOKING_CONFIRMED', 'BOOKING_CANCELLED', 'BOOKING_EXPIRED')
  );
