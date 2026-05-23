package com.ritesh.hoppeconnect;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowInsetsController;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.SharedPreferences;
import android.util.Log;

import com.bumptech.glide.Glide;

import java.util.Map;

import io.appwrite.models.Document;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import de.hdodenhof.circleimageview.CircleImageView;

public class ExploreActivity extends AppCompatActivity {

    private EditText searchEditText;
    private CircleImageView profileImage;

    private LinearLayout catMissed, catMatch, catHelp, catAchievements;
    private CardView cardMissed, cardMatch, cardHelp, cardAchieve;
    private ImageView iconMissed, iconMatch, iconHelp, iconAchieve;
    private TextView labelMissed, labelMatch, labelHelp, labelAchieve;

    private BottomNavigationView bottomNav;

    private static final int CAT_MISSED  = 0;
    private static final int CAT_MATCH   = 1;
    private static final int CAT_HELP    = 2;
    private static final int CAT_ACHIEVE = 3;

    private int selectedCategory = CAT_MISSED;


    private static final int COLOR_ACTIVE_MISSED  = Color.parseColor("#FF8C42"); // orange
    private static final int COLOR_ACTIVE_MATCH   = Color.parseColor("#4F6EF7"); // accent blue
    private static final int COLOR_ACTIVE_HELP    = Color.parseColor("#2DC97E"); // green
    private static final int COLOR_ACTIVE_ACHIEVE = Color.parseColor("#F5A623"); // gold

