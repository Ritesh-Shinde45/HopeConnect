package com.ritesh.hoppeconnect;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.chip.Chip;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.appwrite.models.Document;
import io.appwrite.services.Databases;

public class AdminAllReportsFragment extends Fragment {

    private static final String TAG = "AdminAllReportsFrag";

    private EditText etSearch;
    private Chip chipAll, chipPending, chipActive, chipFound, chipRejected;
    private TextView txtCount;
    private RecyclerView rv;
    private LinearLayout emptyState;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressBar;
    private TabLayout tabLayout;

    private AdminReportRowAdapter adapter;
    private final List<AdminReportModel> allReports = new ArrayList<>();
    private final List<AdminReportModel> displayed  = new ArrayList<>();

    private String statusFilter = "all";
    private String searchQuery  = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inf,
                             @Nullable ViewGroup c, @Nullable Bundle s) {
        return inf.inflate(R.layout.admin_all_reports, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);

        etSearch     = v.findViewById(R.id.et_search_reports);
        chipAll      = v.findViewById(R.id.chip_status_all);
        chipPending  = v.findViewById(R.id.chip_status_pending);
        chipActive   = v.findViewById(R.id.chip_status_active);
        chipFound    = v.findViewById(R.id.chip_status_found);
        chipRejected = v.findViewById(R.id.chip_status_rejected);
        txtCount     = v.findViewById(R.id.txtReportCount);
        rv           = v.findViewById(R.id.rv_all_reports);
        emptyState   = v.findViewById(R.id.empty_state_reports);
        swipeRefresh = v.findViewById(R.id.swipe_refresh_reports);
        progressBar  = v.findViewById(R.id.progressBarAllReports);
        tabLayout    = v.findViewById(R.id.tab_layout_reports);

        tabLayout.addTab(tabLayout.newTab().setText("All"));
        tabLayout.addTab(tabLayout.newTab().setText("Pending"));
        tabLayout.addTab(tabLayout.newTab().setText("Active"));
        tabLayout.addTab(tabLayout.newTab().setText("Found"));
        tabLayout.addTab(tabLayout.newTab().setText("Rejected"));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                String[] filters = {"all", "pending", "active", "found", "rejected"};
                statusFilter = filters[tab.getPosition()];
                applyFilter();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        chipAll.setOnClickListener(x      -> { selectTab(0); statusFilter = "all";      applyFilter(); });
        chipPending.setOnClickListener(x  -> { selectTab(1); statusFilter = "pending";  applyFilter(); });
        chipActive.setOnClickListener(x   -> { selectTab(2); statusFilter = "active";   applyFilter(); });
        chipFound.setOnClickListener(x    -> { selectTab(3); statusFilter = "found";    applyFilter(); });
        chipRejected.setOnClickListener(x -> { selectTab(4); statusFilter = "rejected"; applyFilter(); });

        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AdminReportRowAdapter(displayed, new AdminReportRowAdapter.Listener() {
            @Override public void onApprove(AdminReportModel r, int pos) {
                changeStatus(r, "active", pos);
            }
            @Override public void onFlag(AdminReportModel r, int pos) {
                changeStatus(r, "rejected", pos);
            }
            @Override public void onClick(AdminReportModel r) {
                AdminReportDetailSheet.newInstance(r)
                        .show(getParentFragmentManager(), "detail");
            }
        });
        rv.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                searchQuery = s.toString().trim().toLowerCase();
                applyFilter();
            }
            @Override public void afterTextChanged(Editable e) {}
        });

        swipeRefresh.setOnRefreshListener(this::load);
        load();
    }

    private void selectTab(int idx) {
        TabLayout.Tab tab = tabLayout.getTabAt(idx);
        if (tab != null) tab.select();
    }

    private void load() {
        swipeRefresh.setRefreshing(true);
        progressBar.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                Databases db = AppwriteService.getDatabases();
                List<? extends Document<?>> docs =
                        AppwriteHelper.listAllDocuments(
                                        db, AppwriteService.DB_ID, AppwriteService.COL_REPORTS)
                                .getDocuments();

                List<AdminReportModel> list = new ArrayList<>();
                for (Document<?> doc : docs) list.add(AdminReportModel.fromDocument(doc));

                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    swipeRefresh.setRefreshing(false);
                    progressBar.setVisibility(View.GONE);
                    allReports.clear();
                    allReports.addAll(list);
                    applyFilter();
                });
            } catch (Exception e) {
                Log.e(TAG, "load error", e);
                if (getActivity() != null)
                    getActivity().runOnUiThread(() -> {
                        swipeRefresh.setRefreshing(false);
                        progressBar.setVisibility(View.GONE);
                    });
            }
        }).start();
    }

    private void applyFilter() {
        displayed.clear();
        for (AdminReportModel r : allReports) {
            boolean statusOk = "all".equals(statusFilter) || r.status.equals(statusFilter);
            boolean searchOk = searchQuery.isEmpty()
                    || r.name.toLowerCase().contains(searchQuery)
                    || r.location.toLowerCase().contains(searchQuery);
            if (statusOk && searchOk) displayed.add(r);
        }
        adapter.notifyDataSetChanged();
        txtCount.setText(displayed.size() + " report(s)");
        emptyState.setVisibility(displayed.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void changeStatus(AdminReportModel r, String newStatus, int pos) {
        new Thread(() -> {
            try {
                Databases db = AppwriteService.getDatabases();
                Map<String, Object> update = new HashMap<>();
                update.put("status", newStatus);
                AppwriteHelper.updateDocument(
                        db, AppwriteService.DB_ID, AppwriteService.COL_REPORTS, r.id, update);
                r.status = newStatus;
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    applyFilter();
                    Toast.makeText(getContext(),
                            "Status updated to " + newStatus, Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                Log.e(TAG, "changeStatus error", e);
                if (getActivity() != null)
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(),
                                    "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}