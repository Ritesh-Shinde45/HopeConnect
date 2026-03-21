package com.ritesh.hoppeconnect;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import io.appwrite.models.Document;
import io.appwrite.services.Databases;

public class AdminAnnouncementActivity extends AppCompatActivity {

    private static final String TAG               = "AdminAnnouncementAct";
    private static final String COL_ANNOUNCEMENTS = "announcements";

    private EditText etTitle, etMessage;
    private Spinner spinnerAudience;
    private Button btnSend;
    private ProgressBar progressExport;
    private TextView tvExportStatus, tvNoAnnouncements;
    private RecyclerView rvAnnouncements;

    private AnnouncementAdapter announcementAdapter;
    private final List<AnnouncementItem> sentList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_announcement);
        AppwriteService.init(this);

        // Views
        etTitle            = findViewById(R.id.et_announcement_title);
        etMessage          = findViewById(R.id.et_announcement_message);
        spinnerAudience    = findViewById(R.id.spinner_audience);
        btnSend            = findViewById(R.id.btn_send_announcement);
        progressExport     = findViewById(R.id.progress_export);
        tvExportStatus     = findViewById(R.id.tv_export_status);
        tvNoAnnouncements  = findViewById(R.id.tv_no_announcements);
        rvAnnouncements    = findViewById(R.id.rv_sent_announcements);

        // Back button
        View btnBack = findViewById(R.id.btn_back_announcement);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        // Spinner setup
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"All Users", "Active Users", "New Users"});
        spinnerAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        spinnerAudience.setAdapter(spinnerAdapter);

        // RecyclerView setup
        rvAnnouncements.setLayoutManager(new LinearLayoutManager(this));
        announcementAdapter = new AnnouncementAdapter(sentList);
        rvAnnouncements.setAdapter(announcementAdapter);

        btnSend.setOnClickListener(v -> sendAnnouncement());
        loadSentAnnouncements();
    }

    private void sendAnnouncement() {
        String title    = etTitle.getText().toString().trim();
        String message  = etMessage.getText().toString().trim();
        String audience = spinnerAudience.getSelectedItem().toString();

        if (title.isEmpty())   { etTitle.setError("Required");   return; }
        if (message.isEmpty()) { etMessage.setError("Required"); return; }

        btnSend.setEnabled(false);
        progressExport.setVisibility(View.VISIBLE);
        tvExportStatus.setVisibility(View.GONE);

        String sentAt = new SimpleDateFormat("yyyy-MM-dd HH:mm",
                Locale.getDefault()).format(new Date());

        new Thread(() -> {
            try {
                Databases db = AppwriteService.getDatabases();

                Map<String, Object> data = new HashMap<>();
                data.put("title",    title);
                data.put("message",  message);
                data.put("audience", audience);
                data.put("sentAt",   sentAt);

                String docId = UUID.randomUUID().toString()
                        .replace("-", "").substring(0, 20);

                AppwriteHelper.createDocument(
                        db, AppwriteService.DB_ID, COL_ANNOUNCEMENTS, docId, data);

                runOnUiThread(() -> {
                    progressExport.setVisibility(View.GONE);
                    tvExportStatus.setVisibility(View.VISIBLE);
                    tvExportStatus.setText("Announcement sent successfully ✓");
                    btnSend.setEnabled(true);
                    etTitle.setText("");
                    etMessage.setText("");
                    Toast.makeText(this, "Sent!", Toast.LENGTH_SHORT).show();
                    loadSentAnnouncements();
                });

            } catch (Exception e) {
                Log.e(TAG, "send error", e);
                runOnUiThread(() -> {
                    progressExport.setVisibility(View.GONE);
                    btnSend.setEnabled(true);
                    Toast.makeText(this,
                            "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void loadSentAnnouncements() {
        new Thread(() -> {
            try {
                Databases db = AppwriteService.getDatabases();
                List<? extends Document<?>> docs =
                        AppwriteHelper.listAllDocuments(
                                        db, AppwriteService.DB_ID, COL_ANNOUNCEMENTS)
                                .getDocuments();

                List<AnnouncementItem> list = new ArrayList<>();
                for (Document<?> doc : docs) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> d = (Map<String, Object>) doc.getData();
                    String t  = d.get("title")    != null ? d.get("title").toString()    : "";
                    String au = d.get("audience") != null ? d.get("audience").toString() : "";
                    String sa = d.get("sentAt")   != null ? d.get("sentAt").toString()   : "";
                    list.add(new AnnouncementItem(t, au, sa));
                }

                runOnUiThread(() -> {
                    sentList.clear();
                    sentList.addAll(list);
                    announcementAdapter.notifyDataSetChanged();
                    tvNoAnnouncements.setVisibility(
                            sentList.isEmpty() ? View.VISIBLE : View.GONE);
                });

            } catch (Exception e) {
                Log.e(TAG, "load announcements error", e);
                // Collection may not exist yet — that's fine, just show empty state
                runOnUiThread(() ->
                        tvNoAnnouncements.setVisibility(View.VISIBLE));
            }
        }).start();
    }
}