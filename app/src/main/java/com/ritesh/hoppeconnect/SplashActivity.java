package com.ritesh.hoppeconnect;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.*;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {


    private static final String PREFS    = "hoppe_prefs";
    private static final String KEY_UID  = "logged_in_user_id";
    private static final String KEY_ROLE = "logged_in_role";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView logo   = findViewById(R.id.splashLogo);
        TextView appName = findViewById(R.id.splashAppName);
        TextView tagline = findViewById(R.id.splashTagline);
        View ripple1     = findViewById(R.id.ripple1);
        View ripple2     = findViewById(R.id.ripple2);

       
        ScaleAnimation scale = new ScaleAnimation(0.2f, 1f, 0.2f, 1f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        scale.setDuration(750);
        scale.setInterpolator(new DecelerateInterpolator(1.5f));
        AlphaAnimation fade1 = new AlphaAnimation(0f, 1f);
        fade1.setDuration(750);
        AnimationSet logoAnim = new AnimationSet(true);
        logoAnim.addAnimation(scale);
        logoAnim.addAnimation(fade1);
        logoAnim.setFillAfter(true);
        if (logo != null) logo.startAnimation(logoAnim);

       
        TranslateAnimation slide = new TranslateAnimation(0, 0, 70f, 0f);
        slide.setDuration(600); slide.setStartOffset(600);
        AlphaAnimation fade2 = new AlphaAnimation(0f, 1f);
        fade2.setDuration(600); fade2.setStartOffset(600);
        AnimationSet nameAnim = new AnimationSet(true);
        nameAnim.addAnimation(slide); nameAnim.addAnimation(fade2);
        nameAnim.setFillAfter(true);
        if (appName != null) appName.startAnimation(nameAnim);

       
        AlphaAnimation tagFade = new AlphaAnimation(0f, 1f);
        tagFade.setDuration(700); tagFade.setStartOffset(1000);
        tagFade.setFillAfter(true);
        if (tagline != null) tagline.startAnimation(tagFade);

       
        if (ripple1 != null) startRipple(ripple1, 100);
        if (ripple2 != null) startRipple(ripple2, 600);

        new Handler(Looper.getMainLooper()).postDelayed(this::goNext, 2800);
    }

    private void startRipple(View v, long delay) {
        ScaleAnimation s = new ScaleAnimation(0f, 1f, 0f, 1f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        s.setDuration(1600); s.setStartOffset(delay);
        s.setRepeatCount(Animation.INFINITE); s.setRepeatMode(Animation.RESTART);
        AlphaAnimation a = new AlphaAnimation(0.5f, 0f);
        a.setDuration(1600); a.setStartOffset(delay);
        a.setRepeatCount(Animation.INFINITE); a.setRepeatMode(Animation.RESTART);
        AnimationSet set = new AnimationSet(true);
        set.addAnimation(s); set.addAnimation(a);
        v.startAnimation(set);
    }

    private void goNext() {
       
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String uid  = prefs.getString(KEY_UID, null);
        String role = prefs.getString(KEY_ROLE, "user");

        Intent next;
        if (uid != null) {
           
            if ("admin".equals(role)) {
                next = new Intent(this, AdminDashboardActivity.class);
            } else {
                next = new Intent(this, MainActivity.class);
            }
        } else {
           
            next = new Intent(this, LoginActivity.class);
            next.putExtra("explicit_login", true);
        }

        startActivity(next);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}