package com.ritesh.hoppeconnect;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ritesh.hoppeconnect.databinding.ActivityChangePasswordBinding;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import io.appwrite.exceptions.AppwriteException;
import io.appwrite.models.Document;
import io.appwrite.services.Databases;

public class ChangePasswordActivity extends AppCompatActivity {

    private static final String TAG   = "ChangePassword";
    private static final String PREFS = "hoppe_prefs";

    private ActivityChangePasswordBinding binding;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChangePasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AppwriteService.init(this);

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        currentUserId = prefs.getString("logged_in_user_id", null);
        if (currentUserId == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        binding.ivBack.setOnClickListener(v -> finish());
        binding.btnUpdatePassword.setOnClickListener(v -> attemptChangePassword());
    }

    private void attemptChangePassword() {
        String curPwd  = binding.etCurrentPassword.getText().toString();
        String newPwd  = binding.etNewPassword.getText().toString();
        String confPwd = binding.etConfirmPassword.getText().toString();

        if (curPwd.isEmpty())  { binding.etCurrentPassword.setError("Required"); return; }
        if (newPwd.isEmpty())  { binding.etNewPassword.setError("Required");     return; }
        if (confPwd.isEmpty()) { binding.etConfirmPassword.setError("Required"); return; }

        if (!newPwd.equals(confPwd)) {
            binding.etConfirmPassword.setError("Passwords do not match"); return;
        }
        if (!isPasswordStrong(newPwd)) {
            binding.etNewPassword.setError(
                    "Min 8 chars: uppercase, lowercase, digit & special char"); return;
        }
        if (newPwd.equals(curPwd)) {
            binding.etNewPassword.setError("New password must differ from current"); return;
        }

        setLoading(true);

        new Thread(() -> {
            try {
                Databases db = AppwriteService.getDatabases();

               
                Document<?> doc = AppwriteHelper.getDocument(
                        db,
                        AppwriteService.DB_ID,
                        AppwriteService.COL_USERS,
                        currentUserId
                );

                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) doc.getData();
                String storedHash = (String) data.get("password");

                if (!verifyPassword(curPwd, storedHash)) {
                    runOnUiThread(() -> {
                        setLoading(false);
                        binding.etCurrentPassword.setError("Current password is incorrect");
                    });
                    return;
                }

                String newHash = hashPassword(newPwd);
                Map<String, Object> update = new HashMap<>();
                update.put("password", newHash);

                AppwriteHelper.updateDocument(
                        db,
                        AppwriteService.DB_ID,
                        AppwriteService.COL_USERS,
                        currentUserId,
                        update
                );

                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(this, "Password changed!", Toast.LENGTH_SHORT).show();
                    finish();
                });

            } catch (AppwriteException e) {
                runOnUiThread(() -> { setLoading(false);
                    Log.e(TAG, "Appwrite: " + e.getMessage(), e);
                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show(); });
            } catch (Exception e) {
                runOnUiThread(() -> { setLoading(false);
                    Log.e(TAG, "Error", e);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show(); });
            }
        }).start();
    }

    private boolean verifyPassword(String plain, String stored) {
        if (stored == null || !stored.contains("$")) return false;
        try {
            String[] p  = stored.split("\\$", 2);
            MessageDigest d = MessageDigest.getInstance("SHA-256");
            d.update(p[0].getBytes());
            byte[] hb = d.digest(plain.getBytes());
            StringBuilder hex = new StringBuilder();
            for (byte b : hb) hex.append(String.format(Locale.ROOT, "%02x", b));
            return hex.toString().equals(p[1]);
        } catch (Exception e) { return false; }
    }

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

    private int getPasswordScore(String pwd) {
        int s = 0;
        if (pwd.length() >= 8)                                          s++;
        if (pwd.chars().anyMatch(Character::isUpperCase))               s++;
        if (pwd.chars().anyMatch(Character::isLowerCase))               s++;
        if (pwd.chars().anyMatch(Character::isDigit))                   s++;
        if (pwd.matches(".*[!@#$%^&*()_+\\-=\\[\\]{}|;':\",./<>?].*")) s++;
        return s;
    }
    private boolean isPasswordStrong(String pwd) { return getPasswordScore(pwd) >= 5; }

    private void setLoading(boolean loading) {
        runOnUiThread(() -> {
            binding.btnUpdatePassword.setEnabled(!loading);
            binding.btnUpdatePassword.setText(loading ? "Updating..." : "Update Password");
        });
    }
}