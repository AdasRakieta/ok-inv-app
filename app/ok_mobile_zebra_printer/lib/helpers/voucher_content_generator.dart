// Longer lines needed for Zebra printer commands
// ignore_for_file: lines_longer_than_80_chars

part of '../ok_mobile_zebra_printer.dart';

class VoucherContentGenerator {
  static Future<(String, int)> generateVoucherContent(
    List<Package> packages, {
    required String logoPath,
    required String contractorName,
    required String contractorAddress,
    required String contractorNip,
    required String contractorBdo,
    required String address,
    required String nip,
    required String nr,
    required String printDate,
    required String voucherNumber,
    required double voucherDepositValue,
    required String krs,
    required String court,
    required int expiryDays,
    required String? disclaimer,
    required AppEnvironmentEnum environment,
    bool reprint = false,
    bool showFooter = true,
  }) async {
    var currentVerticalPosition = 150;
    var totalPlastic = 0;
    var totalCan = 0;
    var totalGlass = 0;

    final depositValue = voucherDepositValue
        .toStringAsFixed(2)
        .replaceAll('.', ',');

    final label = StringBuffer()
      ..write('^XA')
      ..write('^CI31');

    // ------------------------- Contractor logo -------------------------
    try {
      imageCache.clear();

      final contractorLogoCommand = await VoucherContentHelper.convertImage(
        logoPath,
      );

      label.write(
        '^FO${ContentGeneratorParams.startPosition}},$currentVerticalPosition,$contractorLogoCommand',
      );
    } catch (e) {
      log('Error loading image: $e');
    } finally {
      // ------------------------- Contractor address -------------------------
      label.write(
        '^FO${ContentGeneratorParams.startWithLogoPosition}},$currentVerticalPosition^APN,15,10^FB${ContentGeneratorParams.textWithLogoBlockWidth}},1,0,L^FDAdres siedziby:\\&^FS',
      );

      currentVerticalPosition += 20;

      final wrappedContractorName = VoucherContentHelper.wrapText(
        contractorName,
        VoucherContentHelper.getLineLengthWithLogo(contractorName),
      );

      for (final line in wrappedContractorName) {
        label.write(
          '^FO${ContentGeneratorParams.startWithLogoPosition}},$currentVerticalPosition^APN,15,10^FB${ContentGeneratorParams.textWithLogoBlockWidth}},1,0,L^FD$line\\&^FS',
        );
        currentVerticalPosition += 20;
      }

      final wrappedContractorAddress = VoucherContentHelper.wrapText(
        contractorAddress,
        VoucherContentHelper.getLineLengthWithLogo(contractorAddress),
      );

      for (var i = 0; i < wrappedContractorAddress.length; i++) {
        label.write(
          '^FO${ContentGeneratorParams.startWithLogoPosition}},$currentVerticalPosition^APN,15,10^FB${ContentGeneratorParams.textWithLogoBlockWidth}},1,0,L^FD${wrappedContractorAddress[i]}\\&^FS',
        );
        if (i == wrappedContractorAddress.length - 1) {
          currentVerticalPosition += 30;
        } else {
          currentVerticalPosition += 20;
        }
      }

      label.write(
        '^FO${ContentGeneratorParams.startWithLogoPosition}},$currentVerticalPosition^APN,15,10^FB${ContentGeneratorParams.textWithLogoBlockWidth}},1,0,L^FDNIP $contractorNip, BDO $contractorBdo\\&^FS',
      );

      currentVerticalPosition += 30;

      // ------------------------- KRS Info (Optional) -------------------------

      if (krs.isNotEmpty && court.isNotEmpty) {
        final wrappedKrsCourt = VoucherContentHelper.wrapText(
          '$court, KRS: $krs',
          VoucherContentHelper.getLineLengthWithLogo('$court, KRS: $krs'),
        );
        for (final line in wrappedKrsCourt) {
          label.write(
            '^FO${ContentGeneratorParams.startWithLogoPosition}}, $currentVerticalPosition^APN,15,10^FB${ContentGeneratorParams.textWithLogoBlockWidth}},1,0,L^FD$line\\&^FS',
          );
          currentVerticalPosition += 20;
        }
      } else if (krs.isNotEmpty) {
        label.write(
          '^FO${ContentGeneratorParams.startWithLogoPosition}}, $currentVerticalPosition^APN,15,10^FB${ContentGeneratorParams.textWithLogoBlockWidth}},1,0,L^FDKRS: $krs\\&^FS',
        );
        currentVerticalPosition += 30;
      } else if (court.isNotEmpty) {
        final wrappedCourt = VoucherContentHelper.wrapText(
          court,
          VoucherContentHelper.getLineLengthWithLogo(court),
        );
        for (var i = 0; i < wrappedCourt.length; i++) {
          label.write(
            '^FO${ContentGeneratorParams.startWithLogoPosition}}, $currentVerticalPosition^APN,15,10^FB${ContentGeneratorParams.textWithLogoBlockWidth}},1,0,L^FD${wrappedCourt[i]}\\&^FS',
          );
          currentVerticalPosition += 20;
        }
      }
      if (krs.isNotEmpty || court.isNotEmpty) {
        currentVerticalPosition += 20;
        label.write(
          '^FO${ContentGeneratorParams.startPosition}},$currentVerticalPosition^GB${ContentGeneratorParams.textBlockWidth}},2,2^FS',
        );
      }

      currentVerticalPosition += 30;

      // ------------------------- Collection Point Address -------------------------

      label.write(
        '^FO${ContentGeneratorParams.startPosition}},$currentVerticalPosition^APN,15,10^FB${ContentGeneratorParams.textBlockWidth}},1,0,L^FDAdres sklepu:\\&^FS',
      );

      currentVerticalPosition += 20;

      final wrappedAddress2 = VoucherContentHelper.wrapText(
        '$address, nr $nr',
        VoucherContentHelper.getLineLengthWithLogo('$address, nr $nr'),
      );

      for (var i = 0; i < wrappedAddress2.length; i++) {
        label.write(
          '^FO${ContentGeneratorParams.startPosition}},$currentVerticalPosition^APN,15,10^FB${ContentGeneratorParams.textBlockWidth}},1,0,L^FD${wrappedAddress2[i]}\\&^FS',
        );

        currentVerticalPosition += 20;
      }

      currentVerticalPosition += 20;

      // ------------------------- Separator -------------------------

      label.write(
        '^FO${ContentGeneratorParams.startPosition}},$currentVerticalPosition^GB${ContentGeneratorParams.textBlockWidth}},2,2^FS',
      );

      currentVerticalPosition += 30;

      // ------------------------- Title -------------------------
      switch (environment) {
        case AppEnvironmentEnum.prd:
          label.write(
            '^FO${ContentGeneratorParams.startPosition}},$currentVerticalPosition^ARN,15,10^FB${ContentGeneratorParams.textBlockWidth}},1,0,L^FDBON KAUCYJNY\\&^FS',
          );
        case _:
          label.write(
            '^FO${ContentGeneratorParams.startPosition}},$currentVerticalPosition^ARN,15,10^FB${ContentGeneratorParams.textBlockWidth}},1,0,L^FDBON TESTOWY\\&^FS',
          );
      }

      // ------------------------- Test title -------------------------

      if (reprint) {
        currentVerticalPosition += 30;

        label.write(
          '^FO${ContentGeneratorParams.startPosition}},$currentVerticalPosition^ARN,15,10^FB${ContentGeneratorParams.textBlockWidth}},1,0,L^FDKOPIA\\&^FS',
        );
      }

      // ------------------------- Print date -------------------------

      currentVerticalPosition += 40;

      label.write(
        '^FO${ContentGeneratorParams.startPosition}},$currentVerticalPosition^APN,15,10^FB${ContentGeneratorParams.textBlockWidth}},1,0,L^FDData wydruku: $printDate\\&^FS',
      );

      currentVerticalPosition += 20;

      // ------------------------- Expiry date -------------------------

      label.write(
        '^FO${ContentGeneratorParams.startPosition}},$currentVerticalPosition^APN,15,10^FB${ContentGeneratorParams.textBlockWidth}},1,0,L^FDTermin ważności: $expiryDays dni\\&^FS',
      );

      currentVerticalPosition += 40;

      // ------------------------- Separator -------------------------

      label.write(
        '^FO${ContentGeneratorParams.startPosition}},$currentVerticalPosition^GB${ContentGeneratorParams.textBlockWidth}},2,2^FS',
      );

      currentVerticalPosition += 30;

      // ------------------------- Summary title -------------------------

      label.write(
        '^FO${ContentGeneratorParams.startPosition}},$currentVerticalPosition^AQN,15,10^FB${ContentGeneratorParams.textBlockWidth}},3,0,L^FDPodsumowanie bonu\\&^FS',
      );

      currentVerticalPosition += 40;

      // ------------------------- Packages summary -------------------------

      final plasticDeposits = <double>{};
      final canDeposits = <double>{};
      final glassDeposits = <double>{};

      for (final package in packages) {
        switch (package.type) {
          case BagType.plastic:
            totalPlastic += package.quantity;
            plasticDeposits.add(package.deposit);
          case BagType.can:
            totalCan += package.quantity;
            canDeposits.add(package.deposit);
          case BagType.glass:
            totalGlass += package.quantity;
            glassDeposits.add(package.deposit);
          case BagType.mix:
          case null:
            continue;
        }
      }

      if (totalPlastic > 0) {
        final depositPlastic = plasticDeposits.first;
        final plasticText =
            '$totalPlastic × ${depositPlastic.toStringAsFixed(2).replaceAll('.', ',')}';

        label
          ..write(
            '^FO${ContentGeneratorParams.startPosition}},$currentVerticalPosition^APN,15,10^FB${ContentGeneratorParams.textBlockWidth}},1,0,L^FDButelka plastikowa\\&^FS',
          )
          ..write(
            '^FO${ContentGeneratorParams.startPosition}},$currentVerticalPosition^APN,15,10^FB${ContentGeneratorParams.textBlockWidth}},1,0,R^FD$plasticText\\&^FS',
          );

        currentVerticalPosition += 20;
      }

      if (totalCan > 0) {
        final depositCan = canDeposits.first;
        final canText =
            '$totalCan × ${depositCan.toStringAsFixed(2).replaceAll('.', ',')}';

        label
          ..write(
            '^FO${ContentGeneratorParams.startPosition}},$currentVerticalPosition^APN,15,10^FB${ContentGeneratorParams.textBlockWidth}},1,0,L^FDPuszka\\&^FS',
          )
          ..write(
            '^FO${ContentGeneratorParams.startPosition}},$currentVerticalPosition^APN,15,10^FB${ContentGeneratorParams.textBlockWidth}},1,0,R^FD$canText\\&^FS',
          );

        currentVerticalPosition += 20;
      }

      if (totalGlass > 0) {
        final depositGlass = glassDeposits.first;
        final glassText =
            '$totalGlass × ${depositGlass.toStringAsFixed(2).replaceAll('.', ',')}';

        label
          ..write(
            '^FO${ContentGeneratorParams.startPosition}},$currentVerticalPosition^APN,15,10^FB${ContentGeneratorParams.textBlockWidth}},1,0,L^FDButelka szklana\\&^FS',
          )
          ..write(
            '^FO${ContentGeneratorParams.startPosition}},$currentVerticalPosition^APN,15,10^FB${ContentGeneratorParams.textBlockWidth}},1,0,R^FD$glassText\\&^FS',
          );

        currentVerticalPosition += 20;
      }

      currentVerticalPosition += 20;

      // ------------------------- Summary -------------------------

      label
        ..write(
          '^FO${ContentGeneratorParams.startPosition}},$currentVerticalPosition^ARN,15,10^FB${ContentGeneratorParams.textBlockWidth}},1,0,L^FDSuma PLN\\&^FS',
        )
        ..write(
          '^FO${ContentGeneratorParams.startPosition}},$currentVerticalPosition^ARN,15,10^FB${ContentGeneratorParams.textBlockWidth}},1,0,R^FD$depositValue\\&^FS',
        );

      currentVerticalPosition += 50;

      // ------------------------- Separator -------------------------

      label.write(
        '^FO${ContentGeneratorParams.startPosition}},$currentVerticalPosition^GB${ContentGeneratorParams.textBlockWidth}},2,2^FS',
      );

      currentVerticalPosition += 30;

      // ------------------------- Disclaimer -------------------------

      if (disclaimer != null) {
        final wrappedDisclaimer = VoucherContentHelper.wrapText(
          disclaimer,
          VoucherContentHelper.getLineLengthWithLogo(disclaimer),
        );

        for (var i = 0; i < wrappedDisclaimer.length; i++) {
          label.write(
            '^FO${ContentGeneratorParams.startPosition}},$currentVerticalPosition^AQN,15,10^FB${ContentGeneratorParams.textBlockWidth}},1,0,C^FD${wrappedDisclaimer[i]}\\&^FS',
          );

          currentVerticalPosition += 30;
        }
      }

      // ------------------------- Barcode -------------------------

      label.write(
        '^FO10,$currentVerticalPosition^BY2,2,80^BCN,80,N,N,N^FD$voucherNumber^FS',
      );

      currentVerticalPosition += 90;

      // voucher number with fixed, central positioning
      label.write(
        '^FO${ContentGeneratorParams.startPosition},$currentVerticalPosition^A0N,20,20^FB${ContentGeneratorParams.textBlockWidth},1,0,C^FD$voucherNumber\\&^FS',
      );

      currentVerticalPosition += 50;

      // ------------------------- Separator -------------------------

      label.write(
        '^FO${ContentGeneratorParams.startPosition}},$currentVerticalPosition^GB${ContentGeneratorParams.textBlockWidth}},2,2^FS',
      );

      currentVerticalPosition += 30;

      if (showFooter) {
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
        }
      }

      currentVerticalPosition += 50;

      label.write('^XZ');
    }

    return (label.toString(), currentVerticalPosition);
  }
}
