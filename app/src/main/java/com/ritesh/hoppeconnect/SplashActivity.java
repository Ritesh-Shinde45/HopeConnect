package com.ritesh.hoppeconnect;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat;

public class SplashActivity extends AppCompatActivity {

    private static final String PREFS    = "hoppe_prefs";
    private static final String KEY_UID  = "logged_in_user_id";
    private static final String KEY_ROLE = "logged_in_role";
    private static final long   TOTAL    = 4200L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView splashLogo = findViewById(R.id.splashLogo);
        ImageView birdView   = findViewById(R.id.birdView);
        TextView  appName    = findViewById(R.id.splashAppName);
        TextView  tagline    = findViewById(R.id.splashTagline);
        View      ripple1    = findViewById(R.id.ripple1);
        View      ripple2    = findViewById(R.id.ripple2);
        View      handGlow   = findViewById(R.id.handGlow);
        View      skyOverlay = findViewById(R.id.skyOverlay);
        View      trailDot1  = findViewById(R.id.trailDot1);
        View      trailDot2  = findViewById(R.id.trailDot2);
        View      trailDot3  = findViewById(R.id.trailDot3);
        ImageView sparkle1   = findViewById(R.id.sparkle1);
        ImageView sparkle2   = findViewById(R.id.sparkle2);
        ImageView sparkle3   = findViewById(R.id.sparkle3);
        ImageView sparkle4   = findViewById(R.id.sparkle4);
        ImageView sparkle5   = findViewById(R.id.sparkle5);

        Handler h = new Handler(Looper.getMainLooper());

        // Set animated bird drawable (wings flap via AnimatedVectorDrawable)
        AnimatedVectorDrawableCompat avd =
                AnimatedVectorDrawableCompat.create(this, R.drawable.animated_bird);
        if (avd != null) birdView.setImageDrawable(avd);

        // Phase 1 (0ms): Logo pop-in + ripples
        startLogoAnimation(splashLogo, ripple1, ripple2);

        // Phase 2 (500ms): Bird appears on hand, wings start flapping
        h.postDelayed(() -> {
            birdView.setAlpha(1f);
            birdView.setScaleX(0.5f);
            birdView.setScaleY(0.5f);
            ObjectAnimator bx = ObjectAnimator.ofFloat(birdView, "scaleX", 0.5f, 0.9f);
            ObjectAnimator by = ObjectAnimator.ofFloat(birdView, "scaleY", 0.5f, 0.9f);
            bx.setDuration(300); by.setDuration(300);
            bx.setInterpolator(new OvershootInterpolator(1.5f));
            by.setInterpolator(new OvershootInterpolator(1.5f));
            AnimatorSet as = new AnimatorSet();
            as.playTogether(bx, by);
            as.start();
            // Start wing flapping animation
            Drawable d = birdView.getDrawable();
            if (d instanceof AnimatedVectorDrawableCompat) {
                ((AnimatedVectorDrawableCompat) d).start();
            } else if (d instanceof AnimatedVectorDrawable) {
                ((AnimatedVectorDrawable) d).start();
            }
        }, 500);

        // Phase 3 (800ms): Hand glow pulse + bird pre-launch bounce
        h.postDelayed(() -> startPreLaunch(splashLogo, handGlow, birdView), 800);

        // Phase 4 (1100ms): Bird flies up
        h.postDelayed(() ->
                        startFlight(birdView, trailDot1, trailDot2, trailDot3,
                                skyOverlay, sparkle1, sparkle2, sparkle3, sparkle4, sparkle5),
                1100);

        // Phase 5 (1900ms): App name + tagline appear
        h.postDelayed(() -> startTextReveal(appName, tagline), 1900);

