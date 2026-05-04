package com.ritesh.hoppeconnect;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class HelpSupportActivity extends AppCompatActivity {

    private ImageView ivBack;
    private LinearLayout layoutEmailSupport, layoutPhoneSupport, layoutReportProblem;
    private LinearLayout layoutFaq1, layoutFaq2, layoutFaq3, layoutFaq4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_support);

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        ivBack = findViewById(R.id.ivBack);
        layoutEmailSupport = findViewById(R.id.layoutEmailSupport);
        layoutPhoneSupport = findViewById(R.id.layoutPhoneSupport);
        layoutReportProblem = findViewById(R.id.layoutReportProblem);
        layoutFaq1 = findViewById(R.id.layoutFaq1);
        layoutFaq2 = findViewById(R.id.layoutFaq2);
        layoutFaq3 = findViewById(R.id.layoutFaq3);
        layoutFaq4 = findViewById(R.id.layoutFaq4);
    }

    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> onBackPressed());

       
        layoutEmailSupport.setOnClickListener(v -> {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:riteshshinde472@gmail.com"));
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Support Request - HopeConnect");

            try {
                startActivity(Intent.createChooser(emailIntent, "Send Email"));
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(this, "No email client installed", Toast.LENGTH_SHORT).show();
            }
        });

       
        layoutPhoneSupport.setOnClickListener(v -> {
            Intent dialIntent = new Intent(Intent.ACTION_DIAL);
            dialIntent.setData(Uri.parse("tel:+918080769308"));

            try {
                startActivity(dialIntent);
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(this, "No phone app available", Toast.LENGTH_SHORT).show();
            }
        });

       
        layoutReportProblem.setOnClickListener(v -> {
           
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:riteshshinde472@gmail.com"));
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Problem Report - HopeConnect");
            emailIntent.putExtra(Intent.EXTRA_TEXT, "Please describe the problem you're experiencing:\n\n");

            try {
                startActivity(Intent.createChooser(emailIntent, "Report Problem"));
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(this, "No email client installed", Toast.LENGTH_SHORT).show();
            }
        });

       
       
        layoutFaq1.setOnClickListener(v -> {
           
            Toast.makeText(this, "FAQ item clicked", Toast.LENGTH_SHORT).show();
        });

        layoutFaq2.setOnClickListener(v -> {
            Toast.makeText(this, "FAQ item clicked", Toast.LENGTH_SHORT).show();
        });

        layoutFaq3.setOnClickListener(v -> {
            Toast.makeText(this, "FAQ item clicked", Toast.LENGTH_SHORT).show();
        });

        layoutFaq4.setOnClickListener(v -> {
            Toast.makeText(this, "FAQ item clicked", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}