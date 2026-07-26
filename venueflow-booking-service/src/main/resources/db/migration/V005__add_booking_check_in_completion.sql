ALTER TABLE booking_reservation
  DROP CHECK ck_booking_reservation_status,
  ADD COLUMN completed_at DATETIME(6) NULL AFTER expired_at,
  ADD CONSTRAINT ck_booking_reservation_status CHECK (
    status IN ('PENDING_CONFIRMATION', 'CONFIRMED', 'CANCELLED', 'EXPIRED', 'COMPLETED')
  );

ALTER TABLE booking_outbox_event
  DROP CHECK ck_booking_outbox_event_type,
  ADD CONSTRAINT ck_booking_outbox_event_type CHECK (
    event_type IN (
      'BOOKING_RESERVATION_CONFIRMED',
      'BOOKING_RESERVATION_CANCELLED',
      'BOOKING_RESERVATION_EXPIRED',
      'BOOKING_RESERVATION_COMPLETED'
    )
  );
