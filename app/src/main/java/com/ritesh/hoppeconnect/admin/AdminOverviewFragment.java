package com.ritesh.hoppeconnect.admin;

import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.ritesh.hoppeconnect.AppwriteHelper;
import com.ritesh.hoppeconnect.AppwriteService;
import com.ritesh.hoppeconnect.R;
import io.appwrite.services.Databases;
import java.util.List;

public class AdminOverviewFragment extends Fragment {

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_overview, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadStats(view);
    }

    private void loadStats(View view) {
        new Thread(() -> {
            try {
                Databases db = AppwriteService.getDatabases();

                // Total users
                int totalUsers = AppwriteHelper
                        .listAllDocuments(db, AppwriteService.DB_ID, AppwriteService.COL_USERS)
                        .getDocuments().size();

                // Pending reports (status == "pending")
                int pending = AppwriteHelper
                        .findDocumentsByField(db, AppwriteService.DB_ID,
                                AppwriteService.COL_REPORTS, "status", "pending")
                        .getDocuments().size();

                // Approved reports
                int approved = AppwriteHelper
                        .findDocumentsByField(db, AppwriteService.DB_ID,
                                AppwriteService.COL_REPORTS, "status", "approved")
                        .getDocuments().size();

                // Spammed reports
                int spammed = AppwriteHelper
                        .findDocumentsByField(db, AppwriteService.DB_ID,
                                AppwriteService.COL_REPORTS, "status", "spammed")
                        .getDocuments().size();

                requireActivity().runOnUiThread(() -> {
                    TextView tvUsers    = view.findViewById(R.id.tvTotalUsers);
                    TextView tvPending  = view.findViewById(R.id.tvPendingReports);
                    TextView tvApproved = view.findViewById(R.id.tvApprovedReports);
                    TextView tvSpammed  = view.findViewById(R.id.tvSpammedReports);

                    if (tvUsers    != null) tvUsers.setText("Total Users: "      + totalUsers);
                    if (tvPending  != null) tvPending.setText("Pending: "        + pending);
                    if (tvApproved != null) tvApproved.setText("Approved: "      + approved);
                    if (tvSpammed  != null) tvSpammed.setText("Spammed/Flagged: " + spammed);
                });
            } catch (Exception e) {
                // silently log; add error UI as needed
                android.util.Log.e("AdminOverview", "Stats load error", e);
            }
        }).start();
    }
}