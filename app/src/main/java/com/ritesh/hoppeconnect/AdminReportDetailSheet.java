package com.ritesh.hoppeconnect;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.HashMap;
import java.util.Map;

import io.appwrite.services.Databases;

public class AdminReportDetailSheet extends BottomSheetDialogFragment {

    private static final String TAG          = "AdminReportDetailSheet";
    private static final String ARG_ID       = "id";
    private static final String ARG_NAME     = "name";
    private static final String ARG_AGE      = "age";
    private static final String ARG_GENDER   = "gender";
    private static final String ARG_LOCATION = "location";
    private static final String ARG_STATUS   = "status";
    private static final String ARG_REPORTER = "reporterName";
    private static final String ARG_CONTACT  = "contact";
    private static final String ARG_PHOTO    = "photoFileId";
    private static final String ARG_DATE     = "createdAt";

    public static AdminReportDetailSheet newInstance(AdminReportModel r) {
        AdminReportDetailSheet sheet = new AdminReportDetailSheet();
        Bundle b = new Bundle();
        b.putString(ARG_ID,       r.id);
        b.putString(ARG_NAME,     r.name);
        b.putString(ARG_AGE,      r.age);
        b.putString(ARG_GENDER,   r.gender);
        b.putString(ARG_LOCATION, r.location);
        b.putString(ARG_STATUS,   r.status);
        b.putString(ARG_REPORTER, r.reporterName);
        b.putString(ARG_CONTACT,  r.contact);
        b.putString(ARG_PHOTO,    r.photoFileId);
        b.putString(ARG_DATE,     r.createdAt);
        sheet.setArguments(b);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inf,
                             @Nullable ViewGroup c, @Nullable Bundle s) {
        return inf.inflate(R.layout.bottom_sheet_report_detail, c, false);

    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);
        Bundle b = requireArguments();

        String id     = b.getString(ARG_ID, "");
        String name   = b.getString(ARG_NAME, "");
        String status = b.getString(ARG_STATUS, "pending");

        ((TextView) v.findViewById(R.id.tv_detail_report_id))
                .setText("ID: " + id.substring(0, Math.min(8, id.length())));
        ((TextView) v.findViewById(R.id.tv_detail_name)).setText(name);
        ((TextView) v.findViewById(R.id.tv_detail_status)).setText(status.toUpperCase());
        ((TextView) v.findViewById(R.id.tv_detail_age)).setText(b.getString(ARG_AGE, "—"));
        ((TextView) v.findViewById(R.id.tv_detail_gender)).setText(b.getString(ARG_GENDER, "—"));
        ((TextView) v.findViewById(R.id.tv_detail_location)).setText(b.getString(ARG_LOCATION, "—"));
        ((TextView) v.findViewById(R.id.tv_detail_date)).setText(b.getString(ARG_DATE, "—"));
        ((TextView) v.findViewById(R.id.tv_detail_reporter)).setText(b.getString(ARG_REPORTER, "—"));
        ((TextView) v.findViewById(R.id.tv_detail_contact)).setText(b.getString(ARG_CONTACT, "—"));

        String photoId = b.getString(ARG_PHOTO, "");
        ImageView ivPhoto = v.findViewById(R.id.iv_detail_photo);
        if (!photoId.isEmpty()) {
            String url = AppwriteService.ENDPOINT + "/storage/buckets/"
                    + AppwriteService.USERS_BUCKET_ID + "/files/"
                    + photoId + "/view?project=" + AppwriteService.PROJECT_ID;
            Glide.with(this).load(url)
                    .placeholder(R.drawable.profile_placeholder)
                    .into(ivPhoto);
        }

        Button btnApprove = v.findViewById(R.id.btn_detail_approve);
        Button btnFake    = v.findViewById(R.id.btn_detail_fake);
        Button btnClose   = v.findViewById(R.id.btn_detail_close);

        boolean isPending = "pending".equals(status);
        btnApprove.setVisibility(isPending ? View.VISIBLE : View.GONE);
        btnFake.setVisibility(isPending ? View.VISIBLE : View.GONE);

        btnApprove.setOnClickListener(x -> updateAndDismiss(id, "active"));
        btnFake.setOnClickListener(x    -> updateAndDismiss(id, "fake"));
        btnClose.setOnClickListener(x   -> dismiss());
    }

    private void updateAndDismiss(String docId, String newStatus) {
        new Thread(() -> {
            try {
                Databases db = AppwriteService.getDatabases();
                Map<String, Object> update = new HashMap<>();
                update.put("status", newStatus);
                AppwriteHelper.updateDocument(
                        db, AppwriteService.DB_ID, AppwriteService.COL_REPORTS,
                        docId, update);
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(),
                            "active".equals(newStatus) ? "Approved ✓" : "Flagged as fake",
                            Toast.LENGTH_SHORT).show();
                    dismiss();
                });
            } catch (Exception e) {
                Log.e(TAG, "updateAndDismiss error", e);
                if (getActivity() != null)
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(),
                                    "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}