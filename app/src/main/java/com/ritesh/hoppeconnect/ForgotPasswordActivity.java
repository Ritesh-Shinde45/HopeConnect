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

import io.appwrite.coroutines.CoroutineCallback;
import io.appwrite.exceptions.AppwriteException;
import io.appwrite.models.Document;
import io.appwrite.services.Account;
import io.appwrite.services.Databases;

public class ForgotPasswordActivity extends AppCompatActivity {

    private static final String TAG = "ForgotPassword";

    private ForgotPasswordBinding binding;

    private String foundDocumentId   = null;
    private String foundEmail        = null;
    private String foundCollectionId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ForgotPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AppwriteService.init(this);

        binding.layoutNewPassword.setVisibility(View.GONE);
        binding.tvResendOTP.setText("Send Reset Email");
        setupClickListeners();
    }

    private void setupClickListeners() {
        binding.ivBack.setOnClickListener(v -> finish());
        binding.tvResendOTP.setOnClickListener(v -> findUserAndSendReset());
        binding.btnLogin.setOnClickListener(v -> {
            if (foundDocumentId == null) {
                Toast.makeText(this,
                        "Tap 'Send Reset Email' first to find your account.",
                        Toast.LENGTH_SHORT).show();
            } else {
                resetPassword();
            }
        });
    }

    // ── Step 1: find user → trigger Appwrite password recovery email ──────────
    private void findUserAndSendReset() {
        String identifier = binding.etLoginUsername.getText().toString().trim();
        if (identifier.isEmpty()) {
            binding.etLoginUsername.setError("Enter your email or username");
            return;
        }

        Log.d(TAG, "findUserAndSendReset — identifier=" + identifier);
        setLoading(true);

        new Thread(() -> {
            try {
                Databases db    = AppwriteService.getDatabases();
                boolean isEmail = identifier.contains("@");
                String field;
                String collectionId;

                if (isEmail && AppwriteService.isAdminEmail(identifier)) {
                    field        = "email";
                    collectionId = AppwriteService.COL_ADMINS;
                } else if (isEmail) {
                    field        = "email";
                    collectionId = AppwriteService.COL_USERS;
                } else {
                    field        = "username";
                    collectionId = AppwriteService.COL_USERS;
                }

                Log.d(TAG, "Searching collection=" + collectionId + "  field=" + field);
                List<? extends Document<?>> docs =
                        AppwriteHelper.findUserByField(db, collectionId, field, identifier)
                                .getDocuments();

                // Fallback: username might be in admins collection
                if (docs.isEmpty() && !isEmail) {
                    Log.d(TAG, "Not found in users — trying admins collection");
                    docs = AppwriteHelper.findUserByField(
                            db, AppwriteService.COL_ADMINS, "username", identifier
                    ).getDocuments();
                    if (!docs.isEmpty()) collectionId = AppwriteService.COL_ADMINS;
                }

                if (docs.isEmpty()) {
                    Log.w(TAG, "No account found for identifier=" + identifier);
                    runOnUiThread(() -> {
                        setLoading(false);
                        binding.etLoginUsername.setError("No account found");
                    });
                    return;
                }

                Document<?> doc = docs.get(0);
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) doc.getData();

                foundDocumentId   = doc.getId();
                foundCollectionId = collectionId;
                foundEmail = data.get("email") != null
                        ? data.get("email").toString() : null;

                Log.d(TAG, "User found — docId=" + foundDocumentId
                        + "  email=" + foundEmail + "  collection=" + foundCollectionId);

                // ── Send Appwrite password recovery email ─────────────────────
                // Uses Appwrite's default mailer (no SMTP needed on Free plan).
                // The redirect URL below is intentional — Appwrite sends the email
                // regardless of whether the redirect is a real deep-link.
                if (foundEmail != null) {
                    Account account = AppwriteService.getAccount();
                    Log.d(TAG, "Calling account.createRecovery for email=" + foundEmail);
                    account.createRecovery(
                            foundEmail,
                            "https://cloud.appwrite.io",   // ← fixed redirect URL
                            new CoroutineCallback<>((result, error) -> {
                                if (error != null) {
                                    Log.e(TAG, "createRecovery FAILED ["
                                            + error.getClass().getSimpleName()
                                            + "]: " + error.getMessage(), error);
                                } else {
                                    Log.d(TAG, "createRecovery SUCCESS — recovery email sent to "
                                            + foundEmail);
                                }
                            })
                    );
                } else {
                    Log.w(TAG, "foundEmail is null — skipping createRecovery");
                }

                final String finalCollId = collectionId;
                runOnUiThread(() -> {
                    setLoading(false);
                    foundCollectionId = finalCollId;
                    binding.tvResendOTP.setText("Resend Reset Email");
                    binding.btnLogin.setText("Reset Password");
                    binding.layoutNewPassword.setVisibility(View.VISIBLE);
                    binding.etLoginOTP.setVisibility(View.GONE);
                    Toast.makeText(this,
                            "Reset email sent to " + foundEmail
                                    + ".\nClick the link in your inbox, "
                                    + "then set a new password below.\n"
                                    + "(Also check spam folder)",
                            Toast.LENGTH_LONG).show();
                });

            } catch (Exception e) {
                Log.e(TAG, "findUserAndSendReset error", e);
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(this,
                            "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    // ── Step 2: update hashed password in Appwrite DB document ───────────────
    private void resetPassword() {
        String newPass  = binding.etNewPassword.getText().toString();
        String confPass = binding.etConfirmPassword.getText().toString();

        if (newPass.isEmpty())          { binding.etNewPassword.setError("Required");      return; }
        if (confPass.isEmpty())         { binding.etConfirmPassword.setError("Required");   return; }
        if (!newPass.equals(confPass))  {
            binding.etConfirmPassword.setError("Passwords do not match"); return;
        }
        if (!isPasswordStrong(newPass)) {
            binding.etNewPassword.setError(
                    "Min 8 chars: uppercase, lowercase, digit & special char"); return;
        }

        Log.d(TAG, "resetPassword — docId=" + foundDocumentId
                + "  collection=" + foundCollectionId);
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
                        foundCollectionId,
                        foundDocumentId,
                        update
                );

                Log.d(TAG, "Password updated successfully in DB");

                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(this,
                            "Password updated! Please log in.", Toast.LENGTH_LONG).show();
                    finish();
                });

            } catch (AppwriteException e) {
                Log.e(TAG, "resetPassword AppwriteException: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(this,
                            "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                Log.e(TAG, "resetPassword error", e);
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(this,
                            "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
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