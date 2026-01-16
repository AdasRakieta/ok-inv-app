part of '../ok_mobile_zebra_printer.dart';

class VoucherContentHelper {
  static const upperCaseThreshold = 0.3;

  static int getLineLengthWithLogo(String text) {
    final capitalCount = text.runes
        .where(
          (r) =>
              String.fromCharCode(r).toUpperCase() == String.fromCharCode(r) &&
              RegExp('[A-Z]').hasMatch(String.fromCharCode(r)),
        )
        .length;
    final letterCount = text.runes
        .where((r) => RegExp('[A-Za-z]').hasMatch(String.fromCharCode(r)))
        .length;
    if (letterCount > 0 && capitalCount / letterCount > upperCaseThreshold) {
      return ContentGeneratorParams.upperCaseMediumTextWrapLength;
    }
    return ContentGeneratorParams.fullMediumTextWrapLength;
  }

  static int determineFullLineLength(String text) {
    final capitalCount = text.runes
        .where(
          (r) =>
              String.fromCharCode(r).toUpperCase() == String.fromCharCode(r) &&
              RegExp('[A-Z]').hasMatch(String.fromCharCode(r)),
        )
        .length;
    final letterCount = text.runes
        .where((r) => RegExp('[A-Za-z]').hasMatch(String.fromCharCode(r)))
        .length;
    if (letterCount > 0 && capitalCount / letterCount > upperCaseThreshold) {
      return ContentGeneratorParams.upperCaseFullTextWrapLength;
    }
    return ContentGeneratorParams.fullTextWrapLength;
  }

  static List<String> wrapText(String input, int maxLength) {
    if (maxLength <= 0) {
      return [input];
    }

    final words = input.split(RegExp(r'\s+'));
    final result = <String>[];
    var currentLine = '';

    for (final word in words) {
      if (word.length > maxLength) {
        if (currentLine.isNotEmpty) {
          result.add(currentLine.trim());
          currentLine = '';
        }
        for (var i = 0; i < word.length; i += maxLength) {
          final end = (i + maxLength < word.length)
              ? i + maxLength
              : word.length;
          if (end == maxLength) {
            result.add(word.substring(i, end));
          } else {
            currentLine = word.substring(i, end);
          }
        }
      } else {
        if ('$currentLine $word'.trim().length > maxLength) {
          result.add(currentLine.trim());
          currentLine = word;
        } else {
          currentLine += ' $word';
        }
      }
    }

    if (currentLine.isNotEmpty) {
      result.add(currentLine.trim());
    }

    return result;
  }

  static Future<String> convertImage(String imagePath) async {
    Uint8List logoData;
    if (imagePath.startsWith('/')) {
      // It's a file path on the device (contractor logo)
      logoData = await File(imagePath).readAsBytes();
    } else {
      // It's an asset path (OK logo)
      logoData = (await rootBundle.load(imagePath)).buffer.asUint8List();
    }

    final original = decodeImage(logoData);
    final resized = copyResize(original!, width: 100);
    final gray = grayscale(resized);

    final width = gray.width;
    final height = gray.height;
    final bytesPerRow = (width + 7) ~/ 8;
    final totalBytes = bytesPerRow * height;

    final rawBytes = <int>[];

    for (var y = 0; y < height; y++) {
      var byte = 0;
      var bitIndex = 7;
      for (var x = 0; x < width; x++) {
        final pixel = gray.getPixel(x, y);
        final luma = getLuminance(pixel);
        final bit = luma < 128 ? 1 : 0;

        byte |= bit << bitIndex;
        bitIndex--;

        if (bitIndex < 0 || x == width - 1) {
          rawBytes.add(byte);
          byte = 0;
          bitIndex = 7;
        }
      }
    }

    final compressed = zlib.encode(Uint8List.fromList(rawBytes));
    final z64 = base64.encode(compressed);

    final imageCommand = '^GFA,$totalBytes,$totalBytes,$bytesPerRow,:Z64:$z64';

    return imageCommand;
  }
}
