package com.ritesh.hoppeconnect;

import android.content.Intent;
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
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.appwrite.services.Databases;
import io.appwrite.services.Storage;

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
    private static final String ARG_USER_ID  = "userId";

    public interface Listener {
        void onReportUpdated();
    }

    private Listener listener;

    public void setListener(Listener l) { this.listener = l; }

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
        b.putString(ARG_USER_ID,  r.reportedBy);
        sheet.setArguments(b);
        return sheet;
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf,
                             @Nullable ViewGroup c, @Nullable Bundle s) {
        return inf.inflate(R.layout.bottom_sheet_report_detail, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);
        Bundle b = requireArguments();

        String id       = b.getString(ARG_ID,       "");
        String name     = b.getString(ARG_NAME,     "");
        String status   = b.getString(ARG_STATUS,   "pending");
        String userId   = b.getString(ARG_USER_ID,  "");
        String photoId  = b.getString(ARG_PHOTO,    "");

        ((TextView) v.findViewById(R.id.tv_detail_report_id))
                .setText("ID: " + id.substring(0, Math.min(8, id.length())));
        ((TextView) v.findViewById(R.id.tv_detail_name))    .setText(name);
        ((TextView) v.findViewById(R.id.tv_detail_status))  .setText(status.toUpperCase());
        ((TextView) v.findViewById(R.id.tv_detail_age))     .setText(b.getString(ARG_AGE,      "—"));
        ((TextView) v.findViewById(R.id.tv_detail_gender))  .setText(b.getString(ARG_GENDER,   "—"));
        ((TextView) v.findViewById(R.id.tv_detail_location)).setText(b.getString(ARG_LOCATION, "—"));
        ((TextView) v.findViewById(R.id.tv_detail_date))    .setText(b.getString(ARG_DATE,     "—"));
        ((TextView) v.findViewById(R.id.tv_detail_contact)) .setText(b.getString(ARG_CONTACT,  "—"));

        ImageView ivPhoto = v.findViewById(R.id.iv_detail_photo);

        if (!photoId.isEmpty()) {
            String url = AppwriteService.ENDPOINT
                    + "/storage/buckets/"
                    + AppwriteService.REPORT_BUCKET_ID
                    + "/files/"
                    + photoId
                    + "/view?project="
                    + AppwriteService.PROJECT_ID;

            Glide.with(this)
                    .load(url)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.person_placeholder)
                    .error(R.drawable.person_placeholder)
                    .into(ivPhoto);
        } else {
            loadFirstReportPhoto(id, ivPhoto);
        }

        TextView  tvReporterId    = v.findViewById(R.id.tv_reporter_id);
        TextView  tvReporterName  = v.findViewById(R.id.tv_reporter_name);
        ImageView ivReporterPhoto = v.findViewById(R.id.iv_reporter_photo);

        String reporterName = b.getString(ARG_REPORTER, "—");
        tvReporterName.setText(reporterName);
        tvReporterId.setText(!userId.isEmpty()
                ? "UID: " + userId.substring(0, Math.min(10, userId.length()))
                : "UID: —");

        if (!userId.isEmpty()) {
            loadReporterProfile(userId, tvReporterName, ivReporterPhoto);
        }

        Button btnApprove    = v.findViewById(R.id.btn_detail_approve);
        Button btnFake       = v.findViewById(R.id.btn_detail_fake);
        Button btnClose      = v.findViewById(R.id.btn_detail_close);
        Button btnDelete     = v.findViewById(R.id.btn_detail_delete);
        Button btnChat       = v.findViewById(R.id.btn_detail_chat);
        Button btnViewDetail = v.findViewById(R.id.btn_view_report_detail);

        boolean isPending = "pending".equalsIgnoreCase(status);
        btnApprove.setVisibility(isPending ? View.VISIBLE : View.GONE);
        btnFake   .setVisibility(isPending ? View.VISIBLE : View.GONE);

        btnApprove   .setOnClickListener(x -> updateAndDismiss(id, "active"));
        btnFake      .setOnClickListener(x -> updateAndDismiss(id, "fake"));
        btnClose     .setOnClickListener(x -> dismiss());
        btnDelete    .setOnClickListener(x -> confirmAndDelete(id));
        btnChat      .setOnClickListener(x -> openChatWithReporter(userId, reporterName));
        btnViewDetail.setOnClickListener(x -> openReportDetail(id));
    }

    private void loadFirstReportPhoto(String reportId, ImageView ivPhoto) {
        new Thread(() -> {
            try {
                Databases db = AppwriteService.getDatabases();
                io.appwrite.models.Document<?> doc = AppwriteHelper.getDocument(
                        db, AppwriteService.DB_ID, AppwriteService.COL_REPORTS, reportId);

                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) doc.getData();

                String firstFileId = "";

                Object photosObj = data.get("photoUrls");
                if (photosObj instanceof List<?>) {
                    for (Object urlObj : (List<?>) photosObj) {
                        if (urlObj == null) continue;
                        String fileId = extractFileIdFromUrl(urlObj.toString());
                        if (!fileId.isEmpty()) {
                            firstFileId = fileId;
                            break;
                        }
                    }
                }

                if (firstFileId.isEmpty()) {
                    firstFileId = strVal(data, "photoFileId");
                }

                if (firstFileId.isEmpty()) return;

                final String finalFileId = firstFileId;
                final String url = AppwriteService.ENDPOINT
                        + "/storage/buckets/"
                        + AppwriteService.REPORT_BUCKET_ID
                        + "/files/"
                        + finalFileId
                        + "/view?project="
                        + AppwriteService.PROJECT_ID;

                if (getActivity() == null) return;
                getActivity().runOnUiThread(() ->
                        Glide.with(this)
                                .load(url)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .placeholder(R.drawable.person_placeholder)
                                .error(R.drawable.person_placeholder)
                                .into(ivPhoto));

            } catch (Exception e) {
                Log.w(TAG, "loadFirstReportPhoto: " + e.getMessage());
            }
        }).start();
    }

    private void loadReporterProfile(String userId,
                                     TextView tvName,
                                     ImageView ivPhoto) {
        new Thread(() -> {
            try {
                Databases db = AppwriteService.getDatabases();
                io.appwrite.models.Document<?> doc = AppwriteHelper.getDocument(
                        db, AppwriteService.DB_ID, AppwriteService.COL_USERS, userId);

                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) doc.getData();

                String name         = strVal(data, "name");
                String username     = strVal(data, "username");
                String photoFileId  = strVal(data, "photoId");

                String displayName = !name.isEmpty() ? name
                        : (!username.isEmpty() ? "@" + username : "Unknown");

                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    tvName.setText(displayName);

                    if (!photoFileId.isEmpty()) {
                        String url = AppwriteService.ENDPOINT
                                + "/storage/buckets/"
                                + AppwriteService.USERS_BUCKET_ID
                                + "/files/"
                                + photoFileId
                                + "/view?project="
                                + AppwriteService.PROJECT_ID;

                        Glide.with(this)
                                .load(url)
                                .circleCrop()
                                .placeholder(R.drawable.person_placeholder)
                                .error(R.drawable.person_placeholder)
                                .into(ivPhoto);
                    }
                });
            } catch (Exception e) {
                Log.w(TAG, "loadReporterProfile: " + e.getMessage());
            }
        }).start();
    }

    private void openReportDetail(String reportId) {
        if (getActivity() == null) return;
        Intent i = new Intent(getActivity(), MissedPersonDetailActivity.class);
        i.putExtra(MissedPersonDetailActivity.EXTRA_REPORT_ID, reportId);
        i.putExtra(MissedPersonDetailActivity.EXTRA_IS_ADMIN, true);
        startActivity(i);
        dismiss();
    }

    private void openChatWithReporter(String reporterUserId, String reporterName) {
        if (reporterUserId.isEmpty()) {
            Toast.makeText(getContext(), "Reporter info not available",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if (getActivity() == null) return;

        android.content.SharedPreferences prefs =
                getActivity().getSharedPreferences("hoppe_prefs",
                        android.content.Context.MODE_PRIVATE);
        String myUserId = prefs.getString("logged_in_user_id", "");

        if (myUserId.isEmpty()) {
            Toast.makeText(getContext(), "Please log in", Toast.LENGTH_SHORT).show();
            return;
        }
        if (myUserId.equals(reporterUserId)) {
            Toast.makeText(getContext(), "You are the reporter", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(getContext(), "Opening chat…", Toast.LENGTH_SHORT).show();
        String finalMyId = myUserId;

        new Thread(() -> {
            try {
                Databases db = AppwriteService.getDatabases();
                java.util.List<? extends io.appwrite.models.Document<?>> chats =
                        AppwriteHelper.getUserChats(db, finalMyId).getDocuments();

                String chatId = null;
                for (io.appwrite.models.Document<?> c : chats) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> cd = (Map<String, Object>) c.getData();
                    String p1 = strVal(cd, "participant1");
                    String p2 = strVal(cd, "participant2");
                    if ((p1.equals(finalMyId) && p2.equals(reporterUserId))
                            || (p1.equals(reporterUserId) && p2.equals(finalMyId))) {
                        chatId = c.getId();
                        break;
                    }
                }

                if (chatId == null) {
                    chatId = java.util.UUID.randomUUID().toString()
                            .replace("-", "").substring(0, 20);
                    Map<String, Object> chatData = new HashMap<>();
                    chatData.put("participant1",     finalMyId);
                    chatData.put("participant2",     reporterUserId);
                    chatData.put("participant1Name",
                            prefs.getString("logged_in_name", "Admin"));
                    chatData.put("participant2Name", reporterName);
                    chatData.put("participants",     finalMyId + "," + reporterUserId);
                    chatData.put("lastMessage",      "Admin inquiry");
                    chatData.put("lastMessageTime",  "");
                    AppwriteHelper.createDocument(db, AppwriteService.DB_ID,
                            AppwriteService.COL_CHATS, chatId, chatData);
                }

                final String finalChatId = chatId;
                if (getActivity() != null)
                    getActivity().runOnUiThread(() -> {
                        Intent i = new Intent(getActivity(), ChatRoomActivity.class);
                        i.putExtra("chatId",      finalChatId);
                        i.putExtra("otherUserId", reporterUserId);
                        i.putExtra("otherName",   reporterName);
                        startActivity(i);
                        dismiss();
                    });

            } catch (Exception e) {
                Log.e(TAG, "openChatWithReporter: " + e.getMessage(), e);
                if (getActivity() != null)
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(),
                                    "Chat error: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show());
            }
        }).start();
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
                    if (listener != null) listener.onReportUpdated();
                    dismiss();
                });
            } catch (Exception e) {
                Log.e(TAG, "updateAndDismiss error", e);
                if (getActivity() != null)
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(),
                                    "Error: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void confirmAndDelete(String docId) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete Report")
                .setMessage("This will permanently delete the report and all associated photos from storage. This cannot be undone.")
                .setPositiveButton("Delete", (d, w) -> performDelete(docId))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performDelete(String docId) {
        new Thread(() -> {
            try {
                Databases db = AppwriteService.getDatabases();
                Storage   st = AppwriteService.getStorage();

                io.appwrite.models.Document<?> doc = AppwriteHelper.getDocument(
                        db, AppwriteService.DB_ID, AppwriteService.COL_REPORTS, docId);

                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) doc.getData();

                Object photosObj = data.get("photoUrls");
                if (photosObj instanceof List<?>) {
                    for (Object urlObj : (List<?>) photosObj) {
                        if (urlObj == null) continue;
                        String fileId = extractFileIdFromUrl(urlObj.toString());
                        if (!fileId.isEmpty()) {
                            try {
                                AppwriteHelper.deleteFile(
                                        st, AppwriteService.REPORT_BUCKET_ID, fileId);
                            } catch (Exception fe) {
                                Log.w(TAG, "Could not delete file " + fileId
                                        + ": " + fe.getMessage());
                            }
                        }
                    }
                }

                String singlePhotoId = requireArguments().getString(ARG_PHOTO, "");
                if (!singlePhotoId.isEmpty()) {
                    try {
                        AppwriteHelper.deleteFile(
                                st, AppwriteService.REPORT_BUCKET_ID, singlePhotoId);
                    } catch (Exception ignored) {}
                }

                AppwriteHelper.deleteDocument(
                        db, AppwriteService.DB_ID, AppwriteService.COL_REPORTS, docId);

                ReportModelCache.remove(docId);

                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(),
                            "Report deleted successfully", Toast.LENGTH_SHORT).show();
                    if (listener != null) listener.onReportUpdated();
                    dismiss();
                });

            } catch (Exception e) {
                Log.e(TAG, "performDelete error", e);
                if (getActivity() != null)
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(),
                                    "Delete failed: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private static String extractFileIdFromUrl(String url) {
        try {
            int idx = url.indexOf("/files/");
            if (idx < 0) return "";
            String after = url.substring(idx + 7);
            int slash = after.indexOf('/');
            return slash > 0 ? after.substring(0, slash) : after;
        } catch (Exception e) {
            return "";
        }
    }

    private static String strVal(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v != null ? v.toString() : "";
    }
}