    private static final int COLOR_INACTIVE_BG    = Color.parseColor("#2A3050"); // dark chip
    private static final int COLOR_INACTIVE_ICON  = Color.parseColor("#8892B8"); // muted icon
    private static final int COLOR_INACTIVE_LABEL = Color.parseColor("#8892B8"); // muted label
    private static final int COLOR_ACTIVE_LABEL   = Color.parseColor("#FFFFFF");
    private static final int COLOR_ACTIVE_ICON    = Color.WHITE;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.explore);
        setNavyStatusBar();

        AppwriteService.init(this);
        initViews();
        loadUserProfile();
        setupSearchForwarding();
        setupCategoryListeners();
        setupBottomNavigation();
        setupBackPress();

        selectedCategory = CAT_MISSED;
        updateCategoryVisualState();
        replaceFragmentSafely(MissedFragment.newInstance());
    }


    private void setNavyStatusBar() {
        Window window = getWindow();

        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(Color.parseColor("#1A1F3C"));


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.setSystemBarsAppearance(
                        0,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                );
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View decorView = window.getDecorView();
            int flags = decorView.getSystemUiVisibility();
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            decorView.setSystemUiVisibility(flags);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        setNavyStatusBar();
        if (bottomNav != null) bottomNav.setSelectedItemId(R.id.nav_explore);
        checkIfUserIsBlocked();
    }


    private void initViews() {
        searchEditText  = safeFindViewById(R.id.searchEditText);
        profileImage    = safeFindViewById(R.id.profileImage);

        if (profileImage != null)
            profileImage.setOnClickListener(v -> safeStartActivity(ProfileActivity.class));

        catMissed      = safeFindViewById(R.id.categoryMissedReports);
        catMatch       = safeFindViewById(R.id.categoryMatchFace);
        catHelp        = safeFindViewById(R.id.categoryYourHelps);
        catAchievements = safeFindViewById(R.id.categoryAchivements);

        cardMissed  = safeFindViewById(R.id.cardMissed);
        cardMatch   = safeFindViewById(R.id.cardMatch);
        cardHelp    = safeFindViewById(R.id.cardHelp);
        cardAchieve = safeFindViewById(R.id.cardAchieve);

        iconMissed  = safeFindViewById(R.id.iconMissed);
        iconMatch   = safeFindViewById(R.id.iconMatch);
        iconHelp    = safeFindViewById(R.id.iconHelp);
        iconAchieve = safeFindViewById(R.id.iconAchieve);

        labelMissed  = safeFindViewById(R.id.labelMissed);
        labelMatch   = safeFindViewById(R.id.labelMatch);
        labelHelp    = safeFindViewById(R.id.labelHelp);
        labelAchieve = safeFindViewById(R.id.labelAchieve);

        bottomNav = safeFindViewById(R.id.bottomNav);
    }


    private void checkIfUserIsBlocked() {
        SharedPreferences prefs =
                getSharedPreferences("hoppe_prefs", MODE_PRIVATE);
        String userId = prefs.getString("logged_in_user_id", null);
        if (userId == null) return;

        new Thread(() -> {
            try {
                Document<?> doc = AppwriteHelper.getDocument(
                        AppwriteService.getDatabases(),
                        AppwriteService.DB_ID,
                        AppwriteService.COL_USERS, userId);

                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) doc.getData();
                String status = data.get("status") != null
                        ? data.get("status").toString() : "active";

                if ("suspended".equals(status)) {
                    runOnUiThread(() -> {
                        prefs.edit().clear().apply();
                        new Thread(() -> {
                            try {
                                AppwriteHelper.deleteCurrentSession(
                                        AppwriteService.getAccount());
                            } catch (Exception ignored) {}
                        }).start();
                        new androidx.appcompat.app.AlertDialog.Builder(this)
                                .setTitle("Account Suspended")
                                .setMessage("Your account has been suspended.")
                                .setCancelable(false)
                                .setPositiveButton("OK", (d, w) -> {
                                    Intent i = new Intent(this, LoginActivity.class);
                                    i.putExtra("explicit_login", true);
                                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                            | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(i);
                                    finish();
                                })
                                .show();
                    });
                }
            } catch (Exception e) {
                Log.w("ExploreActivity", "checkBlocked: " + e.getMessage());
            }
        }).start();
    }

    private void loadUserProfile() {
        SharedPreferences prefs = getSharedPreferences("hoppe_prefs", MODE_PRIVATE);
        String userId = prefs.getString("logged_in_user_id", null);
        if (userId == null) return;

        new Thread(() -> {
            try {
                Document<?> doc = AppwriteHelper.getDocument(
                        AppwriteService.getDatabases(),
                        AppwriteService.DB_ID,
                        AppwriteService.COL_USERS,
                        userId);

                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) doc.getData();
                Object photoId = data.get("photoId");

                runOnUiThread(() -> {
                    if (photoId != null && profileImage != null) {
                        String url = AppwriteService.ENDPOINT
                                + "/storage/buckets/"
                                + AppwriteService.USERS_BUCKET_ID
                                + "/files/" + photoId
                                + "/view?project=" + AppwriteService.PROJECT_ID;

                        Glide.with(ExploreActivity.this)
                                .load(url)
                                .placeholder(R.drawable.ic_person)
                                .error(R.drawable.ic_person)
                                .circleCrop()
                                .into(profileImage);
                    }
                });

            } catch (Exception e) {
                Log.e("ExploreActivity", "Profile load error", e);
            }
        }).start();
    }


    private void replaceFragmentSafely(androidx.fragment.app.Fragment fragment) {
        try {
            if (fragment == null) return;
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .commitAllowingStateLoss();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void setupSearchForwarding() {
        if (searchEditText == null) return;
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override
            public void onTextChanged(CharSequence s, int st, int b, int c) {
                androidx.fragment.app.Fragment current =
                        getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
                if (current instanceof SearchableFragment)
                    ((SearchableFragment) current).onSearch(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupCategoryListeners() {
        if (catMissed != null) catMissed.setOnClickListener(v -> {
            selectedCategory = CAT_MISSED;
            replaceFragmentSafely(MissedFragment.newInstance());
            updateCategoryVisualState();
        });
        if (catMatch != null) catMatch.setOnClickListener(v -> {
            selectedCategory = CAT_MATCH;
            replaceFragmentSafely(MatchFragment.newInstance());
            updateCategoryVisualState();
        });
        if (catHelp != null) catHelp.setOnClickListener(v -> {
            selectedCategory = CAT_HELP;
            replaceFragmentSafely(HelpFragment.newInstance());
            updateCategoryVisualState();
        });
        if (catAchievements != null) catAchievements.setOnClickListener(v -> {
            selectedCategory = CAT_ACHIEVE;
            replaceFragmentSafely(AchievementFragment.newInstance());
            updateCategoryVisualState();
        });
    }

    private void setupBottomNavigation() {
        if (bottomNav == null) return;
        try { bottomNav.setSelectedItemId(R.id.nav_explore); } catch (Exception ignored) {}

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_explore)      { return true; }
            if (id == R.id.nav_profile)      { safeStartActivity(ProfileActivity.class); return true; }
            if (id == R.id.nav_new_report)   { safeStartActivity(NewReportActivity.class); return true; }
            if (id == R.id.nav_chat)         { safeStartActivity(ChatsActivity.class); return true; }
            if (id == R.id.nav_home) {
                Intent i = new Intent(this, MainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(i);
                return true;
            }
            return false;
        });
    }


    private void setupBackPress() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent i = new Intent(ExploreActivity.this, MainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(i);
            }
        });
    }


    private void updateCategoryVisualState() {
        int[] cards  = {0, 0, 0, 0};
        CardView[]  cvs    = {cardMissed, cardMatch, cardHelp, cardAchieve};
        ImageView[] icons  = {iconMissed, iconMatch, iconHelp, iconAchieve};
        TextView[]  labels = {labelMissed, labelMatch, labelHelp, labelAchieve};

        for (int i = 0; i < cvs.length; i++) {
            if (cvs[i]    != null) cvs[i].setCardBackgroundColor(COLOR_INACTIVE_BG);
            if (icons[i]  != null) icons[i].setColorFilter(COLOR_INACTIVE_ICON);
            if (labels[i] != null) labels[i].setTextColor(COLOR_INACTIVE_LABEL);
        }

        int activeBg;
        switch (selectedCategory) {
            case CAT_MISSED:  activeBg = COLOR_ACTIVE_MISSED;  break;
            case CAT_MATCH:   activeBg = COLOR_ACTIVE_MATCH;   break;
            case CAT_HELP:    activeBg = COLOR_ACTIVE_HELP;    break;
            case CAT_ACHIEVE: activeBg = COLOR_ACTIVE_ACHIEVE; break;
            default:          activeBg = COLOR_ACTIVE_MISSED;  break;
        }

        if (cvs[selectedCategory]    != null) cvs[selectedCategory].setCardBackgroundColor(activeBg);
        if (icons[selectedCategory]  != null) icons[selectedCategory].setColorFilter(COLOR_ACTIVE_ICON);
        if (labels[selectedCategory] != null) labels[selectedCategory].setTextColor(COLOR_ACTIVE_LABEL);
    }


    private <T extends View> T safeFindViewById(int id) {
        try { return findViewById(id); }
        catch (Exception e) { return null; }
    }

    private void safeStartActivity(Class<?> cls) {
        try { startActivity(new Intent(this, cls)); }
        catch (Exception e) {
            Toast.makeText(this, "Feature not available", Toast.LENGTH_SHORT).show();
        }
    }
}