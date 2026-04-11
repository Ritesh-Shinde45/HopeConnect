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
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.appwrite.models.Document;
import io.appwrite.services.Databases;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG            = "LoginActivity";
    private static final String PREFS          = "hoppe_prefs";
    private static final String KEY_UID        = "logged_in_user_id";
    private static final String KEY_NAME       = "logged_in_name";
    private static final String KEY_ROLE       = "logged_in_role";
    private static final String ADMIN_USERNAME = "Admin";

   
   
   
   
    private static final List<String> ADMIN_MOBILES = Arrays.asList(
            "8080769308",
            "9096548683"
    );

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
                            if (e.getStatusCode() == 10) {
                                Toast.makeText(this,
                                        "Google Sign-In config error (code 10). "
                                                + "Add SHA-1 fingerprint to Google Cloud Console.",
                                        Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(this,
                                        "Google Sign-In failed (" + e.getStatusCode() + ")",
                                        Toast.LENGTH_SHORT).show();
                            }
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
            routeLoggedInUser(prefs.getString(KEY_ROLE, "user"));
            return;
        }

        setupGoogleSignIn();
        setupClickListeners();
    }

   
    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions
                .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
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

       
       
       
       
        if (identifier.equalsIgnoreCase(ADMIN_USERNAME)
                || ADMIN_MOBILES.contains(identifier)) {
            loginAsAdmin(identifier, password);
        } else {
            loginAsUser(identifier, password);
        }
    }

   
   
   
   
   
   
    private void loginAsAdmin(String identifier, String password) {
        new Thread(() -> {
            try {
                Databases db = AppwriteService.getDatabases();

               
                String field;
                if (identifier.equalsIgnoreCase(ADMIN_USERNAME)) {
                    field = "username";
                } else if (ADMIN_MOBILES.contains(identifier)) {
                    field = "mobile";
                } else if (Patterns.EMAIL_ADDRESS.matcher(identifier).matches()) {
                    field = "email";
                } else {
                    field = "username";
                }

                List<? extends Document<?>> docs =
                        AppwriteHelper.findUserByField(
                                db, AppwriteService.COL_ADMINS, field, identifier
                        ).getDocuments();

               
                if (docs.isEmpty() && field.equals("username")) {
                    docs = AppwriteHelper.findUserByField(
                            db, AppwriteService.COL_ADMINS, "email", identifier
                    ).getDocuments();
                }

                if (docs.isEmpty()) {
                    runOnUiThread(() -> {
                        setLoading(false);
                        binding.etLoginUsername.setError(
                                "Admin account not found. Register first.");
                    });
                    return;
                }

                Document<?> adminDoc = docs.get(0);
                @SuppressWarnings("unchecked")
                Map<String, Object> adminData = (Map<String, Object>) adminDoc.getData();

                String storedHash = (String) adminData.get("password");
                if (!verifyPassword(password, storedHash)) {
                    runOnUiThread(() -> {
                        setLoading(false);
                        binding.etLoginPassword.setError("Incorrect password");
                    });
                    return;
                }

                String uid  = adminDoc.getId();
                String name = adminData.get("name") != null
                        ? adminData.get("name").toString() : "Admin";

                saveSession(uid, name, "admin");

                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(this, "Welcome back, " + name + "!", Toast.LENGTH_SHORT).show();
                    routeLoggedInUser("admin");
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Log.e(TAG, "Admin login error", e);
                    Toast.makeText(this, "Login error: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

   
   
   
   
    private void loginAsUser(String identifier, String password) {
        new Thread(() -> {
            try {
                Databases db = AppwriteService.getDatabases();

                String field = Patterns.EMAIL_ADDRESS.matcher(identifier).matches()
                        ? "email" : "username";

                List<? extends Document<?>> docs =
                        AppwriteHelper.findUserByField(db, field, identifier).getDocuments();

                if (docs.isEmpty()) {
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
                    runOnUiThread(() -> {
                        setLoading(false);
                        binding.etLoginPassword.setError("Incorrect password");
                    });
                    return;
                }

                String uid  = userDoc.getId();
                String name = userData.get("name") != null
                        ? userData.get("name").toString() : "User";

               
                Object roleObj = userData.get("role");
                String role = roleObj != null ? roleObj.toString() : "user";

                saveSession(uid, name, role);

                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(this, "Welcome back, " + name + "!", Toast.LENGTH_SHORT).show();
                    routeLoggedInUser(role);
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

   
   
   
   
    private void handleGoogleLogin(GoogleSignInAccount account) {
        if (account.getEmail() == null) return;
        setLoading(true);
        String email = account.getEmail();

        new Thread(() -> {
            try {
                Databases db = AppwriteService.getDatabases();
                List<? extends Document<?>> docs =
                        AppwriteHelper.findUserByField(db, "email", email).getDocuments();

                if (docs.isEmpty()) {
                    runOnUiThread(() -> {
                        setLoading(false);
                        googleSignInClient.signOut();
                        Intent i = new Intent(this, RegisterActivity.class);
                        i.putExtra("google_email", email);
                        i.putExtra("google_name",  account.getDisplayName());
                        startActivity(i);
                    });
                    return;
                }

                Document<?> userDoc = docs.get(0);
                @SuppressWarnings("unchecked")
                Map<String, Object> userData = (Map<String, Object>) userDoc.getData();
                String uid  = userDoc.getId();
                String name = userData.get("name") != null
                        ? userData.get("name").toString() : "User";

                saveSession(uid, name, "user");

                runOnUiThread(() -> {
                    setLoading(false);
                    googleSignInClient.signOut();
                    Toast.makeText(this, "Welcome, " + name + "!", Toast.LENGTH_SHORT).show();
                    routeLoggedInUser("user");
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

   
   
   

    private void saveSession(String uid, String name, String role) {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString(KEY_UID,  uid)
                .putString(KEY_NAME, name)
                .putString(KEY_ROLE, role)
                .apply();
    }

    private void routeLoggedInUser(String role) {
        Intent i = "admin".equals(role)
                ? new Intent(this, AdminDashboardActivity.class)
                : new Intent(this, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
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
        } catch (Exception e) { return false; }
    }

    private void setLoading(boolean loading) {
        runOnUiThread(() -> {
            binding.btnLogin.setEnabled(!loading);
            binding.btnLogin.setText(loading ? "Logging in..." : "LOGIN");
        });
    }
}