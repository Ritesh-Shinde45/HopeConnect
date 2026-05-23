package com.ritesh.hoppeconnect;

import android.content.SharedPreferences;
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
import java.util.List;
import java.util.Map;

import io.appwrite.models.Document;
import io.appwrite.services.Databases;

public class HelpFragment extends Fragment implements SearchableFragment {

    private static final String TAG = "HelpFragment";

    private RecyclerView recyclerView;
    private LinearLayout emptyLayout;
    private TextView tvEmptyMsg;
    private TextView tvStatTotal, tvStatConfirmed, tvStatPending;
    private ProgressBar progressBar;
    private HelpAdapter adapter;

    private final List<HelpModel> allItems      = new ArrayList<>();
    private final List<HelpModel> displayedList = new ArrayList<>();
    private String searchQuery = "";

    public HelpFragment() {}

    public static HelpFragment newInstance() {
        return new HelpFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_help, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView    = view.findViewById(R.id.rvHelps);
        emptyLayout     = view.findViewById(R.id.tvEmptyHelp);      // now a LinearLayout
        tvEmptyMsg      = view.findViewById(R.id.tvEmptyHelpMsg);
        tvStatTotal     = view.findViewById(R.id.tvStatTotal);
        tvStatConfirmed = view.findViewById(R.id.tvStatConfirmed);
        tvStatPending   = view.findViewById(R.id.tvStatPending);
        progressBar     = view.findViewById(R.id.progressHelp);

        adapter = new HelpAdapter(displayedList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        loadMyHelps();
    }


    private void loadMyHelps() {
        showLoading(true);

        SharedPreferences prefs = requireContext()
                .getSharedPreferences("hoppe_prefs", android.content.Context.MODE_PRIVATE);
        String myUserId = prefs.getString("logged_in_user_id", "");

        if (myUserId.isEmpty()) {
            showEmpty("Log in to see your helps");
            return;
        }

        new Thread(() -> {
            try {
                AppwriteService.init(requireContext());
                Databases db = AppwriteService.getDatabases();

                List<? extends Document<?>> docs =
                        AppwriteHelper.listDocuments(
                                db,
                                AppwriteService.DB_ID,
                                AppwriteService.COL_HELPS,
                                java.util.Arrays.asList(
                                        io.appwrite.Query.Companion.equal("watcherUserId", myUserId),
                                        io.appwrite.Query.Companion.orderDesc("$createdAt"),
                                        io.appwrite.Query.Companion.limit(50)
                                )
                        ).getDocuments();

                List<HelpModel> list = new ArrayList<>();
                int confirmed = 0;
                int pending   = 0;

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

                    if (!h.resolvedAt.isEmpty()) confirmed++;
                    else pending++;
                }

                final int finalConfirmed = confirmed;
                final int finalPending   = pending;

                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    showLoading(false);
                    allItems.clear();
                    allItems.addAll(list);

                    if (tvStatTotal     != null) tvStatTotal.setText(String.valueOf(list.size()));
                    if (tvStatConfirmed != null) tvStatConfirmed.setText(String.valueOf(finalConfirmed));
                    if (tvStatPending   != null) tvStatPending.setText(String.valueOf(finalPending));

                    applyFilter();
                });

            } catch (Exception e) {
                Log.e(TAG, "loadMyHelps: " + e.getMessage(), e);
                if (getActivity() != null)
                    getActivity().runOnUiThread(() -> showEmpty("Could not load helps"));
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
                if ((h.reportName != null && h.reportName.toLowerCase().contains(q)) ||
                        (h.message    != null && h.message.toLowerCase().contains(q))) {
                    displayedList.add(h);
                }
            }
        }
        adapter.notifyDataSetChanged();

        boolean empty = displayedList.isEmpty();
        if (emptyLayout   != null) emptyLayout.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (recyclerView  != null) recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void showLoading(boolean loading) {
        if (progressBar != null)
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void showEmpty(String msg) {
        showLoading(false);
        if (tvEmptyMsg  != null) tvEmptyMsg.setText(msg);
        if (emptyLayout != null) emptyLayout.setVisibility(View.VISIBLE);
        if (recyclerView != null) recyclerView.setVisibility(View.GONE);
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