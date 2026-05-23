package com.ritesh.hoppeconnect;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class PrivacyPolicyActivity extends AppCompatActivity {

    private ImageView ivBack;
    private LinearLayout layoutEmail;
    private LinearLayout layoutGitHub;
    private LinearLayout layoutLinkedIn;

    private static final String EMAIL    = "riteshshinde472@gmail.com";
    private static final String GITHUB   = "https://github.com/Ritesh-Shinde45";
    private static final String LINKEDIN = "https://www.linkedin.com/in/ritesh--shinde";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_policy);

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        ivBack         = findViewById(R.id.ivBack);
        layoutEmail    = findViewById(R.id.layoutEmail);
        layoutGitHub   = findViewById(R.id.layoutGitHub);
        layoutLinkedIn = findViewById(R.id.layoutLinkedIn);
    }

    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> onBackPressed());

        layoutEmail.setOnClickListener(v -> {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:" + EMAIL));
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Privacy Policy Inquiry - HopeConnect");
            emailIntent.putExtra(Intent.EXTRA_TEXT, "Hi Ritesh,\n\n");

            try {
                startActivity(Intent.createChooser(emailIntent, "Send Email"));
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(this, "No email client installed", Toast.LENGTH_SHORT).show();
            }
        });

        layoutGitHub.setOnClickListener(v -> openUrl(GITHUB));

        layoutLinkedIn.setOnClickListener(v -> openUrl(LINKEDIN));
    }

    private void openUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}