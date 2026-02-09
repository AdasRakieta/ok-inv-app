# 📏 Instrukcja Korekcji Etykiet Brother PT-P950NW

## 📍 Lokalizacja Plików

**Główny plik konfiguracji:**
- [`BrotherLabelFormatter.kt`](app/src/main/java/com/example/inventoryapp/utils/BrotherLabelFormatter.kt)

**Plik z rozmiarami etykiet:**
- [`PrinterConfig.kt`](app/src/main/java/com/example/inventoryapp/data/models/PrinterConfig.kt)

---

## 🎯 Parametry Rozmiaru Kodu Kreskowego

### 1️⃣ Bazowa Rozdzielczość (Linia 16)
```kotlin
private const val DOTS_PER_MM = 8  // 203 DPI ≈ 8 dots/mm
```
**Co kontroluje:** Jakość druku - ile pikseli na milimetr
- **Zwiększ** (np. 10) = wyższa rozdzielczość, większy rozmiar danych
- **Zmniejsz** (np. 6) = niższa rozdzielczość, mniejszy rozmiar danych
- ⚠️ **Uwaga:** Nie zmieniaj bez wyraźnej potrzeby!

---

### 2️⃣ Rozmiar Czcionki Bazowej (Linia 18)
```kotlin
private const val TEXT_SIZE_MEDIUM = 27f  // Reduced from 32f to prevent text cutoff
```
**Co kontroluje:** Podstawowy rozmiar tekstu (w punktach)
- **Zwiększ** (np. 30f) = większy tekst **na wszystkich** rozmiarach etykiet
- **Zmniejsz** (np. 24f) = mniejszy tekst **na wszystkich** rozmiarach etykiet

**Przykład:**
```kotlin
private const val TEXT_SIZE_MEDIUM = 30f  // Większy tekst
```

---

### 3️⃣ Proporcje Skalowania Kodów (Funkcja `getScaleFactor` - Linie 28-37)
```kotlin
private fun getScaleFactor(labelLengthMm: Int): Float {
    return when (labelLengthMm) {
        40 -> 0.5f   // Mała - 50%
        50 -> 0.75f  // Średnia - 75%
        70 -> 1.0f   // Duża - 100%
        else -> when {
            labelLengthMm < 45 -> 0.5f
            labelLengthMm < 60 -> 0.75f
            else -> 1.0f
        }
    }
}
```
**Co kontroluje:** Skalowanie **kodów kreskowych** względem rozmiaru etykiety

⚠️ **UWAGA:** Ta funkcja NIE jest używana w nowej wersji! Zamiast tego używane są bezpośrednie wartości poniżej.

---

### 4️⃣ Przestrzeń na Tekst (Linie 107-111)
```kotlin
val textHeight = when (labelLengthMm) {
    40 -> 28   // Mała - zwiększone dla pełnego tekstu
    50 -> 33   // Średnia
    else -> 40 // Duża
}
```
**Co kontroluje:** Ile pikseli jest zarezerwowane dla tekstu pod kodem

**Jak wpływa:**
- **Zwiększ** = więcej miejsca na tekst, **mniejszy** kod kreskowy
- **Zmniejsz** = mniej miejsca na tekst, **większy** kod kreskowy

**Przykład - więcej miejsca na kod kreskowy:**
```kotlin
val textHeight = when (labelLengthMm) {
    40 -> 24   // Mała - mniejsza przestrzeń = większy kod
    50 -> 28   // Średnia
    else -> 35 // Duża
}
```

---

### 5️⃣ Rozmiar Tekstu dla Każdego Rozmiaru Etykiety (Linie 133-137)
```kotlin
val textSize = when (labelLengthMm) {
    40 -> TEXT_SIZE_MEDIUM * 0.7f   // Mała: 70% zamiast 50% - większy tekst
    50 -> TEXT_SIZE_MEDIUM * 0.85f  // Średnia: 85% zamiast 75%
    else -> TEXT_SIZE_MEDIUM        // Duża: 100%
}
```
**Co kontroluje:** Wielkość tekstu dla konkretnego rozmiaru etykiety

**Jak modyfikować:**
- **0.7f** = 70% bazowego rozmiaru (TEXT_SIZE_MEDIUM)
- **0.85f** = 85%
- **TEXT_SIZE_MEDIUM** = 100% (pełny rozmiar)

**Przykład - większy tekst na małych etykietach:**
```kotlin
val textSize = when (labelLengthMm) {
    40 -> TEXT_SIZE_MEDIUM * 0.8f   // Mała: 80% (było 70%)
    50 -> TEXT_SIZE_MEDIUM * 0.9f   // Średnia: 90% (było 85%)
    else -> TEXT_SIZE_MEDIUM        // Duża: 100%
}
```

---

### 6️⃣ Pozycja Tekstu (Linia 151)
```kotlin
val textY = padding + barcodeHeight + padding + (textSize * 0.95f)
```
**Co kontroluje:** Gdzie tekst jest rysowany w pionie

