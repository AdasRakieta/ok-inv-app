// ============================================================
// SKRYPT API DO OBSŁUGI MAGAZYNU (BEZPIECZNY DLA NAGŁÓWKÓW)
// ============================================================

// --- KONFIGURACJA ---
var ID_ARKUSZA = "1MZx5IrJv8ZhhQeFQyFaZeDBbiTYrmhZgLEcaZcX1WRo";

// Konfiguracja (dla informacji)
var KONFIGURACJA = [
  { nazwa: "Skanery", colKod: 1, colStatus: 2 },
  { nazwa: "Drukarki", colKod: 1, colStatus: 2 },
  { nazwa: "Stacje do drukarek", colKod: 1, colStatus: 2 },
  { nazwa: "Stacje Dokujące", colKod: 1, colStatus: 2 },
  { nazwa: "Skanery tc27", colKod: 1, colStatus: 2 },
];
// --- KONIEC KONFIGURACJI ---

/**
 * Główna funkcja obsługująca GET requests
 */
function doGet(e) {
  var nazwaArkusza = e.parameter.arkusz;
  var filterSn = e.parameter.sn;
  
  if (!nazwaArkusza) {
    return ContentService.createTextOutput(JSON.stringify({
      status: "BLAD",
      message: "Podaj nazwę arkusza w URL"
    })).setMimeType(ContentService.MimeType.JSON);
  }

  try {
    var ss = SpreadsheetApp.openById(ID_ARKUSZA);
    var sheet = ss.getSheetByName(nazwaArkusza);

    if (!sheet) {
      return ContentService.createTextOutput(JSON.stringify({
        status: "BLAD",
        message: "Nie znaleziono arkusza: " + nazwaArkusza
      })).setMimeType(ContentService.MimeType.JSON);
    }

    var data = sheet.getDataRange().getValues();
    if (data.length === 0) {
      return ContentService.createTextOutput(JSON.stringify({
        status: "SUKCES",
        data: []
      })).setMimeType(ContentService.MimeType.JSON);
    }

    var headers = data[0];
    var result = [];
    
    for (var i = 1; i < data.length; i++) {
      var rowObj = {};
      var hasData = false;
      
      for (var j = 0; j < headers.length; j++) {
        rowObj[headers[j]] = data[i][j];
        if (data[i][j] !== "") hasData = true;
      }
      
      // Jeśli podano SN do filtrowania, sprawdź pierwszą kolumnę (SN)
      if (filterSn && data[i][0].toString().trim() !== filterSn.toString().trim()) {
        continue;
      }
      
      if (hasData) result.push(rowObj);
    }
    
    return ContentService.createTextOutput(JSON.stringify(result)).setMimeType(ContentService.MimeType.JSON);
    
  } catch (error) {
    return ContentService.createTextOutput(JSON.stringify({
      status: "BLAD",
      message: "Błąd pobierania: " + error.toString()
    })).setMimeType(ContentService.MimeType.JSON);
  }
}

/**
 * Główna funkcja obsługująca POST requests
 */
function doPost(e) {
  try {
    var params = JSON.parse(e.postData.contents);
    var akcja = params.akcja;
    
    Logger.log("Otrzymano żądanie: " + akcja);
    
    if (akcja === "bulk") {
      return obslugaBulk(params);
    } else if (akcja === "insert") {
      return obslugaInsert(params);
    } else if (akcja === "update") {
      return obslugaUpdate(params);
    } else if (akcja === "add_step") {
      return obslugaAddStep(params);
    } else {
      return ContentService.createTextOutput(JSON.stringify({
        success: false,
        message: "Nieznana akcja: " + akcja
      })).setMimeType(ContentService.MimeType.JSON);
    }
  } catch (error) {
    return ContentService.createTextOutput(JSON.stringify({
      success: false,
      message: "Błąd serwera: " + error.toString()
    })).setMimeType(ContentService.MimeType.JSON);
  }
}

/**
 * POPRAWIONA funkcja Bulk - nie nadpisuje nagłówków (Wiersza 1)
 */
