# Konfiguracja Bluetooth i Wireless Direct dla Brother PT-P950NW

## 🔵 Bluetooth Setup (BEZ PAROWANIA!)

### Wymagania
- Urządzenie Android z Bluetooth
- Drukarka Brother PT-P950NW z włączonym Bluetooth
- ⚡ **NOWOŚĆ: Nie wymaga parowania!**

### Instrukcja konfiguracji (UPROSZCZONA)

1. **Na urządzeniu Android:**
   - Przejdź do **Ustawienia** → **Bluetooth**
   - Upewnij się, że Bluetooth jest **włączony**
   - ⚠️ **NIE MUSISZ** parować drukarki!

2. **Znajdź adres MAC drukarki:**
   - Zeskanuj kod QR na drukarce (zawiera MAC)
   - LUB sprawdź w Menu drukarki → Network → Bluetooth → MAC Address
   - Przykład: `A4:34:F1:7D:4A:B8`

3. **W aplikacji:**
   - Otwórz **Kafelek Drukarki** 🖨️ na ekranie głównym
   - Wybierz **Bluetooth** jako metodę połączenia
   - Wprowadź **adres MAC** (np. `A4:34:F1:7D:4A:B8`)
   - Wybierz rozmiar etykiety
   - Kliknij **Save Configuration**
   - Kliknij **Test Print** aby sprawdzić połączenie

### ⚡ Jak to działa (bez parowania):

Aplikacja używa **insecure RFCOMM connection** która:
- ✅ Łączy się bezpośrednio z drukarką po adresie MAC
- ✅ Nie wymaga parowania w ustawieniach Android
- ✅ Nie wymaga PIN-u (123456)
- ✅ Drukarka musi być tylko włączona i w zasięgu Bluetooth
- ✅ Prostsze i szybsze niż tradycyjne parowanie

### Funkcje Bluetooth (BEZ PAROWANIA)
- ✅ **Nie wymaga parowania** - działa od razu!
- ✅ **Nie wymaga PIN-u** - brak skomplikowanych kroków
- ✅ Drukowanie pojedynczych etykiet z kodami kreskowymi
- ✅ Drukowanie testowe
- ✅ Automatyczne łączenie po adresie MAC
- ✅ Obsługa różnych rozmiarów etykiet (29mm, 25mm, 36mm, 24mm)
- ⚡ **Szybkie połączenie** - bez czekania na parowanie

---

## 📶 Wireless Direct Setup

### Czym jest Wireless Direct?
Wireless Direct (WiFi Direct) to bezpośrednie połączenie WiFi między urządzeniem Android a drukarką **bez potrzeby routera**.

### Wymagania
- Urządzenie Android z WiFi
- Drukarka Brother PT-P950NW z włączonym Wireless Direct

### Instrukcja konfiguracji

1. **Włączenie Wireless Direct na drukarce:**
   - Na panelu drukarki przejdź do **Menu** → **Network** → **WLAN** → **Wireless Direct**
   - Włącz **Wireless Direct**
   - Zanotuj wyświetloną **nazwę sieci** (SSID) np. `DIRECT-20219_PT-P950NW`
   - Zanotuj **klucz sieciowy** (hasło) np. `BrotherOK%`
   - Zanotuj **adres IP** drukarki (sprawdź w ustawieniach WLAN, zwykle `192.168.118.1` lub `192.168.50.1`)

2. **Połączenie z drukarką na Android:**
   - Otwórz **Ustawienia** → **WiFi**
   - Znajdź i wybierz sieć drukarki (np. `DIRECT-20219_PT-P950NW`)
   - Wprowadź klucz sieciowy (np. `BrotherOK%`)
   - Poczekaj na połączenie

3. **W aplikacji:**
   - Otwórz **Kafelek Drukarki** 🖨️ na ekranie głównym
   - Wybierz **Wireless Direct** jako metodę połączenia
   - Wprowadź **adres IP** drukarki:
     - Sprawdź w ustawieniach drukarki (Menu → Network → WLAN → Wireless Direct → TCP/IP)
     - Zwykle: `192.168.118.1` lub `192.168.50.1`
   - Port: **9100**
   - Wybierz rozmiar etykiety
   - Kliknij **Save Configuration**
   - Kliknij **Test Print** aby sprawdzić połączenie

