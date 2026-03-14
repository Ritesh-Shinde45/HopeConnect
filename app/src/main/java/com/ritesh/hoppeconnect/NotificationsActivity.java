package com.ritesh.hoppeconnect;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.switchmaterial.SwitchMaterial;

public class NotificationsActivity extends AppCompatActivity {

    private ImageView ivBack;
    private SwitchMaterial switchAllNotifications, switchNewPosts, switchComments,
            switchEvents, switchEmailDigest, switchPromotional;

    private SharedPreferences preferences;
    private static final String PREFS_NAME = "NotificationPrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        initializeViews();
        loadNotificationSettings();
        setupClickListeners();
    }

    private void initializeViews() {
        ivBack = findViewById(R.id.ivBack);
        switchAllNotifications = findViewById(R.id.switchAllNotifications);
        switchNewPosts = findViewById(R.id.switchNewPosts);
        switchComments = findViewById(R.id.switchComments);
        switchEvents = findViewById(R.id.switchEvents);
        switchEmailDigest = findViewById(R.id.switchEmailDigest);
        switchPromotional = findViewById(R.id.switchPromotional);

        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    }

    private void loadNotificationSettings() {
        // Load saved notification preferences
        switchAllNotifications.setChecked(preferences.getBoolean("all_notifications", true));
        switchNewPosts.setChecked(preferences.getBoolean("new_posts", true));
        switchComments.setChecked(preferences.getBoolean("comments", true));
        switchEvents.setChecked(preferences.getBoolean("events", true));
        switchEmailDigest.setChecked(preferences.getBoolean("email_digest", false));
        switchPromotional.setChecked(preferences.getBoolean("promotional", false));
    }

    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> onBackPressed());

        // All Notifications Switch
        switchAllNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            savePreference("all_notifications", isChecked);

            // If turning off all notifications, disable all others
            if (!isChecked) {
                switchNewPosts.setChecked(false);
                switchComments.setChecked(false);
                switchEvents.setChecked(false);
            }

            // Enable/disable other switches based on master switch
            switchNewPosts.setEnabled(isChecked);
            switchComments.setEnabled(isChecked);
            switchEvents.setEnabled(isChecked);
        });

        // New Posts Notifications
        switchNewPosts.setOnCheckedChangeListener((buttonView, isChecked) -> {
            savePreference("new_posts", isChecked);
            updateMasterSwitch();
        });

        // Comments Notifications
        switchComments.setOnCheckedChangeListener((buttonView, isChecked) -> {
            savePreference("comments", isChecked);
            updateMasterSwitch();
        });

        // Events Notifications
        switchEvents.setOnCheckedChangeListener((buttonView, isChecked) -> {
            savePreference("events", isChecked);
            updateMasterSwitch();
        });

        // Email Digest
        switchEmailDigest.setOnCheckedChangeListener((buttonView, isChecked) -> {
            savePreference("email_digest", isChecked);
        });

        // Promotional Emails
        switchPromotional.setOnCheckedChangeListener((buttonView, isChecked) -> {
            savePreference("promotional", isChecked);
        });
    }

    private void updateMasterSwitch() {
        // If all individual switches are off, turn off master switch
        boolean anyEnabled = switchNewPosts.isChecked() ||
                switchComments.isChecked() ||
                switchEvents.isChecked();

        if (!anyEnabled && switchAllNotifications.isChecked()) {
            switchAllNotifications.setChecked(false);
        } else if (anyEnabled && !switchAllNotifications.isChecked()) {
            switchAllNotifications.setChecked(true);
        }
    }

    private void savePreference(String key, boolean value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(key, value);
        editor.apply();

        // Here you can also send the preference to your backend server
        // to sync notification settings across devices
        // syncNotificationSettings(key, value);
    }

    // Method to sync notification settings with server
    private void syncNotificationSettings(String key, boolean value) {
        // Implement API call to sync notification preferences
        // Example:
        /*
        Map<String, Boolean> settings = new HashMap<>();
        settings.put(key, value);

        ApiService.updateNotificationSettings(settings)
            .enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    // Handle success
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    // Handle failure
                }
            });
        */
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}