package com.ritesh.hoppeconnect;

import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.ritesh.hoppeconnect.databinding.ForgotPasswordBinding;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import io.appwrite.exceptions.AppwriteException;
import io.appwrite.models.Document;
import io.appwrite.services.Databases;

public class ForgotPasswordActivity extends AppCompatActivity {

    private static final String TAG = "ForgotPassword";

    private ForgotPasswordBinding binding;

    private String generatedOtp    = null;
    private String foundDocumentId = null;
    private String foundMobile     = null;   // for SMS delivery

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ForgotPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AppwriteService.init(this);

        binding.layoutNewPassword.setVisibility(View.GONE);
        setupClickListeners();
    }

    private void setupClickListeners() {
        binding.ivBack.setOnClickListener(v -> finish());

        binding.tvResendOTP.setOnClickListener(v -> sendOtpForIdentifier());

        binding.btnLogin.setOnClickListener(v -> {
            if (foundDocumentId == null) {
                verifyAndReveal();
            } else {
                resetPassword();
            }
        });
    }

    // ── Step 0: find user and send OTP ────────────────────────────────────────
    private void sendOtpForIdentifier() {
        String identifier = binding.etLoginUsername.getText().toString().trim();
        if (identifier.isEmpty()) {
            binding.etLoginUsername.setError("Enter username or mobile number");
            return;
        }
        setLoading(true);

        new Thread(() -> {
            try {
                Databases db    = AppwriteService.getDatabases();
                String field    = identifier.matches("\\d{10}") ? "mobile" : "username";

                List<? extends Document<?>> docs =
                        AppwriteHelper.findUserByField(db, field, identifier).getDocuments();

                if (docs.isEmpty()) {
                    runOnUiThread(() -> {
                        setLoading(false);
                        binding.etLoginUsername.setError("No account found");
                    });
                    return;
                }

                Document<?> doc = docs.get(0);
                foundDocumentId = doc.getId();

                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) doc.getData();
                foundMobile = data.get("mobile") != null
                        ? data.get("mobile").toString() : null;

                generatedOtp = SmsManagerHelper.generateOtp();
                Log.d(TAG, "Reset OTP: " + generatedOtp); // REMOVE in production

                // Send real SMS if we have the mobile number
                if (foundMobile != null && !foundMobile.isEmpty()) {
                    runOnUiThread(() -> {
                        SmsManagerHelper.sendOtp(this, foundMobile, generatedOtp);
                        setLoading(false);
                        Toast.makeText(this,
                                "OTP sent to " + foundMobile, Toast.LENGTH_SHORT).show();
                        binding.tvResendOTP.setText("Resend OTP");
                    });
                } else {
                    runOnUiThread(() -> {
                        setLoading(false);
                        // Fallback – show in toast (dev only)
                        Toast.makeText(this,
                                "[DEV] OTP: " + generatedOtp
                                        + " (no mobile on file)",
                                Toast.LENGTH_LONG).show();
                        binding.tvResendOTP.setText("Resend OTP");
                    });
                }

            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Log.e(TAG, "OTP send error", e);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    // ── Step 1: verify OTP ────────────────────────────────────────────────────
    private void verifyAndReveal() {
        if (generatedOtp == null) {
            Toast.makeText(this, "Please request an OTP first", Toast.LENGTH_SHORT).show();
            return;
        }
        String entered = binding.etLoginOTP.getText().toString().trim();
        if (!entered.equals(generatedOtp)) {
            binding.etLoginOTP.setError("Incorrect OTP");
            return;
        }
        binding.layoutNewPassword.setVisibility(View.VISIBLE);
        binding.btnLogin.setText("Reset Password");
        Toast.makeText(this, "OTP verified! Set your new password.", Toast.LENGTH_SHORT).show();
    }

    // ── Step 2: update password ───────────────────────────────────────────────
    private void resetPassword() {
        String newPass  = binding.etNewPassword.getText().toString();
        String confPass = binding.etConfirmPassword.getText().toString();

        if (newPass.isEmpty())   { binding.etNewPassword.setError("Required");    return; }
        if (confPass.isEmpty())  { binding.etConfirmPassword.setError("Required"); return; }
        if (!newPass.equals(confPass)) {
            binding.etConfirmPassword.setError("Passwords do not match"); return;
        }
        if (!isPasswordStrong(newPass)) {
            binding.etNewPassword.setError(
                    "Min 8 chars: uppercase, lowercase, digit & special char"); return;
        }

        setLoading(true);

        new Thread(() -> {
            try {
                String hashed = hashPassword(newPass);
                Databases db  = AppwriteService.getDatabases();

                Map<String, Object> update = new HashMap<>();
                update.put("password", hashed);

                AppwriteHelper.updateDocument(
                        db,
                        AppwriteService.DB_ID,
                        AppwriteService.COL_USERS,
                        foundDocumentId,
                        update
                );

                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(this,
                            "Password reset! Please log in.", Toast.LENGTH_LONG).show();
                    finish();
                });

            } catch (AppwriteException e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Log.e(TAG, "Reset error", e);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private int getPasswordScore(String pwd) {
        int s = 0;
        if (pwd.length() >= 8)                                           s++;
        if (pwd.chars().anyMatch(Character::isUpperCase))                s++;
        if (pwd.chars().anyMatch(Character::isLowerCase))                s++;
        if (pwd.chars().anyMatch(Character::isDigit))                    s++;
        if (pwd.matches(".*[!@#$%^&*()_+\\-=\\[\\]{}|;':\",./<>?].*"))  s++;
        return s;
    }
    private boolean isPasswordStrong(String pwd) { return getPasswordScore(pwd) >= 5; }

    private String hashPassword(String plain) throws Exception {
        byte[] saltBytes = new byte[16];
        new SecureRandom().nextBytes(saltBytes);
        String salt = Base64.encodeToString(saltBytes, Base64.NO_WRAP);
        MessageDigest d = MessageDigest.getInstance("SHA-256");
        d.update(salt.getBytes());
        byte[] hb = d.digest(plain.getBytes());
        StringBuilder hex = new StringBuilder();
        for (byte b : hb) hex.append(String.format(Locale.ROOT, "%02x", b));
        return salt + "$" + hex;
    }

    private void setLoading(boolean loading) {
        runOnUiThread(() -> binding.btnLogin.setEnabled(!loading));
    }
}