### Zalety Wireless Direct
- ✅ Szybsze połączenie niż Bluetooth
- ✅ Większy zasięg (do 10m)
- ✅ Nie wymaga zewnętrznej sieci WiFi
- ✅ Stabilne połączenie
- ✅ Obsługa wielu urządzeń jednocześnie

### Uwagi
⚠️ Gdy urządzenie jest połączone z drukarką przez Wireless Direct, **nie masz dostępu do Internetu** (wybierz drugą kartę SIM lub Bluetooth dla danych mobilnych)

---

## 📋 Porównanie metod połączenia

| Funkcja | WiFi | Bluetooth (BEZ PAROWANIA) | Wireless Direct |
|---------|------|---------------------------|-----------------||
| Wymagany router | ✅ Tak | ❌ Nie | ❌ Nie |
| Zasięg | ~50m | ~10m | ~10m |
| Prędkość | Najszybsza | Wolna | Szybka |
| Łatwość konfiguracji | Średnia | **BARDZO ŁATWA** | Łatwa |
| Dostęp do Internetu | ✅ Tak | ✅ Tak | ❌ Nie |
| Wymaga parowania | ❌ Nie | **❌ NIE (NOWOŚĆ!)** | ❌ Nie |
| Rekomendacja | Biuro/Dom | **Mobilne użycie** | W terenie bez WiFi |

---

## 🛠️ Rozwiązywanie problemów

### Błąd: "Invalid configuration"
- Sprawdź czy wprowadzony adres MAC/IP jest poprawny
- Dla Bluetooth: upewnij się że urządzenie jest sparowane
- Dla Wireless Direct: sprawdź czy jesteś połączony z siecią drukarki

### Błąd: "Device not paired"
- ⚠️ **Ten błąd NIE POWINIEN się już pojawić!**
- Aplikacja używa teraz insecure connection (bez parowania)
- Jeśli widzisz ten błąd, zaktualizuj aplikację

### Błąd: "Cannot connect to Bluetooth device"
- Upewnij się że Bluetooth jest włączony
- Sprawdź czy drukarka jest włączona i w zasięgu (~10m)
- Sprawdź czy adres MAC jest poprawny
- Spróbuj zrestartować Bluetooth na urządzeniu
- **NIE MUSISZ** parować drukarki - aplikacja łączy się automatycznie!

### Błąd: "Cannot reach printer via Wireless Direct"
- Sprawdź czy jesteś podłączony do sieci drukarki (WiFi Direct)
- Sprawdź czy adres IP jest poprawny (zwykle `192.168.50.1`)
- Upewnij się że Wireless Direct jest włączony na drukarce
- Spróbuj zrestartować drukarkę

### Test Print nie działa
- Upewnij się że konfiguracja jest zapisana
- Sprawdź czy w drukarce jest taśma z etykietami
- Dla Bluetooth: upewnij się że urządzenie jest sparowane
- Dla Wireless Direct: upewnij się że jesteś połączony z siecią drukarki

---

## 💡 Najlepsze praktyki

1. **Użyj WiFi** jeśli masz stałą sieć i drukujesz często
2. **Użyj Bluetooth (BEZ PAROWANIA!)** jeśli:
   - Pracujesz mobilnie
   - Potrzebujesz dostępu do Internetu
   - Nie chcesz tracić czasu na parowanie
   - Zmieniasz często urządzenia
3. **Użyj Wireless Direct** jeśli pracujesz w terenie bez dostępu do WiFi

---

## 📱 Przykład konfiguracji

### Bluetooth (BEZ PAROWANIA!)
```
Metoda połączenia: Bluetooth
Adres MAC: A4:34:F1:7D:4A:B8
Rozmiar etykiety: 29mm x 90mm

⚡ NOWOŚĆ: Nie wymaga parowania w ustawieniach Android!
Wystarczy włączyć Bluetooth i wprowadzić MAC.
```

### Wireless Direct
```
Metoda połączenia: Wireless Direct
Adres IP: 192.168.118.1
Port: 9100
Rozmiar etykiety: 29mm x 90mm

Uwaga: Sprawdź rzeczywisty adres IP w ustawieniach drukarki:
Menu → Network → WLAN → Wireless Direct → TCP/IP
```

### WiFi
```
Metoda połączenia: WiFi
Adres IP: 192.168.1.100
Port: 9100
Rozmiar etykiety: 29mm x 90mm
```
