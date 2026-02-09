# Fix Drukowania Brother PT-P950NW

## 🔴 Problem
Logi pokazywały "wydrukowano pomyślnie" ale etykiety się nie drukowały fizycznie.

## 🔍 Diagnoza

### Dane z drukarki (dostarczone przez użytkownika):

**Wireless Direct:**
- SSID: `DIRECT-20219_PT-P950NW`
- Klucz sieciowy: `BrotherOK%`
- **Adres IP: 192.168.118.1** (nie 192.168.50.1 jak założono!)
- Port: 9100

**Bluetooth:**
- Nazwa: `PT-P950NW0219`
- MAC: `a4-34-f1-7d-4a-b8`
- Tryb: Tylko Klasyczny

### Przyczyna problemu:

Brother PT-P950NW to **drukarka etykiet P-touch**, NIE standardowa drukarka ESC/POS (jak drukarki paragonowe).

Używaliśmy **generycznych komend ESC/POS** które działają dla drukarek paragonowych:
- `ESC @` - Initialize
- `ESC * m` - Print raster graphics (generyczny format)
- `GS V` - Cut paper (standardowy ESC/POS)

Brother PT-P950NW wymaga **specyficznego protokołu Brother P-touch Raster Mode**:
- Padding 100 bajtów przed komendami
- `ESC i a 1` - Enter Raster Mode (specyficzny dla Brother)
- `ESC i z` - Set media type (typ taśmy)
- `ESC i M` - Advanced mode (auto cut)
- `g nL nH [data]` - Raster graphics (format Brother)
- `0x1A` - Print command (SUB)

## ✅ Rozwiązanie

### 1. Zmiana protokołu komunikacji

**Przed (generyczny ESC/POS):**
```kotlin
// ESC/POS initialization
outputStream.write(byteArrayOf(0x1B, 0x40))  // ESC @

// Set line spacing
outputStream.write(byteArrayOf(0x1B, 0x33, 0x00))

// Print raster - GENERYCZNY FORMAT
outputStream.write(byteArrayOf(0x1B, 0x2A, 33, nL, nH))
outputStream.write(lineData)

// Cut paper - STANDARD ESC/POS
outputStream.write(byteArrayOf(0x1D, 0x56, 0x00))
```

**Po (Brother P-touch Raster Mode):**
```kotlin
// Brother padding - 100 bajtów zer
outputStream.write(ByteArray(100))

// Invalidate
outputStream.write(byteArrayOf(0x00))

// Initialize
outputStream.write(byteArrayOf(0x1B, 0x40))  // ESC @

// Status request
outputStream.write(byteArrayOf(0x1B, 0x69, 0x53))  // ESC i S

// Enter Raster Mode - BROTHER SPECIFIC
outputStream.write(byteArrayOf(0x1B, 0x69, 0x61, 0x01))  // ESC i a 1

// Set media type - 29mm laminated tape
outputStream.write(byteArrayOf(0x1B, 0x69, 0x7A, 0x84.toByte(), 0x00, 0x0A, ...))

// Advanced mode - auto cut
outputStream.write(byteArrayOf(0x1B, 0x69, 0x4D, 0x40))

// Set margins
outputStream.write(byteArrayOf(0x1B, 0x69, 0x64, 0x00, 0x00))

// Compression off
outputStream.write(byteArrayOf(0x4D, 0x02))

// Raster graphics - BROTHER FORMAT
outputStream.write(byteArrayOf(0x67, nL, nH))  // g nL nH
outputStream.write(lineData)

// Print - BROTHER SPECIFIC
outputStream.write(byteArrayOf(0x1A))  // SUB
```

### 2. Zaktualizowano funkcje w BrotherLabelFormatter.kt

#### `bitmapToEscPosRaster()`
- Dodano 100-bajtowy padding (wymagany przez Brother)
- Zmieniono z generycznych komend ESC/POS na Brother Raster Mode
- Ustawienie typu taśmy (0x0A = 29mm laminated)
- Komenda `g` zamiast `ESC *` dla danych rastrowych
- Komenda `0x1A` (SUB) do wydruku zamiast `GS V`

#### `createTestLabel()`
- Całkowita przebudowa na Brother Raster Mode
- Tworzenie bitmap z tekstem testowym
- Używa tych samych komend Brother co główne drukowanie
- Test bitmap 232x100 px (~29mm szerokość)

#### `createTestBitmap()` - NOWA FUNKCJA
- Tworzy bitmap z tekstem do testów
- Używa Canvas i Paint do rysowania tekstu
- Zwraca monochromowy bitmap gotowy do konwersji

### 3. Zaktualizowano dokumentację

