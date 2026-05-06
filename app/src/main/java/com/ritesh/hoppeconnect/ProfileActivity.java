package com.ritesh.hoppeconnect;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import de.hdodenhof.circleimageview.CircleImageView;

import java.util.Map;

import io.appwrite.models.Document;
import io.appwrite.services.Databases;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";
    private static final float DRAG_SLOP = 18f;
    private static final float DISMISS_THRESHOLD = 180f;
    private static final float CIRCLE_TRIGGER_DISTANCE = 400f;
    private static final float BLUR_FADE_DISTANCE = 500f;

    private float touchStartX = 0f;
    private float touchStartY = 0f;
    private boolean isDragging = false;

    private LinearLayout layoutBuyTea;
    private ImageView ivBack, ivNotification, ivEditPhoto;
    private CircleImageView profileImage;
    private TextView tvUserName, tvLocation;
    private LinearLayout layoutEditProfile, layoutChangePassword, layoutNotifications;
    private LinearLayout layoutAbout, layoutPrivacy, layoutHelp, layoutLogout;
    private LinearLayout layoutMyReports;
    private BottomNavigationView bottomNav;
    private FrameLayout imagePreviewContainer;
    private PhotoView fullImagePreview;
    private View blurOverlay;

    private String profileImageUrl;
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
        imagePreviewContainer = findViewById(R.id.imagePreviewContainer);
        fullImagePreview = findViewById(R.id.fullImagePreview);
        blurOverlay = findViewById(R.id.blurOverlay);
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
                        profileImageUrl = url;

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
                        Toast.makeText(ProfileActivity.this, "Load failed", Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void applyCircleProgress(float progress) {
        progress = Math.max(0f, Math.min(1f, progress));
        final float p = progress;
        fullImagePreview.setClipToOutline(true);
        fullImagePreview.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override
            public void getOutline(View v, android.graphics.Outline outline) {
                int w = v.getWidth();
                int h = v.getHeight();
                if (w == 0 || h == 0) return;
                int size = Math.min(w, h);
                int left = (w - size) / 2;
                int top = (h - size) / 2;
                float radius = (size / 2f) * p;
                outline.setRoundRect(left, top, left + size, top + size, radius);
            }
        });
        fullImagePreview.invalidateOutline();
    }

    private void setBlurAlpha(float alpha) {
        if (blurOverlay != null) {
            blurOverlay.setAlpha(Math.max(0f, Math.min(1f, alpha)));
        }
    }

    private void openImagePreview() {
        if (profileImageUrl == null) {
            Toast.makeText(this, "No photo available", Toast.LENGTH_SHORT).show();
            return;
        }

        fullImagePreview.setTranslationX(0f);
        fullImagePreview.setTranslationY(0f);
        fullImagePreview.setScaleX(1f);
        fullImagePreview.setScaleY(1f);
        fullImagePreview.setAlpha(1f);
        fullImagePreview.setScale(1f, false);
        applyCircleProgress(0f);

        if (blurOverlay != null) blurOverlay.setAlpha(0f);
        imagePreviewContainer.setAlpha(0f);
        imagePreviewContainer.setVisibility(View.VISIBLE);

        imagePreviewContainer.animate()
                .alpha(1f)
                .setDuration(280)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        if (blurOverlay != null) {
            blurOverlay.animate().alpha(1f).setDuration(280).start();
        }

        Glide.with(this)
                .load(profileImageUrl)
                .placeholder(R.drawable.ic_person)
                .into(fullImagePreview);
    }

    private void dismissImagePreview() {
        if (blurOverlay != null) {
            blurOverlay.animate().alpha(0f).setDuration(220).start();
        }
        imagePreviewContainer.animate()
                .alpha(0f)
                .setDuration(220)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> {
                    imagePreviewContainer.setVisibility(View.GONE);
                    fullImagePreview.setTranslationX(0f);
                    fullImagePreview.setTranslationY(0f);
                    fullImagePreview.setScaleX(1f);
                    fullImagePreview.setScaleY(1f);
                    fullImagePreview.setAlpha(1f);
                    fullImagePreview.setScale(1f, false);
                    applyCircleProgress(0f);
                })
                .start();
    }

    private void snapBackImagePreview() {
        fullImagePreview.animate()
                .translationX(0f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(400)
                .setInterpolator(new OvershootInterpolator(1.6f))
                .withEndAction(() -> applyCircleProgress(0f))
                .start();

        imagePreviewContainer.animate().alpha(1f).setDuration(300).start();

        if (blurOverlay != null) {
            blurOverlay.animate().alpha(1f).setDuration(300).start();
        }
    }

    private void flyOutAndDismiss(float dx, float dy, float dist) {
        float scale = 900f / Math.max(dist, 1f);
        float targetX = dx * scale;
        float targetY = dy * scale;

        if (blurOverlay != null) {
            blurOverlay.animate().alpha(0f).setDuration(300).start();
        }

        fullImagePreview.animate()
                .translationX(targetX)
                .translationY(targetY)
                .scaleX(0.3f)
                .scaleY(0.3f)
                .alpha(0f)
                .setDuration(340)
                .setInterpolator(new DecelerateInterpolator(1.6f))
                .withEndAction(this::dismissImagePreview)
                .start();

        imagePreviewContainer.animate().alpha(0f).setDuration(300).start();
    }

    private void setupPhotoViewTouchListener() {
        fullImagePreview.setOnTouchListener((v, event) -> {

            if (event.getPointerCount() > 1) {
                isDragging = false;
                return false;
            }

            if (fullImagePreview.getScale() > 1.05f) {
                isDragging = false;
                return false;
            }

            switch (event.getActionMasked()) {

                case MotionEvent.ACTION_DOWN: {
                    touchStartX = event.getRawX();
                    touchStartY = event.getRawY();
                    isDragging = false;
                    return true;
                }

                case MotionEvent.ACTION_MOVE: {
                    float rawX = event.getRawX();
                    float rawY = event.getRawY();
                    float dx = rawX - touchStartX;
                    float dy = rawY - touchStartY;
                    float dist = (float) Math.sqrt(dx * dx + dy * dy);

                    if (!isDragging && dist > DRAG_SLOP) {
                        isDragging = true;
                    }

                    if (isDragging) {
                        fullImagePreview.setTranslationX(dx);
                        fullImagePreview.setTranslationY(dy);
                        fullImagePreview.setAlpha(1f);

                        float blurAlpha = Math.max(0f, 1f - (dist / BLUR_FADE_DISTANCE));
                        setBlurAlpha(blurAlpha);

                        float upwardDrag = Math.max(0f, -dy);
                        float circleProgress = Math.min(1f, upwardDrag / CIRCLE_TRIGGER_DISTANCE);
                        applyCircleProgress(circleProgress);

                        return true;
                    }

                    return true;
                }

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: {
                    if (!isDragging) {
                        isDragging = false;
                        return false;
                    }

                    float upX = event.getRawX();
                    float upY = event.getRawY();
                    float returnDx = upX - touchStartX;
                    float returnDy = upY - touchStartY;
                    float returnDist = (float) Math.sqrt(returnDx * returnDx + returnDy * returnDy);

                    isDragging = false;

                    if (returnDist > DISMISS_THRESHOLD) {
                        flyOutAndDismiss(returnDx, returnDy, returnDist);
                    } else {
                        snapBackImagePreview();
                    }

                    return true;
                }
            }

            return false;
        });
    }

    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> finish());

        ivNotification.setOnClickListener(v ->
                startActivity(new Intent(this, NotificationsActivity.class)));

        profileImage.setOnClickListener(v -> openImagePreview());

        setupPhotoViewTouchListener();

        imagePreviewContainer.setOnClickListener(v -> dismissImagePreview());

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

        if (layoutBuyTea != null)
            layoutBuyTea.setOnClickListener(v ->
                    startActivity(new Intent(this, BuyTeaActivity.class)));

        if (layoutMyReports != null)
            layoutMyReports.setOnClickListener(v ->
                    startActivity(new Intent(this, MyReportsActivity.class)));

        layoutLogout.setOnClickListener(v -> showLogoutDialog());
    }

    private void setupBottomNavigation() {
        bottomNav.setSelectedItemId(R.id.nav_profile);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_profile) return true;
            if (id == R.id.nav_home) { startActivity(new Intent(this, MainActivity.class)); return true; }
            if (id == R.id.nav_explore) { startActivity(new Intent(this, ExploreActivity.class)); return true; }
            if (id == R.id.nav_new_report) { startActivity(new Intent(this, NewReportActivity.class)); return true; }
            if (id == R.id.nav_chat) { startActivity(new Intent(this, ChatsActivity.class)); return true; }
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

    @Override
    public void onBackPressed() {
        if (imagePreviewContainer.getVisibility() == View.VISIBLE) {
            dismissImagePreview();
            return;
        }
        super.onBackPressed();
    }
}