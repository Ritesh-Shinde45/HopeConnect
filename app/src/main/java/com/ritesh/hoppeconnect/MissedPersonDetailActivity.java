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
import androidx.cardview.widget.CardView;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import io.appwrite.services.Databases;

public class MissedPersonDetailActivity extends AppCompatActivity {

    private static final String TAG            = "MissedPersonDetail";
    public  static final String EXTRA_REPORT_ID = "report_id";

    private ViewPager2              imageViewPager;
    private LinearLayout            dotsContainer;
    private CollapsingToolbarLayout collapsingToolbar;
    private TextView tvName, tvAge, tvGender, tvMissingSince, tvDescription;
    private TextView tvContact, tvEmergency1, tvEmergency2, tvEmergency3;
    private TextView tvReportId, tvReportedOn, tvStatusBadge, tvReporterName;
    private LinearLayout layoutEmergency2, layoutEmergency3;
    private LinearLayout btnDownloadPdf, btnSendMail, btnCallEmergency,
            btnChatDetail, btnIveSeenPerson, btnDeleteReport;
    private CardView cardDeleteReport;

    private String reportId        = "";
    private String uploaderUserId  = "";
    private String myUserId        = "";
    private String myRole          = "";

   
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_missed_person_detail);

        AppwriteService.init(this);
        reportId = getIntent().getStringExtra(EXTRA_REPORT_ID);

        SharedPreferences prefs = getSharedPreferences("hoppe_prefs", MODE_PRIVATE);
        myUserId = prefs.getString("logged_in_user_id", "");
        myRole   = prefs.getString("logged_in_role",    "user");

        initViews();
        setupToolbar();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            getWindow().getInsetsController().setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
        }

        ReportModel cached = ReportModelCache.get(reportId);
        if (cached != null) {
            bindData(cached);
        } else if (reportId != null && !reportId.isEmpty()) {
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
        tvReporterName    = findViewById(R.id.tvDetailReporterName);
        layoutEmergency2  = findViewById(R.id.layoutEmergency2);
        layoutEmergency3  = findViewById(R.id.layoutEmergency3);
        btnDownloadPdf    = findViewById(R.id.btnDownloadPdf);
        btnSendMail       = findViewById(R.id.btnSendMail);
        btnCallEmergency  = findViewById(R.id.btnCallEmergency);
        btnChatDetail     = findViewById(R.id.btnChatDetail);
        btnIveSeenPerson  = findViewById(R.id.btnIveSeenPerson);
        btnDeleteReport   = findViewById(R.id.btnDeleteReport);
        cardDeleteReport  = findViewById(R.id.cardDeleteReport);
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
           
            tvReportedOn.setText(new SimpleDateFormat(
                    "dd MMM yyyy, hh:mm a", Locale.getDefault())
                    .format(new Date(model.createdAt)));
        } else {
           
            tvReportedOn.setText("—");
        }

       
        if (tvReporterName != null) {
            tvReporterName.setText("Loading...");
            if (!uploaderUserId.isEmpty()) {
                loadReporterName(uploaderUserId);
            } else {
                tvReporterName.setText("Unknown");
            }
        }

       
        if (tvStatusBadge != null && model.status != null) {
            String status = model.status.toLowerCase(Locale.ROOT);
            switch (status) {
                case "found":
                case "resolved":
                    tvStatusBadge.setText("✅ Found");
                    tvStatusBadge.setBackgroundResource(R.drawable.badge_found);
                    tvStatusBadge.setTextColor(Color.WHITE);
                    break;
                case "pending":
                    tvStatusBadge.setText("⏳ Pending");
                    tvStatusBadge.setBackgroundColor(
                            Color.parseColor("#FF9800"));
                    tvStatusBadge.setTextColor(Color.WHITE);
                    break;
                case "active":
                default:
                    tvStatusBadge.setText("🔴 Missing");
                    tvStatusBadge.setBackgroundResource(R.drawable.badge_missing);
                    tvStatusBadge.setTextColor(Color.WHITE);
                    break;
            }
        }

       
        if (model.locationLat != null && !model.locationLat.isEmpty()) {
            tvMissingSince.setText("Missing since: " + nz(model.missingSince, "Unknown")
                    + "\n📍 " + model.locationLat + ", " + model.locationLng);
            tvMissingSince.setOnClickListener(v -> {
                try {
                    Uri geoUri = Uri.parse("geo:" + model.locationLat + ","
                            + model.locationLng + "?q=" + model.locationLat
                            + "," + model.locationLng);
                    startActivity(new Intent(Intent.ACTION_VIEW, geoUri));
                } catch (Exception e) {
                    Toast.makeText(this, "No maps app found",
                            Toast.LENGTH_SHORT).show();
                }
            });
        }

       
        if (model.documentUrl != null && !model.documentUrl.isEmpty()) {
            tvDescription.append("\n\n📄 Supporting document attached. Tap to view.");
            tvDescription.setOnClickListener(v ->
                    startActivity(Intent.createChooser(
                            new Intent(Intent.ACTION_VIEW,
                                    Uri.parse(model.documentUrl)),
                            "Open Document")));
        }

       
        boolean isReporter = !myUserId.isEmpty() && myUserId.equals(uploaderUserId);
        boolean isAdmin    = "admin".equals(myRole);
        if ((isReporter || isAdmin) && cardDeleteReport != null) {
            cardDeleteReport.setVisibility(View.VISIBLE);
        }

       
        if (btnIveSeenPerson != null) {
            String status = model.status != null
                    ? model.status.toLowerCase(Locale.ROOT) : "active";
            if (isReporter || "found".equals(status) || "resolved".equals(status)) {
               
                btnIveSeenPerson.setVisibility(View.GONE);
            } else {
                btnIveSeenPerson.setVisibility(View.VISIBLE);
            }
        }

        setupImageSlider(model.photoUrls != null
                ? model.photoUrls : new ArrayList<>());
        setupActionButtons(model);
    }

   
    private void loadReporterName(String userId) {
        new Thread(() -> {
            try {
                Databases db = AppwriteService.getDatabases();
                io.appwrite.models.Document<?> doc = AppwriteHelper.getDocument(
                        db, AppwriteService.DB_ID,
                        AppwriteService.COL_USERS, userId);
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) doc.getData();
                String name     = strVal(data, "name");
                String username = strVal(data, "username");
                String display  = !name.isEmpty() ? name
                        : (!username.isEmpty() ? "@" + username : "Unknown");
                runOnUiThread(() -> {
                    if (tvReporterName != null) tvReporterName.setText(display);
                });
            } catch (Exception e) {
                Log.w(TAG, "loadReporterName: " + e.getMessage());
                runOnUiThread(() -> {
                    if (tvReporterName != null) tvReporterName.setText("Unknown");
                });
            }
        }).start();
    }

   
    private void setupImageSlider(List<String> photoUrls) {
        if (photoUrls.isEmpty()) {
            photoUrls = new ArrayList<>();
            photoUrls.add(null);
        }
        final List<String> finalUrls = photoUrls;

        imageViewPager.setAdapter(
                new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                    @NonNull
                    @Override
                    public RecyclerView.ViewHolder onCreateViewHolder(
                            @NonNull ViewGroup parent, int viewType) {
                        View v = LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.item_detail_image, parent, false);
                        return new RecyclerView.ViewHolder(v) {};
                    }
                    @Override
                    public void onBindViewHolder(
                            @NonNull RecyclerView.ViewHolder holder, int position) {
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
        imageViewPager.registerOnPageChangeCallback(
                new ViewPager2.OnPageChangeCallback() {
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
            intent.putExtra(Intent.EXTRA_SUBJECT,
                    "Regarding Missing Person: " + model.name);
            intent.putExtra(Intent.EXTRA_TEXT,
                    "Hello,\n\nI have information about:\n"
                            + "Name: " + nz(model.name, "Unknown") + "\n"
                            + "Report ID: " + nz(model.id, "—")
                            + "\n\nPlease contact me.");
            try {
                startActivity(Intent.createChooser(intent, "Send Email via"));
            } catch (Exception e) {
                Toast.makeText(this, "No email app found",
                        Toast.LENGTH_SHORT).show();
            }
        });

        btnCallEmergency.setOnClickListener(v -> {
            String number = (model.emergencyContact1 != null
                    && !model.emergencyContact1.isEmpty())
                    ? model.emergencyContact1 : model.contact;
            if (number != null && !number.isEmpty()) {
                startActivity(new Intent(Intent.ACTION_DIAL,
                        Uri.parse("tel:" + number.replaceAll("[^0-9+]", ""))));
            } else {
                Toast.makeText(this, "No emergency contact",
                        Toast.LENGTH_SHORT).show();
            }
        });

        btnChatDetail.setOnClickListener(v -> {
            if (uploaderUserId.isEmpty()) {
                Toast.makeText(this, "Uploader info not available",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (myUserId.isEmpty()) {
                Toast.makeText(this, "Please log in to chat",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (myUserId.equals(uploaderUserId)) {
                Toast.makeText(this, "This is your own report",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            startChatWithUser(myUserId, uploaderUserId,
                    "Report Uploader", model.name);
        });

       
        if (btnIveSeenPerson != null) {
            btnIveSeenPerson.setOnClickListener(v -> showIveSeenDialog(model));
        }

       
        if (btnDeleteReport != null) {
            btnDeleteReport.setOnClickListener(v -> confirmDeleteReport(model));
        }
    }

   
    private void showIveSeenDialog(ReportModel model) {
        if (myUserId.isEmpty()) {
            Toast.makeText(this, "Please log in first",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                Databases db = AppwriteService.getDatabases();
                io.appwrite.models.Document<?> myDoc = AppwriteHelper.getDocument(
                        db, AppwriteService.DB_ID,
                        AppwriteService.COL_USERS, myUserId);
                @SuppressWarnings("unchecked")
                Map<String, Object> myData = (Map<String, Object>) myDoc.getData();
                String myName     = strVal(myData, "name");
                String myUsername = strVal(myData, "username");
                String myPhone    = strVal(myData, "mobile");

                runOnUiThread(() ->
                        showLocationInputDialog(model, myName, myUsername, myPhone));

            } catch (Exception e) {
                Log.e(TAG, "loadWatcherProfile: " + e.getMessage(), e);
                SharedPreferences prefs =
                        getSharedPreferences("hoppe_prefs", MODE_PRIVATE);
                String myName = prefs.getString("logged_in_name", "User");
                runOnUiThread(() ->
                        showLocationInputDialog(model, myName, "", ""));
            }
        }).start();
    }

    private void showLocationInputDialog(ReportModel model,
                                         String watcherName,
                                         String watcherUsername,
                                         String watcherPhone) {
        android.widget.EditText etLocation = new android.widget.EditText(this);
        etLocation.setHint("Where did you see them? (area, city)");
        etLocation.setPadding(48, 24, 48, 24);

        new AlertDialog.Builder(this)
                .setTitle("📍 I've Seen This Person")
                .setMessage("Your name (" + watcherName
                        + ") and location will be sent to the reporter.")
                .setView(etLocation)
                .setPositiveButton("Send Notification", (d, w) -> {
                    String location = etLocation.getText().toString().trim();
                    if (location.isEmpty()) location = "Location not specified";
                    submitSighting(model, watcherName,
                            watcherUsername, watcherPhone, location);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void submitSighting(ReportModel model, String watcherName,
                                String watcherUsername, String watcherPhone,
                                String location) {
        String sightingId = UUID.randomUUID().toString()
                .replace("-", "").substring(0, 20);
        String timestamp  = new SimpleDateFormat("dd MMM yyyy, hh:mm a",
                Locale.getDefault()).format(new Date());

        Map<String, Object> sightingData = new HashMap<>();
        sightingData.put("reportId",        model.id);
        sightingData.put("reporterUserId",  uploaderUserId);
        sightingData.put("watcherUserId",   myUserId);
        sightingData.put("watcherName",     watcherName);
        sightingData.put("watcherUsername", watcherUsername);
        sightingData.put("watcherLocation", location);
        sightingData.put("watcherPhone",    watcherPhone);
        sightingData.put("reportName",      nz(model.name, "Unknown"));
        sightingData.put("status",          "pending");
        sightingData.put("createdAt",       timestamp);

        new Thread(() -> {
            try {
                Databases db = AppwriteService.getDatabases();
                AppwriteHelper.createDocument(
                        db, AppwriteService.DB_ID,
                        AppwriteService.COL_SIGHTINGS,
                        sightingId, sightingData);

                String notifId = UUID.randomUUID().toString()
                        .replace("-", "").substring(0, 20);
                Map<String, Object> notifData = new HashMap<>();
                notifData.put("userId",         uploaderUserId);
                notifData.put("type",           "sighting");
                notifData.put("title",          "👀 Someone saw "
                        + nz(model.name, "the person"));
                notifData.put("message",        watcherName
                        + " (@" + watcherUsername + ") saw them at: "
                        + location + ". Phone: " + watcherPhone);
                notifData.put("sightingId",     sightingId);
                notifData.put("reportId",       model.id);
                notifData.put("watcherUserId",  myUserId);
                notifData.put("watcherName",    watcherName);
                notifData.put("watcherUsername",watcherUsername);
                notifData.put("watcherPhone",   watcherPhone);
                notifData.put("watcherLocation",location);
                notifData.put("reportName",     nz(model.name, "Unknown"));
                notifData.put("sentAt",         timestamp);
                notifData.put("isRead",         false);
                notifData.put("resolved",       false);

                AppwriteHelper.createDocument(
                        db, AppwriteService.DB_ID,
                        "notifications", notifId, notifData);

                runOnUiThread(() ->
                        Toast.makeText(this,
                                "✅ Notification sent to the reporter!",
                                Toast.LENGTH_LONG).show());

            } catch (Exception e) {
                Log.e(TAG, "submitSighting error: " + e.getMessage(), e);
                runOnUiThread(() ->
                        Toast.makeText(this,
                                "Error: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
            }
        }).start();
    }

   
    private void confirmDeleteReport(ReportModel model) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Report")
                .setMessage("Are you sure you want to permanently delete \""
                        + nz(model.name, "Unknown") + "\"? This cannot be undone.")
                .setPositiveButton("Delete", (d, w) -> deleteReport(model.id))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteReport(String id) {
        new Thread(() -> {
            try {
                Databases db = AppwriteService.getDatabases();
                AppwriteHelper.deleteDocument(
                        db, AppwriteService.DB_ID,
                        AppwriteService.COL_REPORTS, id);
                ReportModelCache.remove(id);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Report deleted",
                            Toast.LENGTH_SHORT).show();
                    finish();
                });
            } catch (Exception e) {
                Log.e(TAG, "deleteReport error: " + e.getMessage(), e);
                runOnUiThread(() ->
                        Toast.makeText(this,
                                "Delete failed: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
            }
        }).start();
    }

   
    private void startChatWithUser(String myUid, String otherUid,
                                   String otherName, String reportName) {
        Toast.makeText(this, "Opening chat…", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                Databases db = AppwriteService.getDatabases();

                String resolvedName = otherName;
                try {
                    io.appwrite.models.Document<?> doc = AppwriteHelper.getDocument(
                            db, AppwriteService.DB_ID,
                            AppwriteService.COL_USERS, otherUid);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> d = (Map<String, Object>) doc.getData();
                    String n = strVal(d, "name");
                    if (!n.isEmpty()) resolvedName = n;
                } catch (Exception ignored) {}

                List<? extends io.appwrite.models.Document<?>> chats =
                        AppwriteHelper.getUserChats(db, myUid).getDocuments();

                String chatId = null;
                for (io.appwrite.models.Document<?> c : chats) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> cd = (Map<String, Object>) c.getData();
                    String p1 = strVal(cd, "participant1");
                    String p2 = strVal(cd, "participant2");
                    if ((p1.equals(myUid) && p2.equals(otherUid))
                            || (p1.equals(otherUid) && p2.equals(myUid))) {
                        chatId = c.getId();
                        break;
                    }
                }

                if (chatId == null) {
                    chatId = UUID.randomUUID().toString()
                            .replace("-", "").substring(0, 20);
                    SharedPreferences prefs =
                            getSharedPreferences("hoppe_prefs", MODE_PRIVATE);
                    Map<String, Object> chatData = new HashMap<>();
                    chatData.put("participant1",     myUid);
                    chatData.put("participant2",     otherUid);
                    chatData.put("participant1Name",
                            prefs.getString("logged_in_name", "User"));
                    chatData.put("participant2Name", resolvedName);
                    chatData.put("participants",     myUid + "," + otherUid);
                    chatData.put("lastMessage",      "Re: Missing – " + reportName);
                    chatData.put("lastMessageTime",  "");
                    AppwriteHelper.createDocument(
                            db, AppwriteService.DB_ID,
                            AppwriteService.COL_CHATS, chatId, chatData);
                }

                final String finalChatId = chatId;
                final String finalName   = resolvedName;
                runOnUiThread(() -> {
                    Intent i = new Intent(this, ChatRoomActivity.class);
                    i.putExtra("chatId",      finalChatId);
                    i.putExtra("otherUserId", otherUid);
                    i.putExtra("otherName",   finalName);
                    startActivity(i);
                });

            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this,
                                "Chat error: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

   
    private void generateAndSharePdf(ReportModel model) {
        Toast.makeText(this, "Generating PDF with photos…",
                Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                PdfDocument pdf = new PdfDocument();
                Paint p = new Paint();
                p.setAntiAlias(true);

               
                PdfDocument.PageInfo page1Info =
                        new PdfDocument.PageInfo.Builder(595, 842, 1).create();
                PdfDocument.Page page1 = pdf.startPage(page1Info);
                Canvas canvas = page1.getCanvas();

               
                p.setColor(Color.parseColor("#3F51B5"));
                canvas.drawRect(0, 0, 595, 80, p);
                p.setColor(Color.WHITE);
                p.setTextSize(16f); p.setFakeBoldText(true);
                canvas.drawText("HoppeConnect — Missing Person Report", 16, 32, p);
                p.setTextSize(10f); p.setFakeBoldText(false);
                canvas.drawText("Generated: " + new SimpleDateFormat(
                        "MMM dd, yyyy  HH:mm", Locale.getDefault())
                        .format(new Date()), 16, 55, p);

               
                p.setColor(Color.parseColor("#1A1A2E"));
                p.setTextSize(20f); p.setFakeBoldText(true);
                canvas.drawText(nz(model.name, "Unknown"), 16, 110, p);

               
                p.setTextSize(12f); p.setFakeBoldText(false);
                p.setColor(Color.parseColor("#555555"));
                int y = 140; int lh = 24;

               
                drawSectionTitle(canvas, p, "BASIC INFORMATION", y);
                y += lh;
                drawField(canvas, p, "Age",           model.age > 0
                        ? model.age + " yrs" : "N/A",         y); y += lh;
                drawField(canvas, p, "Gender",         nz(model.gender,       "N/A"), y); y += lh;
                drawField(canvas, p, "Missing Since",  nz(model.missingSince, "N/A"), y); y += lh;
                drawField(canvas, p, "Status",         nz(model.status,       "N/A"), y); y += lh;
                drawField(canvas, p, "Report ID",      nz(model.id,           "N/A"), y); y += lh;

                if (model.locationLat != null && !model.locationLat.isEmpty()) {
                    drawField(canvas, p, "GPS",
                            model.locationLat + ", " + model.locationLng, y);
                    y += lh;
                }
                if (model.createdAt > 0) {
                    drawField(canvas, p, "Reported On",
                            new SimpleDateFormat("dd MMM yyyy, hh:mm a",
                                    Locale.getDefault()).format(
                                    new Date(model.createdAt)), y);
                    y += lh;
                }
                y += 8;

               
                drawSectionTitle(canvas, p, "CASE DESCRIPTION", y); y += lh;
                p.setColor(Color.parseColor("#333333"));
                p.setTextSize(12f); p.setFakeBoldText(false);
                String desc = nz(model.description, "No description provided.");
                y = drawWrappedText(canvas, p, desc, 16, y, 563, lh);
                y += 8;

               
                drawSectionTitle(canvas, p, "CONTACT INFORMATION", y); y += lh;
                p.setColor(Color.parseColor("#555555"));
                p.setTextSize(12f);
                drawField(canvas, p, "Primary",     nz(model.contact,           "—"), y); y += lh;
                drawField(canvas, p, "Emergency 1", nz(model.emergencyContact1, "—"), y); y += lh;
                if (model.emergencyContact2 != null && !model.emergencyContact2.isEmpty()) {
                    drawField(canvas, p, "Emergency 2", model.emergencyContact2, y); y += lh;
                }
                if (model.emergencyContact3 != null && !model.emergencyContact3.isEmpty()) {
                    drawField(canvas, p, "Emergency 3", model.emergencyContact3, y); y += lh;
                }

               
                p.setColor(Color.parseColor("#AAAAAA")); p.setTextSize(9f);
                p.setFakeBoldText(false);
                canvas.drawText(
                        "HoppeConnect  •  Verify all information before acting.",
                        16, 830, p);

                pdf.finishPage(page1);

               
                if (model.photoUrls != null && !model.photoUrls.isEmpty()) {
                    int photoPage = 2;
                    for (String photoUrl : model.photoUrls) {
                        if (photoUrl == null || photoUrl.isEmpty()) continue;

                        android.graphics.Bitmap bmp = null;
                        try {
                           
                            bmp = com.bumptech.glide.Glide.with(
                                            getApplicationContext())
                                    .asBitmap()
                                    .load(photoUrl)
                                    .submit(595, 700)
                                    .get(10,
                                            java.util.concurrent.TimeUnit.SECONDS);
                        } catch (Exception imgEx) {
                            Log.w(TAG, "Photo load failed: " + imgEx.getMessage());
                        }

                        PdfDocument.PageInfo photoPageInfo =
                                new PdfDocument.PageInfo.Builder(
                                        595, 842, photoPage++).create();
                        PdfDocument.Page photoP = pdf.startPage(photoPageInfo);
                        Canvas photoCanvas = photoP.getCanvas();

                       
                        Paint bg = new Paint();
                        bg.setColor(Color.WHITE);
                        photoCanvas.drawRect(0, 0, 595, 842, bg);

                       
                        bg.setColor(Color.parseColor("#3F51B5"));
                        photoCanvas.drawRect(0, 0, 595, 44, bg);
                        bg.setColor(Color.WHITE);
                        bg.setTextSize(13f); bg.setFakeBoldText(true);
                        bg.setAntiAlias(true);
                        photoCanvas.drawText(
                                "Photo — " + nz(model.name, "Unknown"),
                                16, 28, bg);

                        if (bmp != null) {
                           
                            float scale = Math.min(
                                    563f / bmp.getWidth(),
                                    740f / bmp.getHeight());
                            int scaledW = (int) (bmp.getWidth()  * scale);
                            int scaledH = (int) (bmp.getHeight() * scale);
                            int offsetX = (595 - scaledW) / 2;
                            int offsetY = 54 + (740 - scaledH) / 2;

                            android.graphics.Rect dst = new android.graphics.Rect(
                                    offsetX, offsetY,
                                    offsetX + scaledW, offsetY + scaledH);
                            photoCanvas.drawBitmap(bmp, null, dst, null);
                            bmp.recycle();
                        } else {
                           
                            Paint ph = new Paint();
                            ph.setColor(Color.parseColor("#EEEEEE"));
                            photoCanvas.drawRect(16, 54, 579, 794, ph);
                            ph.setColor(Color.parseColor("#999999"));
                            ph.setTextSize(16f); ph.setAntiAlias(true);
                            ph.setTextAlign(Paint.Align.CENTER);
                            photoCanvas.drawText("Photo unavailable",
                                    297, 424, ph);
                        }

                       
                        Paint foot = new Paint();
                        foot.setColor(Color.parseColor("#AAAAAA"));
                        foot.setTextSize(9f); foot.setAntiAlias(true);
                        photoCanvas.drawText(
                                "HoppeConnect  •  Photo evidence",
                                16, 830, foot);

                        pdf.finishPage(photoP);
                    }
                }

               
                File outFile = new File(getCacheDir(), "report_"
                        + (model.id != null
                        ? model.id.substring(0, Math.min(6, model.id.length()))
                        : "x") + ".pdf");

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
                        Toast.makeText(this,
                                "PDF saved. Install a PDF viewer.",
                                Toast.LENGTH_LONG).show();
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "PDF gen error: " + e.getMessage(), e);
                runOnUiThread(() ->
                        Toast.makeText(this,
                                "PDF failed: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
            }
        }).start();
    }


    private void drawSectionTitle(Canvas canvas, Paint p,
                                  String title, int y) {
        p.setColor(Color.parseColor("#3F51B5"));
        p.setTextSize(11f);
        p.setFakeBoldText(true);
        canvas.drawText(title, 16, y, p);
       
        p.setStrokeWidth(1f);
        canvas.drawLine(16, y + 3, 579, y + 3, p);
        p.setFakeBoldText(false);
        p.setColor(Color.parseColor("#555555"));
        p.setTextSize(12f);
    }

    private void drawField(Canvas canvas, Paint p,
                           String label, String value, int y) {
        p.setColor(Color.parseColor("#888888"));
        p.setTextSize(11f);
        p.setFakeBoldText(false);
        canvas.drawText(label + ":", 16, y, p);

        p.setColor(Color.parseColor("#333333"));
        p.setTextSize(12f);
        canvas.drawText(value, 130, y, p);
    }

    private int drawWrappedText(Canvas canvas, Paint p,
                                String text, int x, int y,
                                int maxWidth, int lineHeight) {
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            String test = line.length() > 0 ? line + " " + word : word;
            if (p.measureText(test) > maxWidth) {
                canvas.drawText(line.toString(), x, y, p);
                y += lineHeight;
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(test);
            }
        }
        if (line.length() > 0) {
            canvas.drawText(line.toString(), x, y, p);
            y += lineHeight;
        }
        return y;
    }

   
    private void fetchReportFromDb(String id) {
        new Thread(() -> {
            try {
                Databases db = AppwriteService.getDatabases();
                io.appwrite.models.Document<Map<String, Object>> doc =
                        AppwriteHelper.getDocument(
                                db, AppwriteService.DB_ID,
                                AppwriteService.COL_REPORTS, id);

               
                ReportModel rm = MissedFragment.parseDocument(
                        doc.getId(), doc.getData(), doc.getCreatedAt());



                runOnUiThread(() -> bindData(rm));

            } catch (Exception e) {
                Log.e(TAG, "Fetch error: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Could not load report",
                            Toast.LENGTH_SHORT).show();
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