function obslugaBulk(params) {
  var operacje = params.operacje;
  
  if (!operacje || !Array.isArray(operacje) || operacje.length === 0) {
    return ContentService.createTextOutput(JSON.stringify({
      success: false,
      message: "Brak operacji do wykonania"
    })).setMimeType(ContentService.MimeType.JSON);
  }
  
  var ss = SpreadsheetApp.openById(ID_ARKUSZA);
  var updateCount = 0;
  var insertCount = 0;
  var errorCount = 0;
  var errors = [];
  
  // Grupowanie operacji po arkuszach
  var operacjePoArkuszach = {};
  for (var i = 0; i < operacje.length; i++) {
    var op = operacje[i];
    var arkuszNazwa = op.arkusz;
    if (!operacjePoArkuszach[arkuszNazwa]) operacjePoArkuszach[arkuszNazwa] = [];
    operacjePoArkuszach[arkuszNazwa].push(op);
  }
  
  var arkusze = Object.keys(operacjePoArkuszach);
  for (var a = 0; a < arkusze.length; a++) {
    var arkuszNazwa = arkusze[a];
    var arkuszOperacje = operacjePoArkuszach[arkuszNazwa];
    
    try {
      var sheet = ss.getSheetByName(arkuszNazwa);
      if (!sheet) {
        errorCount += arkuszOperacje.length;
        errors.push("Nie znaleziono arkusza: " + arkuszNazwa);
        continue;
      }
      
      var dataRange = sheet.getDataRange();
      var allData = dataRange.getValues(); // Pobieramy wszystko (razem z nagłówkiem)
      
      if (allData.length === 0) {
        errorCount += arkuszOperacje.length;
        errors.push("Pusty arkusz: " + arkuszNazwa);
        continue;
      }
      
      var headers = allData[0];
      
      // Mapowanie kolumn
      var ci = {
        urzadzenie: findColumnIndex(headers, "Urzadzenie"),
        sn: findSnColumnIndex(headers, arkuszNazwa),
        status: findColumnIndex(headers, "Status"),
        kod: findColumnIndex(headers, "Kod"),
        nazwa: findColumnIndex(headers, "Nazwa"),
        miejsce: findColumnIndex(headers, "Miejsce"),
        firma: findColumnIndex(headers, "Firma"),
        komentarz: findColumnIndex(headers, "Komentarz"),
        kategoria: findColumnIndex(headers, "Kategoria"),
        data: findColumnIndex(headers, "Data"),
        model: findColumnIndex(headers, "Model")
      };
      
      if (ci.sn === -1) {
        errorCount += arkuszOperacje.length;
        errors.push("Brak kolumny SN w arkuszu: " + arkuszNazwa);
        continue;
      }
      
      // Mapa SN -> indeks wiersza
      var snToRow = {};
      for (var r = 1; r < allData.length; r++) {
        var sn = allData[r][ci.sn];
        if (sn) snToRow[sn.toString().trim()] = r;
      }
      
      var noweWiersze = [];
      
      // Przetwarzanie operacji
      for (var j = 0; j < arkuszOperacje.length; j++) {
        var op = arkuszOperacje[j];
        
        try {
          if (op.typ === "update") {
            var rowIdx = snToRow[op.serialNumber];
            if (rowIdx !== undefined) {
              var row = allData[rowIdx];
              if (op.status && ci.status !== -1) row[ci.status] = op.status;
              if (op.kod && ci.kod !== -1) row[ci.kod] = op.kod;
              if (op.nazwa && ci.nazwa !== -1) row[ci.nazwa] = op.nazwa;
              if (op.miejsce && ci.miejsce !== -1) row[ci.miejsce] = op.miejsce;
              updateCount++;
            } else {
              errorCount++;
              errors.push("Nie znaleziono SN: " + op.serialNumber);
            }
          } else if (op.typ === "insert") {
            if (!op.dane) continue;
            var nd = op.dane;
            var newRow = new Array(headers.length).fill("");
            
            for (var c = 0; c < headers.length; c++) {
              if (c === ci.urzadzenie && ci.urzadzenie !== -1) newRow[c] = nd.Urzadzenie || "";
              else if (c === ci.sn && ci.sn !== -1) newRow[c] = nd.serialNumber || "";
              else if (c === ci.status && ci.status !== -1) newRow[c] = nd.Status || "";
              else if (c === ci.kod && ci.kod !== -1) newRow[c] = nd.Kod || "";
              else if (c === ci.nazwa && ci.nazwa !== -1) newRow[c] = nd.Nazwa || "";
              else if (c === ci.miejsce && ci.miejsce !== -1) newRow[c] = nd.Miejsce || "";
              else if (c === ci.firma && ci.firma !== -1) newRow[c] = nd.Firma || "";
              else if (c === ci.komentarz && ci.komentarz !== -1) newRow[c] = nd.Komentarz || "";
              else if (c === ci.kategoria && ci.kategoria !== -1) newRow[c] = nd.Kategoria || "";
              else if (c === ci.data && ci.data !== -1) newRow[c] = nd.Data || "";
              else if (c === ci.model && ci.model !== -1) newRow[c] = nd.Model || "";
            }

            var trimmedSn = (nd.serialNumber || "").toString().trim();
            if (trimmedSn && snToRow.hasOwnProperty(trimmedSn)) {
              var existingRowIdx = snToRow[trimmedSn];
              allData[existingRowIdx] = newRow;
              updateCount++;
            } else {
              noweWiersze.push(newRow);
              insertCount++;
            }
          }
        } catch (opError) {
          errorCount++;
          errors.push("Błąd: " + opError.toString());
        }
      }
      
      // --- KLUCZOWA ZMIANA: ZAPISYWANIE Z POMINIĘCIEM NAGŁÓWKA (WIERSZ 1) ---
      // Dzielimy dane: allData[0] to nagłówek, reszta to dane
      if (allData.length > 1) {
        var dataOnly = allData.slice(1); // Odetnij nagłówek
        // Zapisz od wiersza nr 2
        sheet.getRange(2, 1, dataOnly.length, dataOnly[0].length).setValues(dataOnly);
      }
      
      // Dodawanie nowych wierszy (nadal bezpieczne dla nagłówka, bo idzie na koniec)
      if (noweWiersze.length > 0) {
        var lastRow = sheet.getLastRow();
        var insertBeforeFormulas = false;
        if (lastRow > 1) {
          try {
            var lastRowRange = sheet.getRange(lastRow, 1, 1, sheet.getLastColumn());
            var lastRowFormulas = lastRowRange.getFormulas()[0];
            for (var f = 0; f < lastRowFormulas.length; f++) {
              if (lastRowFormulas[f] !== "") { insertBeforeFormulas = true; break; }
            }
          } catch (e) {}
        }

        if (insertBeforeFormulas) {
          sheet.insertRowsBefore(lastRow, noweWiersze.length);
          sheet.getRange(lastRow, 1, noweWiersze.length, noweWiersze[0].length).setValues(noweWiersze);
        } else {
          // Używamy getRange zamiast appendRow dla pewności
          sheet.getRange(lastRow + 1, 1, noweWiersze.length, noweWiersze[0].length).setValues(noweWiersze);
        }
      }
      
    } catch (sheetError) {
      errorCount += arkuszOperacje.length;
      errors.push("Błąd arkusza " + arkuszNazwa + ": " + sheetError.toString());
    }
  }
  
  var message = "Bulk zakończony: " + updateCount + " zaktualizowanych, " + 
                insertCount + " dodanych, " + errorCount + " błędów";
  if (errors.length > 0) message += ". Info: " + errors.join("; ");
  
  return ContentService.createTextOutput(JSON.stringify({
    success: errorCount === 0,
    message: message,
    updateCount: updateCount,
    insertCount: insertCount,
    errorCount: errorCount
  })).setMimeType(ContentService.MimeType.JSON);
}

