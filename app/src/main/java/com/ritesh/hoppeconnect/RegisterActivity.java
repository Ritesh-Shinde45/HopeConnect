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

import io.appwrite.exceptions.AppwriteException;
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

    // ── Google auth gate ──────────────────────────────────────────────────────
    // Set to true only after a successful Google Sign-In in this session.
    // The Sign Up button is disabled until this flag is true.
    private boolean googleAuthCompleted = false;

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
        applyGoogleGate();  // lock UI until Google auth done
    }

    // ── Lock/unlock the form based on googleAuthCompleted ────────────────────
    /**
     * When Google auth is NOT yet completed:
     *   • Sign Up button is disabled and shows a hint
     *   • Email field is read-only (will be filled by Google)
     *   • All other fields are editable so users can browse, but submit is gated
     *
     * When Google auth IS completed:
     *   • Email is locked (cannot be changed after Google sets it)
     *   • Sign Up button becomes active
     */
    private void applyGoogleGate() {
        if (!googleAuthCompleted) {
            // Disable Sign Up button with explanatory hint
            binding.btnSignUp.setEnabled(false);
            binding.btnSignUp.setAlpha(0.5f);
            binding.btnSignUp.setText("Sign in with Google first");

            // Email is set by Google — make it non-editable and show hint
            binding.etEmail.setFocusable(false);
            binding.etEmail.setFocusableInTouchMode(false);
            binding.etEmail.setClickable(false);
            binding.etEmail.setHint("Filled automatically after Google sign-in");
            binding.etEmail.setAlpha(0.6f);

        } else {
            // Google done — enable form submission
            binding.btnSignUp.setEnabled(true);
            binding.btnSignUp.setAlpha(1.0f);
            binding.btnSignUp.setText("SIGNUP");

            // Lock the email so the user cannot change what Google provided
            binding.etEmail.setFocusable(false);
            binding.etEmail.setFocusableInTouchMode(false);
            binding.etEmail.setClickable(false);
            binding.etEmail.setAlpha(0.85f);
        }
    }

    // ── Pre-fill from Google intent (called when navigated from LoginActivity) ─
    private void prefillFromIntent() {
        String googleEmail = getIntent().getStringExtra("google_email");
        String googleName  = getIntent().getStringExtra("google_name");
        if (googleEmail != null) {
            binding.etEmail.setText(googleEmail);
            binding.etName.setText(googleName != null ? googleName : "");
            if (googleName != null) {
                binding.etUsername.setText(
                        googleName.toLowerCase(Locale.ROOT)
                                .replace(" ", "_")
                                .replaceAll("[^a-z0-9_]", ""));
            }
            // Treat a pre-filled Google intent as having already authenticated
            googleAuthCompleted = true;
            applyGoogleGate();
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

    private void handleGoogleResult(GoogleSignInAccount account) {
        if (account.getEmail() == null) return;
        String email       = account.getEmail();
        String displayName = account.getDisplayName() != null ? account.getDisplayName() : "";
        boolean isAdmin    = AppwriteService.isAdminEmail(email);

        Log.d(TAG, "handleGoogleResult — email=" + email + "  isAdmin=" + isAdmin);
        setLoading(true);

        new Thread(() -> {
            try {
                Databases db = AppwriteService.getDatabases();
                String col   = isAdmin ? AppwriteService.COL_ADMINS : AppwriteService.COL_USERS;

                List<? extends Document<?>> docs =
                        AppwriteHelper.findUserByField(db, col, "email", email).getDocuments();

                if (!docs.isEmpty()) {
                    // Already registered — restore session and navigate directly
                    Document<?> doc = docs.get(0);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) doc.getData();
                    String uid  = doc.getId();
                    String name = data.get("name") != null
                            ? data.get("name").toString() : displayName;
                    String role = isAdmin ? "admin" : "user";

                    Log.d(TAG, "Google user already registered — uid=" + uid + "  role=" + role);
                    saveSession(uid, name, role);
                    googleSignInClient.signOut();

                    runOnUiThread(() -> {
                        setLoading(false);
                        Toast.makeText(this,
                                "Welcome back, " + name + "!", Toast.LENGTH_SHORT).show();
                        navigateTo(role);
                    });

                } else {
                    // New user — pre-fill form fields and unlock the Sign Up button
                    Log.d(TAG, "Google user not found in DB — pre-filling registration form");
                    googleSignInClient.signOut();

                    runOnUiThread(() -> {
                        setLoading(false);

                        binding.etEmail.setText(email);
                        binding.etName.setText(displayName);
                        binding.etUsername.setText(
                                displayName.toLowerCase(Locale.ROOT)
                                        .replace(" ", "_")
                                        .replaceAll("[^a-z0-9_]", ""));

                        // ── Unlock the form ───────────────────────────────────
                        googleAuthCompleted = true;
                        applyGoogleGate();

                        Toast.makeText(this,
                                "Google verified ✓  Set a password & address to complete sign-up.",
                                Toast.LENGTH_LONG).show();
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Google handleResult error", e);
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(this,
                            "Google sign-in error: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void showGoogleError(int code) {
        String msg = code == 10
                ? "Google Sign-In error (code 10). Add SHA-1 + Web Client ID."
                : "Google Sign-In failed (code " + code + ")";
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    // ── Password strength indicator ───────────────────────────────────────────
    private void setupPasswordStrengthWatcher() {
        binding.etPass.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                String pwd = s.toString();
                if (pwd.isEmpty()) {
                    binding.tvPasswordStrength.setVisibility(View.GONE);
                    return;
                }
                binding.tvPasswordStrength.setVisibility(View.VISIBLE);
                switch (getPasswordScore(pwd)) {
                    case 0: case 1:
                        binding.tvPasswordStrength.setText("Weak");
                        binding.tvPasswordStrength.setTextColor(Color.RED);
                        break;
                    case 2: case 3:
                        binding.tvPasswordStrength.setText("Moderate");
                        binding.tvPasswordStrength.setTextColor(Color.parseColor("#FFA500"));
                        break;
                    case 4:
                        binding.tvPasswordStrength.setText("Strong");
                        binding.tvPasswordStrength.setTextColor(Color.parseColor("#4CAF50"));
                        break;
                    default:
                        binding.tvPasswordStrength.setText("Very Strong");
                        binding.tvPasswordStrength.setTextColor(Color.parseColor("#1B5E20"));
                        break;
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

        binding.btnSignUp.setOnClickListener(v -> {
            // Double-check gate in case somehow the button is tapped while disabled
            if (!googleAuthCompleted) {
                Toast.makeText(this,
                        "Please sign in with Google first to verify your identity.",
                        Toast.LENGTH_LONG).show();
                return;
            }
            attemptRegistration();
        });

        binding.tvSignIn.setOnClickListener(v -> {
            Intent i = new Intent(this, LoginActivity.class);
            i.putExtra("explicit_login", true);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
        });
    }

    // ── Registration ──────────────────────────────────────────────────────────
    private void attemptRegistration() {
        String name     = binding.etName.getText().toString().trim();
        String username = binding.etUsername.getText().toString().trim();
        String mobile   = binding.etMobile.getText().toString().trim();
        String email    = binding.etEmail.getText().toString().trim();
        String password = binding.etPass.getText().toString();
        String address  = binding.etAddress.getText().toString().trim();

        // ── Validation ────────────────────────────────────────────────────────
        if (name.isEmpty())     { binding.etName.setError("Required");     return; }
        if (username.isEmpty()) { binding.etUsername.setError("Required"); return; }
        // Mobile is optional but must be valid if provided
        if (!mobile.isEmpty() && !mobile.matches("\\d{10}")) {
            binding.etMobile.setError("Enter a valid 10-digit number"); return;
        }
        // Email is set by Google — just sanity-check it is present
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this,
                    "Email is missing. Please sign in with Google again.",
                    Toast.LENGTH_LONG).show();
            googleAuthCompleted = false;
            applyGoogleGate();
            return;
        }
        if (!isPasswordStrong(password)) {
            binding.etPass.setError(
                    "Min 8 chars, uppercase, lowercase, digit & special char"); return;
        }
        if (address.isEmpty()) { binding.etAddress.setError("Required"); return; }

        boolean isAdmin      = AppwriteService.isAdminEmail(email);
        final String fMobile = mobile;

        Log.d(TAG, "attemptRegistration — email=" + email + "  isAdmin=" + isAdmin);
        setLoading(true);

        new Thread(() -> {
            try {
                Databases db    = AppwriteService.getDatabases();
                String checkCol = isAdmin
                        ? AppwriteService.COL_ADMINS
                        : AppwriteService.COL_USERS;

                // ── Already registered? ───────────────────────────────────────
                List<? extends Document<?>> existingDocs =
                        AppwriteHelper.findUserByField(db, checkCol, "email", email)
                                .getDocuments();

                if (!existingDocs.isEmpty()) {
                    Log.d(TAG, "Email already registered — restoring session");
                    Document<?> doc = existingDocs.get(0);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> existingData =
                            (Map<String, Object>) doc.getData();
                    String uid  = doc.getId();
                    String nm   = existingData.get("name") != null
                            ? existingData.get("name").toString() : name;
                    String role = isAdmin ? "admin" : "user";

                    saveSession(uid, nm, role);
                    final String finalRole = role;
                    final String finalName = nm;
                    runOnUiThread(() -> {
                        setLoading(false);
                        Toast.makeText(this,
                                "Already registered! Logging you in as " + finalName,
                                Toast.LENGTH_SHORT).show();
                        navigateTo(finalRole);
                    });
                    return;
                }

                // ── Duplicate username check (non-admin only) ─────────────────
                if (!isAdmin && !AppwriteHelper
                        .findUserByField(db, AppwriteService.COL_USERS, "username", username)
                        .getDocuments().isEmpty()) {
                    Log.w(TAG, "Username already taken: " + username);
                    runOnUiThread(() -> {
                        setLoading(false);
                        binding.etUsername.setError("Username already taken");
                    });
                    return;
                }

                // ── Create Appwrite auth account + session + verification email ─
                boolean authAlreadyExists = false;
                try {
                    Log.d(TAG, "Calling createAccountAndSignIn for email=" + email);
                    AppwriteService.createAccountAndSignIn(email, password, name);
                    Log.d(TAG, "createAccountAndSignIn completed");
                } catch (AppwriteException ae) {
                    Log.w(TAG, "AppwriteException during account creation: code="
                            + ae.getCode() + "  msg=" + ae.getMessage());
                    if (ae.getCode() == 409
                            || (ae.getMessage() != null
                            && ae.getMessage().toLowerCase().contains("already"))) {
                        authAlreadyExists = true;
                        Log.w(TAG, "Auth account already exists — attempting session only");
                        try {
                            AppwriteService.createSessionSync(email, password);
                            Log.d(TAG, "Session created for existing auth account");
                        } catch (Exception sessionEx) {
                            Log.w(TAG, "Session create failed (non-fatal): "
                                    + sessionEx.getMessage());
                        }
                    } else {
                        throw ae;
                    }
                }

                // ── Save user document to Appwrite DB ─────────────────────────
                String hashed = hashPassword(password);
                String userId = UUID.randomUUID().toString()
                        .replace("-", "").substring(0, 20);

                Map<String, Object> data = new HashMap<>();
                data.put("name",     name);
                data.put("username", username);
                data.put("mobile",   fMobile);
                data.put("email",    email);
                data.put("password", hashed);
                data.put("address",  address);

                Log.d(TAG, "Saving DB document to collection=" + checkCol + "  userId=" + userId);
                AppwriteHelper.createDocument(
                        db, AppwriteService.DB_ID, checkCol, userId, data);
                Log.d(TAG, "DB document saved successfully");

                // ── Navigate based on role ────────────────────────────────────
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
                                "Account created! Please log in.",
                                Toast.LENGTH_LONG).show();
                        Intent i = new Intent(this, LoginActivity.class);
                        i.putExtra("explicit_login", true);
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(i);
                        finish();
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "Registration failed", e);
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(this,
                            "Failed: " + (e.getMessage() != null
                                    ? e.getMessage() : "Unknown error"),
                            Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
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
            binding.btnSignUp.setEnabled(!loading && googleAuthCompleted);
            binding.btnSignUp.setText(loading ? "Registering..." : "SIGNUP");
            binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        });
    }
}