package com.ritesh.hoppeconnect;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.appwrite.models.Document;
import io.appwrite.services.Databases;

public class AdminManageUsersFragment extends Fragment {

    private static final String TAG = "AdminManageUsersFrag";

    private EditText etSearch;
    private Chip chipAll, chipActive, chipSuspended;
    private TextView txtCount;
    private RecyclerView rv;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressBar;

    private AdminUserAdapter adapter;
    private final List<UserModel> allUsers  = new ArrayList<>();
    private final List<UserModel> displayed = new ArrayList<>();

    private String filter      = "all";
    private String searchQuery = "";

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s) {
        return inf.inflate(R.layout.admin_all_users, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);

        etSearch      = v.findViewById(R.id.et_search_users);
        chipAll       = v.findViewById(R.id.chip_all);
        chipActive    = v.findViewById(R.id.chip_active);
        chipSuspended = v.findViewById(R.id.chip_suspended);
        txtCount      = v.findViewById(R.id.txtUserCount);
        rv            = v.findViewById(R.id.rv_users);
        swipeRefresh  = v.findViewById(R.id.swipe_refresh_users);
        progressBar   = v.findViewById(R.id.progressBarUsers);

        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AdminUserAdapter(displayed, new AdminUserAdapter.Listener() {
            @Override public void onSuspend(UserModel u, int pos)  { setUserStatus(u, "suspended", pos); }
            @Override public void onActivate(UserModel u, int pos) { setUserStatus(u, "active",    pos); }
        });
        rv.setAdapter(adapter);

        chipAll.setOnClickListener(x       -> { filter="all";       applyFilter(); });
        chipActive.setOnClickListener(x    -> { filter="active";    applyFilter(); });
        chipSuspended.setOnClickListener(x -> { filter="suspended"; applyFilter(); });

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
        progressBar.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                Databases db = AppwriteService.getDatabases();
                List<? extends Document<?>> docs =
                        AppwriteHelper.listAllDocuments(db, AppwriteService.DB_ID, AppwriteService.COL_USERS)
                                .getDocuments();

               
                List<? extends Document<?>> reportDocs =
                        AppwriteHelper.listAllDocuments(db, AppwriteService.DB_ID, AppwriteService.COL_REPORTS)
                                .getDocuments();

               
                Map<String, Integer> reportCount = new HashMap<>();
                for (Document<?> rdoc : reportDocs) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) rdoc.getData();
                    String uid = data.get("reportedBy") != null ? data.get("reportedBy").toString() : "";
                    if (!uid.isEmpty()) reportCount.put(uid, reportCount.getOrDefault(uid, 0) + 1);
                }

                List<UserModel> list = new ArrayList<>();
                for (Document<?> doc : docs) {
                    UserModel u = UserModel.fromDocument(doc);
                    u.reportCount = reportCount.getOrDefault(u.id, 0);
                    list.add(u);
                }

                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    swipeRefresh.setRefreshing(false);
                    progressBar.setVisibility(View.GONE);
                    allUsers.clear();
                    allUsers.addAll(list);
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
        for (UserModel u : allUsers) {
            boolean statusOk = "all".equals(filter) || u.status.equals(filter);
            boolean searchOk = searchQuery.isEmpty()
                    || u.name.toLowerCase().contains(searchQuery)
                    || u.email.toLowerCase().contains(searchQuery);
            if (statusOk && searchOk) displayed.add(u);
        }
        adapter.notifyDataSetChanged();
        txtCount.setText(displayed.size() + " user(s)");
    }

    private void setUserStatus(UserModel u, String newStatus, int pos) {
        new Thread(() -> {
            try {
                Databases db = AppwriteService.getDatabases();
                Map<String, Object> update = new HashMap<>();
                update.put("status", newStatus);
                AppwriteHelper.updateDocument(
                        db, AppwriteService.DB_ID, AppwriteService.COL_USERS, u.id, update
                );
                u.status = newStatus;
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    applyFilter();
                    Toast.makeText(getContext(),
                            u.name + " is now " + newStatus, Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                Log.e(TAG, "setUserStatus error", e);
                if (getActivity() != null)
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}