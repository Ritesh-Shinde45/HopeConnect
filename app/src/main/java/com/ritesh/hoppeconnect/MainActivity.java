package com.ritesh.hoppeconnect;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.appwrite.coroutines.CoroutineCallback;
import io.appwrite.models.Document;
import io.appwrite.models.DocumentList;

public class MainActivity extends AppCompatActivity {

    private static final String TAG  = "MainActivity";
    private static final String PREFS    = "hoppe_prefs";
    private static final String KEY_UID  = "logged_in_user_id";
    private static final String KEY_ROLE = "logged_in_role";

    private ActivityResultLauncher<String[]> permissionLauncher;

    // UI
    private MaterialButton btnAllCases, btnMissing, btnFound,
            btnChildren, btnAdults, btnElderly;
    private MaterialButton selectedFilterButton;
    private BottomNavigationView bottomNav;
    private EditText searchBar;
    private Button btnReadMore;
    private TextView txtMissingCases;
    private ShapeableImageView profileImage;
    private TextView userName;
    private ImageButton btnNotification;
    private ProgressBar homeProgressBar;

    private RecyclerView casesRecyclerView;
    private ReportAdapter caseAdapter;
    private final List<ReportModel> caseList = new ArrayList<>();

    private String storagePermission;
    private long backPressedTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppwriteService.init(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView()
                    .setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        storagePermission = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String uid  = prefs.getString(KEY_UID, null);
        String role = prefs.getString(KEY_ROLE, "user");

        if (uid == null) {
            Intent i = new Intent(this, LoginActivity.class);
            i.putExtra("explicit_login", true);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
            return;
        }

        if ("admin".equals(role)) {
            Intent i = new Intent(this, AdminDashboardActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
            return;
        }

        proceedToInitUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNav != null) bottomNav.setSelectedItemId(R.id.nav_home);
        loadUserProfile();
    }

    private void proceedToInitUI() {
        setContentView(R.layout.activity_main);
        registerPermissionLauncher();
        initializeViews();
        setupCasesRecyclerView();
        setupFilterButtons();
        setupBottomNavigation();
        setupSearchBar();
        setupPromoCard();
        setupBackPress();
        loadUserProfile();
        // ── Only load APPROVED (active) reports on home screen ──
        loadApprovedCases("all", null);
    }

    private void initializeViews() {
        btnAllCases = findViewById(R.id.btnAllCases);
        btnMissing  = findViewById(R.id.btnMissing);
        btnFound    = findViewById(R.id.btnFound);
        btnChildren = findViewById(R.id.btnChildren);
        btnAdults   = findViewById(R.id.btnAdults);
        btnElderly  = findViewById(R.id.btnElderly);

        bottomNav       = findViewById(R.id.bottomNav);
        searchBar       = findViewById(R.id.searchBar);
        btnReadMore     = findViewById(R.id.btnReadMore);
        txtMissingCases = findViewById(R.id.txtMissingCases);
        profileImage    = findViewById(R.id.profileImage);
        userName        = findViewById(R.id.userName);
        btnNotification = findViewById(R.id.btnNotification);
        homeProgressBar = findViewById(R.id.homeProgressBar);
        casesRecyclerView = findViewById(R.id.casesRecyclerView);

        selectedFilterButton = btnAllCases;

        TextView txtSeeAll      = findViewById(R.id.txtSeeAll);
        TextView txtSeeAllCases = findViewById(R.id.txtSeeAllCases);
        if (txtSeeAll != null)
            txtSeeAll.setOnClickListener(v -> openExplore());
        if (txtSeeAllCases != null)
            txtSeeAllCases.setOnClickListener(v -> openExplore());

        if (btnNotification != null)
            btnNotification.setOnClickListener(v ->
                    startActivity(new Intent(this, NotificationActivity.class)));
    }

    private void setupCasesRecyclerView() {
        if (casesRecyclerView == null) return;
        casesRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        caseAdapter = new ReportAdapter(this, caseList, model -> {
            ReportModelCache.put(model);
            Intent intent = new Intent(this, MissedPersonDetailActivity.class);
            intent.putExtra(MissedPersonDetailActivity.EXTRA_REPORT_ID, model.id);
            startActivity(intent);
        });
        casesRecyclerView.setAdapter(caseAdapter);
    }

