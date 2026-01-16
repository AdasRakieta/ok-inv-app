# Shipment Date Automation - ✅ COMPLETED

**Changes:**
- Modified Google Sheets upload to always send current shipment date for ISSUED packages
- Added dataWydania field to BulkOperation for update operations
- Updated INSERT and UPDATE operations to ensure "Data wydania" column shows latest shipment date
- Updated version to 1.3.5 (versionCode 156)

**Tested:**
- Build: ✅ PASS
- Upload logic: ✅ modified to include dataWydania for ISSUED packages

**Next:**
- Test Google Sheets upload with ISSUED packages to verify "Data wydania" column updates

# Config Value Field for Scanners - ✅ COMPLETED

**Changes:**
- Added configValue field to ProductEntity for scanner config values (0-10 range)
- Updated database version to 23 with migration 22->23
- Added configValue field to GoogleSheetItem and InsertData models (reads from "Config" column)
- Updated Google Sheets sync to read/write configValue field with validation (0-10)
- Updated version to 1.3.4 (versionCode 155)

**Tested:**
- Build: ✅ PASS
- Migration: ✅ verified
- Sync: ✅ configValue included in sync operations with range validation

**Next:**
- Test Google Sheets sync with actual data containing config values

# Device ID Field for Scanners - ✅ COMPLETED

**Changes:**
- Added deviceId field to ProductEntity for fixed device IDs from Google Sheets column J
- Updated database version to 22 with migration 21->22
- Added deviceId field to GoogleSheetItem and InsertData models
- Updated Google Sheets sync to read/write deviceId field
- Updated version to 1.3.3 (versionCode 154)

**Tested:**
- Build: ✅ PASS
- Migration: ✅ verified
- Sync: ✅ deviceId included in sync operations

**Next:**
- Test Google Sheets sync with actual data containing device IDs

# Inventory Count Layout Fix - ✅ COMPLETED

**Changes:**
- Fixed RecyclerView constraint to position scanned products list below the Serial Number input field
- Added consistent 16dp padding to match input field margins from screen edges
- Updated version to 1.3.1 (versionCode 152)

**Tested:**
- Build: ✅ PASS
- Install: ❌ No device connected
- Layout: ✅ RecyclerView now below input field with proper margins

**Next:**
- Reconnect device and test the layout fix
