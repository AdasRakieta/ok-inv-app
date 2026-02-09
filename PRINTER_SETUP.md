# Brother PT-P950NW Barcode Label Printing

## Overview
System automatycznego drukowania kodów kreskowych dla drukarki Brother PT-P950NW został zintegrowany z aplikacją inwentaryzacyjną. Użytkownicy mogą teraz łatwo drukować etykiety z kodami kreskowymi Code128 dla urządzeń bezpośrednio z aplikacji.

## Features
- ✅ Drukowanie etykiet z kodem kreskowym Code128
- ✅ Obsługa połączeń WiFi, Bluetooth i Wireless Direct
- ✅ Intuicyjny interfejs konfiguracji drukarki
- ✅ Testowanie połączenia z drukarką
- ✅ Automatyczne skanowanie sieci w poszukiwaniu drukarek
- ✅ Konfigurowalne rozmiary etykiet
- ✅ Drukowanie pojedynczych etykiet ze szczegółów produktu
- 🔜 Drukowanie wsadowe wielu etykiet (planowane)

## Setup Instructions

### 1. Konfiguracja Drukarki Brother PT-P950NW

#### Połączenie WiFi (Zalecane)
1. Upewnij się, że drukarka Brother PT-P950NW jest podłączona do tej samej sieci WiFi co urządzenie Android
2. Sprawdź adres IP drukarki:
   - Menu drukarki → Network → WLAN → IP Address
   - Lub użyj Brother iPrint&Scan aby wykryć adres IP

#### Połączenie Bluetooth
1. Sparuj drukarkę z urządzeniem Android w ustawieniach Bluetooth systemu
2. Zapamiętaj nazwę sparowanego urządzenia

### 2. Konfiguracja w Aplikacji

1. **Otwórz ustawienia drukarki**
   - Z ekranu głównego nawiguj do: Menu → Printer Settings
   - Lub z dowolnego ekranu użyj globalnej akcji nawigacji

2. **Wybierz metodę połączenia**
   - **WiFi**: Wprowadź adres IP (np. 192.168.1.100) i port (domyślnie 9100)
   - **Bluetooth**: Wybierz sparowane urządzenie
   - **Wireless Direct**: Połącz się z siecią ad-hoc drukarki

3. **Skonfiguruj rozmiar etykiety**
   - Wybierz rozmiar z listy rozwijanej (29mm x 90mm to standardowy rozmiar)

4. **Testowe drukowanie**
   - Kliknij "Test Print" aby zweryfikować połączenie
   - Drukarka powinna wydrukować testową etykietę z timestampem

5. **Zapisz konfigurację**
   - Kliknij "Save Configuration"
   - Status zmieni się na "Printer configured" (zielony)

### 3. Drukowanie Etykiet

#### Drukowanie Pojedynczej Etykiety
1. Otwórz szczegóły produktu (ProductDetailsFragment)
2. Upewnij się, że produkt ma przypisany numer seryjny (SN)
3. Kliknij przycisk FAB (Floating Action Button) z ikoną drukarki ![Print Icon](ic_print.xml)
4. Aplikacja automatycznie:
   - Wygeneruje kod kreskowy Code128 z numerem seryjnym
   - Utworzy etykietę z kodem i tekstem SN
   - Wyśle etykietę do drukarki
5. Otrzymasz powiadomienie Snackbar o statusie drukowania

#### Obsługa Błędów
- **"Printer not configured"**: Przejdź do ustawień drukarki i skonfiguruj połączenie
- **"Cannot print label: Serial number is empty"**: Dodaj numer seryjny do produktu
- **"Connection timeout"**: Sprawdź połączenie sieciowe i adres IP drukarki
- **"Print failed: [error]"**: Zobacz logi aplikacji dla szczegółów błędu

## Technical Details

### Architecture
- **Data Models**: `PrinterConfig.kt`, `PrinterModel.kt`
- **Barcode Generation**: `BarcodeGenerator.kt` (ZXing with Code128 support)
- **Label Formatting**: `BrotherLabelFormatter.kt` (ESC/POS raster graphics)
- **Network Communication**: `BrotherPrinterHelper.kt` (TCP socket over WiFi/Bluetooth)
- **Settings Storage**: `PrinterPreferences.kt` (SharedPreferences)

### Supported Barcode Formats
- **Code128**: Alphanumeric, compact, ideal for serial numbers (currently used)
- **QR Code**: 2D matrix barcode (available via `QRCodeGenerator.kt`)
- **EAN-13**: Numeric barcodes (13 digits, available via `BarcodeGenerator.kt`)

### ESC/POS Implementation
System używa uniwersalnych komend ESC/POS do komunikacji z drukarką Brother PT-P950NW:
- `ESC @`: Inicjalizacja drukarki
- `ESC * m nL nH`: Drukowanie grafiki rastrowej
- `GS V m`: Cięcie papieru

Dzięki temu rozwiązaniu nie jest wymagane Brother SDK, co upraszcza integrację.

### Label Sizes
Dostępne rozmiary etykiet (szerokość x wysokość w mm):
- 29mm x 90mm (Standard)
- 25mm x 50mm (Small)
- 36mm x 110mm (Large)
- 24mm x 24mm (Square)

## Troubleshooting

### Drukarka nie odpowiada

**WiFi:**
1. Sprawdź ping do drukarki: `ping 192.168.1.100`
2. Upewnij się, że firewall nie blokuje portu 9100
3. Zrestartuj drukarkę i router
4. Sprawdź czy drukarka i telefon są w tej samej podsieci

**Bluetooth:**
1. Sprawdź sparowanie w ustawieniach systemowych
2. Usuń sparowanie i sparuj ponownie
3. Upewnij się, że Bluetooth jest włączony na drukarce
4. Zrestartuj Bluetooth na telefonie

### Etykiety drukują się z błędami

1. **Puste etykiety**: Sprawdź czy papier jest prawidłowo załadowany
2. **Rozmyte kody**: Zwiększ jakość wydruku w ustawieniach drukarki
3. **Niepełne etykiety**: Zmniejsz rozmiar etykiety lub zwiększ pamięć drukarki
4. **Nieprawidłowe rozmiary**: Upewnij się, że rozmiar etykiety w aplikacji odpowiada rzeczywistemu rozmiarowi papieru

### Logi Debugowania

Sprawdź logi Android Studio/Logcat z tagiem `BrotherPrinterHelper`:
```
adb logcat -s BrotherPrinterHelper
```

## Future Enhancements

- [ ] Batch printing from ProductsListFragment (drukowanie wielu etykiet jednocześnie)
- [ ] Auto-print po zeskanowaniu w BulkAddFragment
- [ ] Obsługa Brother SDK (jeśli zostanie udostępniony publicznie)
- [ ] Niestandardowe szablony etykiet z dodatkowymi polami
- [ ] Drukowanie QR kodów z dodatkowymi danymi (model, manufacturer)
- [ ] Obsługa wielu drukarek jednocześnie
- [ ] Podgląd etykiety przed wydrukiem

## Support

Dla problemów technicznych lub pytań:
- Sprawdź logi aplikacji
- Przeczytaj dokumentację Brother PT-P950NW
- Skontaktuj się z zespołem deweloperskim

## Credits

Implementacja: Integracja Brother PT-P950NW z aplikacją inwentaryzacyjną  
Data: Luty 2026  
Wersja: 1.0.0
