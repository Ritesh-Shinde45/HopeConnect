package com.ritesh.hoppeconnect;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.appwrite.models.Document;
import io.appwrite.services.Databases;

public class AdminOverviewFragment extends Fragment {

    private static final String TAG = "AdminOverviewFragment";

    private TextView tvTotalUsers, tvAnnouncementCount, tvBlockedUsers, tvTotalReports;
    private TextView tvPendingCount, tvResolvedCount;
    private TextView tvStatApproved, tvStatPending, tvStatFake, tvStatResolved;
    private TextView tvLogEmpty;
    private View progressApproved, progressPending, progressFake, progressResolved;
    private RecyclerView rvLog;
    private SwipeRefreshLayout swipeRefresh;
    private ActivityLogAdapter logAdapter;
    private final List<ActivityLogItem> logItems = new ArrayList<>();

    private LinearLayout totalUsersCard, announcementsCard, blockedUsersCard;
    private LinearLayout totalReportsCard, pendingApprovalCard, resolvedCasesCard;
    private LinearLayout exportAsCard, filterByDatesCard;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        try {
            View v = inflater.inflate(R.layout.admin_dashboard, container, false);
            Log.d(TAG, "admin_dashboard inflated successfully");
            return v;
        } catch (Exception e) {
            Log.e(TAG, "Inflation error: " + e.getMessage(), e);
            TextView fallback = new TextView(getContext());
            fallback.setText("Dashboard layout error: " + e.getMessage());
            fallback.setPadding(32, 100, 32, 32);
            return fallback;
        }
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        try {
           
            tvTotalUsers        = v.findViewById(R.id.tv_total_users);
            tvAnnouncementCount = v.findViewById(R.id.tv_announcement_count);
            tvBlockedUsers      = v.findViewById(R.id.tv_blocked_users);
            tvTotalReports      = v.findViewById(R.id.tv_total_reports);
            tvPendingCount      = v.findViewById(R.id.tv_pending_count);
            tvResolvedCount     = v.findViewById(R.id.tv_resolved_count);
            tvStatApproved      = v.findViewById(R.id.tv_stat_approved);
            tvStatPending       = v.findViewById(R.id.tv_stat_pending);
            tvStatFake          = v.findViewById(R.id.tv_stat_fake);
            tvStatResolved      = v.findViewById(R.id.tv_stat_resolved);
            progressApproved    = v.findViewById(R.id.progress_approved);
            progressPending     = v.findViewById(R.id.progress_pending);
            progressFake        = v.findViewById(R.id.progress_fake);
            progressResolved    = v.findViewById(R.id.progress_resolved);
            rvLog               = v.findViewById(R.id.rv_activity_log);
            tvLogEmpty          = v.findViewById(R.id.tv_log_empty);
            swipeRefresh        = v.findViewById(R.id.swipe_refresh_dashboard);

           
            totalUsersCard      = v.findViewById(R.id.totalUsersCard);
            announcementsCard   = v.findViewById(R.id.announcementsCard);
            blockedUsersCard    = v.findViewById(R.id.blockedUsersCard);
            totalReportsCard    = v.findViewById(R.id.totalReportsCard);
            pendingApprovalCard = v.findViewById(R.id.pendingApprovalCard);
            resolvedCasesCard   = v.findViewById(R.id.resolvedCasesCard);
            exportAsCard        = v.findViewById(R.id.exportAsCard);
            filterByDatesCard   = v.findViewById(R.id.filterByDatesCard);

           
            if (swipeRefresh == null) {
                Log.e(TAG, "swipeRefresh is null — admin_dashboard.xml may be wrong version");
                return;
            }
            if (rvLog == null) {
                Log.e(TAG, "rvLog is null");
                return;
            }

            rvLog.setLayoutManager(new LinearLayoutManager(getContext()));
            logAdapter = new ActivityLogAdapter(logItems);
            rvLog.setAdapter(logAdapter);

           
            if (totalUsersCard != null)
                totalUsersCard.setOnClickListener(x ->
                        startActivity(new Intent(getContext(), AdminUsersListActivity.class)
                                .putExtra("filter", "all")));

            if (blockedUsersCard != null)
                blockedUsersCard.setOnClickListener(x ->
                        startActivity(new Intent(getContext(), AdminUsersListActivity.class)
                                .putExtra("filter", "suspended")));

            if (totalReportsCard != null)
                totalReportsCard.setOnClickListener(x ->
                        startActivity(new Intent(getContext(), AdminReportsListActivity.class)
                                .putExtra("filter", "all")));

            if (pendingApprovalCard != null)
                pendingApprovalCard.setOnClickListener(x ->
                        startActivity(new Intent(getContext(), AdminReportsListActivity.class)
                                .putExtra("filter", "pending")));

            if (resolvedCasesCard != null)
                resolvedCasesCard.setOnClickListener(x ->
                        startActivity(new Intent(getContext(), AdminReportsListActivity.class)
                                .putExtra("filter", "resolved")));

            if (announcementsCard != null)
                announcementsCard.setOnClickListener(x ->
                        startActivity(new Intent(getContext(), AdminAnnouncementActivity.class)));

            if (exportAsCard != null)
                exportAsCard.setOnClickListener(x ->
                        startActivity(new Intent(getContext(), AdminExportActivity.class)));

            if (filterByDatesCard != null)
                filterByDatesCard.setOnClickListener(x ->
                        startActivity(new Intent(getContext(), AdminReportsListActivity.class)
                                .putExtra("filter", "date")));

            swipeRefresh.setOnRefreshListener(this::load);
            load();

        } catch (Exception e) {
            Log.e(TAG, "onViewCreated error: " + e.getMessage(), e);
        }
    }

    private void load() {
        if (swipeRefresh == null) {
            Log.e(TAG, "load() called but swipeRefresh is null");
            return;
        }
        swipeRefresh.setRefreshing(true);

        new Thread(() -> {
            try {
                Databases db = AppwriteService.getDatabases();

               
                List<? extends Document<?>> userDocs =
                        AppwriteHelper.listAllDocuments(
                                        db, AppwriteService.DB_ID, AppwriteService.COL_USERS)
                                .getDocuments();
                int totalUsers = userDocs.size();
                int blocked    = 0;
                for (Document<?> doc : userDocs) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) doc.getData();
                    if ("suspended".equals(data.get("status"))) blocked++;
                }

               
                List<? extends Document<?>> reportDocs =
                        AppwriteHelper.listAllDocuments(
                                        db, AppwriteService.DB_ID, AppwriteService.COL_REPORTS)
                                .getDocuments();
                int total    = reportDocs.size();
                int pending  = 0, approved = 0, fake = 0, resolved = 0;

                for (Document<?> doc : reportDocs) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) doc.getData();
                    String status = data.get("status") != null
                            ? data.get("status").toString() : "pending";
                    switch (status) {
                        case "pending":  pending++;  break;
                        case "active":   approved++; break;
                        case "found":
                        case "resolved": resolved++; break;
                        case "fake":
                        case "rejected": fake++;     break;
                    }
                }

               
                List<ActivityLogItem> logs = new ArrayList<>();
                int start = Math.max(0, reportDocs.size() - 5);
                for (int i = reportDocs.size() - 1; i >= start; i--) {
                    Document<?> doc = reportDocs.get(i);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) doc.getData();
                    String name   = data.get("name")   != null ? data.get("name").toString()   : "Unknown";
                    String status = data.get("status") != null ? data.get("status").toString() : "pending";
                    String iso    = doc.getCreatedAt();
                    String date   = (iso != null && iso.length() >= 10) ? iso.substring(0, 10) : "—";
                    String type;
                    switch (status) {
                        case "active":   type = "success"; break;
                        case "pending":  type = "warning"; break;
                        case "fake":
                        case "rejected": type = "danger";  break;
                        default:         type = "info";    break;
                    }
                    logs.add(new ActivityLogItem(
                            "Report: " + name + " [" + status + "]", date, type));
                }

                final int fuTotal    = totalUsers;
                final int fuBlocked  = blocked;
                final int ftTotal    = total;
                final int ftPending  = pending;
                final int ftApproved = approved;
                final int ftFake     = fake;
                final int ftResolved = resolved;
                final List<ActivityLogItem> finalLogs = logs;

                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    if (swipeRefresh == null) return;
                    swipeRefresh.setRefreshing(false);

                   
                    setText(tvTotalUsers,        String.valueOf(fuTotal));
                    setText(tvBlockedUsers,       String.valueOf(fuBlocked));
                    setText(tvTotalReports,       String.valueOf(ftTotal));
                    setText(tvAnnouncementCount,  "0");
                    setText(tvPendingCount,       String.valueOf(ftPending));
                    setText(tvResolvedCount,      String.valueOf(ftResolved));

                    String suffix = " / " + ftTotal;
                    setText(tvStatApproved, ftApproved + suffix);
                    setText(tvStatPending,  ftPending  + suffix);
                    setText(tvStatFake,     ftFake     + suffix);
                    setText(tvStatResolved, ftResolved + suffix);

                    if (ftTotal > 0) {
                        animateBar(progressApproved, ftApproved, ftTotal);
                        animateBar(progressPending,  ftPending,  ftTotal);
                        animateBar(progressFake,     ftFake,     ftTotal);
                        animateBar(progressResolved, ftResolved, ftTotal);
                    }

                    logItems.clear();
                    logItems.addAll(finalLogs);
                    if (logAdapter != null) logAdapter.notifyDataSetChanged();
                    if (tvLogEmpty != null)
                        tvLogEmpty.setVisibility(
                                finalLogs.isEmpty() ? View.VISIBLE : View.GONE);
                });

            } catch (Exception e) {
                Log.e(TAG, "load error: " + e.getMessage(), e);
                if (getActivity() != null)
                    getActivity().runOnUiThread(() -> {
                        if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                    });
            }
        }).start();
    }

    private void setText(TextView tv, String text) {
        if (tv != null) tv.setText(text);
    }

    private void animateBar(View bar, int count, int total) {
        if (bar == null) return;
        bar.post(() -> {
            View parent = (View) bar.getParent();
            if (parent == null) return;
            int parentW = parent.getWidth();
            int w = total > 0 ? (int) ((float) count / total * parentW) : 0;
            ViewGroup.LayoutParams lp = bar.getLayoutParams();
            lp.width = Math.max(w, 0);
            bar.setLayoutParams(lp);
        });
    }
}