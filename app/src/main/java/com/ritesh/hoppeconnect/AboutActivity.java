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
    private android.widget.LinearLayout layoutLinkedIn;
    private android.widget.LinearLayout layoutGitHub;



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
        layoutLinkedIn = findViewById(R.id.layoutLinkedIn);
        layoutGitHub = findViewById(R.id.layoutGitHub);


    }

    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> onBackPressed());

        layoutGitHub.setOnClickListener(v ->
                openUrl("https://github.com/Ritesh-Shinde45", "com.github.android"));

        layoutLinkedIn.setOnClickListener(v ->
                openUrl("https://www.linkedin.com/in/ritesh--shinde", "com.linkedin.android"));
    }

    private void openUrl(String url, String appPackage) {
        android.net.Uri uri = android.net.Uri.parse(url);

        // Try native app first
        try {
            getPackageManager().getPackageInfo(appPackage, 0);
            android.content.Intent appIntent = new android.content.Intent(
                    android.content.Intent.ACTION_VIEW, uri);
            appIntent.setPackage(appPackage);
            startActivity(appIntent);
            return; // opened successfully, stop here
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            // App not installed, fall through to browser
        }

        // Fall back to browser
        try {
            android.content.Intent browserIntent = new android.content.Intent(
                    android.content.Intent.ACTION_VIEW, uri);
            startActivity(browserIntent); // Android picks best available browser
        } catch (android.content.ActivityNotFoundException e) {
            android.widget.Toast.makeText(this,
                    "No browser found", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private void displayAppVersion() {
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName;
           
           
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
