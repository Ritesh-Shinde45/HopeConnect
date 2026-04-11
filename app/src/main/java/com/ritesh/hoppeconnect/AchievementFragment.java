package com.ritesh.hoppeconnect;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.appwrite.models.Document;
import io.appwrite.services.Databases;

public class AchievementFragment extends Fragment implements SearchableFragment {

    private static final String TAG = "AchievementFragment";

    private RecyclerView recyclerView;
    private TextView emptyText, tvTotalHelps, tvSubtitle;
    private ProgressBar progressBar;
    private HelpAdapter adapter;
    private final List<HelpModel> allItems      = new ArrayList<>();
    private final List<HelpModel> displayedList = new ArrayList<>();
    private String searchQuery = "";

    public AchievementFragment() {}

    public static AchievementFragment newInstance() {
        return new AchievementFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_achievements, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView  = view.findViewById(R.id.rvAchievements);
        emptyText     = view.findViewById(R.id.tvEmptyAchievement);
        tvTotalHelps  = view.findViewById(R.id.tvTotalHelps);
        tvSubtitle    = view.findViewById(R.id.tvAchievementSubtitle);
        progressBar   = view.findViewById(R.id.progressAchievement);

        adapter = new HelpAdapter(displayedList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        loadAllHelps();
    }

    /** Loads ALL helps across all users — shows community achievements */
    private void loadAllHelps() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                AppwriteService.init(requireContext());
                Databases db = AppwriteService.getDatabases();

                List<? extends Document<?>> docs =
                        AppwriteHelper.listAllDocuments(
                                db,
                                AppwriteService.DB_ID,
                                AppwriteService.COL_HELPS
                        ).getDocuments();

                List<HelpModel> list = new ArrayList<>();
                for (Document<?> doc : docs) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) doc.getData();
                    HelpModel h = new HelpModel();
                    h.id            = doc.getId();
                    h.watcherUserId = s(data, "watcherUserId");
                    h.watcherName   = s(data, "watcherName");
                    h.reportName    = s(data, "reportName");
                    h.reportId      = s(data, "reportId");
                    h.resolvedAt    = s(data, "resolvedAt");
                    h.message       = s(data, "message");
                    list.add(h);
                }

                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    allItems.clear();
                    allItems.addAll(list);
                    if (tvTotalHelps != null)
                        tvTotalHelps.setText(String.valueOf(list.size()));
                    if (tvSubtitle != null)
                        tvSubtitle.setText("missing persons helped find their families");
                    applyFilter();
                });

            } catch (Exception e) {
                Log.e(TAG, "loadAllHelps: " + e.getMessage(), e);
                if (getActivity() != null)
                    getActivity().runOnUiThread(() ->
                            showEmpty("Could not load achievements"));
            }
        }).start();
    }

    private void applyFilter() {
        displayedList.clear();
        if (searchQuery.isEmpty()) {
            displayedList.addAll(allItems);
        } else {
            String q = searchQuery.toLowerCase();
            for (HelpModel h : allItems) {
                if ((h.reportName  != null && h.reportName.toLowerCase().contains(q)) ||
                        (h.watcherName != null && h.watcherName.toLowerCase().contains(q))) {
                    displayedList.add(h);
                }
            }
        }
        adapter.notifyDataSetChanged();
        boolean empty = displayedList.isEmpty();
        if (emptyText != null)
            emptyText.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (recyclerView != null)
            recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void showEmpty(String msg) {
        if (progressBar != null) progressBar.setVisibility(View.GONE);
        if (emptyText != null) {
            emptyText.setText(msg);
            emptyText.setVisibility(View.VISIBLE);
        }
        if (recyclerView != null) recyclerView.setVisibility(View.GONE);
    }

    @Override
    public void onSearch(String query) {
        searchQuery = query == null ? "" : query.trim();
        applyFilter();
    }

    private String s(Map<String, Object> m, String k) {
        Object v = m.get(k); return v != null ? v.toString() : "";
    }
}