// --- FUNKCJE POMOCNICZE ---

function findSnColumnIndex(headers, sheetName) {
  var snNames = [sheetName];
  if (sheetName === "Skanery tc27") snNames = ["Skanery tc27", "Skanery"];
  else if (sheetName === "Stacje Dokujące") snNames = ["Stacje Dokujące", "Stacje dokujące"];
  
  for (var i = 0; i < headers.length; i++) {
    var h = headers[i].toString().trim();
    for (var j = 0; j < snNames.length; j++) {
      if (h === snNames[j]) return i;
    }
  }
  return -1;
}

function findColumnIndex(headers, columnName) {
  var variants = [columnName];
  if (columnName === "Urzadzenie") variants = ["Urządzenie", "Urzadzenie"];
  else if (columnName === "Miejsce") variants = ["Miejsce", "Miejsce"];
  
  for (var i = 0; i < headers.length; i++) {
    var h = headers[i].toString().trim();
    for (var j = 0; j < variants.length; j++) {
      if (h === variants[j]) return i;
    }
  }
  return -1;
}

/**
 * Obsługa pojedynczego INSERT (bezpieczna dla D1)
 */
function obslugaInsert(params) {
  try {
    var ss = SpreadsheetApp.openById(ID_ARKUSZA);
    var arkuszNazwa = params.arkusz;
    var sheet = ss.getSheetByName(arkuszNazwa);
    
    if (!sheet) return errorResponse("Nie znaleziono arkusza: " + arkuszNazwa);
    
    var dane = params.dane;
    var headers = sheet.getRange(1, 1, 1, sheet.getLastColumn()).getValues()[0];
    
    // Mapowanie 1:1 jak w bulk
    var ci = {
      urzadzenie: findColumnIndex(headers, "Urzadzenie"),
      sn: findSnColumnIndex(headers, arkuszNazwa),
      status: findColumnIndex(headers, "Status"),
      kod: findColumnIndex(headers, "Kod"),
      nazwa: findColumnIndex(headers, "Nazwa"),
      miejsce: findColumnIndex(headers, "Miejsce"),
      firma: findColumnIndex(headers, "Firma"),
      komentarz: findColumnIndex(headers, "Komentarz"),
      kategoria: findColumnIndex(headers, "Kategoria"),
      data: findColumnIndex(headers, "Data"),
      model: findColumnIndex(headers, "Model")
    };
    
    var newRow = new Array(headers.length).fill("");
    for (var c = 0; c < headers.length; c++) {
      if (c === ci.urzadzenie && ci.urzadzenie !== -1) newRow[c] = dane.Urzadzenie || "";
      else if (c === ci.sn && ci.sn !== -1) newRow[c] = dane.serialNumber || "";
      else if (c === ci.status && ci.status !== -1) newRow[c] = dane.Status || "";
      else if (c === ci.kod && ci.kod !== -1) newRow[c] = dane.Kod || "";
      else if (c === ci.nazwa && ci.nazwa !== -1) newRow[c] = dane.Nazwa || "";
      else if (c === ci.miejsce && ci.miejsce !== -1) newRow[c] = dane.Miejsce || "";
      else if (c === ci.firma && ci.firma !== -1) newRow[c] = dane.Firma || "";
      else if (c === ci.komentarz && ci.komentarz !== -1) newRow[c] = dane.Komentarz || "";
      else if (c === ci.kategoria && ci.kategoria !== -1) newRow[c] = dane.Kategoria || "";
      else if (c === ci.data && ci.data !== -1) newRow[c] = dane.Data || "";
      else if (c === ci.model && ci.model !== -1) newRow[c] = dane.Model || "";
    }
    
    // Sprawdzamy czy SN istnieje (UPSERT)
    var existingData = sheet.getDataRange().getValues();
    var snCol = findSnColumnIndex(existingData[0], arkuszNazwa);
    var foundRow = -1;
    if (snCol !== -1) {
      for (var r = 1; r < existingData.length; r++) {
        if (existingData[r][snCol] && existingData[r][snCol].toString().trim() === (dane.serialNumber || "").toString().trim()) {
          foundRow = r + 1;
          break;
        }
      }
    }

    if (foundRow !== -1) {
      // UPDATE - piszemy w konkretny wiersz (nigdy nie będzie to wiersz 1)
      sheet.getRange(foundRow, 1, 1, newRow.length).setValues([newRow]);
    } else {
      // INSERT na końcu lub przed formułami
      var lastRow = sheet.getLastRow();
      var insertBeforeFormulas = false;
      if (lastRow > 1) {
        try {
            var forms = sheet.getRange(lastRow, 1, 1, sheet.getLastColumn()).getFormulas()[0];
            for (var f=0; f<forms.length; f++) if(forms[f]!=="") insertBeforeFormulas=true;
        } catch(e){}
      }
      
      if (insertBeforeFormulas) {
        sheet.insertRowsBefore(lastRow, 1);
        sheet.getRange(lastRow, 1, 1, newRow.length).setValues([newRow]);
      } else {
        sheet.appendRow(newRow);
      }
    }
    
    return successResponse("Dodano/Zaktualizowano wiersz w " + arkuszNazwa);
    
  } catch (error) {
    return errorResponse("Błąd INSERT: " + error.toString());
  }
}

