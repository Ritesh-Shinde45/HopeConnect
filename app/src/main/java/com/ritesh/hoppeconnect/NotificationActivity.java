package com.ritesh.hoppeconnect;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.appwrite.models.Document;
import io.appwrite.services.Databases;

public class NotificationActivity extends AppCompatActivity {

    private static final String TAG   = "NotificationsActivity";
    private static final String PREFS = "hoppe_prefs";
    private static final String COL_ANNOUNCEMENTS = "announcements";

    private RecyclerView rv;
    private ProgressBar progress;
    private LinearLayout emptyState;
    private TextView tvUnreadCount, btnMarkAllRead;
    private TabLayout tabLayout;

    private NotificationAdapter adapter;

    // All loaded data separated by type
    private final List<NotificationModel> allAnnouncements = new ArrayList<>();
    private final List<NotificationModel> allReports       = new ArrayList<>();
    private final List<NotificationModel> myReports        = new ArrayList<>();

    // Currently displayed
    private final List<NotificationModel> displayed = new ArrayList<>();

    private int currentTab = 0; // 0=Announcements, 1=Reports, 2=My Reports
    private String myUserId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView()
                    .setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
        AppwriteService.init(this);

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        myUserId = prefs.getString("logged_in_user_id", "");

        rv             = findViewById(R.id.rvNotifications);
        progress       = findViewById(R.id.progressNotif);
        emptyState     = findViewById(R.id.emptyState);
        tvUnreadCount  = findViewById(R.id.tvUnreadCount);
        btnMarkAllRead = findViewById(R.id.btnMarkAllRead);
        tabLayout      = findViewById(R.id.tabLayout);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Tabs
        tabLayout.addTab(tabLayout.newTab().setText("📢 Announcements"));
        tabLayout.addTab(tabLayout.newTab().setText("🔍 Reports"));
        tabLayout.addTab(tabLayout.newTab().setText("👤 Mine"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                applyTab();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter(this, displayed, notif -> {
            notif.isRead = true;
            adapter.notifyDataSetChanged();
            updateUnreadCount();
            // Open report detail for report-type notifications
            if (notif.type == NotificationModel.TYPE_REPORT
                    || notif.type == NotificationModel.TYPE_MY_REPORT) {
                android.content.Intent i =
                        new android.content.Intent(this, MissedPersonDetailActivity.class);
                i.putExtra(MissedPersonDetailActivity.EXTRA_REPORT_ID, notif.id);
                startActivity(i);
            }
        });
        rv.setAdapter(adapter);

        btnMarkAllRead.setOnClickListener(v -> markAllRead());

        loadAll();
    }

    // ── Load all three categories in parallel ─────────────────────────────────

    private void loadAll() {
        progress.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);

        new Thread(() -> {
            try {
                Databases db = AppwriteService.getDatabases();

                // 1. Announcements
                loadAnnouncements(db);

                // 2. Recent approved reports
                loadApprovedReports(db);

                // 3. My reports — check for status updates (found)
                if (!myUserId.isEmpty()) {
                    loadMyReports(db);
                }

                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    applyTab();
                });

            } catch (Exception e) {
                Log.e(TAG, "loadAll error: " + e.getMessage(), e);
                runOnUiThread(() -> progress.setVisibility(View.GONE));
            }
        }).start();
    }

    private void loadAnnouncements(Databases db) {
        try {
            List<? extends Document<?>> docs =
                    AppwriteHelper.listDocuments(
                            db,
                            AppwriteService.DB_ID,
                            COL_ANNOUNCEMENTS,
                            Arrays.asList(
                                    io.appwrite.Query.Companion.orderDesc("$createdAt"),
                                    io.appwrite.Query.Companion.limit(30)
                            )
                    ).getDocuments();

            for (Document<?> doc : docs) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) doc.getData();
                String title    = s(data, "title",    "Announcement");
                String message  = s(data, "message",  "");
                String audience = s(data, "audience", "All Users");
                String sentAt   = s(data, "sentAt",   "");

                allAnnouncements.add(new NotificationModel(
                        doc.getId(),
                        title,
                        message + (audience.isEmpty() ? "" : "  •  To: " + audience),
                        sentAt,
                        "",
                        false,
                        NotificationModel.TYPE_ANNOUNCEMENT
                ));
            }
        } catch (Exception e) {
            Log.w(TAG, "loadAnnouncements: " + e.getMessage());
            // Collection may not exist yet — that's fine
        }
    }

    private void loadApprovedReports(Databases db) {
        try {
            List<? extends Document<?>> docs =
                    AppwriteHelper.listDocuments(
                            db,
                            AppwriteService.DB_ID,
                            AppwriteService.COL_REPORTS,
                            Arrays.asList(
                                    io.appwrite.Query.Companion.equal("status", "active"),
                                    io.appwrite.Query.Companion.orderDesc("$createdAt"),
                                    io.appwrite.Query.Companion.limit(50)
                            )
                    ).getDocuments();

            for (Document<?> doc : docs) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) doc.getData();

                String name     = s(data, "name",     "Unknown");
                String location = s(data, "location", "");
                String gender   = s(data, "gender",   "");
                String iso      = doc.getCreatedAt();

                String body = name
                        + (gender.isEmpty()   ? "" : " • " + gender)
                        + (location.isEmpty() ? "" : " • " + location);

                String photoUrl = extractPhoto(data);

                allReports.add(new NotificationModel(
                        doc.getId(),
                        "New approved missing person",
                        body,
                        formatTime(iso),
                        photoUrl,
                        false,
                        NotificationModel.TYPE_REPORT
                ));
            }
        } catch (Exception e) {
            Log.w(TAG, "loadApprovedReports: " + e.getMessage());
        }
    }

    private void loadMyReports(Databases db) {
        try {
            // Fetch reports uploaded by the current user
            List<? extends Document<?>> docs =
                    AppwriteHelper.listDocuments(
                            db,
                            AppwriteService.DB_ID,
                            AppwriteService.COL_REPORTS,
                            Arrays.asList(
                                    io.appwrite.Query.Companion.equal("userId", myUserId),
                                    io.appwrite.Query.Companion.orderDesc("$createdAt"),
                                    io.appwrite.Query.Companion.limit(50)
                            )
                    ).getDocuments();

            for (Document<?> doc : docs) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) doc.getData();

                String name     = s(data, "name",     "Unknown");
                String status   = s(data, "status",   "pending");
                String location = s(data, "location", "");
                String iso      = doc.getCreatedAt();
                String photoUrl = extractPhoto(data);

                String title, body;
                boolean isUnread;

                switch (status) {
                    case "found":
                        // ── Person found — highlight this ──────────────────
                        title    = "🎉 " + name + " has been FOUND!";
                        body     = "Great news! The missing person you reported"
                                + (location.isEmpty() ? "" : " from " + location)
                                + " has been found.";
                        isUnread = true; // always show as unread
                        break;
                    case "active":
                        title    = "✅ Report approved";
                        body     = name + "'s report is now live and visible to the public.";
                        isUnread = false;
                        break;
                    case "fake":
                    case "rejected":
                        title    = "❌ Report flagged";
                        body     = name + "'s report was flagged as fake by an admin.";
                        isUnread = true;
                        break;
                    case "resolved":
                        title    = "✅ Case resolved";
                        body     = name + "'s case has been marked as resolved.";
                        isUnread = false;
                        break;
                    default: // pending
                        title    = "⏳ Report pending";
                        body     = name + "'s report is waiting for admin approval.";
                        isUnread = false;
                        break;
                }

                myReports.add(new NotificationModel(
                        doc.getId(),
                        title,
                        body,
                        formatTime(iso),
                        photoUrl,
                        !isUnread,
                        NotificationModel.TYPE_MY_REPORT
                ));
            }
        } catch (Exception e) {
            Log.w(TAG, "loadMyReports: " + e.getMessage());
        }
    }

    // ── Tab switching ──────────────────────────────────────────────────────────

    private void applyTab() {
        displayed.clear();
        switch (currentTab) {
            case 0: displayed.addAll(allAnnouncements); break;
            case 1: displayed.addAll(allReports);       break;
            case 2: displayed.addAll(myReports);        break;
        }
        adapter.notifyDataSetChanged();
        updateUnreadCount();

        boolean isEmpty = displayed.isEmpty();
        emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rv.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void markAllRead() {
        for (NotificationModel n : displayed) n.isRead = true;
        adapter.notifyDataSetChanged();
        updateUnreadCount();
    }

    private void updateUnreadCount() {
        int unread = 0;
        for (NotificationModel n : displayed) if (!n.isRead) unread++;
        tvUnreadCount.setText(unread == 0 ? "All caught up ✓" : unread + " unread");
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private String extractPhoto(Map<String, Object> data) {
        Object pf = data.get("photoUrls");
        if (pf instanceof List && !((List<?>) pf).isEmpty()) {
            String raw = ((List<?>) pf).get(0).toString();
            return raw.contains("project=")
                    ? raw
                    : raw + "?project=" + AppwriteService.PROJECT_ID;
        }
        return "";
    }

    private String formatTime(String iso) {
        if (iso == null || iso.length() < 10) return "—";
        try {
            SimpleDateFormat in  = new SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault());
            SimpleDateFormat out = new SimpleDateFormat(
                    "dd MMM, hh:mm a", Locale.getDefault());
            Date d = in.parse(iso);
            return d != null ? out.format(d) : iso.substring(0, 10);
        } catch (Exception e) {
            return iso.substring(0, 10);
        }
    }

    private String s(Map<String, Object> m, String key, String def) {
        Object v = m.get(key);
        return (v != null && !v.toString().isEmpty()) ? v.toString() : def;
    }
}