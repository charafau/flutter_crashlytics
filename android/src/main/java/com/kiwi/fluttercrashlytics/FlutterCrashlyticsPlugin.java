package com.kiwi.fluttercrashlytics;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;
import io.fabric.sdk.android.Fabric;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.PluginRegistry;

import java.util.List;
import java.util.Map;

public class FlutterCrashlyticsPlugin implements MethodChannel.MethodCallHandler {

    private final Context context;

    public FlutterCrashlyticsPlugin(Context context) {
        this.context = context;
    }

    public static void registerWith(PluginRegistry.Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "flutter_crashlytics");
        channel.setMethodCallHandler(new FlutterCrashlyticsPlugin(registrar.context()));
    }

    @Override
    public void onMethodCall(MethodCall methodCall, MethodChannel.Result result) {
        if (methodCall.method.equals("initialize")) {
            Fabric.with(context, new Crashlytics());
            result.success(null);
        } else {
            if (Fabric.isInitialized()) {
                onInitialisedMethodCall(methodCall, result);
            } else {
                result.success(null);
            }
        }
    }

    void onInitialisedMethodCall(MethodCall call, MethodChannel.Result result) {
        final CrashlyticsCore core = Crashlytics.getInstance().core;
        if (call.method.equals("log")) {
            if (call.arguments instanceof String) {
                core.log(call.arguments.toString());
            } else {
                try {
                    final List<Object> info = (List<Object>) call.arguments;
                    core.log(info.get(0).toString() + ": " + info.get(1) + " " + info.get(2));
                } catch (ClassCastException ex) {
                    Log.d("FlutterCrashlytics", "" + ex.getMessage());
                }
            }
            result.success(null);
        }

        if (call.method.equals("setInfo")) {
            final Map<String, Object> info = (Map<String, Object>) call.arguments;
            final Object currentInfo = info.get("value");
            if (currentInfo instanceof String) {
                core.setString((String) info.get("key"), info.get("value").toString());
            }
            if (currentInfo instanceof Integer) {
                core.setInt((String) info.get("key"), (Integer) info.get("value"));
            }
            if (currentInfo instanceof Double) {
                core.setDouble((String) info.get("key"), (Double) info.get("value"));
            }
            if (currentInfo instanceof Boolean) {
                core.setBool((String) info.get("key"), (Boolean) info.get("value"));
            }
            if (currentInfo instanceof Float) {
                core.setFloat((String) info.get("key"), (Float) info.get("value"));
            }
            if (currentInfo instanceof Long) {
                core.setLong((String) info.get("key"), (Long) info.get("value"));
            }
            result.success(null);
        }

        if (call.method.equals("setUserInfo")) {
            final Map<String, String> info = (Map<String, String>) call.arguments;
            core.setUserEmail(info.get("email"));
            core.setUserName(info.get("name"));
            core.setUserIdentifier(info.get("id"));
            result.success(null);
        }

        if (call.method.equals("reportCrash")) {
            final Map<String, Object> exception = (Map<String, Object>) call.arguments;
            final boolean forceCrash = tryParseForceCrash(exception.get("forceCrash"));
            final FlutterException throwable = Utils.create(exception);
            if (forceCrash) {
                //Start a new activity to not crash directly under onMethod call, or it will crash JNI instead of a clean exception
                final Intent intent = new Intent(context, CrashActivity.class);
                intent.putExtra("exception", throwable);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                context.startActivity(intent);
            } else {
                core.logException(throwable);
            }

            result.success(null);

        } else {
            result.notImplemented();
        }

    }

    boolean tryParseForceCrash(Object object) {
        if (object instanceof Boolean) {
            return (boolean) object;
        }
        return false;
    }

}
