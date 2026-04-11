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
import io.appwrite.models.DocumentList;
import io.appwrite.services.Databases;

public class MissedFragment extends Fragment implements SearchableFragment {

    private static final String TAG = "MissedFragment";
    private static final String DATABASE_ID = AppwriteService.DB_ID;
    private static final String COLLECTION_ID = "reports";
    private androidx.recyclerview.widget.RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private ReportAdapter adapter;
    private final List<ReportModel> allReports = new ArrayList<>();
    private final List<ReportModel> visibleReports = new ArrayList<>();
    private Databases databases;

    public MissedFragment() {}

    public static MissedFragment newInstance() {
        return new MissedFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
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

        loadReportsFromNetwork();
        return v;
    }

    private void loadReportsFromNetwork() {
        if (databases == null) {
            Log.e(TAG, "Databases service is null");
            return;
        }
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        try {
            List<String> queries = new ArrayList<>();
            queries.add(Query.Companion.orderDesc("$createdAt"));

            databases.listDocuments(
                    DATABASE_ID,
                    COLLECTION_ID,
                    queries,
                    new CoroutineCallback<DocumentList<Map<String, Object>>>((result, error) -> {
                        if (getActivity() == null) return;

                        requireActivity().runOnUiThread(() -> {
                            if (progressBar != null) progressBar.setVisibility(View.GONE);
                        });

                        if (error != null) {
                            Log.e(TAG, "listDocuments error: " + error.getMessage(), error);
                            requireActivity().runOnUiThread(() -> {
                                if (tvEmpty != null) tvEmpty.setVisibility(View.VISIBLE);
                            });
                            return;
                        }

                        try {
                            allReports.clear();
                            List<io.appwrite.models.Document<Map<String, Object>>> docs =
                                    result.getDocuments();

                            for (io.appwrite.models.Document<Map<String, Object>> doc : docs) {
                                Map<String, Object> data = doc.getData();
                                ReportModel rm = parseDocument(doc.getId(), data);
                                allReports.add(rm);
                            }

                            visibleReports.clear();
                            visibleReports.addAll(allReports);

                            requireActivity().runOnUiThread(() -> {
                                adapter.notifyDataSetChanged();
                                if (tvEmpty != null) {
                                    tvEmpty.setVisibility(
                                            visibleReports.isEmpty() ? View.VISIBLE : View.GONE);
                                }
                            });

                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing documents: " + e.getMessage(), e);
                        }
                    })
            );
        } catch (Exception e) {
            Log.e(TAG, "Exception when listing: " + e.getMessage(), e);
            if (progressBar != null) progressBar.setVisibility(View.GONE);
        }
    }

    
    public static ReportModel parseDocument(String docId, Map<String, Object> data) {
        ReportModel rm = new ReportModel();
        rm.id = docId;

        rm.name = str(data, "name", "Unknown");
        rm.gender = str(data, "gender", "");
        rm.missingSince = str(data, "missingSince", "");
        rm.contact = str(data, "contact", "");
        rm.emergencyContact1 = str(data, "emergencyContact1", "");
        rm.emergencyContact2 = str(data, "emergencyContact2", "");
        rm.emergencyContact3 = str(data, "emergencyContact3", "");
        rm.description = str(data, "description", "");
        rm.status = str(data, "status", "active");
        rm.userId = str(data, "userId", "");
        rm.locationLat = str(data, "locationLat", "");
        rm.locationLng = str(data, "locationLng", "");
        rm.documentUrl = str(data, "documentUrl", "");

       
        Object ageObj = data.get("age");
        if (ageObj instanceof Number) {
            rm.age = ((Number) ageObj).intValue();
        } else if (ageObj != null) {
            try { rm.age = Integer.parseInt(ageObj.toString()); } catch (Exception ignored) {}
        }

       
       
        Object timeObj = data.get("createdAt");
        if (timeObj instanceof Number) {
            rm.createdAt = ((Number) timeObj).longValue();
        }

       
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
                    tvEmpty.setVisibility(visibleReports.isEmpty() ? View.VISIBLE : View.GONE);
                }
            });
        }
    }
}