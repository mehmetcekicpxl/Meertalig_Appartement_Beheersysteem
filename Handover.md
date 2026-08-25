# Handover & Migration Notes

This document was created following the migration from the original repository (`Apartment_Management_System`) to establish a clean handover for ongoing development.

## Current Status
- Project structure successfully migrated.
- Baseline setup verified; no code modifications introduced yet.
- Architecture and implementation roadmap finalized.

## Implementation Roadmap (Next Steps)

### 1. Internationalization (i18n) & Localization
- [ ] Extract all hardcoded strings from Java files into `strings.xml`.
- [ ] Extract all UI layout text into `strings.xml`.
- [ ] Create `res/values-nl/strings.xml` and provide Dutch translations.
- [ ] Implement dynamic currency formatting based on locale (TL / €).

### 2. Maintenance Fee (Aidat) Feature
- [ ] Increment database schema version in `DatabaseHelper` and add `aidat_amount` (Real/Double) column.
- [ ] Update `Apartment` data model with the `aidatAmount` field.
- [ ] Add view and edit controls for maintenance fees in `ApartmentDetailActivity`.
- [ ] Ensure clear separation between Rent and Maintenance Fee entries.

## Developer / Agent Instructions
Any contributor or AI assistant continuing this project should follow the roadmap steps sequentially, ensuring modularity and clean commits.
