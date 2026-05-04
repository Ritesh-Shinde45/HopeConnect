package com.ritesh.hoppeconnect;

import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.appwrite.models.Document;
import io.appwrite.services.Databases;

public class AdminReportsListActivity extends AppCompatActivity {

    private static final String TAG = "AdminReportsListAct";

    private RecyclerView rv;
    private SwipeRefreshLayout swipeRefresh;
    private TextView txtCount, txtTitle;
    private EditText etSearch;

    private AdminReportRowAdapter adapter;
    private final List<AdminReportModel> allReports = new ArrayList<>();
    private final List<AdminReportModel> displayed  = new ArrayList<>();

    private String filterMode  = "all";
    private String searchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_reports_list);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView()
                    .setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
        AppwriteService.init(this);

        filterMode = getIntent().getStringExtra("filter");
        if (filterMode == null) filterMode = "all";

        txtTitle    = findViewById(R.id.tv_reports_list_title);
        txtCount    = findViewById(R.id.tv_reports_list_count);
        etSearch    = findViewById(R.id.et_search_reports_list);
        rv          = findViewById(R.id.rv_reports_list);
        swipeRefresh = findViewById(R.id.swipe_refresh_reports_list);

        String title;
        switch (filterMode) {
            case "pending":  title = "Pending Reports";  break;
            case "resolved": title = "Resolved Cases";   break;
            case "active":   title = "Active Reports";   break;
            case "fake":     title = "Flagged Reports";  break;
            default:         title = "All Reports";      break;
        }
        txtTitle.setText(title);

        findViewById(R.id.btn_back_reports).setOnClickListener(v -> finish());

        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminReportRowAdapter(displayed, new AdminReportRowAdapter.Listener() {
            @Override public void onApprove(AdminReportModel r, int pos) {
                updateStatus(r, "active", pos);
            }
            @Override public void onFlag(AdminReportModel r, int pos) {
                updateStatus(r, "rejected", pos);
            }
            @Override public void onClick(AdminReportModel r) {
                AdminReportDetailSheet.newInstance(r)
                        .show(getSupportFragmentManager(), "detail");
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

    private void load() {
        swipeRefresh.setRefreshing(true);
        new Thread(() -> {
            try {
                Databases db = AppwriteService.getDatabases();
                List<? extends Document<?>> docs =
                        AppwriteHelper.listAllDocuments(
                                        db, AppwriteService.DB_ID, AppwriteService.COL_REPORTS)
                                .getDocuments();

                List<AdminReportModel> list = new ArrayList<>();
                for (Document<?> doc : docs) list.add(AdminReportModel.fromDocument(doc));

                runOnUiThread(() -> {
                    swipeRefresh.setRefreshing(false);
                    allReports.clear();
                    allReports.addAll(list);
                    applyFilter();
                });
            } catch (Exception e) {
                Log.e(TAG, "load error", e);
                runOnUiThread(() -> swipeRefresh.setRefreshing(false));
            }
        }).start();
    }

    private void applyFilter() {
        displayed.clear();
        for (AdminReportModel r : allReports) {
            boolean statusOk;
            switch (filterMode) {
                case "pending":  statusOk = "pending".equals(r.status);  break;
                case "resolved": statusOk = "resolved".equals(r.status) || "found".equals(r.status); break;
                case "active":   statusOk = "active".equals(r.status);   break;
                case "fake":     statusOk = "fake".equals(r.status) || "rejected".equals(r.status);  break;
                default:         statusOk = true; break;
            }
            boolean searchOk = searchQuery.isEmpty()
                    || r.name.toLowerCase().contains(searchQuery)
                    || r.location.toLowerCase().contains(searchQuery);
            if (statusOk && searchOk) displayed.add(r);
        }
        adapter.notifyDataSetChanged();
        txtCount.setText(displayed.size() + " report(s)");
    }

    private void updateStatus(AdminReportModel r, String newStatus, int pos) {
        new Thread(() -> {
            try {
                Databases db = AppwriteService.getDatabases();
                Map<String, Object> update = new HashMap<>();
                update.put("status", newStatus);
                AppwriteHelper.updateDocument(
                        db, AppwriteService.DB_ID, AppwriteService.COL_REPORTS, r.id, update);
                r.status = newStatus;
                runOnUiThread(() -> {
                    applyFilter();
                    Toast.makeText(this, "Updated to " + newStatus, Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                Log.e(TAG, "updateStatus error", e);
                runOnUiThread(() ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}