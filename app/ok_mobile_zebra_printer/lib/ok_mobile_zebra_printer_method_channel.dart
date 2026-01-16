part of 'ok_mobile_zebra_printer.dart';

class MethodChannelOkMobileZebraPrinter extends OkMobileZebraPrinterPlatform {
  @visibleForTesting
  final methodChannel = const MethodChannel('ok_mobile_zebra_printer');

  @override
  Future<String?> printDocument(
    String macAddress,
    String content,
    int labelLength,
  ) async {
    return methodChannel
        .invokeMethod<String>('printDocument', <String, dynamic>{
          'mac_address': macAddress,
          'message': content,
          'label_length': labelLength.toString(),
        });
  }
}
