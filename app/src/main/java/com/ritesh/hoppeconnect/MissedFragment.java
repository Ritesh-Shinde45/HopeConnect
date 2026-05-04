package com.ritesh.hoppeconnect;

import android.content.Intent;
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

import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.appwrite.Query;
import io.appwrite.coroutines.CoroutineCallback;
import io.appwrite.models.Document;
import io.appwrite.models.DocumentList;
import io.appwrite.services.Databases;

public class MissedFragment extends Fragment implements SearchableFragment {

    private static final String TAG           = "MissedFragment";
    private static final String DATABASE_ID   = AppwriteService.DB_ID;
    private static final String COLLECTION_ID = AppwriteService.COL_REPORTS;

    private androidx.recyclerview.widget.RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView    tvEmpty, tvResultCount;
    private ReportAdapter adapter;

    private final List<ReportModel> allReports     = new ArrayList<>();
    private final List<ReportModel> visibleReports = new ArrayList<>();

    private Databases databases;
    private String searchQuery  = "";
    private String activeFilter = "all";

    private Chip chipAll, chipMissing, chipFound,
            chipChildren, chipAdults, chipElderly,
            chipMale, chipFemale;

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

        recyclerView  = v.findViewById(R.id.missedRecyclerView);
        progressBar   = v.findViewById(R.id.progressBar);
        tvEmpty       = v.findViewById(R.id.emptyText);
        tvResultCount = v.findViewById(R.id.tvResultCount);

        chipAll      = v.findViewById(R.id.chipAll);
        chipMissing  = v.findViewById(R.id.chipMissing);
        chipFound    = v.findViewById(R.id.chipFound);
        chipChildren = v.findViewById(R.id.chipChildren);
        chipAdults   = v.findViewById(R.id.chipAdults);
        chipElderly  = v.findViewById(R.id.chipElderly);
        chipMale     = v.findViewById(R.id.chipMale);
        chipFemale   = v.findViewById(R.id.chipFemale);

