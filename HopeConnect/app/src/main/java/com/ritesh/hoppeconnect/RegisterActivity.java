package com.ritesh.hoppeconnect;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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
import androidx.core.content.ContextCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.ritesh.hoppeconnect.databinding.RegisterPageBinding;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.appwrite.models.Document;
import io.appwrite.services.Databases;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG   = "RegisterActivity";
    private static final String PREFS = "hoppe_prefs";
    private static final String KEY_UID  = "logged_in_user_id";
    private static final String KEY_NAME = "logged_in_name";
    private static final String KEY_ROLE = "logged_in_role";

   
   
   
    private static final List<String> ADMIN_MOBILES = Arrays.asList(
            "8080769308",
            "9096548683"
    );

    private RegisterPageBinding binding;
    private GoogleSignInClient  googleSignInClient;

    private String  generatedOtp    = null;
    private boolean otpVerified     = false;
    private boolean isAdminRegister = false;

   
   
   
    private final ActivityResultLauncher<String> smsPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    granted -> {
                        if (granted) {
                            String mobile = binding.etMobile.getText().toString().trim();
                            if (!mobile.isEmpty()) sendOtpInternal(mobile);
                        } else {
                            binding.btnSendOtp.setEnabled(true);
                            binding.btnSendOtp.setText("Send OTP");
                            Toast.makeText(this,
                                    "SMS permission denied. Grant it in App Settings to receive OTP.",
                                    Toast.LENGTH_LONG).show();
                        }
                    });

   
    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        try {
                            GoogleSignInAccount account =
                                    GoogleSignIn.getSignedInAccountFromIntent(result.getData())
                                            .getResult(ApiException.class);
                            prefillFromGoogle(account);
                        } catch (ApiException e) {
                            Log.e(TAG, "Google sign-in failed: " + e.getStatusCode(), e);
                            if (e.getStatusCode() == 10) {
                                Toast.makeText(this,
                                        "Google Sign-In config error (code 10). "
                                                + "Add SHA-1 fingerprint to Google Cloud Console.",
                                        Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(this,
                                        "Google Sign-In failed: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

   
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = RegisterPageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AppwriteService.init(this);

        setupGoogleSignIn();
        setupPasswordStrengthWatcher();
        setupClickListeners();

       
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

   
    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions
                .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail().requestProfile().build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void prefillFromGoogle(GoogleSignInAccount account) {
        googleSignInClient.signOut();
        if (account.getDisplayName() != null) {
            binding.etName.setText(account.getDisplayName());
            binding.etUsername.setText(
                    account.getDisplayName().toLowerCase(Locale.ROOT)
                            .replace(" ", "_")
                            .replaceAll("[^a-z0-9_]", ""));
        }
        if (account.getEmail() != null) binding.etEmail.setText(account.getEmail());
        Toast.makeText(this,
                "Details filled from Google. Please complete the remaining fields.",
                Toast.LENGTH_LONG).show();
    }

   
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

   
    private void setupClickListeners() {

        binding.btnGoogleAuth.setOnClickListener(v ->
                googleSignInClient.signOut().addOnCompleteListener(t ->
                        googleSignInLauncher.launch(googleSignInClient.getSignInIntent())));

        binding.btnSendOtp.setOnClickListener(v -> {
            String mobile = binding.etMobile.getText().toString().trim();
            if (mobile.length() != 10 || !mobile.matches("\\d{10}")) {
                binding.etMobile.setError("Enter a valid 10-digit mobile number");
                return;
            }
            sendOtp(mobile);
        });

        binding.btnSignUp.setOnClickListener(v -> attemptRegistration());

        binding.tvSignIn.setOnClickListener(v -> {
            Intent i = new Intent(this, LoginActivity.class);
            i.putExtra("explicit_login", true);
            startActivity(i);
            finish();
        });
    }

   

    
    private void sendOtp(String mobile) {

       
        isAdminRegister = ADMIN_MOBILES.contains(mobile);
        if (isAdminRegister) {
            Toast.makeText(this,
                    "Admin number detected. Fill all fields to complete admin registration.",
                    Toast.LENGTH_LONG).show();
        }

       
        binding.btnSendOtp.setEnabled(false);
        binding.btnSendOtp.setText("Sending…");

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
           
            smsPermissionLauncher.launch(android.Manifest.permission.SEND_SMS);
        } else {
            sendOtpInternal(mobile);
        }
    }

    
    private void sendOtpInternal(String mobile) {
        generatedOtp = SmsManagerHelper.generateOtp();
        otpVerified  = false;

        Log.d(TAG, "OTP for " + mobile + " [" + (isAdminRegister ? "ADMIN" : "USER") + "]: "
                + generatedOtp); 

        SmsManagerHelper.sendOtp(this, mobile, generatedOtp);

        binding.btnSendOtp.setEnabled(false);
        binding.btnSendOtp.setText("Sent ✓");
        Toast.makeText(this, "OTP sent to " + mobile, Toast.LENGTH_SHORT).show();
    }

    private boolean verifyOtp() {
        String entered = binding.etMobileOtp.getText().toString().trim();
        if (generatedOtp == null) {
            Toast.makeText(this, "Please request an OTP first", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (entered.equals(generatedOtp)) { otpVerified = true; return true; }
        binding.etMobileOtp.setError("Incorrect OTP");
        return false;
    }

   
    private void attemptRegistration() {
        String name     = binding.etName.getText().toString().trim();
        String username = binding.etUsername.getText().toString().trim();
        String mobile   = binding.etMobile.getText().toString().trim();
        String email    = binding.etEmail.getText().toString().trim();
        String password = binding.etPass.getText().toString();
        String address  = binding.etAddress.getText().toString().trim();

       
        if (name.isEmpty())        { binding.etName.setError("Required");          return; }
        if (username.isEmpty())    { binding.etUsername.setError("Required");      return; }
        if (mobile.length() != 10) { binding.etMobile.setError("Invalid number"); return; }
        if (!verifyOtp())          { return; }
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.setError("Valid email required"); return;
        }
        if (!isPasswordStrong(password)) {
            binding.etPass.setError("Min 8 chars, uppercase, lowercase, digit & special char");
            return;
        }
        if (address.isEmpty()) { binding.etAddress.setError("Required"); return; }

       
        isAdminRegister = ADMIN_MOBILES.contains(mobile);

        setLoading(true);

        new Thread(() -> {
            try {
                Databases db = AppwriteService.getDatabases();

               
                if (isAdminRegister) {
                    List<? extends Document<?>> existing =
                            AppwriteHelper.findUserByField(
                                    db, AppwriteService.COL_ADMINS, "mobile", mobile
                            ).getDocuments();

                    if (!existing.isEmpty()) {
                        runOnUiThread(() -> {
                            setLoading(false);
                            Toast.makeText(this,
                                    "Admin already registered. Please use Login.",
                                    Toast.LENGTH_LONG).show();
                            Intent i = new Intent(this, LoginActivity.class);
                            i.putExtra("explicit_login", true);
                            startActivity(i);
                            finish();
                        });
                        return;
                    }
                }

               
                if (!isAdminRegister) {
                    List<? extends Document<?>> byEmail =
                            AppwriteHelper.findUserByField(db, "email", email).getDocuments();
                    if (!byEmail.isEmpty()) {
                        runOnUiThread(() -> {
                            setLoading(false);
                            binding.etEmail.setError("Email already registered");
                        });
                        return;
                    }
                    List<? extends Document<?>> byUser =
                            AppwriteHelper.findUserByField(db, "username", username).getDocuments();
                    if (!byUser.isEmpty()) {
                        runOnUiThread(() -> {
                            setLoading(false);
                            binding.etUsername.setError("Username already taken");
                        });
                        return;
                    }
                }

               
                AppwriteService.createAccountAndSignIn(email, password, name);

               
                String hashedPassword = hashPassword(password);
                String userId = java.util.UUID.randomUUID().toString()
                        .replace("-", "").substring(0, 20);

               
                Map<String, Object> data = new HashMap<>();
                data.put("name",     name);
                data.put("username", username);
                data.put("mobile",   mobile);
                data.put("email",    email);
                data.put("password", hashedPassword);
                data.put("address",  address);
                data.put("role",     isAdminRegister ? "admin" : "user");

               
                AppwriteHelper.createDocument(
                        db,
                        AppwriteService.DB_ID,
                        isAdminRegister ? AppwriteService.COL_ADMINS : AppwriteService.COL_USERS,
                        userId,
                        data
                );

               
                String role = isAdminRegister ? "admin" : "user";
                getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                        .putString(KEY_UID,  userId)
                        .putString(KEY_NAME, name)
                        .putString(KEY_ROLE, role)
                        .apply();

               
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(this,
                            (isAdminRegister ? "Admin account created! " : "")
                                    + "Welcome, " + name + "!",
                            Toast.LENGTH_SHORT).show();

                    Intent i = isAdminRegister
                            ? new Intent(this, AdminDashboardActivity.class)
                            : new Intent(this, MainActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                    finish();
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Log.e(TAG, "Registration failed", e);
                    Toast.makeText(this,
                            "Failed: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"),
                            Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

   
    private String hashPassword(String plainText) throws Exception {
        byte[] saltBytes = new byte[16];
        new SecureRandom().nextBytes(saltBytes);
        String salt = Base64.encodeToString(saltBytes, Base64.NO_WRAP);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(salt.getBytes());
        byte[] hashBytes = digest.digest(plainText.getBytes());
        StringBuilder hex = new StringBuilder();
        for (byte b : hashBytes) hex.append(String.format(Locale.ROOT, "%02x", b));
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