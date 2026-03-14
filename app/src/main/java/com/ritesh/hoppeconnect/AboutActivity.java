package com.ritesh.hoppeconnect;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AboutActivity extends AppCompatActivity {

    private ImageView ivBack;
    private TextView tvVersionName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_about);

        initializeViews();
        setupClickListeners();
        displayAppVersion();
    }

    private void initializeViews() {
        ivBack = findViewById(R.id.ivBack);
        // You can add a TextView for version if you want to display it dynamically
        // tvVersionName = findViewById(R.id.tvVersionName);
    }

    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> onBackPressed());
    }

    private void displayAppVersion() {
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName;
            // If you have a TextView for version, update it here
            // tvVersionName.setText("Version " + version);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
