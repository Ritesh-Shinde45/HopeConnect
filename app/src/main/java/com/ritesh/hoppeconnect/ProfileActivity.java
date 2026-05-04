package com.ritesh.hoppeconnect;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import de.hdodenhof.circleimageview.CircleImageView;

import java.util.Map;

import io.appwrite.models.Document;
import io.appwrite.services.Databases;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";

    private LinearLayout layoutBuyTea;

    private ImageView ivBack, ivNotification, ivEditPhoto;
    private CircleImageView profileImage;
    private TextView tvUserName, tvLocation;

    private LinearLayout layoutEditProfile, layoutChangePassword, layoutNotifications;
    private LinearLayout layoutAbout, layoutPrivacy, layoutHelp, layoutLogout;
    private LinearLayout layoutMyReports;

    private BottomNavigationView bottomNav;

    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        AppwriteService.init(this);

        SharedPreferences prefs = getSharedPreferences("hoppe_prefs", MODE_PRIVATE);
        currentUserId = prefs.getString("logged_in_user_id", null);

        if (currentUserId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        loadProfile();
        setupClickListeners();
        setupBottomNavigation();
    }

    private void initializeViews() {

        ivBack = findViewById(R.id.ivBack);
        ivNotification = findViewById(R.id.ivNotification);
        layoutBuyTea = findViewById(R.id.layoutBuyTea);

        profileImage = findViewById(R.id.profileImage);
        ivEditPhoto = findViewById(R.id.ivEditPhoto);

        tvUserName = findViewById(R.id.tvUserName);
        tvLocation = findViewById(R.id.tvLocation);

        layoutEditProfile = findViewById(R.id.layoutEditProfile);
        layoutChangePassword = findViewById(R.id.layoutChangePassword);
        layoutNotifications = findViewById(R.id.layoutNotifications);

        layoutAbout = findViewById(R.id.layoutAbout);
        layoutPrivacy = findViewById(R.id.layoutPrivacy);
        layoutHelp = findViewById(R.id.layoutHelp);

        layoutLogout = findViewById(R.id.layoutLogout);
        layoutMyReports = findViewById(R.id.layoutMyReports);

        bottomNav = findViewById(R.id.bottomNav);
    }

    private void loadProfile() {

        new Thread(() -> {

            try {

                Databases db = AppwriteService.getDatabases();

                Document<?> doc = AppwriteHelper.getDocument(
                        db,
                        AppwriteService.DB_ID,
                        AppwriteService.COL_USERS,
                        currentUserId
                );

                Map<String, Object> data = (Map<String, Object>) doc.getData();

                runOnUiThread(() -> {

                    Object name = data.get("name");
                    Object location = data.get("address");

                    if (name != null) tvUserName.setText(name.toString());
                    if (location != null) tvLocation.setText(location.toString());

                    Object photoId = data.get("photoId");

                    if (photoId != null) {

                        String url = AppwriteService.ENDPOINT
                                + "/storage/buckets/"
                                + AppwriteService.USERS_BUCKET_ID
                                + "/files/"
                                + photoId
                                + "/view?project="
                                + AppwriteService.PROJECT_ID;

                        Glide.with(ProfileActivity.this)
                                .load(url)
                                .placeholder(R.drawable.ic_person)
                                .error(R.drawable.ic_person)
                                .circleCrop()
                                .into(profileImage);
                    }

                });

            } catch (Exception e) {

                Log.e(TAG, "load error", e);

                runOnUiThread(() ->
                        Toast.makeText(ProfileActivity.this,
                                "Load failed",
                                Toast.LENGTH_LONG).show());
            }

        }).start();
    }

    private void setupClickListeners() {

        ivBack.setOnClickListener(v -> finish());

        ivNotification.setOnClickListener(v ->
                startActivity(new Intent(this, NotificationsActivity.class)));

        ivEditPhoto.setOnClickListener(v ->
                startActivity(new Intent(this, EditProfileActivity.class)));

        layoutEditProfile.setOnClickListener(v ->
                startActivity(new Intent(this, EditProfileActivity.class)));

        layoutChangePassword.setOnClickListener(v ->
                startActivity(new Intent(this, ChangePasswordActivity.class)));

        layoutNotifications.setOnClickListener(v ->
                startActivity(new Intent(this, NotificationsActivity.class)));

        layoutAbout.setOnClickListener(v ->
                startActivity(new Intent(this, AboutActivity.class)));

        layoutPrivacy.setOnClickListener(v ->
                startActivity(new Intent(this, PrivacyPolicyActivity.class)));

        layoutHelp.setOnClickListener(v ->
                startActivity(new Intent(this, HelpSupportActivity.class)));

        if (layoutBuyTea != null) {
            layoutBuyTea.setOnClickListener(v ->
                    startActivity(new Intent(this, BuyTeaActivity.class)));
        }

        if (layoutMyReports != null) {
            layoutMyReports.setOnClickListener(v ->
                    startActivity(new Intent(this, MyReportsActivity.class)));
        }

        layoutLogout.setOnClickListener(v -> showLogoutDialog());
    }

    private void setupBottomNavigation() {

        bottomNav.setSelectedItemId(R.id.nav_profile);

        bottomNav.setOnItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_profile) {
                return true;
            }

            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                return true;
            }

            if (id == R.id.nav_explore) {
                startActivity(new Intent(this, ExploreActivity.class));
                return true;
            }

            if (id == R.id.nav_new_report) {
                startActivity(new Intent(this, NewReportActivity.class));
                return true;
            }

            if (id == R.id.nav_chat) {
                startActivity(new Intent(this, ChatsActivity.class));
                return true;
            }

            return false;
        });
    }

    private void showLogoutDialog() {

        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> performLogout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performLogout() {

        getSharedPreferences("hoppe_prefs", MODE_PRIVATE).edit().clear().apply();

        Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, RegisterActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfile();
        bottomNav.setSelectedItemId(R.id.nav_profile);
    }
}