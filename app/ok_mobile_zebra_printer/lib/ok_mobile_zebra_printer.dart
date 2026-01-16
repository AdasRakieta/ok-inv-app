import 'dart:convert';
import 'dart:developer';
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:image/image.dart';
import 'package:ok_mobile_common/ok_mobile_common.dart';
import 'package:ok_mobile_data/ok_mobile_data.dart';
import 'package:ok_mobile_domain/ok_mobile_domain.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

part 'package:ok_mobile_zebra_printer/helpers/content_generator_params.dart';
part 'package:ok_mobile_zebra_printer/helpers/pickup_confirmation_generator.dart';
part 'package:ok_mobile_zebra_printer/helpers/voucher_content_generator.dart';
part 'package:ok_mobile_zebra_printer/helpers/voucher_content_helper.dart';
part 'package:ok_mobile_zebra_printer/ok_mobile_zebra_printer_method_channel.dart';
part 'package:ok_mobile_zebra_printer/ok_mobile_zebra_printer_platform_interface.dart';

class OkMobileZebraPrinter {
  Future<String?> printDocument(
    String macAddress,
    String documentContent,
    int labelLength,
  ) async {
    await _requestPermissions();
    final errorMessage = await OkMobileZebraPrinterPlatform.instance
        .printDocument(macAddress, documentContent, labelLength);

    if (errorMessage != null) {
      LoggerService().trackError(errorMessage);
      log(errorMessage);
      return ExceptionHelper.handleZebraError(errorMessage);
    }
    return null;
  }

  Future<void> _requestPermissions() async {
    final statuses = await [
      Permission.bluetoothConnect,
      Permission.bluetoothScan,
    ].request();
    log('-----------PERMISSIONS-----------');
    statuses.forEach((key, value) => log('$key is $value'));
    log('-----------PERMISSIONS-----------');
  }
}
