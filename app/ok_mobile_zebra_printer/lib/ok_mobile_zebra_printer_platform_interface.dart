part of 'ok_mobile_zebra_printer.dart';

abstract class OkMobileZebraPrinterPlatform extends PlatformInterface {
  OkMobileZebraPrinterPlatform() : super(token: _token);

  static final Object _token = Object();

  static OkMobileZebraPrinterPlatform _instance =
      MethodChannelOkMobileZebraPrinter();

  static OkMobileZebraPrinterPlatform get instance => _instance;

  static set instance(OkMobileZebraPrinterPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> printDocument(
    String macAddress,
    String content,
    int labelLength,
  ) {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }
}
