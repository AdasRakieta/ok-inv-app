Plan: Generowanie i skanowanie QR dla lokalizacji

TL;DR
Dodanie funkcji generowania i zapisywania na urządzeniu QR przypisanego na stałe do lokalizacji (używamy pola `code` w `WarehouseLocationEntity` lub opcjonalnego `qrUid`). Po zeskanowaniu QR aplikacja automatycznie przekierowuje do widoku szczegółów tej lokalizacji. Rekomendowane podejście: Option 1 — użyć istniejącego `QRCodeGenerator` (ZXing) + zapisać obraz do `MediaStore` (scoped storage) i dodać `ScannerFragment` z CameraX + ML Kit do dekodowania oraz deep link `invapp://location/{code}`.

Steps
1. Decyzja o identyfikatorze QR (blokujące dla migracji)
   - Opcja A (szybsza, rekomendowana): użyć istniejącego pola `WarehouseLocationEntity.code` jako trwałego identyfikatora QR.
   - Opcja B (stabilna): dodać pole `qrUid: String` (UUID) do encji i wykonać migrację oraz backfill. *Depends on step 2 if chosen.*

2. Dostosowanie modelu danych (tylko przy Opcji B)
   - Zmiana w `WarehouseLocationEntity.kt`, DAO i repozytorium.
   - Dodać Room migration + skrypt backfill (mapowanie `code` -> `qrUid`). *Blocking for code change flows.*

3. QR generation & save helper
   - Reużyć `app/src/main/java/com/example/inventoryapp/utils/QRCodeGenerator.kt` do wygenerowania `Bitmap` z payloadem (np. `invapp://location/{code}` lub zawartość JSON z `type`+`code`).
   - Dodać nowy helper `app/src/main/java/com/example/inventoryapp/utils/MediaStoreHelper.kt` do zapisu Bitmappa w `MediaStore.Images` (scoped storage) i zwrócenia `Uri` do udostępniania. Fallback: użyć `FileHelper.kt` dla starszych urządzeń.

4. UI: przycisk "Zapisz / Udostępnij QR" w szczegółach lokalizacji
   - Edytować `WarehouseLocationDetailsFragment.kt`: dodać opcję w toolbar/FAB -> generuj QR -> zapisz przez `MediaStoreHelper` -> pokaż potwierdzenie/akcje udostępnienia.
   - (Opcjonalne) Dodać per-item akcję w `WarehouseLocationsListAdapter.kt`.

5. Scanner (kamera + ML Kit)
   - Utworzyć `ScannerFragment.kt` wykorzystujący `fragment_bulk_scan.xml` (PreviewView) + CameraX + ML Kit barcode scanning.
   - Po pomyślnym odczycie: wywołać `WarehouseLocationRepository.getLocationByCode(decoded)` i:
     - jeśli znajdzie -> nawiguj do `warehouseLocationDetailsFragment` (przekaż argumenty lub użyj deep linku);
     - jeśli brak -> proponuj stworzenie lokalizacji w `AddLocationFragment` z wstępnie wypełnionym `code`.

6. Nawigacja / Deep links
   - Dodać deep link do `warehouseLocationDetailsFragment` w `app/src/main/res/navigation/nav_graph.xml`:
     - np. `invapp://location/{code}` i obsługa argumentu `code` w fragmencie.
   - (Opcjonalnie) dodać `intent-filter` w `AndroidManifest.xml` jeśli chcesz, aby linki zewnętrzne otwierały aplikację.

7. Uprawnienia
   - Kamera: runtime `CAMERA` przy użyciu CameraX (fragment skanujący). Manifest ma już deklarację; dodać runtime prompt i fallback.
   - Zapisywanie: preferować `MediaStore` (Android 10+) — zwykle bez `WRITE_EXTERNAL_STORAGE`; tylko fallback używa istniejących uprawnień/legacy `FileHelper`.

8. Testy i weryfikacja
   - Unit tests: generator QR zwraca oczekiwany payload -> test bitmapy (rozmiar/format) oraz MediaStoreHelper zwraca URI.
   - Integration: DAO lookup po `code` po dekodowaniu.
   - Instrumentation/UI: test skanujący fragment (emulator camera feed lub fake image) -> nawigacja do fragmentu szczegółów.
   - Manual: wygeneruj QR, zapisz, zeskanuj wewnątrz aplikacji i sprawdź przekierowanie / obsługę nieznanej lokalizacji.

