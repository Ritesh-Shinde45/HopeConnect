package com.ritesh.hoppeconnect;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import io.appwrite.models.Document;
import io.appwrite.services.Databases;

public class NotificationActivity extends AppCompatActivity {

    private static final String TAG               = "NotificationActivity";
    private static final String PREFS             = "hoppe_prefs";
    private static final String COL_ANNOUNCEMENTS = "announcements";

    private RecyclerView rv;
    private ProgressBar  progress;
    private LinearLayout emptyState;
    private TextView     tvUnreadCount, btnMarkAllRead;
    private TabLayout    tabLayout;

    private NotificationAdapter adapter;

    private final List<NotificationModel> allAnnouncements = new ArrayList<>();
    private final List<NotificationModel> allReports       = new ArrayList<>();
    private final List<NotificationModel> myReports        = new ArrayList<>();
    private final List<NotificationModel> displayed        = new ArrayList<>();

    private int    currentTab = 0;
    private String myUserId   = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        AppwriteService.init(this);

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        myUserId = prefs.getString("logged_in_user_id", "");

        rv             = findViewById(R.id.rvNotifications);
        progress       = findViewById(R.id.progressNotif);
        emptyState     = findViewById(R.id.emptyState);
        tvUnreadCount  = findViewById(R.id.tvUnreadCount);
        btnMarkAllRead = findViewById(R.id.btnMarkAllRead);
        tabLayout      = findViewById(R.id.tabLayout);

        // ── Back button → MainActivity ────────────────────────────────────────
        findViewById(R.id.btnBack).setOnClickListener(v -> goHome());

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

            String id = notif.id != null ? notif.id : "";

