package com.ritesh.hoppeconnect;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.ritesh.hoppeconnect.databinding.RegisterPageBinding;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import io.appwrite.models.Document;
import io.appwrite.services.Databases;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG   = "RegisterActivity";
    private static final String PREFS = "hoppe_prefs";
    private static final String KEY_UID  = "logged_in_user_id";
    private static final String KEY_NAME = "logged_in_name";
    private static final String KEY_ROLE = "logged_in_role";

    private RegisterPageBinding binding;
    private GoogleSignInClient  googleSignInClient;

    // ── Google Sign-In launcher ───────────────────────────────────────────────
    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        try {
                            GoogleSignInAccount account =
                                    GoogleSignIn.getSignedInAccountFromIntent(result.getData())
                                            .getResult(ApiException.class);
                            handleGoogleResult(account);
                        } catch (ApiException e) {
                            Log.e(TAG, "Google sign-in failed: " + e.getStatusCode(), e);
                            showGoogleError(e.getStatusCode());
                        }
                    });

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = RegisterPageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AppwriteService.init(this);

        setupGoogleSignIn();
        setupPasswordStrengthWatcher();
        setupClickListeners();
        prefillFromIntent();
    }

    // ── Pre-fill from intent (Google redirect from LoginActivity) ────────────
    private void prefillFromIntent() {
        String googleEmail = getIntent().getStringExtra("google_email");
        String googleName  = getIntent().getStringExtra("google_name");
        if (googleEmail != null) binding.etEmail.setText(googleEmail);
        if (googleName  != null) {
            binding.etName.setText(googleName);
            binding.etUsername.setText(
                    googleName.toLowerCase(Locale.ROOT)
                            .replace(" ", "_")
                            .replaceAll("[^a-z0-9_]", ""));
        }
    }

    // ── Google Sign-In ────────────────────────────────────────────────────────
    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions
                .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .requestProfile()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    /**
     * Google result on Register screen:
     * - If email already in DB → log in directly (no re-registration needed)
     * - If new email → pre-fill fields, let user complete form
     */
    private void handleGoogleResult(GoogleSignInAccount account) {
        if (account.getEmail() == null) return;
        String email       = account.getEmail();
        String displayName = account.getDisplayName() != null ? account.getDisplayName() : "";
        boolean isAdmin    = AppwriteService.isAdminEmail(email);

        setLoading(true);

        new Thread(() -> {
            try {
                Databases db = AppwriteService.getDatabases();
                String col   = isAdmin ? AppwriteService.COL_ADMINS : AppwriteService.COL_USERS;

                List<? extends Document<?>> docs =
                        AppwriteHelper.findUserByField(db, col, "email", email).getDocuments();

                if (!docs.isEmpty()) {
                    // ── Already registered → log in directly ─────────────────
                    Document<?> doc = docs.get(0);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) doc.getData();
                    String uid  = doc.getId();
                    String name = data.get("name") != null
                            ? data.get("name").toString() : displayName;
                    String role = isAdmin ? "admin" : "user";

                    saveSession(uid, name, role);
                    googleSignInClient.signOut();

                    runOnUiThread(() -> {
                        setLoading(false);
                        Toast.makeText(this,
                                "Welcome back, " + name + "!", Toast.LENGTH_SHORT).show();
                        navigateTo(role);   // clears entire back stack
                    });
                } else {
                    // ── New account → pre-fill fields ─────────────────────────
                    runOnUiThread(() -> {
                        setLoading(false);
                        googleSignInClient.signOut();
                        binding.etEmail.setText(email);
                        binding.etName.setText(displayName);
                        binding.etUsername.setText(
                                displayName.toLowerCase(Locale.ROOT)
                                        .replace(" ", "_")
                                        .replaceAll("[^a-z0-9_]", ""));
                        Toast.makeText(this,
                                "Details filled from Google. "
                                        + "Set a password and address to complete.",
                                Toast.LENGTH_LONG).show();
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Log.e(TAG, "Google handleResult error", e);
                    Toast.makeText(this,
                            "Google sign-in error: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void showGoogleError(int code) {
        String msg = code == 10
                ? "Google Sign-In error (code 10). Add SHA-1 + Web Client ID to Google Cloud Console."
                : "Google Sign-In failed (code " + code + ")";
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    // ── Password strength ─────────────────────────────────────────────────────
    private void setupPasswordStrengthWatcher() {
        binding.etPass.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                String pwd = s.toString();
                if (pwd.isEmpty()) { binding.tvPasswordStrength.setVisibility(View.GONE); return; }
                binding.tvPasswordStrength.setVisibility(View.VISIBLE);
                switch (getPasswordScore(pwd)) {
                    case 0: case 1:
                        binding.tvPasswordStrength.setText("Weak");
                        binding.tvPasswordStrength.setTextColor(Color.RED); break;
                    case 2: case 3:
                        binding.tvPasswordStrength.setText("Moderate");
                        binding.tvPasswordStrength.setTextColor(Color.parseColor("#FFA500")); break;
                    case 4:
                        binding.tvPasswordStrength.setText("Strong");
                        binding.tvPasswordStrength.setTextColor(Color.parseColor("#4CAF50")); break;
                    default:
                        binding.tvPasswordStrength.setText("Very Strong");
                        binding.tvPasswordStrength.setTextColor(Color.parseColor("#1B5E20")); break;
                }
            }
        });
    }

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

    // ── Click listeners ───────────────────────────────────────────────────────
    private void setupClickListeners() {
        binding.btnGoogleAuth.setOnClickListener(v ->
                googleSignInClient.signOut().addOnCompleteListener(t ->
                        googleSignInLauncher.launch(googleSignInClient.getSignInIntent())));

        binding.btnSignUp.setOnClickListener(v -> attemptRegistration());

        // FIX: always finish() this activity when going to Login
        // so it is fully removed from the back stack
        binding.tvSignIn.setOnClickListener(v -> {
            Intent i = new Intent(this, LoginActivity.class);
            i.putExtra("explicit_login", true);
            // Clear the task so LoginActivity is the only screen
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
        });
    }

    // ── Registration ──────────────────────────────────────────────────────────
    private void attemptRegistration() {
        String name     = binding.etName.getText().toString().trim();
        String username = binding.etUsername.getText().toString().trim();
        String email    = binding.etEmail.getText().toString().trim();
        String password = binding.etPass.getText().toString();
        String address  = binding.etAddress.getText().toString().trim();

        if (name.isEmpty())    { binding.etName.setError("Required");     return; }
        if (username.isEmpty()){ binding.etUsername.setError("Required"); return; }
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.setError("Valid email required"); return;
        }
        if (!isPasswordStrong(password)) {
            binding.etPass.setError(
                    "Min 8 chars, uppercase, lowercase, digit & special char"); return;
        }
        if (address.isEmpty()) { binding.etAddress.setError("Required"); return; }

        boolean isAdmin = AppwriteService.isAdminEmail(email);
        setLoading(true);

        new Thread(() -> {
            try {
                Databases db    = AppwriteService.getDatabases();
                String checkCol = isAdmin
                        ? AppwriteService.COL_ADMINS
                        : AppwriteService.COL_USERS;

                // ── Duplicate email check ─────────────────────────────────────
                if (!AppwriteHelper.findUserByField(db, checkCol, "email", email)
                        .getDocuments().isEmpty()) {
                    runOnUiThread(() -> {
                        setLoading(false);
                        binding.etEmail.setError("Email already registered");
                    });
                    return;
                }

                // ── Duplicate username check (non-admin only) ─────────────────
                if (!isAdmin && !AppwriteHelper
                        .findUserByField(db, "username", username)
                        .getDocuments().isEmpty()) {
                    runOnUiThread(() -> {
                        setLoading(false);
                        binding.etUsername.setError("Username already taken");
                    });
                    return;
                }

                // 1) Appwrite auth account + session (+ verification email for non-admin)
                AppwriteService.createAccountAndSignIn(email, password, name);

                // 2) Hash password for DB
                String hashed = hashPassword(password);
                String userId = UUID.randomUUID().toString()
                        .replace("-", "").substring(0, 20);

                // 3) Build document
                Map<String, Object> data = new HashMap<>();
                data.put("name",     name);
                data.put("username", username);
                data.put("email",    email);
                data.put("password", hashed);
                data.put("address",  address);
                data.put("role",     isAdmin ? "admin" : "user");

                // 4) Save to correct collection
                AppwriteHelper.createDocument(
                        db, AppwriteService.DB_ID, checkCol, userId, data);

                // 5) For admin: save session and go directly to dashboard
                //    For users: go to login to verify email first
                if (isAdmin) {
                    saveSession(userId, name, "admin");
                    runOnUiThread(() -> {
                        setLoading(false);
                        Toast.makeText(this,
                                "Admin account created! Welcome, " + name + "!",
                                Toast.LENGTH_SHORT).show();
                        navigateTo("admin");
                    });
                } else {
                    runOnUiThread(() -> {
                        setLoading(false);
                        Toast.makeText(this,
                                "Account created! Check your email (" + email
                                        + ") for a verification link, then log in.",
                                Toast.LENGTH_LONG).show();
                        // FIX: clear entire task so Register is gone from back stack
                        Intent i = new Intent(this, LoginActivity.class);
                        i.putExtra("explicit_login", true);
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(i);
                        finish();
                    });
                }

            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Log.e(TAG, "Registration failed", e);
                    Toast.makeText(this,
                            "Failed: " + (e.getMessage() != null ? e.getMessage() : "Unknown"),
                            Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Navigate to the correct screen and CLEAR the entire back stack.
     * This is the fix for the redirect loop — no previous activity remains.
     */
    private void navigateTo(String role) {
        Intent i = "admin".equals(role)
                ? new Intent(this, AdminDashboardActivity.class)
                : new Intent(this, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    private void saveSession(String uid, String name, String role) {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString(KEY_UID,  uid)
                .putString(KEY_NAME, name)
                .putString(KEY_ROLE, role)
                .apply();
    }

    private String hashPassword(String plainText) throws Exception {
        byte[] saltBytes = new byte[16];
        new SecureRandom().nextBytes(saltBytes);
        String salt = Base64.encodeToString(saltBytes, Base64.NO_WRAP);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(salt.getBytes());
        byte[] hashBytes = digest.digest(plainText.getBytes());
        StringBuilder hex = new StringBuilder();
        for (byte b : hashBytes)
            hex.append(String.format(Locale.ROOT, "%02x", b));
        return salt + "$" + hex;
    }

    private void setLoading(boolean loading) {
        runOnUiThread(() -> {
            binding.btnSignUp.setEnabled(!loading);
            binding.btnSignUp.setText(loading ? "Registering..." : "SIGNUP");
            binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        });
    }
}