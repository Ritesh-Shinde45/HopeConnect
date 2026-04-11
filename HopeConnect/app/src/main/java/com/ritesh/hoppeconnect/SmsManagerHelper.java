package com.ritesh.hoppeconnect;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


public class SmsManagerHelper {

    private static final String TAG             = "SmsManagerHelper";
    public  static final int    PERMISSION_CODE = 1001;

   

    
    public static String generateOtp() {
        int otp = (int)(Math.random() * 900_000) + 100_000;
        return String.valueOf(otp);
    }

   

    
    public static boolean hasSmsPermission(Context ctx) {
        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED;
    }

    
    public static void requestSmsPermission(Activity activity) {
        ActivityCompat.requestPermissions(
                activity,
                new String[]{Manifest.permission.SEND_SMS},
                PERMISSION_CODE
        );
    }

   

    
    public static void sendOtp(Context context, String mobile, String otp) {

       
        if (!hasSmsPermission(context)) {
            Log.w(TAG, "SEND_SMS permission not granted");
            Toast.makeText(context,
                    "SMS permission required to send OTP", Toast.LENGTH_SHORT).show();
            if (context instanceof Activity) {
                requestSmsPermission((Activity) context);
            }
            return;
        }

       
        String message = "Your HoppeConnect OTP is: " + otp
                + ". Valid for 10 minutes. Do not share this with anyone.";

       
        String SENT_ACTION     = "com.ritesh.hoppeconnect.SMS_SENT_" + mobile;
        String DELIVERED_ACTION = "com.ritesh.hoppeconnect.SMS_DELIVERED_" + mobile;

        PendingIntent sentPI = PendingIntent.getBroadcast(
                context, 0,
                new Intent(SENT_ACTION),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );
        PendingIntent deliveredPI = PendingIntent.getBroadcast(
                context, 0,
                new Intent(DELIVERED_ACTION),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

       
        context.registerReceiver(new BroadcastReceiver() {
                                     @Override public void onReceive(Context ctx, Intent intent) {
                                         switch (getResultCode()) {
                                             case Activity.RESULT_OK:
                                                 Log.d(TAG, "SMS sent successfully to " + mobile);
                                                 break;
                                             case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                                                 Log.e(TAG, "Generic SMS failure");
                                                 showError(ctx, "SMS send failed (generic)");
                                                 break;
                                             case SmsManager.RESULT_ERROR_NO_SERVICE:
                                                 Log.e(TAG, "No SMS service");
                                                 showError(ctx, "No SMS service available");
                                                 break;
                                             case SmsManager.RESULT_ERROR_NULL_PDU:
                                                 Log.e(TAG, "Null PDU");
                                                 showError(ctx, "SMS send failed (null PDU)");
                                                 break;
                                             case SmsManager.RESULT_ERROR_RADIO_OFF:
                                                 Log.e(TAG, "Radio off");
                                                 showError(ctx, "SMS send failed (radio off)");
                                                 break;
                                             default:
                                                 Log.e(TAG, "Unknown result: " + getResultCode());
                                         }
                                         try { ctx.unregisterReceiver(this); } catch (Exception ignored) {}
                                     }
                                 }, new IntentFilter(SENT_ACTION),
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                        ? Context.RECEIVER_NOT_EXPORTED : 0);

        context.registerReceiver(new BroadcastReceiver() {
                                     @Override public void onReceive(Context ctx, Intent intent) {
                                         if (getResultCode() == Activity.RESULT_OK) {
                                             Log.d(TAG, "SMS delivered to " + mobile);
                                         }
                                         try { ctx.unregisterReceiver(this); } catch (Exception ignored) {}
                                     }
                                 }, new IntentFilter(DELIVERED_ACTION),
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                        ? Context.RECEIVER_NOT_EXPORTED : 0);

       
        try {
            SmsManager smsManager;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                smsManager = context.getSystemService(SmsManager.class);
            } else {
               
                smsManager = SmsManager.getDefault();
            }

           
            String formattedNumber = mobile.startsWith("+") ? mobile : "+91" + mobile;

            smsManager.sendTextMessage(
                    formattedNumber,
                    null,          
                    message,
                    sentPI,
                    deliveredPI
            );

            Log.d(TAG, "sendTextMessage() called for " + formattedNumber);

        } catch (Exception e) {
            Log.e(TAG, "sendTextMessage failed: " + e.getMessage(), e);
            Toast.makeText(context,
                    "Failed to send OTP: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

   

    private static void showError(Context ctx, String msg) {
        if (ctx instanceof Activity) {
            ((Activity) ctx).runOnUiThread(() ->
                    Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show());
        }
    }
}