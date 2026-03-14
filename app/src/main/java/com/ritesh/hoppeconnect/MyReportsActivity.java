package com.ritesh.hoppeconnect;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.appwrite.Query;
import io.appwrite.coroutines.CoroutineCallback;
import io.appwrite.models.DocumentList;
import io.appwrite.services.Databases;

public class MyReportsActivity extends AppCompatActivity {

    private static final String TAG = "MyReportsActivity";

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private ImageView ivBack;

    private ReportAdapter adapter;
    private final List<ReportModel> reports = new ArrayList<>();

    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_reports);

        AppwriteService.init(this);

        SharedPreferences prefs = getSharedPreferences("hoppe_prefs", MODE_PRIVATE);
        currentUserId = prefs.getString("logged_in_user_id", null);

        ivBack       = findViewById(R.id.ivBack);
        recyclerView = findViewById(R.id.myReportsRecyclerView);
        progressBar  = findViewById(R.id.progressBar);
        tvEmpty      = findViewById(R.id.emptyText);

        ivBack.setOnClickListener(v -> finish());

        StaggeredGridLayoutManager lm =
                new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(lm);

        adapter = new ReportAdapter(this, reports, model -> {
            ReportModelCache.put(model);
            Intent intent = new Intent(this, MissedPersonDetailActivity.class);
            intent.putExtra(MissedPersonDetailActivity.EXTRA_REPORT_ID, model.id);
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        if (currentUserId == null) {
            tvEmpty.setVisibility(View.VISIBLE);
            tvEmpty.setText("Please log in to see your reports.");
            return;
        }

        loadMyReports();
    }

    private void loadMyReports() {
        progressBar.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                Databases db = AppwriteService.getDatabases();

                List<String> queries = new ArrayList<>();
                queries.add(Query.Companion.equal("userId", currentUserId));
                queries.add(Query.Companion.orderDesc("$createdAt"));

                db.listDocuments(
                        AppwriteService.DB_ID,
                        "reports",
                        queries,
                        new CoroutineCallback<DocumentList<Map<String, Object>>>(
                                (result, error) -> {
                                    runOnUiThread(() -> progressBar.setVisibility(View.GONE));

                                    if (error != null) {
                                        Log.e(TAG, "load error: " + error.getMessage(), error);
                                        runOnUiThread(() -> {
                                            tvEmpty.setVisibility(View.VISIBLE);
                                            tvEmpty.setText("Failed to load reports.");
                                        });
                                        return;
                                    }

                                    reports.clear();
                                    for (io.appwrite.models.Document<Map<String, Object>> doc
                                            : result.getDocuments()) {
                                        reports.add(MissedFragment.parseDocument(
                                                doc.getId(), doc.getData()));
                                    }

                                    runOnUiThread(() -> {
                                        adapter.notifyDataSetChanged();
                                        tvEmpty.setVisibility(
                                                reports.isEmpty() ? View.VISIBLE : View.GONE);
                                        if (reports.isEmpty()) {
                                            tvEmpty.setText("You haven't submitted any reports yet.");
                                        }
                                    });
                                })
                );

            } catch (Exception e) {
                Log.e(TAG, "exception: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error loading reports", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
}