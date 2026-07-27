# VenueFlow frontend design system

The visual direction was generated in Google Stitch and verified in Google AI Studio on
2026-07-27. The implementation uses the design language only; it does not adopt AI-generated
mock business features.

## Mineral Editorial

- Primary: `#004545` / `#005f5f`
- Secondary accent: `#d4a373`
- Tertiary: `#7d4726`
- Surface: `#f9f9fc`
- Ink: `#1a1c1e`
- Outline: `#bec9c8`
- Display type: Playfair Display
- Body type: Inter
- Base spacing unit: 8 px
- Content width: 1280 px
- Standard radius: 4 px; media/card radius: 8 px

The interface should feel like editorial hospitality rather than a generic SaaS dashboard:
strong venue imagery or architectural cover art, restrained borders instead of large shadows,
serif display headings, clear whitespace, and compact status pills.

## Product boundaries

The frontend stays connected to the existing API Gateway and supports only implemented
capabilities: authentication, profile bootstrap, resource discovery/search, open slot selection,
booking creation and transitions, and booking notifications. Payments, pricing, favorites,
reviews, social login, admin screens, and invented backend state are intentionally excluded.
