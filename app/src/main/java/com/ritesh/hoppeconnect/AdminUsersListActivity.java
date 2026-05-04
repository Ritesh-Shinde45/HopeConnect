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

public class AdminUsersListActivity extends AppCompatActivity {

    private static final String TAG = "AdminUsersListAct";

    private RecyclerView rv;
    private SwipeRefreshLayout swipeRefresh;
    private TextView txtCount, txtTitle;
    private EditText etSearch;

    private AdminUserAdapter adapter;
    private final List<UserModel> allUsers  = new ArrayList<>();
    private final List<UserModel> displayed = new ArrayList<>();

    private String filterMode  = "all";
    private String searchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_users_list);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView()
                    .setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
        AppwriteService.init(this);

        filterMode = getIntent().getStringExtra("filter");
        if (filterMode == null) filterMode = "all";

        txtTitle    = findViewById(R.id.tv_users_list_title);
        txtCount    = findViewById(R.id.tv_users_list_count);
        etSearch    = findViewById(R.id.et_search_users_list);
        rv          = findViewById(R.id.rv_users_list);
        swipeRefresh = findViewById(R.id.swipe_refresh_users_list);

        txtTitle.setText("suspended".equals(filterMode) ? "Blocked Users" : "All Users");

        findViewById(R.id.btn_back_users).setOnClickListener(v -> finish());

        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminUserAdapter(displayed, new AdminUserAdapter.Listener() {
            @Override public void onSuspend(UserModel u, int pos)  { setStatus(u, "suspended"); }
            @Override public void onActivate(UserModel u, int pos) { setStatus(u, "active");    }
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
                List<? extends Document<?>> userDocs =
                        AppwriteHelper.listAllDocuments(
                                        db, AppwriteService.DB_ID, AppwriteService.COL_USERS)
                                .getDocuments();

                List<? extends Document<?>> reportDocs =
                        AppwriteHelper.listAllDocuments(
                                        db, AppwriteService.DB_ID, AppwriteService.COL_REPORTS)
                                .getDocuments();

                Map<String, Integer> reportCount = new HashMap<>();
                for (Document<?> rdoc : reportDocs) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) rdoc.getData();
                    String uid = data.get("userId") != null ? data.get("userId").toString() : "";
                    if (!uid.isEmpty()) reportCount.put(uid, reportCount.getOrDefault(uid, 0) + 1);
                }

                List<UserModel> list = new ArrayList<>();
                for (Document<?> doc : userDocs) {
                    UserModel u = UserModel.fromDocument(doc);
                    u.reportCount = reportCount.getOrDefault(u.id, 0);
                    list.add(u);
                }

                runOnUiThread(() -> {
                    swipeRefresh.setRefreshing(false);
                    allUsers.clear();
                    allUsers.addAll(list);
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
        for (UserModel u : allUsers) {
            boolean statusOk = "all".equals(filterMode) || u.status.equals(filterMode);
            boolean searchOk = searchQuery.isEmpty()
                    || u.name.toLowerCase().contains(searchQuery)
                    || u.email.toLowerCase().contains(searchQuery);
            if (statusOk && searchOk) displayed.add(u);
        }
        adapter.notifyDataSetChanged();
        txtCount.setText(displayed.size() + " user(s)");
    }

    private void setStatus(UserModel u, String newStatus) {
        new Thread(() -> {
            try {
                Databases db = AppwriteService.getDatabases();
                Map<String, Object> update = new HashMap<>();
                update.put("status", newStatus);
                AppwriteHelper.updateDocument(
                        db, AppwriteService.DB_ID, AppwriteService.COL_USERS, u.id, update);
                u.status = newStatus;
                runOnUiThread(() -> {
                    applyFilter();
                    Toast.makeText(this, u.name + " is now " + newStatus, Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                Log.e(TAG, "setStatus error", e);
                runOnUiThread(() ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}