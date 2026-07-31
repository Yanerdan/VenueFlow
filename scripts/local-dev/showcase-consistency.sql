SET NAMES utf8mb4;

SELECT COUNT(*)
FROM venueflow_booking.booking_reservation booking
LEFT JOIN venueflow_resource.resource_slot slot ON slot.id = booking.slot_id
LEFT JOIN venueflow_resource.resource_slot_allocation allocation
  ON allocation.operation_id = booking.allocation_operation_id
WHERE booking.booking_no LIKE 'VF-SHOW-%'
  AND booking.status IN ('PENDING_CONFIRMATION', 'CONFIRMED')
  AND (
    slot.id IS NULL OR slot.status <> 'OPEN'
    OR slot.allocated_quantity <> booking.quantity
    OR allocation.id IS NULL OR allocation.slot_id <> booking.slot_id
    OR allocation.operation_type <> 'ALLOCATE'
    OR allocation.quantity <> booking.quantity
    OR allocation.occupied_quantity_after <> booking.quantity
  );

SELECT
  (
    SELECT COUNT(*)
    FROM venueflow_booking.booking_reservation booking
    LEFT JOIN venueflow_resource.resource resource ON resource.id = booking.resource_id
    LEFT JOIN venueflow_resource.resource_slot slot ON slot.id = booking.slot_id
    LEFT JOIN venueflow_user.user_profile profile ON profile.id = booking.user_id
    WHERE booking.booking_no LIKE 'VF-SHOW-%'
      AND (resource.id IS NULL OR slot.id IS NULL OR profile.id IS NULL)
  )
  +
  (
    SELECT COUNT(*)
    FROM venueflow_booking.booking_reconciliation_intent intent
    JOIN venueflow_booking.booking_reservation booking ON booking.request_id = intent.request_id
    WHERE booking.booking_no LIKE 'VF-SHOW-%'
      AND intent.booking_id <> booking.id
  )
  +
  (
    SELECT COUNT(*)
    FROM venueflow_booking.booking_outbox_event event
    JOIN venueflow_booking.booking_reservation booking ON booking.booking_no = event.aggregate_id
    WHERE booking.booking_no LIKE 'VF-SHOW-%'
      AND booking.status IN ('PENDING_CONFIRMATION', 'CONFIRMED')
  );

SELECT COUNT(*)
FROM venueflow_auth.auth_credentials
WHERE username LIKE 'acceptance\_%'
   OR username LIKE 'browser\_%'
   OR username REGEXP '^user_[0-9]+$'
   OR username = 'venue_user01';