            if (id.startsWith("SIGHTING:")) {
                String[] parts = id.split(":");
                if (parts.length >= 5) {
                    showSightingActionsDialog(parts[2], parts[3], parts[4], notif.title);
                }
            } else if (id.startsWith("FOLLOWUP:")) {
                String[] parts = id.split(":");
                if (parts.length >= 5) {
                    showFollowUpDialog(parts[1], parts[2], parts[4]);
                }
            } else if (notif.type == NotificationModel.TYPE_REPORT
                    || notif.type == NotificationModel.TYPE_MY_REPORT) {
                Intent i = new Intent(this, MissedPersonDetailActivity.class);
                i.putExtra(MissedPersonDetailActivity.EXTRA_REPORT_ID, notif.id);
                startActivity(i);
            }
        });

        rv.setAdapter(adapter);
        btnMarkAllRead.setOnClickListener(v -> markAllRead());
        loadAll();
    }

    // ── Back button: always go to MainActivity ────────────────────────────────
    @Override
    public void onBackPressed() {
        goHome();
    }

    private void goHome() {
        Intent i = new Intent(this, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        finish();
    }

    // ── Load ──────────────────────────────────────────────────────────────────
    private void loadAll() {
        allAnnouncements.clear();
        allReports.clear();
        myReports.clear();

        if (progress   != null) progress.setVisibility(View.VISIBLE);
        if (emptyState != null) emptyState.setVisibility(View.GONE);

        new Thread(() -> {
            try {
                Databases db = AppwriteService.getDatabases();
                loadAnnouncements(db);
                loadApprovedReports(db);
                if (!myUserId.isEmpty()) loadMyReports(db);

                // ── Sort each list: most recent first ─────────────────────────
                sortByTime(allAnnouncements);
                sortByTime(allReports);
                sortByTime(myReports);

                runOnUiThread(() -> {
                    if (progress != null) progress.setVisibility(View.GONE);
                    applyTab();
                });
            } catch (Exception e) {
                Log.e(TAG, "loadAll error: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    if (progress != null) progress.setVisibility(View.GONE);
                    Toast.makeText(this,
                            "Failed to load: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    /**
     * Sort a list of NotificationModel so the most recent item comes first.
     * Items whose time cannot be parsed are pushed to the bottom.
     */
    private void sortByTime(List<NotificationModel> list) {
        Collections.sort(list, (a, b) -> {
            long ta = parseTimestamp(a.time);
            long tb = parseTimestamp(b.time);
            return Long.compare(tb, ta); // descending
        });
    }

    /**
     * Tries multiple date formats and returns epoch millis, or 0 on failure.
     */
    private long parseTimestamp(String raw) {
        if (raw == null || raw.isEmpty()) return 0L;
        String[] formats = {
                "yyyy-MM-dd'T'HH:mm:ss.SSSSSS",
                "yyyy-MM-dd'T'HH:mm:ss",
                "dd MMM yyyy, hh:mm a",
                "dd MMM, hh:mm a",
                "dd MMM yyyy"
        };
        for (String fmt : formats) {
            try {
                Date d = new SimpleDateFormat(fmt, Locale.getDefault()).parse(raw);
                if (d != null) return d.getTime();
            } catch (Exception ignored) {}
        }
        return 0L;
    }

    // ── Load Announcements ────────────────────────────────────────────────────
    private void loadAnnouncements(Databases db) {
        try {
            List<? extends Document<?>> docs =
                    AppwriteHelper.listDocuments(
                            db, AppwriteService.DB_ID, COL_ANNOUNCEMENTS,
                            Arrays.asList(
                                    io.appwrite.Query.Companion.orderDesc("$createdAt"),
                                    io.appwrite.Query.Companion.limit(30)
                            )).getDocuments();

            for (Document<?> doc : docs) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) doc.getData();
                String title    = s(data, "title",    "Announcement");
                String message  = s(data, "message",  "");
                String audience = s(data, "audience", "All Users");
                String sentAt   = s(data, "sentAt",   doc.getCreatedAt());

                allAnnouncements.add(new NotificationModel(
                        doc.getId(), title,
                        message + (audience.isEmpty() ? "" : "  •  To: " + audience),
                        sentAt, "", false,
                        NotificationModel.TYPE_ANNOUNCEMENT));
            }
        } catch (Exception e) {
            Log.w(TAG, "loadAnnouncements: " + e.getMessage());
        }
    }

    // ── Load Approved Reports ─────────────────────────────────────────────────
    private void loadApprovedReports(Databases db) {
        try {
            List<? extends Document<?>> docs =
                    AppwriteHelper.listDocuments(
                            db, AppwriteService.DB_ID, AppwriteService.COL_REPORTS,
                            Arrays.asList(
                                    io.appwrite.Query.Companion.equal("status", "active"),
                                    io.appwrite.Query.Companion.orderDesc("$createdAt"),
                                    io.appwrite.Query.Companion.limit(50)
                            )).getDocuments();

            for (Document<?> doc : docs) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) doc.getData();
                String name     = s(data, "name",     "Unknown");
                String location = s(data, "location", "");
                String gender   = s(data, "gender",   "");
                String iso      = doc.getCreatedAt();
                String body     = name
                        + (gender.isEmpty()   ? "" : " • " + gender)
                        + (location.isEmpty() ? "" : " • " + location);

                allReports.add(new NotificationModel(
                        doc.getId(),
                        "New approved missing person",
                        body, formatTime(iso), extractPhoto(data), false,
                        NotificationModel.TYPE_REPORT));
            }
        } catch (Exception e) {
            Log.w(TAG, "loadApprovedReports: " + e.getMessage());
        }
    }

    // ── Load My Reports ───────────────────────────────────────────────────────
    private void loadMyReports(Databases db) {
        try {
            List<? extends Document<?>> myReportDocs =
                    AppwriteHelper.listDocuments(
                            db, AppwriteService.DB_ID, AppwriteService.COL_REPORTS,
                            Arrays.asList(
                                    io.appwrite.Query.Companion.equal("userId", myUserId),
                                    io.appwrite.Query.Companion.orderDesc("$createdAt"),
                                    io.appwrite.Query.Companion.limit(30)
                            )).getDocuments();

            for (Document<?> doc : myReportDocs) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) doc.getData();
                String name     = s(data, "name",   "Unknown");
                String status   = s(data, "status", "pending");
                String iso      = doc.getCreatedAt();
                String photoUrl = extractPhoto(data);

                String  title;
                String  body;
                boolean isUnread;

                switch (status) {
                    case "found":
                        title = "🎉 " + name + " has been FOUND!";
                        body  = "The missing person you reported has been found.";
                        isUnread = true; break;
                    case "active":
                        title = "✅ Report approved";
                        body  = name + "'s report is now visible to the public.";
                        isUnread = false; break;
                    case "fake": case "rejected":
                        title = "❌ Report flagged";
                        body  = name + "'s report was flagged by an admin.";
                        isUnread = true; break;
                    case "resolved":
                        title = "✅ Case resolved";
                        body  = name + "'s case has been marked resolved.";
                        isUnread = false; break;
                    default:
                        title = "⏳ Report pending approval";
                        body  = name + " is waiting for admin review.";
                        isUnread = false; break;
                }

                myReports.add(new NotificationModel(
                        doc.getId(), title, body,
                        formatTime(iso), photoUrl, !isUnread,
                        NotificationModel.TYPE_MY_REPORT));
            }

            loadSightingNotifications(db);

        } catch (Exception e) {
            Log.w(TAG, "loadMyReports: " + e.getMessage());
        }
    }

    // ── Load Sighting Notifications ───────────────────────────────────────────
    private void loadSightingNotifications(Databases db) {
        try {
            List<? extends Document<?>> notifDocs =
                    AppwriteHelper.listDocuments(
                            db, AppwriteService.DB_ID, "notifications",
                            Arrays.asList(
                                    io.appwrite.Query.Companion.equal("userId", myUserId),
                                    io.appwrite.Query.Companion.equal("type", "sighting"),
                                    io.appwrite.Query.Companion.orderDesc("$createdAt"),
                                    io.appwrite.Query.Companion.limit(30)
                            )).getDocuments();

            for (Document<?> doc : notifDocs) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) doc.getData();

                String title         = s(data, "title",           "👀 Sighting Report");
                String message       = s(data, "message",         "");
                String sentAt        = s(data, "sentAt",          doc.getCreatedAt());
                String watcherUserId = s(data, "watcherUserId",   "");
                String watcherName   = s(data, "watcherName",     "");
                String watcherPhone  = s(data, "watcherPhone",    "");
                String watcherLoc    = s(data, "watcherLocation", "");
                String reportId      = s(data, "reportId",        "");
                String sightingId    = s(data, "sightingId",      "");
                boolean isRead       = Boolean.parseBoolean(s(data, "isRead",   "false"));
                boolean resolved     = Boolean.parseBoolean(s(data, "resolved", "false"));

                String body = message.isEmpty()
                        ? watcherName + " saw them at: " + watcherLoc : message;

                String compositeId = "SIGHTING:" + sightingId + ":"
                        + watcherUserId + ":" + watcherPhone + ":" + reportId;

                myReports.add(new NotificationModel(
                        compositeId, title, body, sentAt, "",
                        isRead, NotificationModel.TYPE_MY_REPORT));

                if (!resolved) {
                    String followUpId = "FOLLOWUP:" + sightingId + ":"
                            + watcherUserId + ":" + watcherPhone + ":" + reportId;
                    myReports.add(new NotificationModel(
                            followUpId,
                            "❓ Did " + watcherName + "'s help resolve the case?",
                            "Tap Yes if they helped you find "
                                    + s(data, "reportName", "the person")
                                    + ", or No if not.",
                            sentAt, "", false,
                            NotificationModel.TYPE_MY_REPORT));
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "loadSightingNotifications: " + e.getMessage());
        }
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────
    private void showSightingActionsDialog(String watcherUserId, String watcherPhone,
                                           String reportId, String title) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setItems(new String[]{"📞 Call Watcher", "💬 Chat with Watcher"}, (d, which) -> {
                    if (which == 0) {
                        if (!watcherPhone.isEmpty())
                            startActivity(new Intent(Intent.ACTION_DIAL,
                                    Uri.parse("tel:" + watcherPhone.replaceAll("[^0-9+]", ""))));
                        else Toast.makeText(this, "Phone not available", Toast.LENGTH_SHORT).show();
                    } else {
                        if (!watcherUserId.isEmpty()) {
                            Intent i = new Intent(this, ChatsActivity.class);
                            i.putExtra("direct_user_id",   watcherUserId);
                            i.putExtra("direct_user_name", "Watcher");
                            startActivity(i);
                        }
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void showFollowUpDialog(String sightingId, String watcherUserId, String reportId) {
        new AlertDialog.Builder(this)
                .setTitle("Did their help resolve the case?")
                .setMessage("If yes, the watcher earns a help badge and gets notified.")
                .setPositiveButton("✅ Yes, they helped!", (d, w) ->
                        markCaseResolvedByWatcher(sightingId, watcherUserId, reportId))
                .setNegativeButton("❌ No", (d, w) -> {
                    markFollowUpAnswered(sightingId);
                    Toast.makeText(this, "Thank you for the feedback", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    // ── Resolve ───────────────────────────────────────────────────────────────
    private void markCaseResolvedByWatcher(String sightingId, String watcherUserId,
                                           String reportId) {
        new Thread(() -> {
            try {
                Databases db = AppwriteService.getDatabases();
                String ts = new SimpleDateFormat("dd MMM yyyy, hh:mm a",
                        Locale.getDefault()).format(new Date());
                SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
                String myName = prefs.getString("logged_in_name",    "user");
                String myUid  = prefs.getString("logged_in_user_id", "");

                Map<String, Object> sightingUpdate = new HashMap<>();
                sightingUpdate.put("status",   "resolved");
                sightingUpdate.put("resolved", true);
                AppwriteHelper.updateDocument(db, AppwriteService.DB_ID,
                        AppwriteService.COL_SIGHTINGS, sightingId, sightingUpdate);

                if (!reportId.isEmpty()) {
                    try {
                        Map<String, Object> reportUpdate = new HashMap<>();
                        reportUpdate.put("status", "found");
                        AppwriteHelper.updateDocument(db, AppwriteService.DB_ID,
                                AppwriteService.COL_REPORTS, reportId, reportUpdate);
                    } catch (Exception re) {
                        Log.w(TAG, "Could not update report status: " + re.getMessage());
                    }
                }

                String helpId = UUID.randomUUID().toString().replace("-", "").substring(0, 20);
                Map<String, Object> helpData = new HashMap<>();
                helpData.put("watcherUserId",  watcherUserId);
                helpData.put("reporterUserId", myUid);
                helpData.put("reportId",       reportId);
                helpData.put("resolvedAt",     ts);
                helpData.put("message",        "Helped locate a missing person reported by " + myName);
                AppwriteHelper.createDocument(db, AppwriteService.DB_ID,
                        AppwriteService.COL_HELPS, helpId, helpData);

                String notifId = UUID.randomUUID().toString().replace("-", "").substring(0, 20);
                Map<String, Object> notifData = new HashMap<>();
                notifData.put("userId",   watcherUserId);
                notifData.put("type",     "help_confirmed");
                notifData.put("title",    "🌟 You helped find a missing person!");
                notifData.put("message",  "Your sighting report helped resolve the case. Thank you!");
                notifData.put("sentAt",   ts);
                notifData.put("isRead",   false);
                notifData.put("reportId", reportId);
                AppwriteHelper.createDocument(db, AppwriteService.DB_ID,
                        "notifications", notifId, notifData);

                runOnUiThread(() -> {
                    Toast.makeText(this, "✅ Watcher notified!", Toast.LENGTH_LONG).show();
                    loadAll();
                });

            } catch (Exception e) {
                Log.e(TAG, "markCaseResolvedByWatcher: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(this,
                        "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void markFollowUpAnswered(String sightingId) {
        new Thread(() -> {
            try {
                Map<String, Object> update = new HashMap<>();
                update.put("resolved", true);
                AppwriteHelper.updateDocument(AppwriteService.getDatabases(),
                        AppwriteService.DB_ID, AppwriteService.COL_SIGHTINGS,
                        sightingId, update);
            } catch (Exception e) {
                Log.w(TAG, "markFollowUpAnswered: " + e.getMessage());
            }
        }).start();
    }

    // ── Tab ───────────────────────────────────────────────────────────────────
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
        if (emptyState != null) emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        if (rv != null)         rv.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void markAllRead() {
        for (NotificationModel n : displayed) n.isRead = true;
        adapter.notifyDataSetChanged();
        updateUnreadCount();
    }

    private void updateUnreadCount() {
        int unread = 0;
        for (NotificationModel n : displayed) if (!n.isRead) unread++;
        if (tvUnreadCount != null)
            tvUnreadCount.setText(unread == 0 ? "All caught up ✓" : unread + " unread");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private String extractPhoto(Map<String, Object> data) {
        Object pf = data.get("photoUrls");
        if (pf instanceof List && !((List<?>) pf).isEmpty()) {
            String raw = ((List<?>) pf).get(0).toString();
            return raw.contains("project=")
                    ? raw : raw + "?project=" + AppwriteService.PROJECT_ID;
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

    private String s(Map<String, Object> m, String key) {
        return s(m, key, "");
    }
}