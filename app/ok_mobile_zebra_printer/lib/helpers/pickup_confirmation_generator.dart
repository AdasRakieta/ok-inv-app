// Longer lines needed for Zebra printer commands
// ignore_for_file: lines_longer_than_80_chars

part of '../ok_mobile_zebra_printer.dart';

class PickupConfirmationGenerator {
  static Future<(String, int)> generatePickupConfirmationContent({
    required String pickupCode,
    required String pickupDate,
    required String contractorName,
    required String contractorAddress,
    required String contractorNip,
    required String collectionPointName,
    required String collectionPointNumber,
    required String collectionPointAddress,
    required String collectionPointNip,
    required int bagsCount,
    required String countingCenterName,
    required String countingCenterAddress,
    required String countingCenterNip,
  }) async {
    var currentVerticalPosition = 250;

    final label = StringBuffer()
      ..write('^XA')
      ..write('^CI31');

    {
      // ------------------------- Title ------------------------------

      label.write(
        '^FO${ContentGeneratorParams.startPosition}},$currentVerticalPosition^ARN,15,10^FB${ContentGeneratorParams.textBlockWidth}},1,0,L^FDWydanie opakowań kaucyjnych\\&^FS',
      );

      currentVerticalPosition += 50;

      // ---------------------- Pickup number -------------------------

      label.write(
        '^FO${ContentGeneratorParams.startPosition}},$currentVerticalPosition^APN,15,10^FB${ContentGeneratorParams.textBlockWidth}},1,0,L^FDNumer wydania: $pickupCode\\&^FS',
      );

      currentVerticalPosition += 20;

      // ------------------------ Pickup date ----------------------------

      label.write(
        '^FO${ContentGeneratorParams.startPosition}},$currentVerticalPosition^APN,15,10^FB${ContentGeneratorParams.textBlockWidth}},1,0,L^FDData wydania: $pickupDate\\&^FS',
      );

      currentVerticalPosition += 40;
      // ------------------------- Separator -------------------------

      label.write(
        '^FO${ContentGeneratorParams.startPosition}},$currentVerticalPosition^GB${ContentGeneratorParams.textBlockWidth}},2,2^FS',
      );

      currentVerticalPosition += 30;

      // ------------------------- Issued bags count -------------------------

      label
        ..write(
          '^FO${ContentGeneratorParams.startPosition}},$currentVerticalPosition^AQN,15,10^FB${ContentGeneratorParams.textBlockWidth}},1,0,L^FDIlość wydanych worków:\\&^FS',
        )
        ..write(
          '^FO${ContentGeneratorParams.startPosition}},$currentVerticalPosition^AQN,15,10^FB${ContentGeneratorParams.textBlockWidth}},1,0,R^FD$bagsCount\\&^FS',
        );

      currentVerticalPosition += 40;

      // ------------------------- Separator -----------------------------

      label.write(
        '^FO${ContentGeneratorParams.startPosition}},$currentVerticalPosition^GB${ContentGeneratorParams.textBlockWidth}},2,2^FS',
      );

      currentVerticalPosition += 30;

      // ------------------ Collection Point Data Title ----------------

      label.write(
        '^FO${ContentGeneratorParams.startPosition}},$currentVerticalPosition^AQN,15,10^FB${ContentGeneratorParams.textBlockWidth}},3,0,L^FDDane nadawcy\\&^FS',
      );

      currentVerticalPosition += 40;

      // ------------------ Contractor Data - name ----------------------------
      final wrappedContractorName = VoucherContentHelper.wrapText(
        'Nazwa kontrahenta: $contractorName',
        VoucherContentHelper.determineFullLineLength(
          'Nazwa kontrahenta: $contractorName',
        ),
      );

      for (var i = 0; i < wrappedContractorName.length; i++) {
        label.write(
          '^FO${ContentGeneratorParams.startPosition}},$currentVerticalPosition^APN,15,10^FB${ContentGeneratorParams.textBlockWidth}},2,0,L^FD${wrappedContractorName[i]}\\&^FS',
        );

        currentVerticalPosition += 20;
      }

      // ------------------ Contractor Data - address ----------------------------

      final wrappedContractorAddress = VoucherContentHelper.wrapText(
        'Adres: $contractorAddress',
        VoucherContentHelper.determineFullLineLength(
          'Adres: $contractorAddress',
        ),
      );

      for (var i = 0; i < wrappedContractorAddress.length; i++) {
        label.write(
          '^FO${ContentGeneratorParams.startPosition}},$currentVerticalPosition^APN,15,10^FB${ContentGeneratorParams.textBlockWidth}},2,0,L^FD${wrappedContractorAddress[i]}\\&^FS',
        );

        currentVerticalPosition += 20;
      }

      // ------------------ Contractor Data - NIP ----------------------------

      label.write(
        '^FO${ContentGeneratorParams.startPosition}},$currentVerticalPosition^APN,15,10^FB${ContentGeneratorParams.textBlockWidth}},1,0,L^FDNIP kontrahenta: $contractorNip\\&^FS',
      );

      currentVerticalPosition += 40;

      // ------------------ Collection Point Data  - name ----------------------------

      final wrappedCollectionPointName = VoucherContentHelper.wrapText(
        'Nazwa punktu zbiórki: $collectionPointName',
        VoucherContentHelper.determineFullLineLength(
          'Nazwa punktu zbiórki: $collectionPointName',
        ),
      );

      for (var i = 0; i < wrappedCollectionPointName.length; i++) {
        label.write(
          '^FO${ContentGeneratorParams.startPosition}},$currentVerticalPosition^APN,15,10^FB${ContentGeneratorParams.textBlockWidth}},1,0,L^FD${wrappedCollectionPointName[i]}\\&^FS',
        );
        currentVerticalPosition += 20;
      }
      // ------------------ Collection Point Data  - number ----------------------------

      label.write(
        '^FO${ContentGeneratorParams.startPosition}},$currentVerticalPosition^APN,15,10^FB${ContentGeneratorParams.textBlockWidth}},1,0,L^FDNumer punktu zbiórki klienta: $collectionPointNumber\\&^FS',
      );

      currentVerticalPosition += 20;

      // ------------------ Collection Point Data  - address ----------------------------

      final wrappedCollectionPointAddress = VoucherContentHelper.wrapText(
        'Adres punktu zbiórki: $collectionPointAddress',
        VoucherContentHelper.determineFullLineLength(
          'Adres punktu zbiórki: $collectionPointAddress',
        ),
      );

      for (var i = 0; i < wrappedCollectionPointAddress.length; i++) {
        label.write(
          '^FO${ContentGeneratorParams.startPosition}},$currentVerticalPosition^APN,15,10^FB${ContentGeneratorParams.textBlockWidth}},1,0,L^FD${wrappedCollectionPointAddress[i]}\\&^FS',
        );

        currentVerticalPosition += 20;
      }

      // ------------------ Collection Point Data  - NIP ----------------------------

      label.write(
        '^FO${ContentGeneratorParams.startPosition}},$currentVerticalPosition^APN,15,10^FB${ContentGeneratorParams.textBlockWidth}},1,0,L^FDNIP punktu zbiórki: $collectionPointNip\\&^FS',
      );

      currentVerticalPosition += 40;

      // ------------------------- Sender signature -------------------------

      currentVerticalPosition += 100;

      label.write(
        '^FO${ContentGeneratorParams.startPosition}},$currentVerticalPosition^APN,20,20^FD....................................................................................^FS',
      );
      currentVerticalPosition += 20;

      label.write(
        '^FO${ContentGeneratorParams.startPosition}},$currentVerticalPosition^APN,15,10^FB${ContentGeneratorParams.textBlockWidth}},1,0,L^FD( Podpis nadawcy )\\&^FS',
      );

      currentVerticalPosition += 40;

      // ------------------------- Separator -------------------------

      label.write(
        '^FO${ContentGeneratorParams.startPosition}},$currentVerticalPosition^GB${ContentGeneratorParams.textBlockWidth}},2,2^FS',
      );

      currentVerticalPosition += 30;

      // ----------------------- Carrier data - title -------------------------

      label.write(
        '^FO${ContentGeneratorParams.startPosition}},$currentVerticalPosition^AQN,30,20^FB${ContentGeneratorParams.textBlockWidth}},1,0,L^FDDane przewoźnika\\&^FS',
      );

      // ------------------ Carrier name and car number ----------------------

      currentVerticalPosition += 120;
      label.write(
        '^FO${ContentGeneratorParams.startPosition}},$currentVerticalPosition^APN,20,20^FD.....................................................................................^FS',
      );
      currentVerticalPosition += 20;

      label.write(
        '^FO${ContentGeneratorParams.startPosition}},$currentVerticalPosition^APN,15,10^FB${ContentGeneratorParams.textBlockWidth}},1,0,L^FD( Nazwa przewoźnika / nr auta )\\&^FS',
      );

      // ------------------ Driver signature ----------------------

      currentVerticalPosition += 120;
      label.write(
        '^FO${ContentGeneratorParams.startPosition}},$currentVerticalPosition^APN,20,20^FD....................................................................................^FS',
      );
      currentVerticalPosition += 20;

      label.write(
        '^FO${ContentGeneratorParams.startPosition}},$currentVerticalPosition^APN,15,10^FB${ContentGeneratorParams.textBlockWidth}},1,0,L^FD( Podpis kierowcy )\\&^FS',
      );

      currentVerticalPosition += 40;

      // ------------------------- Separator -------------------------

      label.write(
        '^FO${ContentGeneratorParams.startPosition}},$currentVerticalPosition^GB${ContentGeneratorParams.textBlockWidth}},2,2^FS',
      );

      currentVerticalPosition += 30;

      // ------------------------- Receiver data - title -------------------------

      label.write(
        '^FO${ContentGeneratorParams.startPosition}},$currentVerticalPosition^AQN,30,20^FB${ContentGeneratorParams.textBlockWidth}},1,0,L^FDDane odbiorcy\\&^FS',
      );

      currentVerticalPosition += 40;

      // ------------------------- Receiver data -------------------------

      final wrappedCCName = VoucherContentHelper.wrapText(
        'Nazwa kontrahenta: $countingCenterName',
        VoucherContentHelper.determineFullLineLength(
          'Nazwa kontrahenta: $countingCenterName',
        ),
      );

      for (var i = 0; i < wrappedCCName.length; i++) {
        label.write(
          '^FO${ContentGeneratorParams.startPosition}},$currentVerticalPosition^APN,15,10^FB${ContentGeneratorParams.textBlockWidth}},1,0,L^FD${wrappedCCName[i]}\\&^FS',
        );

        currentVerticalPosition += 20;
      }

      // ------------------ Receiver data - address ----------------------

      final wrappedCCAddress = VoucherContentHelper.wrapText(
        'Adres: $countingCenterAddress',
        VoucherContentHelper.determineFullLineLength(
          'Adres: $countingCenterAddress',
        ),
      );

      for (var i = 0; i < wrappedCCAddress.length; i++) {
        label.write(
          '^FO${ContentGeneratorParams.startPosition}},$currentVerticalPosition^APN,15,10^FB${ContentGeneratorParams.textBlockWidth}},1,0,L^FD${wrappedCCAddress[i]}\\&^FS',
        );

        currentVerticalPosition += 20;
      }

      label.write(
        '^FO${ContentGeneratorParams.startPosition}},$currentVerticalPosition^APN,15,10^FB${ContentGeneratorParams.textBlockWidth}},1,0,L^FDNIP kontrahenta: $countingCenterNip\\&^FS',
      );

      currentVerticalPosition += 40;

      // ------------------ Receiver signature ----------------------

      currentVerticalPosition += 100;
      label.write(
        '^FO${ContentGeneratorParams.startPosition}},$currentVerticalPosition^APN,20,20^FD...................................................................................^FS',
      );
      currentVerticalPosition += 20;

      label.write(
        '^FO${ContentGeneratorParams.startPosition}},$currentVerticalPosition^APN,15,10^FB${ContentGeneratorParams.textBlockWidth}},1,0,L^FD( Podpis odbiorcy )\\&^FS',
      );

      currentVerticalPosition += 40;

      // ------------------------- Separator -------------------------

      label.write(
        '^FO${ContentGeneratorParams.startPosition}},$currentVerticalPosition^GB${ContentGeneratorParams.textBlockWidth}},2,2^FS',
      );

      currentVerticalPosition += 30;

      // ------------------------- OK logo -------------------------
      try {
        imageCache.clear();

        final kaulfandLogoCommand = await VoucherContentHelper.convertImage(
          'packages/ok_mobile_zebra_printer/assets/ok_logo.png',
        );

        label.write(
          '^FO${ContentGeneratorParams.startPosition}},$currentVerticalPosition,$kaulfandLogoCommand',
        );
      } catch (e) {
        log('Error loading image: $e');
      } finally {
        // ------------------------- Operator address -------------------------

        label.write(
          '^FO${ContentGeneratorParams.startWithLogoPosition}},$currentVerticalPosition^APN,15,10^FB${ContentGeneratorParams.textWithLogoBlockWidth}},1,0,L^FDPodmiot reprezentujący:\\&^FS',
        );

        currentVerticalPosition += 20;

        // TODO change the static address to dynamic
        label.write(
          '^FO${ContentGeneratorParams.startWithLogoPosition}},$currentVerticalPosition^APN,15,10^FB${ContentGeneratorParams.textWithLogoBlockWidth}},1,0,L^FDOK Operator Kaucyjny SA\\&^FS',
        );

        currentVerticalPosition += 20;

        label.write(
          '^FO${ContentGeneratorParams.startWithLogoPosition}},$currentVerticalPosition^APN,15,10^FB${ContentGeneratorParams.textWithLogoBlockWidth}},1,0,L^FDul. Kokotek 33, 41-700 Ruda Śląska\\&^FS',
        );

        currentVerticalPosition += 30;

        label.write(
          '^FO${ContentGeneratorParams.startWithLogoPosition}},$currentVerticalPosition^APN,15,10^FB${ContentGeneratorParams.textWithLogoBlockWidth}},1,0,L^FDKRS 0001102670, NIP 6412566866\\&^FS',
        );

        currentVerticalPosition += 20;

        label.write(
          '^FO${ContentGeneratorParams.startWithLogoPosition}},$currentVerticalPosition^APN,15,10^FB${ContentGeneratorParams.textWithLogoBlockWidth}},1,0,L^FDREGON 528472233, BDO 000653216\\&^FS',
        );

        currentVerticalPosition += 50;

        // ------------------------- Separator -------------------------

        label.write(
          '^FO${ContentGeneratorParams.startPosition}},$currentVerticalPosition^GB${ContentGeneratorParams.textBlockWidth}},2,2^FS',
        );

        currentVerticalPosition += 30;

        label
          ..write('^PQ2') //  this line makes document be printed twice
          ..write('^XZ');
      }
    }

    return (label.toString(), currentVerticalPosition);
  }
}