9. UX / edge cases
   - Dodaj informację, że QR jest trwale przypisany do `code` (lub `qrUid`) i że zmiana `code` unieważnia istniewą etykietę (jeśli nie używasz `qrUid`).
   - Dodaj możliwość ponownego generowania QR i opcji udostępniania/drukowania.

10. Handoff & Deliverables
   - Commity: drobne zmiany krok-po-kroku (model -> utils -> UI -> scanner -> nav -> tests).
   - Dokument: krótka instrukcja w README (gdzie zapisywane obrazy, jak skanować).

Relevant files
- `app/src/main/java/com/example/inventoryapp/data/local/entities/WarehouseLocationEntity.kt` — encja lokalizacji (pole `code`).
- `app/src/main/java/com/example/inventoryapp/data/local/dao/WarehouseLocationDao.kt` — DAO (metody `getLocationById`, `getLocationByCode`).
- `app/src/main/java/com/example/inventoryapp/data/repository/WarehouseLocationRepository.kt` — repozytorium (lookup po `code`).
- `app/src/main/java/com/example/inventoryapp/ui/warehouse/WarehouseLocationDetailsFragment.kt` — dodać przycisk generowania/zapisu QR.
- `app/src/main/java/com/example/inventoryapp/ui/warehouse/WarehouseLocationsListAdapter.kt` — opcjonalnie dodać akcję per-card.
- `app/src/main/java/com/example/inventoryapp/ui/warehouse/AddLocationFragment.kt` — pre-fill przy tworzeniu z zeskanowanego kodu.
- `app/src/main/java/com/example/inventoryapp/ui/assign/AssignByScanFragment.kt` — integracja ze skanowaniem/hardwarowym scannerem (ew. fallback).
- `app/src/main/java/com/example/inventoryapp/utils/QRCodeGenerator.kt` — istniejący generator ZXing (reuse).
- `app/src/main/java/com/example/inventoryapp/utils/FileHelper.kt` — istniejący zapis (legacy fallback).
- `app/src/main/res/navigation/nav_graph.xml` — dodać deep link.
- `app/src/main/res/layout/fragment_bulk_scan.xml` — użyć jako baza CameraX preview.
- (new) `app/src/main/java/com/example/inventoryapp/utils/MediaStoreHelper.kt` — helper zapisu PNG do MediaStore.
- (new) `app/src/main/java/com/example/inventoryapp/ui/scan/ScannerFragment.kt` — fragment skanujący.

Verification (konkretne kroki)
1. Unit: uruchom testy jednostkowe:
```bash
gradlew.bat :app:testDebugUnitTest
```
2. Instrumentation (device/emulator): 
```bash
gradlew.bat :app:connectedDebugAndroidTest
```
3. Manual test:
- Wejdź do ekranu lokalizacji -> „Zapisz QR” -> potwierdź zapis (sprawdź galerię lub folder exports).
- Otwórz „Skaner” w aplikacji -> zeskanuj zapisany QR -> aplikacja powinna otworzyć szczegóły lokalizacji.
- Zeskanuj QR z nieznaną wartością -> aplikacja powinna zaproponować utworzenie nowej lokalizacji z prefilled `code`.

Decisions / Assumptions
- Domyślnie używamy `WarehouseLocationEntity.code` jako trwałego identyfikatora QR (szybsze, brak migracji).
- Payload QR: stosujemy prosty schemat deep link `invapp://location/{code}` (łatwy do parsowania i nawigacji wewnątrz aplikacji).
- Zapisywać obrazy w `MediaStore` (scoped storage) aby uniknąć dodatkowych uprawnień na Android 10+.
- Użyjemy CameraX + ML Kit do skanowania (biblioteki już w `build.gradle.kts`).

Further Considerations / Pytania
1. Czy wolisz, żeby QR zawierał `code` (szybciej) czy generować oddzielne `qrUid` (trwalsze przy zmianie kodów)?
- Powinnien być generowany qrUid
2. Czy chcesz, aby QR był generowany automatycznie przy tworzeniu lokalizacji, czy tylko na żądanie w UI?
- Automatycznie przy tworzeniu lokalizacji oraz zapisywany do bazy danych, a opcja w UI będzie tylko do ponownego zapisu/udostępnienia, musi być też opcja wyświetlenia kodu QR na urządzeniu.
3. Czy chcesz możliwość masowego eksportu QR (ZIP/PDF) dla całego magazynu?
- Tak całość za pomocą ZIP/PDF.

---

Plik wygenerowany automatycznie bez frontmatter.