        StaggeredGridLayoutManager staggered =
                new StaggeredGridLayoutManager(2,
                        StaggeredGridLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(staggered);

        adapter = new ReportAdapter(requireContext(), visibleReports, model -> {
            ReportModelCache.put(model);
            Intent intent = new Intent(requireContext(),
                    MissedPersonDetailActivity.class);
            intent.putExtra(MissedPersonDetailActivity.EXTRA_REPORT_ID,
                    model.id);
            requireContext().startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        AppwriteService.init(requireContext());
        databases = AppwriteService.getDatabases();

        setupChipListeners();
        loadByFilter("all");

        return v;
    }

   
    private void setupChipListeners() {
        if (chipAll      != null) chipAll.setOnClickListener(x      -> loadByFilter("all"));
        if (chipMissing  != null) chipMissing.setOnClickListener(x  -> loadByFilter("missing"));
        if (chipFound    != null) chipFound.setOnClickListener(x    -> loadByFilter("found"));
        if (chipChildren != null) chipChildren.setOnClickListener(x -> loadByFilter("children"));
        if (chipAdults   != null) chipAdults.setOnClickListener(x   -> loadByFilter("adults"));
        if (chipElderly  != null) chipElderly.setOnClickListener(x  -> loadByFilter("elderly"));
        if (chipMale     != null) chipMale.setOnClickListener(x     -> loadByFilter("male"));
        if (chipFemale   != null) chipFemale.setOnClickListener(x   -> loadByFilter("female"));
    }

   
    private void loadByFilter(String filter) {
        activeFilter = filter;
        allReports.clear();
        visibleReports.clear();

        if (progressBar   != null) progressBar.setVisibility(View.VISIBLE);
        if (tvEmpty       != null) tvEmpty.setVisibility(View.GONE);
        if (tvResultCount != null) tvResultCount.setText("Loading...");
        if (adapter       != null) adapter.notifyDataSetChanged();

        if (databases == null) {
            Log.e(TAG, "Databases service is null");
            return;
        }

        if ("all".equals(filter)) {
            loadAllApproved();
            return;
        }

        try {
            List<String> queries = new ArrayList<>();
            queries.add(Query.Companion.orderDesc("$createdAt"));
            queries.add(Query.Companion.limit(100));

            switch (filter) {
                case "missing":
                    queries.add(Query.Companion.equal("status", "active"));
                    break;
                case "found":
                    queries.add(Query.Companion.equal("status", "found"));
                    break;
                case "children":
                    queries.add(Query.Companion.equal("status", "active"));
                    queries.add(Query.Companion.lessThanEqual("age", 17));
                    break;
                case "adults":
                    queries.add(Query.Companion.equal("status", "active"));
                    queries.add(Query.Companion.between("age", 18, 59));
                    break;
                case "elderly":
                    queries.add(Query.Companion.equal("status", "active"));
                    queries.add(Query.Companion.greaterThanEqual("age", 60));
                    break;
                case "male":
                    queries.add(Query.Companion.equal("status", "active"));
                    queries.add(Query.Companion.equal("gender", "Male"));
                    break;
                case "female":
                    queries.add(Query.Companion.equal("status", "active"));
                    queries.add(Query.Companion.equal("gender", "Female"));
                    break;
            }

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
                                    Log.e(TAG, "filter error: "
                                            + error.getMessage());
                                    if (getActivity() != null)
                                        getActivity().runOnUiThread(() -> {
                                            if (tvEmpty != null) {
                                                tvEmpty.setText(
                                                        "Error loading reports");
                                                tvEmpty.setVisibility(
                                                        View.VISIBLE);
                                            }
                                            if (tvResultCount != null)
                                                tvResultCount.setText(
                                                        "0 report(s) found");
                                        });
                                    return;
                                }

                                allReports.clear();
                                addDocsToList(result, allReports);

                                if (getActivity() != null)
                                    getActivity().runOnUiThread(
                                            MissedFragment.this::applySearch);
                            })
            );

        } catch (Exception e) {
            Log.e(TAG, "loadByFilter exception: " + e.getMessage(), e);
            if (getActivity() != null)
                getActivity().runOnUiThread(() -> {
                    if (progressBar != null)
                        progressBar.setVisibility(View.GONE);
                });
        }
    }

   
    private void loadAllApproved() {
        if (databases == null) return;
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        try {
            List<String> activeQ = new ArrayList<>();
            activeQ.add(Query.Companion.equal("status", "active"));
            activeQ.add(Query.Companion.orderDesc("$createdAt"));
            activeQ.add(Query.Companion.limit(50));

            databases.listDocuments(
                    DATABASE_ID,
                    COLLECTION_ID,
                    activeQ,
                    new CoroutineCallback<DocumentList<Map<String, Object>>>(
                            (activeResult, activeError) -> {
                                if (getActivity() == null) return;

                                if (activeError != null) {
                                    Log.e(TAG, "active error: "
                                            + activeError.getMessage());
                                    loadAllAndFilterInMemory();
                                    return;
                                }

                                addDocsToList(activeResult, allReports);
                                loadFoundReports();
                            })
            );

        } catch (Exception e) {
            Log.e(TAG, "loadAllApproved exception: " + e.getMessage(), e);
            if (getActivity() != null)
                getActivity().runOnUiThread(() -> {
                    if (progressBar != null)
                        progressBar.setVisibility(View.GONE);
                });
        }
    }

   
    private void loadFoundReports() {
        try {
            List<String> foundQ = new ArrayList<>();
            foundQ.add(Query.Companion.equal("status", "found"));
            foundQ.add(Query.Companion.orderDesc("$createdAt"));
            foundQ.add(Query.Companion.limit(50));

            databases.listDocuments(
                    DATABASE_ID,
                    COLLECTION_ID,
                    foundQ,
                    new CoroutineCallback<DocumentList<Map<String, Object>>>(
                            (foundResult, foundError) -> {
                                if (getActivity() == null) return;

                                if (foundError == null && foundResult != null) {
                                    addDocsToList(foundResult, allReports);
                                }

                               
                                allReports.sort((a, b) ->
                                        Long.compare(b.createdAt, a.createdAt));

                                if (getActivity() != null)
                                    getActivity().runOnUiThread(() -> {
                                        if (progressBar != null)
                                            progressBar.setVisibility(View.GONE);
                                        applySearch();
                                    });
                            })
            );
        } catch (Exception e) {
            Log.w(TAG, "loadFoundReports: " + e.getMessage());
            if (getActivity() != null)
                getActivity().runOnUiThread(() -> {
                    if (progressBar != null)
                        progressBar.setVisibility(View.GONE);
                    applySearch();
                });
        }
    }

   
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
                                    Log.e(TAG, "fallback error: "
                                            + error.getMessage());
                                    if (getActivity() != null)
                                        getActivity().runOnUiThread(() -> {
                                            if (tvEmpty != null)
                                                tvEmpty.setVisibility(View.VISIBLE);
                                        });
                                    return;
                                }

                                allReports.clear();

                               
                                @SuppressWarnings("unchecked")
                                List<Document<Map<String, Object>>> docs =
                                        (List<Document<Map<String, Object>>>)
                                                (List<?>) result.getDocuments();

                                for (Document<Map<String, Object>> doc : docs) {
                                    ReportModel rm = parseDocument(
                                            doc.getId(),
                                            doc.getData(),
                                            doc.getCreatedAt());
                                    if ("active".equals(rm.status)
                                            || "found".equals(rm.status)) {
                                        allReports.add(rm);
                                    }
                                }

                                if (getActivity() != null)
                                    getActivity().runOnUiThread(
                                            MissedFragment.this::applySearch);
                            })
            );
        } catch (Exception e) {
            Log.e(TAG, "fallback exception: " + e.getMessage(), e);
        }
    }

    
    @SuppressWarnings("unchecked")
    private void addDocsToList(
            DocumentList<Map<String, Object>> result,
            List<ReportModel> target) {
        List<Document<Map<String, Object>>> docs =
                (List<Document<Map<String, Object>>>)
                        (List<?>) result.getDocuments();
        for (Document<Map<String, Object>> doc : docs) {
            target.add(parseDocument(
                    doc.getId(),
                    doc.getData(),
                    doc.getCreatedAt()));
        }
    }

   
    private void applySearch() {
        visibleReports.clear();
        String q = searchQuery.trim().toLowerCase();

        for (ReportModel r : allReports) {
            if (q.isEmpty()) {
                visibleReports.add(r);
            } else {
                boolean match =
                        (r.name != null
                                && r.name.toLowerCase().contains(q)) ||
                                (r.description != null
                                        && r.description.toLowerCase().contains(q)) ||
                                (r.missingSince != null
                                        && r.missingSince.toLowerCase().contains(q));
                if (match) visibleReports.add(r);
            }
        }

        adapter.notifyDataSetChanged();

        boolean empty = visibleReports.isEmpty();
        if (tvEmpty != null) {
            tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            if (empty) tvEmpty.setText("No reports found");
        }
        if (recyclerView != null)
            recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (tvResultCount != null)
            tvResultCount.setText(visibleReports.size() + " report(s) found");
    }

    @Override
    public void onSearch(String query) {
        searchQuery = query == null ? "" : query.trim();
        applySearch();
    }

   
    public static ReportModel parseDocument(String docId,
                                            Map<String, Object> data) {
        return parseDocument(docId, data, null);
    }

   
    public static ReportModel parseDocument(String docId,
                                            Map<String, Object> data,
                                            String createdAtIso) {
        ReportModel rm = new ReportModel();
        rm.id = docId;

        rm.name              = str(data, "name",              "Unknown");
        rm.gender            = str(data, "gender",            "");
        rm.missingSince      = str(data, "missingSince",      "");
        rm.contact           = str(data, "contact",           "");
        rm.emergencyContact1 = str(data, "emergencyContact1", "");
        rm.emergencyContact2 = str(data, "emergencyContact2", "");
        rm.emergencyContact3 = str(data, "emergencyContact3", "");
        rm.description       = str(data, "description",       "");
        rm.status            = str(data, "status",            "pending");
        rm.userId            = str(data, "userId",            "");
        rm.locationLat       = str(data, "locationLat",       "");
        rm.locationLng       = str(data, "locationLng",       "");
        rm.documentUrl       = str(data, "documentUrl",       "");

       
        Object ageObj = data.get("age");
        if (ageObj instanceof Number) {
            rm.age = ((Number) ageObj).intValue();
        } else if (ageObj != null) {
            try { rm.age = Integer.parseInt(ageObj.toString()); }
            catch (Exception ignored) {}
        }

       
        Object timeObj = data.get("createdAt");
        if (timeObj instanceof Number) {
            rm.createdAt = ((Number) timeObj).longValue();
        }

       
        if (rm.createdAt == 0 && createdAtIso != null
                && createdAtIso.length() >= 10) {
            try {
                java.text.SimpleDateFormat sdf =
                        new java.text.SimpleDateFormat(
                                "yyyy-MM-dd'T'HH:mm:ss.SSSSSS",
                                java.util.Locale.getDefault());
                java.util.Date d = sdf.parse(createdAtIso);
                if (d != null) rm.createdAt = d.getTime();
            } catch (Exception e1) {
                try {
                    java.text.SimpleDateFormat sdf2 =
                            new java.text.SimpleDateFormat(
                                    "yyyy-MM-dd",
                                    java.util.Locale.getDefault());
                    java.util.Date d = sdf2.parse(
                            createdAtIso.substring(0, 10));
                    if (d != null) rm.createdAt = d.getTime();
                } catch (Exception ignored) {}
            }
        }

       
        Object pf = data.get("photoUrls");
        if (pf instanceof List) {
            List<?> pL = (List<?>) pf;
            List<String> urls = new ArrayList<>();
            for (int i = 0; i < pL.size(); i++) {
                Object u = pL.get(i);
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

    private static String str(Map<String, Object> data,
                              String key, String fallback) {
        Object v = data.get(key);
        return (v != null && !v.toString().isEmpty())
                ? v.toString() : fallback;
    }
}