    private void openExplore() {
        Intent i = new Intent(this, ExploreActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(i);
    }

    private void setupBackPress() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                long now = System.currentTimeMillis();
                if (now - backPressedTime < 2000) {
                    finishAffinity();
                } else {
                    backPressedTime = now;
                    Toast.makeText(MainActivity.this,
                            "Press back again to exit", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void registerPermissionLauncher() {
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> Log.d(TAG, "permissions: " + result));
    }

    private void loadUserProfile() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String userId = prefs.getString(KEY_UID, null);

        String cached = prefs.getString("user_display_name", "");
        if (!cached.isEmpty() && userName != null)
            userName.setText("Hi, " + cached);

        if (userId == null) return;

        new Thread(() -> {
            try {
                Document<?> doc;
                try {
                    doc = AppwriteHelper.getDocument(
                            AppwriteService.getDatabases(),
                            AppwriteService.DB_ID,
                            AppwriteService.COL_USERS,
                            userId);
                } catch (Exception e) {
                    doc = AppwriteHelper.getDocument(
                            AppwriteService.getDatabases(),
                            AppwriteService.DB_ID,
                            AppwriteService.COL_ADMINS,
                            userId);
                }

                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) doc.getData();
                String name = data.get("name") != null ? data.get("name").toString() : "User";
                prefs.edit().putString("user_display_name", name).apply();

                runOnUiThread(() -> {
                    if (userName != null) userName.setText("Hi, " + name);

                    Object photoId = data.get("photoId");
                    if (photoId != null && profileImage != null) {
                        String url = AppwriteService.ENDPOINT
                                + "/storage/buckets/"
                                + AppwriteService.USERS_BUCKET_ID
                                + "/files/" + photoId
                                + "/view?project=" + AppwriteService.PROJECT_ID;
                        Glide.with(MainActivity.this)
                                .load(url)
                                .circleCrop()
                                .placeholder(R.drawable.profile_placeholder)
                                .error(R.drawable.profile_placeholder)
                                .into(profileImage);
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "loadUserProfile: " + e.getMessage(), e);
            }
        }).start();
    }

    /**
     * Loads ONLY approved (status = "active") or found reports.
     * filterType: "all"      → active + found (all approved)
     *             "missing"  → active only
     *             "found"    → found only
     *             "ageGroup" → approved + age filter
     */
    private void loadApprovedCases(String filterType, String filterValue) {
        if (homeProgressBar != null) homeProgressBar.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                List<String> queries = new ArrayList<>();
                queries.add(io.appwrite.Query.Companion.orderDesc("$createdAt"));
                queries.add(io.appwrite.Query.Companion.limit(20));

                // ── Status filter — always restrict to approved reports ──
                if ("missing".equals(filterType)) {
                    // Active (approved missing) only
                    queries.add(io.appwrite.Query.Companion.equal("status", "active"));
                } else if ("found".equals(filterType)) {
                    // Found only
                    queries.add(io.appwrite.Query.Companion.equal("status", "found"));
                } else if ("ageGroup".equals(filterType) && filterValue != null) {
                    // Approved reports in this age group
                    queries.add(io.appwrite.Query.Companion.equal("status", "active"));
                    switch (filterValue) {
                        case "Children":
                            queries.add(io.appwrite.Query.Companion.lessThanEqual("age", 17));
                            break;
                        case "Adults":
                            queries.add(io.appwrite.Query.Companion.between("age", 18, 59));
                            break;
                        case "Elderly":
                            queries.add(io.appwrite.Query.Companion.greaterThanEqual("age", 60));
                            break;
                    }
                } else {
                    // "all" — show both active and found (approved reports only)
                    // Appwrite free plan doesn't support OR queries easily,
                    // so we fetch active reports — found reports show in "Found" tab
                    queries.add(io.appwrite.Query.Companion.equal("status", "active"));
                }

                AppwriteService.getDatabases().listDocuments(
                        AppwriteService.DB_ID,
                        AppwriteService.COL_REPORTS,
                        queries,
                        new CoroutineCallback<DocumentList<Map<String, Object>>>(
                                (result, error) -> {
                                    runOnUiThread(() -> {
                                        if (homeProgressBar != null)
                                            homeProgressBar.setVisibility(View.GONE);
                                    });
                                    if (error != null) {
                                        Log.e(TAG, "loadApprovedCases error: "
                                                + error.getMessage());
                                        return;
                                    }
                                    List<ReportModel> fresh = new ArrayList<>();
                                    for (io.appwrite.models.Document<Map<String, Object>> doc
                                            : result.getDocuments()) {
                                        ReportModel m = MissedFragment.parseDocument(
                                                doc.getId(), doc.getData());
                                        // Double-check status in memory
                                        // (in case Appwrite index is stale)
                                        if ("active".equals(m.status)
                                                || "found".equals(m.status)) {
                                            fresh.add(m);
                                        }
                                    }
                                    runOnUiThread(() -> {
                                        caseList.clear();
                                        caseList.addAll(fresh);
                                        if (caseAdapter != null)
                                            caseAdapter.notifyDataSetChanged();
                                        if (fresh.isEmpty() && txtMissingCases != null) {
                                            Log.d(TAG, "No approved cases found");
                                        }
                                    });
                                })
                );
            } catch (Exception e) {
                Log.e(TAG, "loadApprovedCases exception: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    if (homeProgressBar != null)
                        homeProgressBar.setVisibility(View.GONE);
                });
            }
        }).start();
    }

    private void setupSearchBar() {
        if (searchBar == null) return;
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String q = s.toString().trim();
                if (q.isEmpty()) loadApprovedCases("all", null);
                else searchApprovedCases(q);
            }
        });
    }

    /**
     * Searches only within approved reports.
     */
    private void searchApprovedCases(String query) {
        new Thread(() -> {
            try {
                List<String> queries = new ArrayList<>();
                queries.add(io.appwrite.Query.Companion.search("name", query));
                queries.add(io.appwrite.Query.Companion.equal("status", "active"));
                queries.add(io.appwrite.Query.Companion.limit(20));

                AppwriteService.getDatabases().listDocuments(
                        AppwriteService.DB_ID,
                        AppwriteService.COL_REPORTS,
                        queries,
                        new CoroutineCallback<DocumentList<Map<String, Object>>>(
                                (result, error) -> {
                                    if (error != null) {
                                        Log.e(TAG, "searchApprovedCases error: "
                                                + error.getMessage());
                                        return;
                                    }
                                    List<ReportModel> found = new ArrayList<>();
                                    for (io.appwrite.models.Document<Map<String, Object>> doc
                                            : result.getDocuments()) {
                                        ReportModel m = MissedFragment.parseDocument(
                                                doc.getId(), doc.getData());
                                        if ("active".equals(m.status)
                                                || "found".equals(m.status)) {
                                            found.add(m);
                                        }
                                    }
                                    runOnUiThread(() -> {
                                        caseList.clear();
                                        caseList.addAll(found);
                                        if (caseAdapter != null)
                                            caseAdapter.notifyDataSetChanged();
                                    });
                                })
                );
            } catch (Exception e) {
                Log.e(TAG, "searchApprovedCases: " + e.getMessage(), e);
            }
        }).start();
    }

    private void setupFilterButtons() {
        List<MaterialButton> all = new ArrayList<>();
        all.add(btnAllCases); all.add(btnMissing); all.add(btnFound);
        all.add(btnChildren); all.add(btnAdults);  all.add(btnElderly);

        btnAllCases.setOnClickListener(v -> {
            selectFilter(btnAllCases, all);
            if (txtMissingCases != null) txtMissingCases.setText("Approved Cases");
            loadApprovedCases("all", null);
        });
        btnMissing.setOnClickListener(v -> {
            selectFilter(btnMissing, all);
            if (txtMissingCases != null) txtMissingCases.setText("Active Missing Cases");
            loadApprovedCases("missing", null);
        });
        btnFound.setOnClickListener(v -> {
            selectFilter(btnFound, all);
            if (txtMissingCases != null) txtMissingCases.setText("Found Cases");
            loadApprovedCases("found", null);
        });
        btnChildren.setOnClickListener(v -> {
            selectFilter(btnChildren, all);
            if (txtMissingCases != null) txtMissingCases.setText("Children Cases");
            loadApprovedCases("ageGroup", "Children");
        });
        btnAdults.setOnClickListener(v -> {
            selectFilter(btnAdults, all);
            if (txtMissingCases != null) txtMissingCases.setText("Adults Cases");
            loadApprovedCases("ageGroup", "Adults");
        });
        btnElderly.setOnClickListener(v -> {
            selectFilter(btnElderly, all);
            if (txtMissingCases != null) txtMissingCases.setText("Elderly Cases");
            loadApprovedCases("ageGroup", "Elderly");
        });
    }

    private void selectFilter(MaterialButton selected, List<MaterialButton> all) {
        for (MaterialButton b : all) {
            if (b == null) continue;
            b.setBackgroundColor(Color.WHITE);
            b.setTextColor(ContextCompat.getColor(this, R.color.filter_text_unselected));
            b.setStrokeColor(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.filter_border)));
            b.setIconTint(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.filter_icon_unselected)));
        }
        if (selected == null) return;
        selected.setBackgroundColor(
                ContextCompat.getColor(this, R.color.filter_bg_selected));
        selected.setTextColor(
                ContextCompat.getColor(this, R.color.filter_text_selected));
        selected.setStrokeColor(ColorStateList.valueOf(Color.TRANSPARENT));
        selected.setIconTint(ColorStateList.valueOf(
                selected.getId() == R.id.btnAllCases
                        ? ContextCompat.getColor(this, R.color.fire_orange)
                        : ContextCompat.getColor(this, R.color.filter_icon_selected)));
        selectedFilterButton = selected;
    }

    private void setupBottomNavigation() {
        if (bottomNav == null) return;
        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) return true;

            Intent i = null;
            if      (id == R.id.nav_explore)   i = new Intent(this, ExploreActivity.class);
            else if (id == R.id.nav_chat)       i = new Intent(this, ChatsActivity.class);
            else if (id == R.id.nav_profile)    i = new Intent(this, ProfileActivity.class);
            else if (id == R.id.nav_new_report) {
                startActivity(new Intent(this, NewReportActivity.class));
                return true;
            }

            if (i != null) {
                i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(i);
            }
            return true;
        });
    }

    private void setupPromoCard() {
        if (btnReadMore != null)
            btnReadMore.setOnClickListener(v ->
                    startActivity(new Intent(this, TechnologyWithHumanityActivity.class)));
    }
}