        // Navigate away
        h.postDelayed(this::goNext, TOTAL);
    }

    private void startLogoAnimation(ImageView logo, View r1, View r2) {
        logo.setAlpha(0f); logo.setScaleX(0.1f); logo.setScaleY(0.1f);
        ObjectAnimator a  = ObjectAnimator.ofFloat(logo, "alpha",  0f, 1f);
        ObjectAnimator sx = ObjectAnimator.ofFloat(logo, "scaleX", 0.1f, 1f);
        ObjectAnimator sy = ObjectAnimator.ofFloat(logo, "scaleY", 0.1f, 1f);
        a.setDuration(650); sx.setDuration(650); sy.setDuration(650);
        sx.setInterpolator(new OvershootInterpolator(1.6f));
        sy.setInterpolator(new OvershootInterpolator(1.6f));
        AnimatorSet as = new AnimatorSet();
        as.playTogether(a, sx, sy);
        as.start();
        startRipple(r1, 150);
        startRipple(r2, 650);
    }

    private void startPreLaunch(ImageView logo, View handGlow, ImageView bird) {
        ObjectAnimator glowA = ObjectAnimator.ofFloat(handGlow, "alpha", 0f, 0.9f);
        glowA.setDuration(300); glowA.start();
        ObjectAnimator gsx = ObjectAnimator.ofFloat(handGlow, "scaleX", 1f, 1.3f, 1f);
        ObjectAnimator gsy = ObjectAnimator.ofFloat(handGlow, "scaleY", 1f, 1.3f, 1f);
        gsx.setDuration(350); gsy.setDuration(350);
        gsx.setStartDelay(300); gsy.setStartDelay(300);
        gsx.start(); gsy.start();
        // Crouch-then-spring bounce
        ObjectAnimator bounce = ObjectAnimator.ofFloat(bird, "translationY",
                0f, 14f, -28f, -12f, 0f);
        bounce.setDuration(300);
        bounce.setInterpolator(new DecelerateInterpolator());
        bounce.start();
        ObjectAnimator logoFade = ObjectAnimator.ofFloat(logo, "alpha", 1f, 0.5f);
        logoFade.setDuration(300); logoFade.start();
    }

    private void startFlight(ImageView bird,
                             View dot1, View dot2, View dot3, View sky,
                             ImageView sp1, ImageView sp2, ImageView sp3,
                             ImageView sp4, ImageView sp5) {
        int screenH = getResources().getDisplayMetrics().heightPixels;
        ObjectAnimator bY  = ObjectAnimator.ofFloat(bird, "translationY", 0f, -(screenH * 0.60f));
        ObjectAnimator bX  = ObjectAnimator.ofFloat(bird, "translationX", 0f, 70f);
        ObjectAnimator bSX = ObjectAnimator.ofFloat(bird, "scaleX", 0.9f, 0.12f);
        ObjectAnimator bSY = ObjectAnimator.ofFloat(bird, "scaleY", 0.9f, 0.12f);
        ObjectAnimator bA  = ObjectAnimator.ofFloat(bird, "alpha",  1f, 0f);
        ObjectAnimator bR  = ObjectAnimator.ofFloat(bird, "rotation", 0f, -30f, -18f);
        bY.setDuration(1000);  bY.setInterpolator(new AccelerateInterpolator(1.3f));
        bX.setDuration(1000);  bX.setInterpolator(new DecelerateInterpolator(2f));
        bSX.setDuration(1000); bSX.setInterpolator(new AccelerateInterpolator(1.1f));
        bSY.setDuration(1000); bSY.setInterpolator(new AccelerateInterpolator(1.1f));
        bA.setDuration(420);   bA.setStartDelay(580);
        bR.setDuration(1000);
        AnimatorSet flight = new AnimatorSet();
        flight.playTogether(bY, bX, bSX, bSY, bA, bR);
        flight.start();
        flashDot(dot1,   0L, 180L);
        flashDot(dot2, 200L, 180L);
        flashDot(dot3, 400L, 180L);
        ObjectAnimator skyA = ObjectAnimator.ofFloat(sky, "alpha", 0f, 0.4f);
        skyA.setDuration(1000); skyA.setInterpolator(new DecelerateInterpolator()); skyA.start();
        popSparkle(sp1, 180L); popSparkle(sp2, 320L); popSparkle(sp3, 480L);
        popSparkle(sp4,  90L); popSparkle(sp5, 410L);
    }

    private void startTextReveal(TextView appName, TextView tagline) {
        appName.setAlpha(0f); appName.setTranslationY(50f);
        ObjectAnimator nY = ObjectAnimator.ofFloat(appName, "translationY", 50f, 0f);
        ObjectAnimator nA = ObjectAnimator.ofFloat(appName, "alpha", 0f, 1f);
        nY.setDuration(650); nA.setDuration(650);
        nY.setInterpolator(new FastOutSlowInInterpolator());
        AnimatorSet ns = new AnimatorSet(); ns.playTogether(nY, nA); ns.start();
        tagline.setAlpha(0f);
        ObjectAnimator tA = ObjectAnimator.ofFloat(tagline, "alpha", 0f, 1f);
        tA.setDuration(600); tA.setStartDelay(220); tA.start();
    }

    private void startRipple(View v, long delay) {
        v.setScaleX(0.05f); v.setScaleY(0.05f); v.setAlpha(0.65f);
        ObjectAnimator sx = ObjectAnimator.ofFloat(v, "scaleX", 0.05f, 1f);
        ObjectAnimator sy = ObjectAnimator.ofFloat(v, "scaleY", 0.05f, 1f);
        ObjectAnimator a  = ObjectAnimator.ofFloat(v, "alpha",  0.65f, 0f);
        for (ObjectAnimator o : new ObjectAnimator[]{sx, sy, a}) {
            o.setDuration(1800); o.setStartDelay(delay);
            o.setRepeatCount(ValueAnimator.INFINITE); o.setRepeatMode(ValueAnimator.RESTART);
        }
        sx.setInterpolator(new DecelerateInterpolator(1.5f));
        sy.setInterpolator(new DecelerateInterpolator(1.5f));
        AnimatorSet rs = new AnimatorSet(); rs.playTogether(sx, sy, a); rs.start();
    }

    private void flashDot(View dot, long delay, long duration) {
        dot.setAlpha(0f);
        ObjectAnimator in  = ObjectAnimator.ofFloat(dot, "alpha", 0f, 0.9f);
        ObjectAnimator out = ObjectAnimator.ofFloat(dot, "alpha", 0.9f, 0f);
        in.setDuration(duration / 2);  in.setStartDelay(delay);
        out.setDuration(duration / 2); out.setStartDelay(delay + duration / 2);
        in.start(); out.start();
    }

    private void popSparkle(final ImageView sp, long delay) {
        sp.setAlpha(0f); sp.setScaleX(0f); sp.setScaleY(0f);
        ObjectAnimator sx = ObjectAnimator.ofFloat(sp, "scaleX", 0f, 1.5f, 1f);
        ObjectAnimator sy = ObjectAnimator.ofFloat(sp, "scaleY", 0f, 1.5f, 1f);
        ObjectAnimator a  = ObjectAnimator.ofFloat(sp, "alpha",  0f, 1f,   0.8f);
        sx.setDuration(480); sy.setDuration(480); a.setDuration(480);
        sx.setStartDelay(delay); sy.setStartDelay(delay); a.setStartDelay(delay);
        sx.setInterpolator(new OvershootInterpolator(2.8f));
        sy.setInterpolator(new OvershootInterpolator(2.8f));
        AnimatorSet pop = new AnimatorSet();
        pop.playTogether(sx, sy, a);
        pop.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                ObjectAnimator pulse = ObjectAnimator.ofFloat(sp, "alpha", 0.8f, 0.1f, 0.8f);
                pulse.setDuration(1000); pulse.setRepeatCount(ValueAnimator.INFINITE);
                pulse.setRepeatMode(ValueAnimator.REVERSE); pulse.start();
            }
        });
        pop.start();
    }

    private void goNext() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String uid  = prefs.getString(KEY_UID,  null);
        String role = prefs.getString(KEY_ROLE, "user");
        Intent next;
        if (uid != null) {
            next = "admin".equals(role)
                    ? new Intent(this, AdminDashboardActivity.class)
                    : new Intent(this, MainActivity.class);
        } else {
            next = new Intent(this, LoginActivity.class);
            next.putExtra("explicit_login", true);
        }
        startActivity(next);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}