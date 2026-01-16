import 'package:flutter_test/flutter_test.dart';
import 'package:ok_mobile_zebra_printer/ok_mobile_zebra_printer.dart';

void main() {
  group('text wrapping', () {
    test('wrap standard text within 1 line', () {
      const text = 'Hello World';
      const maxWidth = 20;
      final wrappedText = VoucherContentHelper.wrapText(text, maxWidth);
      expect(wrappedText, [text]);
    });

    test('wrap standard text within 2 lines', () {
      const text = 'Hello World';
      const maxWidth = 10;
      final wrappedText = VoucherContentHelper.wrapText(text, maxWidth);
      expect(wrappedText, ['Hello', 'World']);
    });

    test('wrap text with longer than limit word and short next word', () {
      const text = 'Hello fantastic World';
      const maxWidth = 8;
      final wrappedText = VoucherContentHelper.wrapText(text, maxWidth);
      expect(wrappedText, ['Hello', 'fantasti', 'c World']);
    });

    test('wrap text with longer than limit word and long next word', () {
      const text = 'Hello fantastic World';
      const maxWidth = 5;
      final wrappedText = VoucherContentHelper.wrapText(text, maxWidth);
      expect(wrappedText, ['Hello', 'fanta', 'stic', 'World']);
    });

    test('handle non-positive width', () {
      const text = 'Hello World';
      const maxWidth = -2;
      final wrappedText = VoucherContentHelper.wrapText(text, maxWidth);
      expect(wrappedText, [text]);
    });

    group('VoucherContentHelper.determineLineLength', () {
      test('returns upperCaseMediumTextWrapLength for mostly uppercase', () {
        const text = 'THIS IS AN UPPErcase text';
        final result = VoucherContentHelper.getLineLengthWithLogo(text);
        expect(result, ContentGeneratorParams.upperCaseMediumTextWrapLength);
      });

      test('returns fullMediumTextWrapLength for mostly lowercase', () {
        const text = 'This Is A Normal Text';
        final result = VoucherContentHelper.getLineLengthWithLogo(text);
        expect(result, ContentGeneratorParams.fullMediumTextWrapLength);
      });

      test('returns fullMediumTextWrapLength for no letters', () {
        const text = r'1234567890!@#$%^&*()';
        final result = VoucherContentHelper.getLineLengthWithLogo(text);
        expect(result, ContentGeneratorParams.fullMediumTextWrapLength);
      });
    });
  });
}
