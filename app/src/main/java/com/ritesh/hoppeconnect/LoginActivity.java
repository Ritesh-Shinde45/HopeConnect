package com.ritesh.hoppeconnect;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.ritesh.hoppeconnect.databinding.LoginPageBinding;

import java.security.MessageDigest;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.appwrite.models.Document;
import io.appwrite.services.Databases;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG   = "LoginActivity";
    private static final String PREFS = "hoppe_prefs";
    private static final String KEY_UID  = "logged_in_user_id";
    private static final String KEY_NAME = "logged_in_name";
    private static final String KEY_ROLE = "logged_in_role";

    private LoginPageBinding   binding;
    private GoogleSignInClient googleSignInClient;

   
    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        try {
                            GoogleSignInAccount account =
                                    GoogleSignIn.getSignedInAccountFromIntent(result.getData())
                                            .getResult(ApiException.class);
                            handleGoogleLogin(account);
                        } catch (ApiException e) {
                            Log.e(TAG, "Google login failed code=" + e.getStatusCode(), e);
                            String msg = e.getStatusCode() == 10
                                    ? "Google Sign-In error (code 10). Add SHA-1 + Web Client ID."
                                    : "Google Sign-In failed (" + e.getStatusCode() + ")";
                            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                        }
                    });

   
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = LoginPageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AppwriteService.init(this);

       
        boolean explicitOpen = getIntent().getBooleanExtra("explicit_login", false);
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        if (!explicitOpen && prefs.contains(KEY_UID)) {
            Log.d(TAG, "Session found in prefs — auto-navigating");
            navigateTo(prefs.getString(KEY_ROLE, "user"));
            return;
        }

        setupGoogleSignIn();
        setupClickListeners();
    }

   
    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions
                .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .requestProfile()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void setupClickListeners() {
        binding.btnLogin.setOnClickListener(v -> attemptLogin());

        binding.btnGoogleAuth.setOnClickListener(v ->
                googleSignInClient.signOut().addOnCompleteListener(t ->
                        googleSignInLauncher.launch(googleSignInClient.getSignInIntent())));

        binding.tvForgotPassword.setOnClickListener(v ->
                startActivity(new Intent(this, ForgotPasswordActivity.class)));

        binding.tvSignUp.setOnClickListener(v -> {
            Intent i = new Intent(this, RegisterActivity.class);
            i.putExtra("explicit_login", true);
            startActivity(i);
        });
    }

   
    private void attemptLogin() {
        String identifier = binding.etLoginUsername.getText().toString().trim();
        String password   = binding.etLoginPassword.getText().toString();

        if (identifier.isEmpty()) { binding.etLoginUsername.setError("Required"); return; }
        if (password.isEmpty())   { binding.etLoginPassword.setError("Required"); return; }

        setLoading(true);
        Log.d(TAG, "attemptLogin — identifier=" + identifier);

        new Thread(() -> {
            try {
                Databases db    = AppwriteService.getDatabases();
                boolean isAdmin = AppwriteService.isAdminEmail(identifier);
                boolean isEmail = Patterns.EMAIL_ADDRESS.matcher(identifier).matches();

                String collectionId;
                String field;

                if (isAdmin) {
                    collectionId = AppwriteService.COL_ADMINS;
                    field        = "email";
                } else if (isEmail) {
                    collectionId = AppwriteService.COL_USERS;
                    field        = "email";
                } else {
                    collectionId = AppwriteService.COL_USERS;
                    field        = "username";
                }

                Log.d(TAG, "Searching collection=" + collectionId + "  field=" + field);

                List<? extends Document<?>> docs =
                        AppwriteHelper.findUserByField(db, collectionId, field, identifier)
                                .getDocuments();

               
                if (docs.isEmpty() && !isEmail && !isAdmin) {
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

                Document<?> userDoc = docs.get(0);
                @SuppressWarnings("unchecked")
                Map<String, Object> userData = (Map<String, Object>) userDoc.getData();

                String storedHash = (String) userData.get("password");
                if (!verifyPassword(password, storedHash)) {
                    Log.w(TAG, "Password mismatch for identifier=" + identifier);
                    runOnUiThread(() -> {
                        setLoading(false);
                        binding.etLoginPassword.setError("Incorrect password");
                    });
                    return;
                }

                String emailInDoc = userData.get("email") != null
                        ? userData.get("email").toString() : identifier;
                String role = AppwriteService.isAdminEmail(emailInDoc) ? "admin" : "user";
                Log.d(TAG, "Password OK — email=" + emailInDoc + "  role=" + role);

               
                if (!"admin".equals(role)) {
                    try {
                        Log.d(TAG, "Creating session to check email verification");
                        AppwriteService.createSessionSync(emailInDoc, password);

                        boolean verified = AppwriteService.isEmailVerified();
                        Log.d(TAG, "Email verified=" + verified);

                        if (!verified) {
                           
                           
                            Log.d(TAG, "Email not verified — resending verification email");
                            new Thread(AppwriteService::resendVerificationEmail).start();

                            runOnUiThread(() -> {
                                setLoading(false);
                                Toast.makeText(this,
                                        "Email not verified.\n"
                                                + "A new verification link has been sent to:\n"
                                                + emailInDoc + "\n"
                                                + "Check your inbox and spam folder.",
                                        Toast.LENGTH_LONG).show();
                            });
                            return;
                        }
                    } catch (Exception sessionEx) {
                       
                       
                       
                        Log.w(TAG, "Session create for verification check failed: "
                                + sessionEx.getMessage());
                    }
                }

                String uid  = userDoc.getId();
                String name = userData.get("name") != null
                        ? userData.get("name").toString() : "User";

                saveSession(uid, name, role);
                Log.d(TAG, "Login success — uid=" + uid + "  name=" + name + "  role=" + role);

                final String finalRole = role;
                final String finalName = name;
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(this,
                            "Welcome back, " + finalName + "!", Toast.LENGTH_SHORT).show();
                    navigateTo(finalRole);
                });

            } catch (Exception e) {
                Log.e(TAG, "Login error", e);
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(this,
                            "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

   
    private void handleGoogleLogin(GoogleSignInAccount account) {
        if (account.getEmail() == null) return;
        setLoading(true);

        String email       = account.getEmail();
        String displayName = account.getDisplayName() != null
                ? account.getDisplayName() : "User";
        boolean isAdmin    = AppwriteService.isAdminEmail(email);

        Log.d(TAG, "handleGoogleLogin — email=" + email + "  isAdmin=" + isAdmin);
        googleSignInClient.signOut();

        if (isAdmin) {
            new Thread(() -> {
                boolean alreadyRegistered = false;
                String savedUid  = null;
                String savedName = displayName;

                try {
                    List<? extends Document<?>> adminDocs =
                            AppwriteHelper.listAllDocuments(
                                    AppwriteService.getDatabases(),
                                    AppwriteService.DB_ID,
                                    AppwriteService.COL_ADMINS
                            ).getDocuments();

                    if (!adminDocs.isEmpty()) {
                        alreadyRegistered = true;
                        Document<?> doc = adminDocs.get(0);
                        @SuppressWarnings("unchecked")
                        Map<String, Object> data = (Map<String, Object>) doc.getData();
                        savedUid  = doc.getId();
                        savedName = data.get("name") != null
                                ? data.get("name").toString() : displayName;
                    }
                } catch (Exception dbEx) {
                    Log.w(TAG, "COL_ADMINS list failed, falling back to prefs: "
                            + dbEx.getMessage());
                    SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
                    if (prefs.contains(KEY_UID)
                            && "admin".equals(prefs.getString(KEY_ROLE, ""))) {
                        alreadyRegistered = true;
                        savedUid  = prefs.getString(KEY_UID, null);
                        savedName = prefs.getString(KEY_NAME, displayName);
                    }
                }

                final boolean registered = alreadyRegistered;
                final String  finalUid   = savedUid;
                final String  finalName  = savedName;

                runOnUiThread(() -> {
                    setLoading(false);
                    if (registered && finalUid != null) {
                        saveSession(finalUid, finalName, "admin");
                        Toast.makeText(this,
                                "Welcome back, " + finalName + "!", Toast.LENGTH_SHORT).show();
                        navigateTo("admin");
                    } else {
                        Intent i = new Intent(this, RegisterActivity.class);
                        i.putExtra("google_email", email);
                        i.putExtra("google_name",  displayName);
                        startActivity(i);
                    }
                });
            }).start();

        } else {
            new Thread(() -> {
                try {
                    Databases db = AppwriteService.getDatabases();
                    List<? extends Document<?>> docs =
                            AppwriteHelper.findUserByField(
                                    db, AppwriteService.COL_USERS, "email", email
                            ).getDocuments();

                    if (docs.isEmpty()) {
                        Log.d(TAG, "Google user not found — redirecting to RegisterActivity");
                        runOnUiThread(() -> {
                            setLoading(false);
                            Intent i = new Intent(this, RegisterActivity.class);
                            i.putExtra("google_email", email);
                            i.putExtra("google_name",  displayName);
                            startActivity(i);
                        });
                        return;
                    }

                    Document<?> doc = docs.get(0);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) doc.getData();
                    String uid  = doc.getId();
                    String name = data.get("name") != null
                            ? data.get("name").toString() : displayName;

                    saveSession(uid, name, "user");
                    Log.d(TAG, "Google login success — uid=" + uid + "  name=" + name);

                    runOnUiThread(() -> {
                        setLoading(false);
                        Toast.makeText(this, "Welcome, " + name + "!", Toast.LENGTH_SHORT).show();
                        navigateTo("user");
                    });

                } catch (Exception e) {
                    Log.e(TAG, "Google user login error", e);
                    runOnUiThread(() -> {
                        setLoading(false);
                        Toast.makeText(this,
                                "Google login error: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });
                }
            }).start();
        }
    }

   
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

    private boolean verifyPassword(String plain, String storedHash) {
        if (storedHash == null || !storedHash.contains("$")) return false;
        try {
            String[] p  = storedHash.split("\\$", 2);
            String salt = p[0];
            String hash = p[1];
            MessageDigest d = MessageDigest.getInstance("SHA-256");
            d.update(salt.getBytes());
            byte[] hb = d.digest(plain.getBytes());
            StringBuilder hex = new StringBuilder();
            for (byte b : hb) hex.append(String.format(Locale.ROOT, "%02x", b));
            return hex.toString().equals(hash);
        } catch (Exception e) {
            Log.e(TAG, "verifyPassword error", e);
            return false;
        }
    }

    private void setLoading(boolean loading) {
        runOnUiThread(() -> {
            binding.btnLogin.setEnabled(!loading);
            binding.btnLogin.setText(loading ? "Logging in..." : "LOGIN");
        });
    }
}