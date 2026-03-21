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
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.appwrite.Query;
import io.appwrite.coroutines.CoroutineCallback;
import io.appwrite.exceptions.AppwriteException;
import io.appwrite.models.DocumentList;
import io.appwrite.services.Databases;

public class MissedFragment extends Fragment implements SearchableFragment {

    private static final String TAG           = "MissedFragment";
    private static final String DATABASE_ID   = AppwriteService.DB_ID;
    private static final String COLLECTION_ID = AppwriteService.COL_REPORTS;

    private androidx.recyclerview.widget.RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private ReportAdapter adapter;
    private final List<ReportModel> allReports     = new ArrayList<>();
    private final List<ReportModel> visibleReports = new ArrayList<>();
    private Databases databases;

    public MissedFragment() {}

    public static MissedFragment newInstance() {
        return new MissedFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_missed, container, false);

        recyclerView = v.findViewById(R.id.missedRecyclerView);
        progressBar  = v.findViewById(R.id.progressBar);
        tvEmpty      = v.findViewById(R.id.emptyText);

        StaggeredGridLayoutManager staggered =
                new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(staggered);

        adapter = new ReportAdapter(requireContext(), visibleReports, model -> {
            ReportModelCache.put(model);
            android.content.Intent intent =
                    new android.content.Intent(requireContext(), MissedPersonDetailActivity.class);
            intent.putExtra(MissedPersonDetailActivity.EXTRA_REPORT_ID, model.id);
            requireContext().startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        AppwriteService.init(requireContext());
        databases = AppwriteService.getDatabases();

        loadApprovedReports();
        return v;
    }

    /**
     * Loads ONLY approved reports (status = "active" or "found").
     * Pending, fake, and rejected reports are never shown to regular users.
     */
    private void loadApprovedReports() {
        if (databases == null) {
            Log.e(TAG, "Databases service is null");
            return;
        }
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        try {
            // ── Query 1: active (approved missing) reports ────────────────
            List<String> activeQueries = new ArrayList<>();
            activeQueries.add(Query.Companion.equal("status", "active"));
            activeQueries.add(Query.Companion.orderDesc("$createdAt"));
            activeQueries.add(Query.Companion.limit(50));

            databases.listDocuments(
                    DATABASE_ID,
                    COLLECTION_ID,
                    activeQueries,
                    new CoroutineCallback<DocumentList<Map<String, Object>>>(
                            (activeResult, activeError) -> {
                                if (getActivity() == null) return;

                                if (activeError != null) {
                                    Log.e(TAG, "active query error: "
                                            + activeError.getMessage(), activeError);
                                    // Fall back to unfiltered if status field missing
                                    loadAllAndFilterInMemory();
                                    return;
                                }

                                // ── Query 2: found reports ────────────────
                                List<String> foundQueries = new ArrayList<>();
                                foundQueries.add(Query.Companion.equal("status", "found"));
                                foundQueries.add(Query.Companion.orderDesc("$createdAt"));
                                foundQueries.add(Query.Companion.limit(50));

                                try {
                                    databases.listDocuments(
                                            DATABASE_ID,
                                            COLLECTION_ID,
                                            foundQueries,
                                            new CoroutineCallback<DocumentList<Map<String, Object>>>(
                                                    (foundResult, foundError) -> {
                                                        if (getActivity() == null) return;

                                                        getActivity().runOnUiThread(() -> {
                                                            if (progressBar != null)
                                                                progressBar.setVisibility(View.GONE);

                                                            allReports.clear();

                                                            // Add active reports
                                                            for (io.appwrite.models.Document<Map<String, Object>> doc
                                                                    : activeResult.getDocuments()) {
                                                                allReports.add(parseDocument(
                                                                        doc.getId(), doc.getData()));
                                                            }

                                                            // Add found reports
                                                            if (foundError == null && foundResult != null) {
                                                                for (io.appwrite.models.Document<Map<String, Object>> doc
                                                                        : foundResult.getDocuments()) {
                                                                    allReports.add(parseDocument(
                                                                            doc.getId(), doc.getData()));
                                                                }
                                                            }

                                                            visibleReports.clear();
                                                            visibleReports.addAll(allReports);
                                                            adapter.notifyDataSetChanged();

                                                            if (tvEmpty != null) {
                                                                tvEmpty.setVisibility(
                                                                        visibleReports.isEmpty()
                                                                                ? View.VISIBLE
                                                                                : View.GONE);
                                                            }

                                                            Log.d(TAG, "Loaded "
                                                                    + allReports.size()
                                                                    + " approved reports");
                                                        });
                                                    })
                                    );
                                } catch (AppwriteException e) {
                                    throw new RuntimeException(e);
                                }
                            })
            );

        } catch (Exception e) {
            Log.e(TAG, "Exception when listing: " + e.getMessage(), e);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                });
            }
        }
    }

    /**
     * Fallback: if status field doesn't exist in Appwrite yet,
     * fetch all and filter in memory.
     * Remove this method once status column is confirmed added.
     */
    private void loadAllAndFilterInMemory() {
        try {
            List<String> queries = new ArrayList<>();
            queries.add(Query.Companion.orderDesc("$createdAt"));
            queries.add(Query.Companion.limit(100));

            databases.listDocuments(
                    DATABASE_ID,
                    COLLECTION_ID,
                    queries,
                    new CoroutineCallback<DocumentList<Map<String, Object>>>(
                            (result, error) -> {
                                if (getActivity() == null) return;

                                getActivity().runOnUiThread(() -> {
                                    if (progressBar != null)
                                        progressBar.setVisibility(View.GONE);
                                });

                                if (error != null) {
                                    Log.e(TAG, "fallback error: " + error.getMessage());
                                    getActivity().runOnUiThread(() -> {
                                        if (tvEmpty != null) tvEmpty.setVisibility(View.VISIBLE);
                                    });
                                    return;
                                }

                                allReports.clear();
                                for (io.appwrite.models.Document<Map<String, Object>> doc
                                        : result.getDocuments()) {
                                    ReportModel rm = parseDocument(doc.getId(), doc.getData());
                                    // In-memory filter: only active or found
                                    // If status is missing treat as pending — don't show
                                    if ("active".equals(rm.status)
                                            || "found".equals(rm.status)) {
                                        allReports.add(rm);
                                    }
                                }

                                getActivity().runOnUiThread(() -> {
                                    visibleReports.clear();
                                    visibleReports.addAll(allReports);
                                    adapter.notifyDataSetChanged();
                                    if (tvEmpty != null) {
                                        tvEmpty.setVisibility(
                                                visibleReports.isEmpty()
                                                        ? View.VISIBLE : View.GONE);
                                    }
                                    Log.d(TAG, "Fallback loaded "
                                            + allReports.size() + " approved reports");
                                });
                            })
            );
        } catch (Exception e) {
            Log.e(TAG, "fallback exception: " + e.getMessage(), e);
        }
    }

    /**
     * Central parser — reads every field saved by NewReportActivity.
     */
    public static ReportModel parseDocument(String docId, Map<String, Object> data) {
        ReportModel rm = new ReportModel();
        rm.id = docId;

        rm.name             = str(data, "name",             "Unknown");
        rm.gender           = str(data, "gender",           "");
        rm.missingSince     = str(data, "missingSince",     "");
        rm.contact          = str(data, "contact",          "");
        rm.emergencyContact1 = str(data, "emergencyContact1", "");
        rm.emergencyContact2 = str(data, "emergencyContact2", "");
        rm.emergencyContact3 = str(data, "emergencyContact3", "");
        rm.description      = str(data, "description",      "");
        rm.status           = str(data, "status",           "pending");
        rm.userId           = str(data, "userId",           "");
        rm.locationLat      = str(data, "locationLat",      "");
        rm.locationLng      = str(data, "locationLng",      "");
        rm.documentUrl      = str(data, "documentUrl",      "");

        // Age
        Object ageObj = data.get("age");
        if (ageObj instanceof Number) {
            rm.age = ((Number) ageObj).intValue();
        } else if (ageObj != null) {
            try { rm.age = Integer.parseInt(ageObj.toString()); }
            catch (Exception ignored) {}
        }

        // createdAt timestamp
        Object timeObj = data.get("createdAt");
        if (timeObj instanceof Number) {
            rm.createdAt = ((Number) timeObj).longValue();
        }

        // Photo URLs — append project ID so Appwrite serves them publicly
        Object pf = data.get("photoUrls");
        if (pf instanceof List) {
            List<?> pL = (List<?>) pf;
            List<String> urls = new ArrayList<>();
            for (Object u : pL) {
                if (u != null) {
                    String url = u.toString();
                    if (!url.contains("project=")) {
                        url = url + "?project=" + AppwriteService.PROJECT_ID;
                    }
                    urls.add(url);
                }
            }
            rm.photoUrls = urls;
        }

        return rm;
    }

    private static String str(Map<String, Object> data, String key, String fallback) {
        Object v = data.get(key);
        return (v != null && !v.toString().isEmpty()) ? v.toString() : fallback;
    }

    @Override
    public void onSearch(String query) {
        String q = query == null ? "" : query.trim().toLowerCase();
        visibleReports.clear();
        if (q.isEmpty()) {
            visibleReports.addAll(allReports);
        } else {
            for (ReportModel r : allReports) {
                // allReports already contains only approved — just search within them
                if ((r.name != null && r.name.toLowerCase().contains(q)) ||
                        (r.description != null && r.description.toLowerCase().contains(q)) ||
                        (r.missingSince != null && r.missingSince.toLowerCase().contains(q))) {
                    visibleReports.add(r);
                }
            }
        }
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                adapter.notifyDataSetChanged();
                if (tvEmpty != null) {
                    tvEmpty.setVisibility(
                            visibleReports.isEmpty() ? View.VISIBLE : View.GONE);
                }
            });
        }
    }
}