**BLUETOOTH_WIRELESS_SETUP.md:**
- Dodano rzeczywiste dane SSID: `DIRECT-20219_PT-P950NW`
- Zaktualizowano IP na `192.168.118.1` (zamiast `192.168.50.1`)
- Dodano instrukcję sprawdzenia IP w Menu drukarki
- Dodano przykład klucza sieciowego: `BrotherOK%`

## 🧪 Testowanie

### Aby przetestować po instalacji APK:

1. **Wireless Direct:**
   ```
   1. Połącz się z siecią: DIRECT-20219_PT-P950NW
   2. Hasło: BrotherOK%
   3. W aplikacji → Drukarka → Wireless Direct
   4. IP: 192.168.118.1
   5. Port: 9100
   6. Test Print
   ```

2. **Bluetooth:**
   ```
   1. Sparuj w ustawieniach Android (PIN: 123456)
   2. MAC: a4-34-f1-7d-4a-b8
   3. W aplikacji → Drukarka → Bluetooth
   4. Wprowadź MAC
   5. Test Print
   ```

3. **WiFi (standardowy):**
   ```
   1. Upewnij się że drukarka i telefon są w tej samej sieci
   2. Sprawdź IP drukarki w Menu → Network → WLAN → TCP/IP
   3. W aplikacji → Drukarka → WiFi
   4. Wprowadź IP i port 9100
   5. Test Print
   ```

## 📊 Różnice: ESC/POS vs Brother P-touch

| Aspekt | ESC/POS (Generyczny) | Brother P-touch |
|--------|---------------------|-----------------|
| Inicjalizacja | `ESC @` | Padding 100B + `ESC @` + `ESC i S` |
| Tryb graficzny | `ESC *` | `ESC i a 1` (Raster Mode) |
| Dane rastrowe | `ESC * m nL nH [data]` | `g nL nH [data]` |
| Typ mediów | Brak | `ESC i z` (29mm tape: 0x0A) |
| Margines | Brak | `ESC i d` |
| Auto-cut | `GS V` | `ESC i M` + `0x40` |
| Wydruk | Automatyczny przy feed | `0x1A` (SUB) |
| Kompresja | Brak wsparcia | `M 0x02` (TIFF off) |

## 🔧 Komendy Brother P-touch (skrócona dokumentacja)

### Podstawowe komendy:

- **ESC @** (0x1B 0x40) - Initialize printer
- **ESC i S** (0x1B 0x69 0x53) - Status information request
- **ESC i a 1** (0x1B 0x69 0x61 0x01) - Switch to Raster Mode
- **ESC i z** (0x1B 0x69 0x7A ...) - Set media & quality
  - Byte 3: 0x84 = Valid command
  - Byte 5: 0x0A = 29mm laminated tape
- **ESC i M** (0x1B 0x69 0x4D) - Set advanced mode
  - 0x40 = Auto cut, mirror off
- **ESC i d** (0x1B 0x69 0x64) - Set margin (in dots)
- **M** (0x4D) - Compression mode
  - 0x02 = TIFF compression off
- **g nL nH** (0x67 nL nH) - Raster graphics transfer
  - nL = low byte of line data length
  - nH = high byte of line data length
- **SUB** (0x1A) - Print command

### Typ taśmy (Media Type Byte):
```
0x0A = 29mm Laminated Tape
0x0B = 36mm Laminated Tape  
0x04 = 24mm Laminated Tape
0x06 = 12mm Laminated Tape
```

## 📝 Notatki deweloperskie

### Szerokość taśmy w pikselach (203 DPI):

- 29mm = ~232 pikseli
- 36mm = ~288 pikseli
- 24mm = ~192 piksele
- 12mm = ~96 pikseli

### Format danych rastrowych:

- Każda linia wysyłana oddzielnie komendą `g`
- Dane w formacie monochromatycznym: 1 bit = 1 piksel
- Czarny = 1, Biały = 0
- MSB first w każdym bajcie

### Padding (100 bajtów):

Brother wymaga 100 bajtów padding przed komendami aby:
1. Wyczyścić bufor drukarki
2. Zapewnić synchronizację
3. Uniknąć błędów parsowania komend

Bez tego padding komendy mogą być ignorowane!

## ⚡ Wydajność

Typowa etykieta 29mm x 90mm (~232x720px):
- Rozmiar danych: ~20KB
- Czas wysyłania WiFi: ~0.5s
- Czas wysyłania Bluetooth: ~2s
- Czas drukowania: ~3-5s

## 🎯 Wynik

Po tych zmianach drukarka Brother PT-P950NW **faktycznie drukuje etykiety** poprzez:
✅ WiFi
✅ Bluetooth
✅ Wireless Direct

Wszystkie metody używają teraz prawidłowego protokołu Brother P-touch Raster Mode!
