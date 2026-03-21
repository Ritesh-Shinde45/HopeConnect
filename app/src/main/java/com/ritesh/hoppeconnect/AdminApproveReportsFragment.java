package com.ritesh.hoppeconnect;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.appwrite.models.Document;
import io.appwrite.services.Databases;

public class AdminApproveReportsFragment extends Fragment {

    private static final String TAG = "AdminApproveReportsFrag";

    private RecyclerView rv;
    private TextView txtEmpty, tvHeaderCount;
    private LinearLayout emptyState, loadingState;
    private SwipeRefreshLayout swipeRefresh;
    private Chip chipActive, chipFound, chipAll;

    private PendingReportAdapter adapter;
    private final List<AdminReportModel> allPending = new ArrayList<>();
    private final List<AdminReportModel> displayed  = new ArrayList<>();

    private String chipFilter = "all";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        try {
            return inflater.inflate(R.layout.admin_approve_requests, container, false);
        } catch (Exception e) {
            Log.e(TAG, "Inflation error: " + e.getMessage(), e);
            TextView fallback = new TextView(getContext());
            fallback.setText("Layout error: " + e.getMessage());
            fallback.setPadding(32, 100, 32, 32);
            return fallback;
        }
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        try {
            rv            = v.findViewById(R.id.approvedRecyclerView);
            txtEmpty      = v.findViewById(R.id.txtEmptyApproved);
            emptyState    = v.findViewById(R.id.emptyStateApprove);
            loadingState  = v.findViewById(R.id.loadingStateApprove);
            tvHeaderCount = v.findViewById(R.id.tv_pending_header_count);
            swipeRefresh  = v.findViewById(R.id.approvedSwipeRefresh);
            chipActive    = v.findViewById(R.id.chipActive);
            chipFound     = v.findViewById(R.id.chipFound);
            chipAll       = v.findViewById(R.id.chipAll);

            if (rv == null || swipeRefresh == null) {
                Log.e(TAG, "Critical views missing");
                return;
            }

            rv.setLayoutManager(new LinearLayoutManager(getContext()));
            adapter = new PendingReportAdapter(getContext(), displayed,
                    new PendingReportAdapter.Listener() {
                        @Override public void onApprove(AdminReportModel r, int pos)  { updateStatus(r, "active", pos); }
                        @Override public void onFlagFake(AdminReportModel r, int pos) { updateStatus(r, "fake",   pos); }
                        @Override public void onClick(AdminReportModel r)             { showDetail(r); }
                    });
            rv.setAdapter(adapter);

            if (chipAll    != null) chipAll.setOnClickListener(x    -> { chipFilter = "all";    applyFilter(); });
            if (chipActive != null) chipActive.setOnClickListener(x -> { chipFilter = "active"; applyFilter(); });
            if (chipFound  != null) chipFound.setOnClickListener(x  -> { chipFilter = "found";  applyFilter(); });

            swipeRefresh.setOnRefreshListener(this::load);
            setState("loading");
            load();

        } catch (Exception e) {
            Log.e(TAG, "onViewCreated error: " + e.getMessage(), e);
        }
    }

    private void setState(String state) {
        if (loadingState != null)
            loadingState.setVisibility("loading".equals(state) ? View.VISIBLE : View.GONE);
        if (rv != null)
            rv.setVisibility("list".equals(state) ? View.VISIBLE : View.GONE);
        if (emptyState != null)
            emptyState.setVisibility("empty".equals(state) ? View.VISIBLE : View.GONE);
    }

    private void load() {
        new Thread(() -> {
            try {
                Databases db = AppwriteService.getDatabases();
                List<? extends Document<?>> docs =
                        AppwriteHelper.listAllDocuments(
                                        db, AppwriteService.DB_ID, AppwriteService.COL_REPORTS)
                                .getDocuments();

                Log.d(TAG, "Total docs: " + docs.size());

                List<AdminReportModel> list = new ArrayList<>();
                for (Document<?> doc : docs) {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> data = (Map<String, Object>) doc.getData();
                        Object statusObj = data.get("status");

                        AdminReportModel model = AdminReportModel.fromDocument(doc);
                        if (statusObj == null || "pending".equals(model.status)) {
                            model.status = "pending";
                            list.add(model);
                        }
                    } catch (Exception ignored) {
                        Log.w(TAG, "Skipping doc: " + doc.getId());
                    }
                }

                Log.d(TAG, "Pending count: " + list.size());

                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                    allPending.clear();
                    allPending.addAll(list);
                    if (tvHeaderCount != null)
                        tvHeaderCount.setText(list.size() + " pending report(s)");
                    applyFilter();
                });

            } catch (Exception e) {
                Log.e(TAG, "load error: " + e.getMessage(), e);
                if (getActivity() != null)
                    getActivity().runOnUiThread(() -> {
                        if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                        setState("empty");
                        if (tvHeaderCount != null)
                            tvHeaderCount.setText("Failed to load");
                        Toast.makeText(getContext(),
                                "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
            }
        }).start();
    }

    private void applyFilter() {
        displayed.clear();
        for (AdminReportModel r : allPending) {
            if ("all".equals(chipFilter) || chipFilter.equals(r.status))
                displayed.add(r);
        }
        adapter.notifyDataSetChanged();
        setState(displayed.isEmpty() ? "empty" : "list");
    }

    private void updateStatus(AdminReportModel r, String newStatus, int pos) {
        new Thread(() -> {
            try {
                Databases db = AppwriteService.getDatabases();
                Map<String, Object> update = new HashMap<>();
                update.put("status", newStatus);
                AppwriteHelper.updateDocument(
                        db, AppwriteService.DB_ID, AppwriteService.COL_REPORTS, r.id, update);

                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    allPending.remove(r);
                    int idx = displayed.indexOf(r);
                    if (idx >= 0) { displayed.remove(idx); adapter.notifyItemRemoved(idx); }
                    if (tvHeaderCount != null)
                        tvHeaderCount.setText(allPending.size() + " pending report(s)");
                    setState(displayed.isEmpty() ? "empty" : "list");
                    Toast.makeText(getContext(),
                            "active".equals(newStatus) ? "Approved ✓" : "Flagged as fake",
                            Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                Log.e(TAG, "updateStatus error", e);
                if (getActivity() != null)
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(),
                                    "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void showDetail(AdminReportModel r) {
        try {
            AdminReportDetailSheet.newInstance(r)
                    .show(getParentFragmentManager(), "detail");
        } catch (Exception e) {
            Log.e(TAG, "showDetail error", e);
        }
    }
}