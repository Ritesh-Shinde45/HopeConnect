package com.ritesh.hoppeconnect;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class PrivacyPolicyActivity extends AppCompatActivity {

    private ImageView ivBack;
    private TextView txtContact;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_policy);

        initializeViews();
        setupClickListeners();
        setupClickableLinks();
    }

    private void initializeViews() {
        ivBack = findViewById(R.id.ivBack);
        txtContact = findViewById(R.id.txtContact);
    }

    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> onBackPressed());
    }

    private void setupClickableLinks() {

        String text = "Have questions about this Privacy Policy or how your data is handled? Reach out:\n\n" +
                "📧 riteshshinde472@gmail.com\n" +
                "🔗 linkedin.com/in/ritesh--shinde";

        SpannableString spannable = new SpannableString(text);

        // Email Click
        int emailStart = text.indexOf("riteshshinde472@gmail.com");
        int emailEnd = emailStart + "riteshshinde472@gmail.com".length();

        ClickableSpan emailClick = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                emailIntent.setData(Uri.parse("mailto:riteshshinde472@gmail.com"));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Regarding Privacy Policy");
                startActivity(emailIntent);
            }
        };

        spannable.setSpan(emailClick, emailStart, emailEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // LinkedIn Click
        int linkStart = text.indexOf("linkedin.com/in/ritesh--shinde");
        int linkEnd = linkStart + "linkedin.com/in/ritesh--shinde".length();

        ClickableSpan linkedinClick = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                String url = "https://www.linkedin.com/in/ritesh--shinde";
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            }
        };

        spannable.setSpan(linkedinClick, linkStart, linkEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        txtContact.setText(spannable);
        txtContact.setMovementMethod(LinkMovementMethod.getInstance());
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}