/**
 * Obsługa pojedynczego UPDATE (bezpieczna dla D1)
 */
function obslugaUpdate(params) {
  try {
    var ss = SpreadsheetApp.openById(ID_ARKUSZA);
    var sheet = ss.getSheetByName(params.arkusz);
    if (!sheet) return errorResponse("Nie znaleziono arkusza");
    
    var allData = sheet.getDataRange().getValues();
    var headers = allData[0];
    
    var ci = {
      sn: findSnColumnIndex(headers, params.arkusz),
      status: findColumnIndex(headers, "Status"),
      kod: findColumnIndex(headers, "Kod"),
      nazwa: findColumnIndex(headers, "Nazwa"),
      miejsce: findColumnIndex(headers, "Miejsce")
    };
    
    var rowIndex = -1;
    // Pętla od 1, więc pomijamy nagłówek
    for (var r = 1; r < allData.length; r++) {
      if (allData[r][ci.sn] && allData[r][ci.sn].toString().trim() === params.serialNumber) {
        rowIndex = r;
        break;
      }
    }
    
    if (rowIndex === -1) return errorResponse("Nie znaleziono SN: " + params.serialNumber);
    
    // Zapisujemy do rowIndex + 1 (czyli min. wiersz 2)
    var rowNum = rowIndex + 1;
    if (params.status && ci.status !== -1) sheet.getRange(rowNum, ci.status + 1).setValue(params.status);
    if (params.kod && ci.kod !== -1) sheet.getRange(rowNum, ci.kod + 1).setValue(params.kod);
    if (params.nazwa && ci.nazwa !== -1) sheet.getRange(rowNum, ci.nazwa + 1).setValue(params.nazwa);
    if (params.miejsce && ci.miejsce !== -1) sheet.getRange(rowNum, ci.miejsce + 1).setValue(params.miejsce);
    
    return successResponse("Zaktualizowano wiersz " + rowNum);
    
  } catch (error) {
    return errorResponse("Błąd UPDATE: " + error.toString());
  }
}