**Jak wpływa:**
- **Zwiększ 0.95f** (np. 1.1f) = tekst **niżej**, dalej od kodu
- **Zmniejsz 0.95f** (np. 0.85f) = tekst **wyżej**, bliżej kodu

**Przykład - większy odstęp:**
```kotlin
val textY = padding + barcodeHeight + padding + (textSize * 1.05f)
```

---

### 7️⃣ Marginesy (Linia 106)
```kotlin
val padding = 1  // Minimal padding for all sizes
```
**Co kontroluje:** Białe marginesy dookoła etykiety

**Jak wpływa:**
- **Zwiększ** (np. 3) = więcej białej przestrzeni dookoła
- **Zmniejsz** (np. 0) = zero marginesów (może obcinać!)

**Przykład - małe marginesy:**
```kotlin
val padding = 2  // Małe marginesy dla wszystkich rozmiarów
```

---

## 📐 Rozmiar Kodu Kreskowego (Funkcja calculateBarcodeSize)

### 8️⃣ Skalowanie Szerokości i Wysokości Kodu (Linie 315-325)
```kotlin
fun calculateBarcodeSize(tapeWidthMm: Int, labelLengthMm: Int): Pair<Int, Int> {
    val scale = getScaleFactor(labelLengthMm)  // NIE UŻYWANE w obecnej wersji!
    
    val textSpaceMm = (5 * scale).toInt().coerceAtLeast(3)
    val effectivePrintHeightMm = (tapeWidthMm * 0.85).toInt()
    
    val baseWidthPx = (labelLengthMm * DOTS_PER_MM * 0.98).toInt()
    val baseHeightPx = ((effectivePrintHeightMm - textSpaceMm) * DOTS_PER_MM * 0.95).toInt()
    
    // Skalowanie rozmiaru kodu kreskowego
    val widthPx = (baseWidthPx * (0.5f + scale * 0.5f)).toInt()  // 50%-100%
    val heightPx = (baseHeightPx * (0.5f + scale * 0.5f)).toInt()
    
    return Pair(widthPx, heightPx)
}
```

**Co kontroluje:** Początkowy rozmiar kodu kreskowego przed skalowaniem w createCompositeLabelBitmap

**Kluczowe mnożniki:**
- **0.98** = kod zajmuje 98% szerokości etykiety
  - Zwiększ (np. 1.0) = szerszy kod kreskowy
  - Zmniejsz (np. 0.95) = węższy kod kreskowy

- **0.95** = kod zajmuje 95% wysokości
  - Zwiększ (np. 0.98) = wyższy kod
  - Zmniejsz (np. 0.90) = niższy kod

**Przykład - szerszy i wyższy kod:**
```kotlin
val baseWidthPx = (labelLengthMm * DOTS_PER_MM * 1.0).toInt()   // 100% szerokości
val baseHeightPx = ((effectivePrintHeightMm - textSpaceMm) * DOTS_PER_MM * 0.98).toInt()  // 98% wysokości
```

---

## 📦 Rozmiary Etykiet (PrinterConfig.kt)

### 9️⃣ Dostępne Rozmiary (Linie 83-92)
```kotlin
val LABEL_SIZES = listOf(
    Pair(29, 40),   // Mała (4cm)
    Pair(29, 50),   // Średnia (5cm) - default
    Pair(29, 70)    // Duża (7cm)
)

val LABEL_SIZE_NAMES = listOf(
    "Mała",
    "Średnia",
    "Duża"
)
```

**Co kontroluje:** Jakie rozmiary są dostępne do wyboru w aplikacji
- Pierwszy numer (29) = szerokość taśmy w mm
- Drugi numer (40/50/70) = długość etykiety w mm

**Jak dodać nowy rozmiar:**
```kotlin
val LABEL_SIZES = listOf(
    Pair(29, 30),   // Extra Mała (3cm) - NOWA!
    Pair(29, 40),   // Mała (4cm)
    Pair(29, 50),   // Średnia (5cm)
    Pair(29, 70),   // Duża (7cm)
    Pair(29, 90)    // Extra Duża (9cm) - NOWA!
)

val LABEL_SIZE_NAMES = listOf(
    "XS (3cm)",
    "Mała (4cm)",
    "Średnia (5cm)",
    "Duża (7cm)",
    "XL (9cm)"
)
```

⚠️ **WAŻNE:** Po dodaniu nowych rozmiarów, dodaj dla nich wartości w `textHeight` i `textSize` w BrotherLabelFormatter.kt!

---

## 🔧 Jak Zastosować Zmiany

### 1. Edytuj Plik
Otwórz [`BrotherLabelFormatter.kt`](app/src/main/java/com/example/inventoryapp/utils/BrotherLabelFormatter.kt) i zmień wybrane parametry.

### 2. Zbuduj APK
```powershell
.\gradlew assembleDebug
```

