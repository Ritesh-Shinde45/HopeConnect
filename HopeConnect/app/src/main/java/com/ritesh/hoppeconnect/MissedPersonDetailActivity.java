package com.ritesh.hoppeconnect;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsetsController;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.CollapsingToolbarLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.appwrite.services.Databases;

public class MissedPersonDetailActivity extends AppCompatActivity {

    private static final String TAG = "MissedPersonDetail";
    public static final String EXTRA_REPORT_ID = "report_id";

   
    private ViewPager2 imageViewPager;
    private LinearLayout dotsContainer;
    private CollapsingToolbarLayout collapsingToolbar;
    private TextView tvName, tvAge, tvGender, tvMissingSince, tvDescription;
    private TextView tvContact, tvEmergency1, tvEmergency2, tvEmergency3;
    private TextView tvReportId, tvReportedOn, tvStatusBadge;
    private LinearLayout layoutEmergency2, layoutEmergency3;
    private LinearLayout btnDownloadPdf, btnSendMail, btnCallEmergency,
            btnChatDetail, btnReportFalse;

   
    private String reportId;
    private String uploaderUserId = "";

   

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_missed_person_detail);

        reportId = getIntent().getStringExtra(EXTRA_REPORT_ID);
        initViews();
        setupToolbar();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            getWindow().getInsetsController().setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            );
        }
        ReportModel cached = ReportModelCache.get(reportId);
        if (cached != null) {
            bindData(cached);
        } else if (reportId != null) {
            fetchReportFromDb(reportId);
        } else {
            Toast.makeText(this, "Report not found", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

   

    private void initViews() {
        imageViewPager    = findViewById(R.id.imageViewPager);
        dotsContainer     = findViewById(R.id.dotsContainer);
        collapsingToolbar = findViewById(R.id.collapsingToolbar);
        tvStatusBadge     = findViewById(R.id.statusBadgeDetail);
        tvName            = findViewById(R.id.tvDetailName);
        tvAge             = findViewById(R.id.tvDetailAge);
        tvGender          = findViewById(R.id.tvDetailGender);
        tvMissingSince    = findViewById(R.id.tvDetailMissingSince);
        tvDescription     = findViewById(R.id.tvDetailDescription);
        tvContact         = findViewById(R.id.tvDetailContact);
        tvEmergency1      = findViewById(R.id.tvDetailEmergency1);
        tvEmergency2      = findViewById(R.id.tvDetailEmergency2);
        tvEmergency3      = findViewById(R.id.tvDetailEmergency3);
        tvReportId        = findViewById(R.id.tvDetailReportId);
        tvReportedOn      = findViewById(R.id.tvDetailReportedOn);
        layoutEmergency2  = findViewById(R.id.layoutEmergency2);
        layoutEmergency3  = findViewById(R.id.layoutEmergency3);
        btnDownloadPdf    = findViewById(R.id.btnDownloadPdf);
        btnSendMail       = findViewById(R.id.btnSendMail);
        btnCallEmergency  = findViewById(R.id.btnCallEmergency);
        btnChatDetail     = findViewById(R.id.btnChatDetail);
        btnReportFalse    = findViewById(R.id.btnReportFalse);
    }

    private void setupToolbar() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

   

    private void bindData(ReportModel model) {
        uploaderUserId = nz(model.userId, "");

        collapsingToolbar.setTitle(nz(model.name, "Unknown"));
        tvName.setText(nz(model.name, "Unknown"));
        tvAge.setText(model.age > 0 ? model.age + " yrs" : "Age N/A");
        tvGender.setText(nz(model.gender, "Not specified"));
        tvMissingSince.setText(nz(model.missingSince, "Unknown"));
        tvDescription.setText(nz(model.description, "No description provided."));
        tvContact.setText(nz(model.contact, "Not provided"));
        tvEmergency1.setText(nz(model.emergencyContact1, "Not provided"));

        if (model.emergencyContact2 != null && !model.emergencyContact2.isEmpty()) {
            layoutEmergency2.setVisibility(View.VISIBLE);
            tvEmergency2.setText(model.emergencyContact2);
        }
        if (model.emergencyContact3 != null && !model.emergencyContact3.isEmpty()) {
            layoutEmergency3.setVisibility(View.VISIBLE);
            tvEmergency3.setText(model.emergencyContact3);
        }

        tvReportId.setText(model.id != null
                ? "#" + model.id.substring(0, Math.min(8, model.id.length())) : "—");

        if (model.createdAt > 0) {
            tvReportedOn.setText(new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    .format(new Date(model.createdAt)));
        } else {
            tvReportedOn.setText("—");
        }

        tvStatusBadge.setText(model.status != null && !model.status.isEmpty()
                ? model.status.substring(0, 1).toUpperCase() + model.status.substring(1)
                : "Missing");

       
        if (model.locationLat != null && !model.locationLat.isEmpty()) {
            String gpsText = "Missing since: " + nz(model.missingSince, "Unknown")
                    + "\n📍 " + model.locationLat + ", " + model.locationLng;
            tvMissingSince.setText(gpsText);
            tvMissingSince.setOnClickListener(v -> {
                try {
                    Uri geoUri = Uri.parse("geo:" + model.locationLat + "," + model.locationLng
                            + "?q=" + model.locationLat + "," + model.locationLng);
                    startActivity(new Intent(Intent.ACTION_VIEW, geoUri));
                } catch (Exception e) {
                    Toast.makeText(this, "No maps app found", Toast.LENGTH_SHORT).show();
                }
            });
        }

       
        if (model.documentUrl != null && !model.documentUrl.isEmpty()) {
            tvDescription.append("\n\n📄 Supporting document attached. Tap to view.");
            tvDescription.setOnClickListener(v -> {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(model.documentUrl));
                startActivity(Intent.createChooser(i, "Open Document"));
            });
        }

        setupImageSlider(model.photoUrls != null ? model.photoUrls : new ArrayList<>());
        setupActionButtons(model);
    }

   

    private void setupImageSlider(List<String> photoUrls) {
        if (photoUrls.isEmpty()) {
            photoUrls = new ArrayList<>();
            photoUrls.add(null);
        }
        final List<String> finalUrls = photoUrls;

        imageViewPager.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_detail_image, parent, false);
                return new RecyclerView.ViewHolder(v) {};
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                ImageView img = holder.itemView.findViewById(R.id.sliderImage);
                String url = finalUrls.get(position);
                if (url != null && !url.isEmpty()) {
                    Glide.with(MissedPersonDetailActivity.this)
                            .load(url)
                            .placeholder(R.drawable.person_placeholder)
                            .error(R.drawable.person_placeholder)
                            .centerCrop()
                            .into(img);
                } else {
                    img.setImageResource(R.drawable.person_placeholder);
                }
            }

            @Override
            public int getItemCount() { return finalUrls.size(); }
        });

        buildDotIndicators(finalUrls.size());
        imageViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateDotIndicators(position);
            }
        });
    }

    private void buildDotIndicators(int count) {
        dotsContainer.removeAllViews();
        if (count <= 1) return;
        for (int i = 0; i < count; i++) {
            TextView dot = new TextView(this);
            dot.setText("●");
            dot.setTextSize(10f);
            dot.setPadding(4, 0, 4, 0);
            dot.setTextColor(i == 0 ? Color.WHITE : 0x88FFFFFF);
            dotsContainer.addView(dot);
        }
    }

    private void updateDotIndicators(int selected) {
        for (int i = 0; i < dotsContainer.getChildCount(); i++) {
            ((TextView) dotsContainer.getChildAt(i))
                    .setTextColor(i == selected ? Color.WHITE : 0x88FFFFFF);
        }
    }

   

    private void setupActionButtons(ReportModel model) {
        btnDownloadPdf.setOnClickListener(v -> generateAndSharePdf(model));

        btnSendMail.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("message/rfc822");
            intent.putExtra(Intent.EXTRA_SUBJECT, "Regarding Missing Person: " + model.name);
            intent.putExtra(Intent.EXTRA_TEXT,
                    "Hello,\n\nI have information about:\n"
                            + "Name: " + nz(model.name, "Unknown") + "\n"
                            + "Report ID: " + nz(model.id, "—") + "\n\nPlease contact me.");
            try {
                startActivity(Intent.createChooser(intent, "Send Email via"));
            } catch (Exception e) {
                Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show();
            }
        });

        btnCallEmergency.setOnClickListener(v -> {
            String number = (model.emergencyContact1 != null && !model.emergencyContact1.isEmpty())
                    ? model.emergencyContact1 : model.contact;
            if (number != null && !number.isEmpty()) {
                startActivity(new Intent(Intent.ACTION_DIAL,
                        Uri.parse("tel:" + number.replaceAll("[^0-9+]", ""))));
            } else {
                Toast.makeText(this, "No emergency contact available", Toast.LENGTH_SHORT).show();
            }
        });

        btnChatDetail.setOnClickListener(v -> {
            if (uploaderUserId.isEmpty()) {
                Toast.makeText(this, "Uploader info not available", Toast.LENGTH_SHORT).show();
                return;
            }
            SharedPreferences prefs = getSharedPreferences("hoppe_prefs", MODE_PRIVATE);
            String myUserId = prefs.getString("logged_in_user_id", null);
            if (myUserId == null) {
                Toast.makeText(this, "Please log in to chat", Toast.LENGTH_SHORT).show();
                return;
            }
            if (myUserId.equals(uploaderUserId)) {
                Toast.makeText(this, "This is your own report", Toast.LENGTH_SHORT).show();
                return;
            }
            startChatWithUploader(myUserId, uploaderUserId, model.name);
        });

        btnReportFalse.setOnClickListener(v -> showFlagDialog(model));
    }

   

    private void startChatWithUploader(String myUserId, String otherUserId, String reportName) {
        Toast.makeText(this, "Opening chat…", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                Databases db = AppwriteService.getDatabases();
                List<? extends io.appwrite.models.Document<?>> chats =
                        AppwriteHelper.getUserChats(db, myUserId).getDocuments();

                String chatId = null;
                for (io.appwrite.models.Document<?> c : chats) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> cd = (Map<String, Object>) c.getData();
                    String p1 = strVal(cd, "participant1");
                    String p2 = strVal(cd, "participant2");
                    if ((p1.equals(myUserId) && p2.equals(otherUserId))
                            || (p1.equals(otherUserId) && p2.equals(myUserId))) {
                        chatId = c.getId();
                        break;
                    }
                }

                if (chatId == null) {
                    chatId = java.util.UUID.randomUUID().toString()
                            .replace("-", "").substring(0, 20);
                    SharedPreferences prefs = getSharedPreferences("hoppe_prefs", MODE_PRIVATE);
                    Map<String, Object> chatData = new java.util.HashMap<>();
                    chatData.put("participant1",     myUserId);
                    chatData.put("participant2",     otherUserId);
                    chatData.put("participant1Name", prefs.getString("logged_in_name", "User"));
                    chatData.put("participant2Name", "Report Uploader");
                    chatData.put("participants",     myUserId + "," + otherUserId);
                    chatData.put("lastMessage",      "Re: Missing – " + reportName);
                    chatData.put("lastMessageTime",  "");
                    AppwriteHelper.createDocument(db, AppwriteService.DB_ID,
                            AppwriteService.COL_CHATS, chatId, chatData);
                }

                final String finalChatId = chatId;
                runOnUiThread(() -> {
                    Intent i = new Intent(this, ChatRoomActivity.class);
                    i.putExtra("chatId",      finalChatId);
                    i.putExtra("otherUserId", otherUserId);
                    i.putExtra("otherName",   "Report Uploader");
                    startActivity(i);
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                        "Chat error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

   

    private void showFlagDialog(ReportModel model) {
        String[] options = {
                "Person has been found",
                "False / misleading report",
                "Duplicate report",
                "Spam or irrelevant",
                "Other"
        };
        new AlertDialog.Builder(this)
                .setTitle("Flag this Report")
                .setItems(options, (d, which) ->
                        Toast.makeText(this, "Flagged: " + options[which],
                                Toast.LENGTH_SHORT).show())
                .setNegativeButton("Cancel", null)
                .show();
    }

   

    private void generateAndSharePdf(ReportModel model) {
        Toast.makeText(this, "Generating PDF…", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                PdfDocument pdf = new PdfDocument();
                PdfDocument.PageInfo pageInfo =
                        new PdfDocument.PageInfo.Builder(595, 842, 1).create();
                PdfDocument.Page page = pdf.startPage(pageInfo);
                Canvas canvas = page.getCanvas();
                Paint p = new Paint();
                p.setAntiAlias(true);

               
                p.setColor(Color.WHITE);
                canvas.drawRect(0, 0, 595, 842, p);

               
                p.setColor(Color.parseColor("#3F51B5"));
                canvas.drawRect(0, 0, 595, 80, p);
                p.setColor(Color.WHITE);
                p.setTextSize(17f); p.setFakeBoldText(true);
                canvas.drawText("HoppeConnect — Missing Person Report", 16, 30, p);
                p.setTextSize(10f); p.setFakeBoldText(false);
                canvas.drawText("Generated: " + new SimpleDateFormat("MMM dd, yyyy HH:mm",
                        Locale.getDefault()).format(new Date()), 16, 54, p);

               
                p.setColor(Color.parseColor("#1A1A2E"));
                p.setTextSize(17f); p.setFakeBoldText(true);
                canvas.drawText(nz(model.name, "Unknown"), 16, 104, p);

               
                p.setTextSize(12f); p.setFakeBoldText(false);
                p.setColor(Color.parseColor("#555555"));
                int y = 130; int lh = 22;
                canvas.drawText("Age:           " + (model.age > 0 ? model.age + " yrs" : "N/A"),             16, y, p); y += lh;
                canvas.drawText("Gender:        " + nz(model.gender,       "N/A"), 16, y, p); y += lh;
                canvas.drawText("Missing Since: " + nz(model.missingSince, "N/A"), 16, y, p); y += lh;
                canvas.drawText("Status:        " + nz(model.status,       "N/A"), 16, y, p); y += lh;
                canvas.drawText("Report ID:     " + nz(model.id,           "N/A"), 16, y, p); y += lh;
                if (model.locationLat != null && !model.locationLat.isEmpty()) {
                    canvas.drawText("GPS Location:  " + model.locationLat + ", " + model.locationLng, 16, y, p);
                    y += lh;
                }
                y += 10;

               
                p.setColor(Color.parseColor("#3F51B5")); p.setFakeBoldText(true);
                canvas.drawText("DESCRIPTION", 16, y, p); y += lh;
                p.setColor(Color.parseColor("#444444")); p.setFakeBoldText(false);
                String desc = nz(model.description, "No description.");
                StringBuilder lineB = new StringBuilder();
                for (String word : desc.split(" ")) {
                    String test = lineB.length() > 0 ? lineB + " " + word : word;
                    if (p.measureText(test) > 560f) {
                        canvas.drawText(lineB.toString(), 16, y, p);
                        y += lh;
                        lineB = new StringBuilder(word);
                    } else {
                        lineB = new StringBuilder(test);
                    }
                }
                if (lineB.length() > 0) { canvas.drawText(lineB.toString(), 16, y, p); y += lh; }
                y += 10;

               
                p.setColor(Color.parseColor("#3F51B5")); p.setFakeBoldText(true);
                canvas.drawText("CONTACTS", 16, y, p); y += lh;
                p.setColor(Color.parseColor("#444444")); p.setFakeBoldText(false);
                canvas.drawText("Primary:     " + nz(model.contact,           "—"), 16, y, p); y += lh;
                canvas.drawText("Emergency 1: " + nz(model.emergencyContact1, "—"), 16, y, p); y += lh;
                canvas.drawText("Emergency 2: " + nz(model.emergencyContact2, "—"), 16, y, p); y += lh;
                canvas.drawText("Emergency 3: " + nz(model.emergencyContact3, "—"), 16, y, p);

               
                p.setColor(Color.parseColor("#AAAAAA")); p.setTextSize(9f);
                canvas.drawText("HoppeConnect. Verify all information before acting.", 16, 828, p);

                pdf.finishPage(page);

               
                File outFile = new File(getCacheDir(), "report_"
                        + (model.id != null
                        ? model.id.substring(0, Math.min(6, model.id.length()))
                        : "x")
                        + ".pdf");

                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    pdf.writeTo(fos);
                }
                pdf.close();

                runOnUiThread(() -> {
                    try {
                       
                        Uri uri = FileProvider.getUriForFile(
                                this,
                                getPackageName() + ".fileprovider",
                                outFile);
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(uri, "application/pdf");
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(Intent.createChooser(intent, "Open PDF"));
                    } catch (Exception e2) {
                        Log.e(TAG, "PDF share error: " + e2.getMessage(), e2);
                        Toast.makeText(this,
                                "PDF saved. Please install a PDF viewer app.",
                                Toast.LENGTH_LONG).show();
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "PDF gen error: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(this,
                        "PDF failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

   

    private void fetchReportFromDb(String id) {
        AppwriteService.init(getApplicationContext());
        new Thread(() -> {
            try {
                Databases db = AppwriteService.getDatabases();
                io.appwrite.models.Document<Map<String, Object>> doc =
                        AppwriteHelper.getDocument(db, AppwriteService.DB_ID, "reports", id);
                ReportModel rm = MissedFragment.parseDocument(doc.getId(), doc.getData());
                runOnUiThread(() -> bindData(rm));
            } catch (Exception e) {
                Log.e(TAG, "Fetch error: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Could not load report", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        }).start();
    }

   

    private String nz(String s, String fallback) {
        return (s != null && !s.isEmpty()) ? s : fallback;
    }

    private static String strVal(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v != null ? v.toString() : "";
    }
}