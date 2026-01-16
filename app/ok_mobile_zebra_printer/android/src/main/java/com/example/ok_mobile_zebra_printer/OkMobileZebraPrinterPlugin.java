package com.example.ok_mobile_zebra_printer;

import android.os.Looper;
import androidx.annotation.NonNull;
import com.zebra.sdk.comm.*;
import com.zebra.sdk.device.ZebraIllegalArgumentException;
import com.zebra.sdk.printer.*;
import com.zebra.sdk.printer.discovery.*;
import com.zebra.sdk.settings.SettingsException;
import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import java.util.*;

public class OkMobileZebraPrinterPlugin implements FlutterPlugin, MethodCallHandler {
  private MethodChannel channel;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "ok_mobile_zebra_printer");
    channel.setMethodCallHandler(this);
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    if (call.method.equals("printDocument")) {
      sendZplOverBluetooth(call.argument("mac_address"), call.argument("message"), call.argument("label_length"), new Listener<String>() {
        public void on(String listenerResult) {
          result.success(listenerResult);
        }
      });
    } else {
      result.notImplemented();
    }
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    channel.setMethodCallHandler(null);
  }

    private void sendZplOverBluetooth(final String printerMacAddress, final String message, final String labelLength, Listener<String> listener) {
      new Thread(
        new Runnable() {
          public void run() {
            try {
              Connection thePrinterConn = new BluetoothConnectionInsecure(printerMacAddress);
              Looper.prepare();
              thePrinterConn.open();
              ZebraPrinterLinkOs linkOsPrinter = ZebraPrinterFactory.getLinkOsPrinter(thePrinterConn);
              linkOsPrinter.setSetting("zpl.label_length", labelLength);
              thePrinterConn.write(message.getBytes("windows-1250"));
              Thread.sleep(1000);
              thePrinterConn.close();
              listener.on(null);
              Looper.myLooper().quit();
            } catch (Exception e) {
              listener.on(e.toString());
            }
          }
        }
      ).start();
    }
}