### 3. Zainstaluj na Urządzeniu
```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

### 4. Testuj Wydruk
Otwórz aplikację i przetestuj wydruk dla wszystkich rozmiarów etykiet.

---

## 🎨 Przykładowe Scenariusze

### Scenariusz 1: Większy Tekst na Małych Etykietach
**Problem:** Tekst na małych etykietach (4cm) jest za mały

**Rozwiązanie:**
```kotlin
// Linia 134 - zwiększ mnożnik
40 -> TEXT_SIZE_MEDIUM * 0.8f   // Było 0.7f, teraz 0.8f (80%)
```

---

### Scenariusz 2: Większy Kod Kreskowy, Mniejszy Tekst
**Problem:** Kod kreskowy za mały, tekst za duży

**Rozwiązanie:**
```kotlin
// Linia 107-111 - zmniejsz textHeight (mniej miejsca dla tekstu)
val textHeight = when (labelLengthMm) {
    40 -> 24   // Było 28
    50 -> 28   // Było 33
    else -> 35 // Było 40
}

// Linia 133-137 - zmniejsz textSize
val textSize = when (labelLengthMm) {
    40 -> TEXT_SIZE_MEDIUM * 0.6f   // Było 0.7f
    50 -> TEXT_SIZE_MEDIUM * 0.75f  // Było 0.85f
    else -> TEXT_SIZE_MEDIUM * 0.9f // Było 1.0
}
```

---

### Scenariusz 3: Większe Marginesy (Więcej Białej Przestrzeni)
**Problem:** Kod i tekst za blisko krawędzi

**Rozwiązanie:**
```kotlin
// Linia 106 - zwiększ padding
val padding = 3  // Było 1
```

---

### Scenariusz 4: Wszystko Większe (Kod + Tekst)
**Problem:** Całość za mała

**Rozwiązanie:**
```kotlin
// 1. Większa bazowa czcionka (linia 18)
private const val TEXT_SIZE_MEDIUM = 30f  // Było 27f

// 2. Mniej agresywne skalowanie tekstu (linie 133-137)
val textSize = when (labelLengthMm) {
    40 -> TEXT_SIZE_MEDIUM * 0.8f   // Było 0.7f
    50 -> TEXT_SIZE_MEDIUM * 0.9f   // Było 0.85f
    else -> TEXT_SIZE_MEDIUM        // Bez zmian
}

// 3. Szerszy kod kreskowy (linia 320 w calculateBarcodeSize)
val baseWidthPx = (labelLengthMm * DOTS_PER_MM * 1.0).toInt()  // Było 0.98
```

---

## 📊 Tabela Szybkiego Odniesienia

| Co Chcesz Zmienić | Plik | Linia | Parametr | Zwiększ/Zmniejsz |
|-------------------|------|-------|----------|------------------|
| **Rozmiar tekstu (wszystkie)** | BrotherLabelFormatter.kt | 18 | `TEXT_SIZE_MEDIUM` | ↑ = większy |
| **Rozmiar tekstu (Mała)** | BrotherLabelFormatter.kt | 134 | `0.7f` | ↑ = większy |
| **Rozmiar tekstu (Średnia)** | BrotherLabelFormatter.kt | 135 | `0.85f` | ↑ = większy |
| **Rozmiar tekstu (Duża)** | BrotherLabelFormatter.kt | 136 | `TEXT_SIZE_MEDIUM` | ↑ = większy |
| **Wysokość kodu kreskowego** | BrotherLabelFormatter.kt | 107-111 | `textHeight` | ↓ = wyższy kod |
| **Szerokość kodu kreskowego** | BrotherLabelFormatter.kt | 320 | `0.98` | ↑ = szerszy |
| **Marginesy** | BrotherLabelFormatter.kt | 106 | `padding` | ↑ = więcej białej przestrzeni |
| **Odstęp tekst-kod** | BrotherLabelFormatter.kt | 151 | `0.95f` | ↑ = większy odstęp |
| **Rozmiary etykiet** | PrinterConfig.kt | 83-92 | `LABEL_SIZES` | Dodaj nowe pary (width, height) |

---

## ⚠️ Ważne Wskazówki

1. **Zmieniaj po jednym parametrze na raz** - łatwiej śledzić efekt
2. **Testuj na wszystkich rozmiarach** - małe/średnie/duże etykiety
3. **Zachowaj kopię zapasową** - `git commit` przed zmianami
4. **Nie zmieniaj DOTS_PER_MM** - chyba że wiesz co robisz!
5. **Sprawdzaj czy tekst się mieści** - za duży może być obcięty

---

## 🔗 Linki do Plików

- [BrotherLabelFormatter.kt - Główny plik](app/src/main/java/com/example/inventoryapp/utils/BrotherLabelFormatter.kt)
- [PrinterConfig.kt - Rozmiary etykiet](app/src/main/java/com/example/inventoryapp/data/models/PrinterConfig.kt)
- [PROJECT_PLAN.md - Plan projektu](PROJECT_PLAN.md)

---

**Ostatnia aktualizacja:** 9 lutego 2026
**Wersja:** 1.0
