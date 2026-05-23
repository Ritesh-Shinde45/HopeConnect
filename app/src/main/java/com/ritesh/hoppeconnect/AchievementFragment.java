package com.ritesh.hoppeconnect;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.appwrite.models.Document;
import io.appwrite.services.Databases;

public class AchievementFragment extends Fragment implements SearchableFragment {

    private static final String TAG = "AchievementFragment";

    private RecyclerView recyclerView;
    private LinearLayout emptyLayout;
    private TextView tvEmptyAchievement;
    private TextView tvTotalHelps, tvAchievementSubtitle;
    private TextView tvStatSightings, tvStatResolved, tvStatHelpers;
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

        recyclerView          = view.findViewById(R.id.rvAchievements);
        emptyLayout           = view.findViewById(R.id.layoutEmptyAchievement);
        tvEmptyAchievement    = view.findViewById(R.id.tvEmptyAchievement);
        tvTotalHelps          = view.findViewById(R.id.tvTotalHelps);
        tvAchievementSubtitle = view.findViewById(R.id.tvAchievementSubtitle);
        tvStatSightings       = view.findViewById(R.id.tvStatSightings);
        tvStatResolved        = view.findViewById(R.id.tvStatResolved);
        tvStatHelpers         = view.findViewById(R.id.tvStatHelpers);
        progressBar           = view.findViewById(R.id.progressAchievement);

        adapter = new HelpAdapter(displayedList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        loadAllHelps();
    }


    private void loadAllHelps() {
        showLoading(true);

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

                List<HelpModel> list      = new ArrayList<>();
                Set<String>     helperIds = new HashSet<>();  // unique helper users
                int             resolved  = 0;                // entries with resolvedAt set

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

                    if (!h.resolvedAt.isEmpty()) resolved++;

                    if (!h.watcherUserId.isEmpty()) helperIds.add(h.watcherUserId);
                }

                final int finalResolved = resolved;
                final int finalHelpers  = helperIds.size();
                // "Sightings" = total help records submitted (resolved or not)
                final int finalSightings = list.size();
                // "Total" in the hero = resolved cases only (the real impact number)
                final int finalTotal     = resolved;

                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    showLoading(false);
                    allItems.clear();
                    allItems.addAll(list);

                    if (tvTotalHelps != null)
                        tvTotalHelps.setText(String.valueOf(finalTotal));
                    if (tvAchievementSubtitle != null)
                        tvAchievementSubtitle.setText("missing persons helped find their families");

                    if (tvStatSightings != null)
                        tvStatSightings.setText(String.valueOf(finalSightings));
                    if (tvStatResolved  != null)
                        tvStatResolved.setText(String.valueOf(finalResolved));
                    if (tvStatHelpers   != null)
                        tvStatHelpers.setText(String.valueOf(finalHelpers));

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
        if (emptyLayout  != null) emptyLayout.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (recyclerView != null) recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void showLoading(boolean loading) {
        if (progressBar != null)
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void showEmpty(String msg) {
        showLoading(false);
        if (tvEmptyAchievement != null) tvEmptyAchievement.setText(msg);
        if (emptyLayout        != null) emptyLayout.setVisibility(View.VISIBLE);
        if (recyclerView       != null) recyclerView.setVisibility(View.GONE);
    }

    @Override
    public void onSearch(String query) {
        searchQuery = (query == null) ? "" : query.trim();
        applyFilter();
    }


    private String s(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v != null ? v.toString() : "";
    }
}