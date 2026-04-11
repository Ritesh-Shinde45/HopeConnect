package com.ritesh.hoppeconnect;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowInsetsController;
import android.widget.EditText;
import android.widget.ImageButton;
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

    private static final int CAT_MISSED = 0;
    private static final int CAT_MATCH = 1;
    private static final int CAT_HELP = 2;
    private static final int CAT_ACHIEVE = 3;

    private int selectedCategory = CAT_MISSED;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.explore);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            getWindow().getInsetsController().setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            );
        }

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

    // ── Back press → go to MainActivity ──────────────────────────────────────
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

    private void initViews() {
        searchEditText = safeFindViewById(R.id.searchEditText);
        profileImage = safeFindViewById(R.id.profileImage);

        if (profileImage != null) {
            profileImage.setOnClickListener(v -> safeStartActivity(ProfileActivity.class));
        }

        catMissed = safeFindViewById(R.id.categoryMissedReports);
        catMatch = safeFindViewById(R.id.categoryMatchFace);
        catHelp = safeFindViewById(R.id.categoryYourHelps);
        catAchievements = safeFindViewById(R.id.categoryAchivements);

        cardMissed = safeFindViewById(R.id.cardMissed);
        cardMatch = safeFindViewById(R.id.cardMatch);
        cardHelp = safeFindViewById(R.id.cardHelp);
        cardAchieve = safeFindViewById(R.id.cardAchieve);

        iconMissed = safeFindViewById(R.id.iconMissed);
        iconMatch = safeFindViewById(R.id.iconMatch);
        iconHelp = safeFindViewById(R.id.iconHelp);
        iconAchieve = safeFindViewById(R.id.iconAchieve);

        labelMissed = safeFindViewById(R.id.labelMissed);
        labelMatch = safeFindViewById(R.id.labelMatch);
        labelHelp = safeFindViewById(R.id.labelHelp);
        labelAchieve = safeFindViewById(R.id.labelAchieve);

        bottomNav = safeFindViewById(R.id.bottomNav);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNav != null) bottomNav.setSelectedItemId(R.id.nav_explore);
        checkIfUserIsBlocked();
    }

    private void checkIfUserIsBlocked() {
        SharedPreferences prefs =
                getSharedPreferences("hoppe_prefs", MODE_PRIVATE);
        String userId = prefs.getString("logged_in_user_id", null);
        if (userId == null) return;

        new Thread(() -> {
            try {
                io.appwrite.models.Document<?> doc =
                        AppwriteHelper.getDocument(
                                AppwriteService.getDatabases(),
                                AppwriteService.DB_ID,
                                AppwriteService.COL_USERS, userId);
                @SuppressWarnings("unchecked")
                Map<String, Object> data =
                        (Map<String, Object>) doc.getData();
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
                                + "/files/"
                                + photoId
                                + "/view?project="
                                + AppwriteService.PROJECT_ID;

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

    private <T extends View> T safeFindViewById(int id) {
        try {
            return findViewById(id);
        } catch (Exception e) {
            return null;
        }
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
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                androidx.fragment.app.Fragment current =
                        getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
                if (current instanceof SearchableFragment) {
                    ((SearchableFragment) current).onSearch(s.toString());
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupCategoryListeners() {
        if (catMissed != null) {
            catMissed.setOnClickListener(v -> {
                selectedCategory = CAT_MISSED;
                replaceFragmentSafely(MissedFragment.newInstance());
                updateCategoryVisualState();
            });
        }

        if (catMatch != null) {
            catMatch.setOnClickListener(v -> {
                selectedCategory = CAT_MATCH;
                replaceFragmentSafely(MatchFragment.newInstance());
                updateCategoryVisualState();
            });
        }

        if (catHelp != null) {
            catHelp.setOnClickListener(v -> {
                selectedCategory = CAT_HELP;
                replaceFragmentSafely(HelpFragment.newInstance());
                updateCategoryVisualState();
            });
        }

        if (catAchievements != null) {
            catAchievements.setOnClickListener(v -> {
                selectedCategory = CAT_ACHIEVE;
                replaceFragmentSafely(AchievementFragment.newInstance());
                updateCategoryVisualState();
            });
        }
    }

    private void setupBottomNavigation() {
        if (bottomNav == null) return;

        try {
            bottomNav.setSelectedItemId(R.id.nav_explore);
        } catch (Exception ignored) {}

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_explore) {
                return true;
            } else if (itemId == R.id.nav_profile) {
                safeStartActivity(ProfileActivity.class);
                return true;
            } else if (itemId == R.id.nav_new_report) {
                safeStartActivity(NewReportActivity.class);
                return true;
            } else if (itemId == R.id.nav_chat) {
                safeStartActivity(ChatsActivity.class);
                return true;
            } else if (itemId == R.id.nav_home) {
                Intent i = new Intent(this, MainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(i);
                return true;
            }

            return false;
        });
    }

    private void safeStartActivity(Class<?> cls) {
        try {
            Intent i = new Intent(this, cls);
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "Feature not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateCategoryVisualState() {
        int inactiveBg        = Color.parseColor("#F1F3F6");
        int inactiveIconTint  = Color.parseColor("#AAAAAA");
        int inactiveLabel     = Color.parseColor("#444444");

        int activeBg          = Color.parseColor("#FFA726");
        int activeIconTint    = Color.WHITE;
        int activeLabel       = Color.parseColor("#222222");

        if (cardMissed  != null) cardMissed.setCardBackgroundColor(inactiveBg);
        if (cardMatch   != null) cardMatch.setCardBackgroundColor(inactiveBg);
        if (cardHelp    != null) cardHelp.setCardBackgroundColor(inactiveBg);
        if (cardAchieve != null) cardAchieve.setCardBackgroundColor(inactiveBg);

        if (iconMissed  != null) iconMissed.setColorFilter(inactiveIconTint);
        if (iconMatch   != null) iconMatch.setColorFilter(inactiveIconTint);
        if (iconHelp    != null) iconHelp.setColorFilter(inactiveIconTint);
        if (iconAchieve != null) iconAchieve.setColorFilter(inactiveIconTint);

        if (labelMissed  != null) labelMissed.setTextColor(inactiveLabel);
        if (labelMatch   != null) labelMatch.setTextColor(inactiveLabel);
        if (labelHelp    != null) labelHelp.setTextColor(inactiveLabel);
        if (labelAchieve != null) labelAchieve.setTextColor(inactiveLabel);

        switch (selectedCategory) {
            case CAT_MISSED:
                if (cardMissed  != null) cardMissed.setCardBackgroundColor(activeBg);
                if (iconMissed  != null) iconMissed.setColorFilter(activeIconTint);
                if (labelMissed != null) labelMissed.setTextColor(activeLabel);
                break;
            case CAT_MATCH:
                if (cardMatch  != null) cardMatch.setCardBackgroundColor(activeBg);
                if (iconMatch  != null) iconMatch.setColorFilter(activeIconTint);
                if (labelMatch != null) labelMatch.setTextColor(activeLabel);
                break;
            case CAT_HELP:
                if (cardHelp  != null) cardHelp.setCardBackgroundColor(activeBg);
                if (iconHelp  != null) iconHelp.setColorFilter(activeIconTint);
                if (labelHelp != null) labelHelp.setTextColor(activeLabel);
                break;
            case CAT_ACHIEVE:
                if (cardAchieve  != null) cardAchieve.setCardBackgroundColor(activeBg);
                if (iconAchieve  != null) iconAchieve.setColorFilter(activeIconTint);
                if (labelAchieve != null) labelAchieve.setTextColor(activeLabel);
                break;
        }
    }
}