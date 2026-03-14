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

/**
 * SmsManagerHelper
 * ─────────────────────────────────────────────────────────────────────────────
 * Sends a 6-digit OTP to a mobile number using the device's own SIM card.
 *
 * PERMISSIONS needed in AndroidManifest.xml (already NOT in your manifest – add them):
 *   <uses-permission android:name="android.permission.SEND_SMS" />
 *   <uses-permission android:name="android.permission.RECEIVE_SMS" />   ← optional, for delivery
 *
 * USAGE (from RegisterActivity / ForgotPasswordActivity):
 *   String otp = SmsManagerHelper.generateOtp();          // keep reference
 *   SmsManagerHelper.sendOtp(this, mobileNumber, otp);
 *
 * NOTE: This uses the device's own SIM/carrier to send SMS.
 *       For production, prefer a gateway (MSG91, Twilio) so you are not charged
 *       per-SMS from the user's plan and you can track delivery.
 */
public class SmsManagerHelper {

    private static final String TAG             = "SmsManagerHelper";
    public  static final int    PERMISSION_CODE = 1001;

    // ── Generate OTP ─────────────────────────────────────────────────────────

    /** Returns a random 6-digit OTP string. */
    public static String generateOtp() {
        int otp = (int)(Math.random() * 900_000) + 100_000;
        return String.valueOf(otp);
    }

    // ── Permission check ─────────────────────────────────────────────────────

    /** Returns true if SEND_SMS is already granted. */
    public static boolean hasSmsPermission(Context ctx) {
        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Requests SEND_SMS permission from the user.
     * Handle the result in onRequestPermissionsResult of the calling Activity.
     */
    public static void requestSmsPermission(Activity activity) {
        ActivityCompat.requestPermissions(
                activity,
                new String[]{Manifest.permission.SEND_SMS},
                PERMISSION_CODE
        );
    }

    // ── Send OTP ─────────────────────────────────────────────────────────────

    /**
     * Sends an SMS containing {@code otp} to {@code mobile}.
     *
     * @param context    calling context (Activity / Service)
     * @param mobile     10-digit mobile number (no country code needed for India via SmsManager;
     *                   add "+91" prefix if required by your carrier)
     * @param otp        the OTP string to send
     */
    public static void sendOtp(Context context, String mobile, String otp) {

        // 1) Permission guard
        if (!hasSmsPermission(context)) {
            Log.w(TAG, "SEND_SMS permission not granted");
            Toast.makeText(context,
                    "SMS permission required to send OTP", Toast.LENGTH_SHORT).show();
            if (context instanceof Activity) {
                requestSmsPermission((Activity) context);
            }
            return;
        }

        // 2) Build the message
        String message = "Your HoppeConnect OTP is: " + otp
                + ". Valid for 10 minutes. Do not share this with anyone.";

        // 3) Sent / delivered intents for logging
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

        // 4) Register one-shot receivers
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

        // 5) Send
        try {
            SmsManager smsManager;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                smsManager = context.getSystemService(SmsManager.class);
            } else {
                //noinspection deprecation
                smsManager = SmsManager.getDefault();
            }

            // Prefix country code for India if you want international format
            String formattedNumber = mobile.startsWith("+") ? mobile : "+91" + mobile;

            smsManager.sendTextMessage(
                    formattedNumber,
                    null,           // SC address – null = default
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

    // ── Utility ───────────────────────────────────────────────────────────────

    private static void showError(Context ctx, String msg) {
        if (ctx instanceof Activity) {
            ((Activity) ctx).runOnUiThread(() ->
                    Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show());
        }
    }
}