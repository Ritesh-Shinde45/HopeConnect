package com.ritesh.hoppeconnect.admin;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.ritesh.hoppeconnect.AppwriteHelper;
import com.ritesh.hoppeconnect.AppwriteService;
import com.ritesh.hoppeconnect.R;
import io.appwrite.models.Document;
import io.appwrite.services.Databases;
import java.util.*;

public class AdminApproveReportsFragment extends Fragment {

    private ListView listView;
    private ArrayAdapter<String> adapter;
    private final List<String> reportTitles = new ArrayList<>();
    private final List<String> reportIds    = new ArrayList<>();

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_approve, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        listView = view.findViewById(R.id.lvPendingReports);

        adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, reportTitles);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, v, pos, id) ->
                approveReport(reportIds.get(pos), pos));

        loadPendingReports();
    }

    private void loadPendingReports() {
        new Thread(() -> {
            try {
                Databases db = AppwriteService.getDatabases();
                List<? extends Document<?>> docs =
                        AppwriteHelper.findDocumentsByField(
                                db, AppwriteService.DB_ID,
                                AppwriteService.COL_REPORTS,
                                "status", "pending"
                        ).getDocuments();

                reportTitles.clear();
                reportIds.clear();
                for (Document<?> doc : docs) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) doc.getData();
                    String title = data.containsKey("title")
                            ? data.get("title").toString() : doc.getId();
                    reportTitles.add(title);
                    reportIds.add(doc.getId());
                }

                requireActivity().runOnUiThread(() -> adapter.notifyDataSetChanged());
            } catch (Exception e) {
                android.util.Log.e("AdminApprove", "Load error", e);
            }
        }).start();
    }

    private void approveReport(String docId, int position) {
        new Thread(() -> {
            try {
                Databases db = AppwriteService.getDatabases();
                Map<String, Object> update = new HashMap<>();
                update.put("status", "approved");
                AppwriteHelper.updateDocument(
                        db, AppwriteService.DB_ID,
                        AppwriteService.COL_REPORTS, docId, update);

                requireActivity().runOnUiThread(() -> {
                    reportTitles.remove(position);
                    reportIds.remove(position);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(requireContext(), "Report approved!", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                android.util.Log.e("AdminApprove", "Approve error", e);
            }
        }).start();
    }
}