function successResponse(msg) {
  return ContentService.createTextOutput(JSON.stringify({success: true, message: msg})).setMimeType(ContentService.MimeType.JSON);
}
function errorResponse(msg) {
  return ContentService.createTextOutput(JSON.stringify({success: false, message: msg})).setMimeType(ContentService.MimeType.JSON);
}

/**
 * Obsługa dodawania nowego kroku do historii urządzenia
 */
function obslugaAddStep(params) {
  try {
    var arkuszNazwa = params.arkusz;
    var serialNumber = params.serialNumber;
    var krok = params.krok;
    var data = params.data;
    
    if (!arkuszNazwa || !serialNumber || !krok || !data) {
      return errorResponse("Brak wymaganych parametrów: arkusz, serialNumber, krok, data");
    }
    
    var ss = SpreadsheetApp.openById(ID_ARKUSZA);
    var sheet = ss.getSheetByName(arkuszNazwa);
    
    if (!sheet) {
      return errorResponse("Nie znaleziono arkusza: " + arkuszNazwa);
    }
    
    var allData = sheet.getDataRange().getValues();
    var headers = allData[0];
    
    // Znajdź kolumnę SN (zakładamy pierwszą kolumnę)
    var snCol = 0; // SN w pierwszej kolumnie
    
    var rowIndex = -1;
    for (var r = 1; r < allData.length; r++) {
      if (allData[r][snCol] && allData[r][snCol].toString().trim() === serialNumber.toString().trim()) {
        rowIndex = r;
        break;
      }
    }
    
    if (rowIndex !== -1) {
      // Istniejący wiersz - dodaj na końcu
      var rowNum = rowIndex + 1;
      var lastCol = sheet.getLastColumn();
      sheet.getRange(rowNum, lastCol + 1).setValue(krok);
      sheet.getRange(rowNum, lastCol + 2).setValue(data);
    } else {
      // Nowy wiersz
      var newRow = [serialNumber, krok, data];
      sheet.appendRow(newRow);
    }
    
    return successResponse("Dodano krok do historii dla SN: " + serialNumber);
    
  } catch (error) {
    return errorResponse("Błąd add_step: " + error.toString());
  }
}

function testBulkInsert() {
  Logger.log("=== TEST BULK ===");
  var testParams = {
    akcja: "bulk",
    operacje: [{
      typ: "insert",
      arkusz: "Skanery",
      serialNumber: "TEST_BEZ_D1",
      dane: { serialNumber: "TEST_BEZ_D1", Urzadzenie: "Test", Status: "Test", Kod: "123", Nazwa: "Test", Miejsce: "Test" }
    }]
  };
  var res = obslugaBulk(testParams);
  Logger.log(res.getContent());
}

function testAddStep() {
  Logger.log("=== TEST ADD STEP ===");
  var testParams = {
    akcja: "add_step",
    arkusz: "Historia", // Załóżmy nazwę arkusza
    serialNumber: "S25013524202057",
    krok: "Wydano: Test",
    data: "2025-12-17 14:00"
  };
  var res = obslugaAddStep(testParams);
  Logger.log(res.getContent());
}