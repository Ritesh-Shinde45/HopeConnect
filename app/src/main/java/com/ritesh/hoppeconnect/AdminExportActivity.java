package com.ritesh.hoppeconnect;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.Map;

import io.appwrite.models.Document;
import io.appwrite.services.Databases;

public class AdminExportActivity extends AppCompatActivity {

    private static final String TAG = "AdminExportAct";

    private ProgressBar progress;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_export);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView()
                    .setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
        AppwriteService.init(this);

        progress = findViewById(R.id.progress_export);
        tvStatus = findViewById(R.id.tv_export_status);

        findViewById(R.id.btn_back_export).setOnClickListener(v -> finish());

        Button btnUsersCSV    = findViewById(R.id.btn_export_users_csv);
        Button btnReportsJSON = findViewById(R.id.btn_export_reports_json);
        Button btnFakeCSV     = findViewById(R.id.btn_export_fake_reports);

        btnUsersCSV.setOnClickListener(v    -> exportUsersCSV());
        btnReportsJSON.setOnClickListener(v -> exportReportsJSON());
        btnFakeCSV.setOnClickListener(v     -> exportFakeReportsCSV());
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        tvStatus.setVisibility(View.GONE);
    }

    private void showStatus(String msg) {
        tvStatus.setText(msg);
        tvStatus.setVisibility(View.VISIBLE);
    }

    private void exportUsersCSV() {
        setLoading(true);
        new Thread(() -> {
            try {
                Databases db = AppwriteService.getDatabases();
                List<? extends Document<?>> docs =
                        AppwriteHelper.listAllDocuments(
                                        db, AppwriteService.DB_ID, AppwriteService.COL_USERS)
                                .getDocuments();

                StringBuilder csv = new StringBuilder("Name,Email,Username,Status,Joined\n");
                for (Document<?> doc : docs) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> d = (Map<String, Object>) doc.getData();
                    csv.append(safe(d, "name")).append(",")
                            .append(safe(d, "email")).append(",")
                            .append(safe(d, "username")).append(",")
                            .append(safe(d, "status")).append(",")
                            .append(doc.getCreatedAt() != null
                                    ? doc.getCreatedAt().substring(0, 10) : "").append("\n");
                }
                shareFile("users_export.csv", csv.toString(), "text/csv");
            } catch (Exception e) {
                Log.e(TAG, "exportUsersCSV error", e);
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void exportReportsJSON() {
        setLoading(true);
        new Thread(() -> {
            try {
                Databases db = AppwriteService.getDatabases();
                List<? extends Document<?>> docs =
                        AppwriteHelper.listAllDocuments(
                                        db, AppwriteService.DB_ID, AppwriteService.COL_REPORTS)
                                .getDocuments();

                StringBuilder json = new StringBuilder("[\n");
                for (int i = 0; i < docs.size(); i++) {
                    Document<?> doc = docs.get(i);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> d = (Map<String, Object>) doc.getData();
                    json.append("  {")
                            .append("\"id\":\"").append(doc.getId()).append("\",")
                            .append("\"name\":\"").append(safe(d, "name")).append("\",")
                            .append("\"status\":\"").append(safe(d, "status")).append("\",")
                            .append("\"location\":\"").append(safe(d, "location")).append("\",")
                            .append("\"createdAt\":\"").append(
                                    doc.getCreatedAt() != null ? doc.getCreatedAt() : "")
                            .append("\"}")
                            .append(i < docs.size() - 1 ? "," : "").append("\n");
                }
                json.append("]");
                shareFile("reports_export.json", json.toString(), "application/json");
            } catch (Exception e) {
                Log.e(TAG, "exportReportsJSON error", e);
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void exportFakeReportsCSV() {
        setLoading(true);
        new Thread(() -> {
            try {
                Databases db = AppwriteService.getDatabases();
                List<? extends Document<?>> docs =
                        AppwriteHelper.findDocumentsByField(
                                        db, AppwriteService.DB_ID, AppwriteService.COL_REPORTS,
                                        "status", "fake")
                                .getDocuments();

                StringBuilder csv = new StringBuilder("Name,Location,Status,Date\n");
                for (Document<?> doc : docs) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> d = (Map<String, Object>) doc.getData();
                    csv.append(safe(d, "name")).append(",")
                            .append(safe(d, "location")).append(",")
                            .append(safe(d, "status")).append(",")
                            .append(doc.getCreatedAt() != null
                                    ? doc.getCreatedAt().substring(0, 10) : "").append("\n");
                }
                shareFile("fake_reports.csv", csv.toString(), "text/csv");
            } catch (Exception e) {
                Log.e(TAG, "exportFakeCSV error", e);
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void shareFile(String fileName, String content, String mimeType) throws Exception {
        File dir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (dir != null && !dir.exists()) dir.mkdirs();
        File file = new File(dir, fileName);
        FileWriter fw = new FileWriter(file);
        fw.write(content);
        fw.close();

        Uri uri = FileProvider.getUriForFile(this,
                getPackageName() + ".fileprovider", file);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType(mimeType);
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        runOnUiThread(() -> {
            setLoading(false);
            showStatus("File ready: " + fileName);
            startActivity(Intent.createChooser(shareIntent, "Share " + fileName));
        });
    }

    private String safe(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v != null ? v.toString().replace(",", ";").replace("\"", "'") : "";
    }
}