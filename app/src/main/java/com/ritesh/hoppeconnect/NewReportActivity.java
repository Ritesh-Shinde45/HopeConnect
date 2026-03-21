package com.ritesh.hoppeconnect;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.view.WindowInsetsController;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import io.appwrite.Permission;
import io.appwrite.Role;
import io.appwrite.coroutines.CoroutineCallback;
import io.appwrite.models.DocumentList;
import io.appwrite.services.Account;
import io.appwrite.services.Databases;
import io.appwrite.services.Storage;

public class NewReportActivity extends AppCompatActivity {

    private static final String TAG           = "NewReportActivity";
    private static final String BUCKET_ID     = "gvjgvjgvjgvjvg"; // ← your bucket ID
    private static final String DATABASE_ID   = AppwriteService.DB_ID;
    private static final String COLLECTION_ID = "reports";
    private static final int    MAX_REPORTS   = 5;

    // ── Form fields ───────────────────────────────────────────────────────────
    private TextInputEditText etName, etAge, etMissingSince, etContact,
            etEmergencyContact1, etEmergencyContact2, etEmergencyContact3, etDescription;
    private AutoCompleteTextView etGender;
    private MaterialButton btnSubmit;
    private ImageButton btnBack;
    private TextView tvLocationStatus, tvDocumentName;

    // ── Photo slots ───────────────────────────────────────────────────────────
    private final List<ImageView>    photoImageViews    = new ArrayList<>();
    private final List<LinearLayout> photoPlaceholders  = new ArrayList<>();
    private final List<ImageButton>  photoRemoveButtons = new ArrayList<>();
    private final List<View>         photoCards         = new ArrayList<>();
    private final List<Uri>          photoUris          =
            new ArrayList<>(Arrays.asList(null, null, null, null, null));
    private int currentPhotoIndex = 0;

    // ── Document ──────────────────────────────────────────────────────────────
    private Uri documentUri = null;

    // ── GPS ───────────────────────────────────────────────────────────────────
    private String locationLat = "";
    private String locationLng = "";
    private FusedLocationProviderClient fusedLocation;

    // ── Launchers ─────────────────────────────────────────────────────────────
    private ActivityResultLauncher<String>   pickImageLauncher;
    private ActivityResultLauncher<String>   requestPermLauncher;
    private ActivityResultLauncher<String>   pickDocLauncher;
    private ActivityResultLauncher<String[]> locationPermLauncher;

    // ── Appwrite ──────────────────────────────────────────────────────────────
    private Storage   storage;
    private Databases databases;
    private Account   account;

    private Calendar calendar;
    private SimpleDateFormat dateFormat;

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_report);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().getInsetsController().setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            );
        }

        try { AppwriteService.init(getApplicationContext()); }
        catch (Exception e) { Log.w(TAG, "init: " + e.getMessage()); }
        try { storage   = AppwriteService.getStorage();   } catch (Exception e) { storage   = null; }
        try { databases = AppwriteService.getDatabases(); } catch (Exception e) { databases = null; }
        try { account   = AppwriteService.getAccount();   } catch (Exception e) { account   = null; }

        fusedLocation = LocationServices.getFusedLocationProviderClient(this);
        calendar      = Calendar.getInstance();
        dateFormat    = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        initViews();
        setupGenderDropdown();
        setupDatePicker();
        setupPhotoUpload();
        setupDocumentUpload();
        setupLocationButton();
        setupBackButton();
        setupSubmitButton();
    }

    // ── Init views ────────────────────────────────────────────────────────────

    private void initViews() {
        etName              = findViewById(R.id.etName);
        etAge               = findViewById(R.id.etAge);
        etGender            = findViewById(R.id.etGender);
        etMissingSince      = findViewById(R.id.etMissingSince);
        etContact           = findViewById(R.id.etContact);
        etEmergencyContact1 = findViewById(R.id.etEmergencyContact1);
        etEmergencyContact2 = findViewById(R.id.etEmergencyContact2);
        etEmergencyContact3 = findViewById(R.id.etEmergencyContact3);
        etDescription       = findViewById(R.id.etDescription);
        btnSubmit           = findViewById(R.id.btnSubmit);
        btnBack             = findViewById(R.id.btnBack);
        tvLocationStatus    = findViewById(R.id.tvLocationStatus);
        tvDocumentName      = findViewById(R.id.tvDocumentName);

        for (int i = 1; i <= 5; i++) {
            int imgId    = getResId("imgPhoto"          + i);
            int phId     = getResId("uploadPlaceholder" + i);
            int removeId = getResId("btnRemovePhoto"    + i);
            int cardId   = getResId("photoCard"         + i);

            photoImageViews.add(imgId    != 0 ? (ImageView)    findViewById(imgId)    : null);
            View ph = phId != 0 ? findViewById(phId) : null;
            photoPlaceholders.add(ph instanceof LinearLayout ? (LinearLayout) ph : null);
            photoRemoveButtons.add(removeId != 0 ? (ImageButton) findViewById(removeId) : null);
            photoCards.add(cardId != 0 ? findViewById(cardId) : null);
        }
    }

    private int getResId(String name) {
        return getResources().getIdentifier(name, "id", getPackageName());
    }

    // ── Gender dropdown ───────────────────────────────────────────────────────

    private void setupGenderDropdown() {
        etGender.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line,
                new String[]{"Male", "Female", "Other"}));
    }

    // ── Date picker ───────────────────────────────────────────────────────────

    private void setupDatePicker() {
        etMissingSince.setOnClickListener(v -> {
            DatePickerDialog d = new DatePickerDialog(this,
                    (view, y, m, day) -> {
                        calendar.set(y, m, day);
                        etMissingSince.setText(dateFormat.format(calendar.getTime()));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH));
            d.getDatePicker().setMaxDate(System.currentTimeMillis());
            d.show();
        });
    }

    // ── Photo upload ──────────────────────────────────────────────────────────

    private void setupPhotoUpload() {
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> { if (uri != null) setPhotoUri(currentPhotoIndex, uri); });

        requestPermLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) pickImageLauncher.launch("image/*");
                    else Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                });

        for (int i = 0; i < photoCards.size(); i++) {
            final int idx = i;
            View card = photoCards.get(idx);
            if (card != null) {
                card.setOnClickListener(v -> {
                    if (photoUris.get(idx) == null) {
                        currentPhotoIndex = idx;
                        launchImagePicker();
                    }
                });
            }
            ImageButton remove = photoRemoveButtons.get(idx);
            if (remove != null) remove.setOnClickListener(v -> removePhoto(idx));
        }
    }

    private void launchImagePicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                pickImageLauncher.launch("image/*");
            } else {
                requestPermLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            pickImageLauncher.launch("image/*");
        }
    }

    private void setPhotoUri(int index, Uri uri) {
        photoUris.set(index, uri);
        ImageView img    = photoImageViews.get(index);
        LinearLayout ph  = photoPlaceholders.get(index);
        ImageButton rem  = photoRemoveButtons.get(index);
        if (img != null) { img.setImageURI(uri); img.setVisibility(View.VISIBLE); }
        if (ph  != null) ph.setVisibility(View.GONE);
        if (rem != null) rem.setVisibility(View.VISIBLE);
    }

    private void removePhoto(int index) {
        photoUris.set(index, null);
        ImageView img    = photoImageViews.get(index);
        LinearLayout ph  = photoPlaceholders.get(index);
        ImageButton rem  = photoRemoveButtons.get(index);
        if (img != null) { img.setImageURI(null); img.setVisibility(View.GONE); }
        if (ph  != null) ph.setVisibility(View.VISIBLE);
        if (rem != null) rem.setVisibility(View.GONE);
    }

    // ── Document upload ───────────────────────────────────────────────────────

    private void setupDocumentUpload() {
        pickDocLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        documentUri = uri;
                        String name = getFileName(uri);
                        if (tvDocumentName != null) {
                            tvDocumentName.setText("📄 " + name);
                            tvDocumentName.setVisibility(View.VISIBLE);
                        }
                    }
                });

        View btnDoc = findViewById(R.id.btnPickDocument);
        if (btnDoc != null) btnDoc.setOnClickListener(v ->
                pickDocLauncher.launch("application/pdf"));
    }

    // ── GPS location ──────────────────────────────────────────────────────────

    private void setupLocationButton() {
        locationPermLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                grants -> {
                    if (Boolean.TRUE.equals(grants.get(Manifest.permission.ACCESS_FINE_LOCATION))) {
                        fetchGps();
                    } else {
                        Toast.makeText(this, "Location permission denied",
                                Toast.LENGTH_SHORT).show();
                    }
                });

        View btnLoc = findViewById(R.id.btnGetLocation);
        if (btnLoc != null) {
            btnLoc.setOnClickListener(v -> {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    fetchGps();
                } else {
                    locationPermLauncher.launch(new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION});
                }
            });
        }
    }

    private void fetchGps() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) return;
        fusedLocation.getLastLocation().addOnSuccessListener(loc -> {
            if (loc != null) {
                locationLat = String.valueOf(loc.getLatitude());
                locationLng = String.valueOf(loc.getLongitude());
                if (tvLocationStatus != null) {
                    tvLocationStatus.setText("📍 " + locationLat + ", " + locationLng);
                    tvLocationStatus.setVisibility(View.VISIBLE);
                }
                Toast.makeText(this, "Location captured!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Could not get location. Try again.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ── Back button ───────────────────────────────────────────────────────────

    private void setupBackButton() {
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
    }

    // ── Submit ────────────────────────────────────────────────────────────────

    private void setupSubmitButton() {
        btnSubmit.setOnClickListener(v -> {
            if (!validateForm()) return;
            btnSubmit.setEnabled(false);

            android.content.SharedPreferences prefs =
                    getSharedPreferences("hoppe_prefs", MODE_PRIVATE);
            String userId = prefs.getString("logged_in_user_id", null);

            if (userId != null && !userId.isEmpty()) {
                checkLimitThenUpload(userId);
            } else {
                uploadAndSave(null);
            }
        });
    }

    private boolean validateForm() {
        boolean ok = true;
        if (safeText(etName).isEmpty())        { etName.setError("Required");        ok = false; }
        String ageStr = safeText(etAge);
        if (ageStr.isEmpty()) {
            etAge.setError("Required"); ok = false;
        } else {
            try {
                int a = Integer.parseInt(ageStr);
                if (a < 0 || a > 120) { etAge.setError("Invalid age"); ok = false; }
            } catch (NumberFormatException e) { etAge.setError("Invalid age"); ok = false; }
        }
        if (safeText(etGender).isEmpty())       { etGender.setError("Required");      ok = false; }
        if (safeText(etMissingSince).isEmpty()) { etMissingSince.setError("Required"); ok = false; }
        if (safeText(etDescription).isEmpty())  { etDescription.setError("Required"); ok = false; }
        if (safeText(etEmergencyContact1).isEmpty()
                && safeText(etEmergencyContact2).isEmpty()
                && safeText(etEmergencyContact3).isEmpty()) {
            Toast.makeText(this, "At least one emergency contact required",
                    Toast.LENGTH_SHORT).show();
            ok = false;
        }
        boolean hasPhoto = false;
        for (Uri u : photoUris) if (u != null) { hasPhoto = true; break; }
        if (!hasPhoto) {
            Toast.makeText(this, "At least one photo required", Toast.LENGTH_SHORT).show();
            ok = false;
        }
        return ok;
    }

    // ── Limit check ───────────────────────────────────────────────────────────

    private void checkLimitThenUpload(String userId) {
        if (databases == null) { uploadAndSave(userId); return; }
        try {
            databases.listDocuments(DATABASE_ID, COLLECTION_ID, new ArrayList<>(),
                    new CoroutineCallback<DocumentList<Map<String, Object>>>((result, error) -> {
                        if (error != null) { uploadAndSave(userId); return; }
                        int count = 0;
                        for (io.appwrite.models.Document<Map<String, Object>> doc
                                : result.getDocuments()) {
                            Map<String, Object> m = doc.getData();
                            Object uid = m.get("userId");
                            Object st  = m.get("status");
                            if (uid != null && uid.toString().equals(userId)
                                    && st != null && st.toString().equals("active")) count++;
                        }
                        if (count >= MAX_REPORTS) {
                            runOnUiThread(() -> {
                                Toast.makeText(this,
                                        "You already have " + MAX_REPORTS + " active reports.",
                                        Toast.LENGTH_LONG).show();
                                btnSubmit.setEnabled(true);
                            });
                        } else {
                            uploadAndSave(userId);
                        }
                    }));
        } catch (Exception e) {
            Log.w(TAG, "listDocuments error: " + e.getMessage());
            uploadAndSave(userId);
        }
    }

    // ── Upload photos ─────────────────────────────────────────────────────────

    private void uploadAndSave(String userId) {
        List<Uri> toUpload = new ArrayList<>();
        for (Uri u : photoUris) if (u != null) toUpload.add(u);

        if (toUpload.isEmpty()) {
            runOnUiThread(() -> {
                Toast.makeText(this, "No photos to upload", Toast.LENGTH_SHORT).show();
                btnSubmit.setEnabled(true);
            });
            return;
        }

        runOnUiThread(() -> Toast.makeText(this,
                "Uploading " + toUpload.size() + " photo(s)…", Toast.LENGTH_SHORT).show());

        final int total = toUpload.size();
        final AtomicInteger doneCount = new AtomicInteger(0);
        final AtomicInteger failCount = new AtomicInteger(0);
        final CopyOnWriteArrayList<String> uploadedUrls = new CopyOnWriteArrayList<>();

        for (int i = 0; i < toUpload.size(); i++) {
            final int idx = i;
            final Uri uri = toUpload.get(i);
            new Thread(() -> {
                try {
                    if (storage == null) throw new Exception("Storage not initialized");

                    byte[] bytes  = readBytes(uri);
                    String fileId = UUID.randomUUID().toString().replace("-", "");

                    io.appwrite.models.File uploaded;
                    try {
                        uploaded = AppwriteHelper.uploadFileBlocking(
                                storage, BUCKET_ID, fileId,
                                bytes, "photo_" + idx + ".jpg", "image/jpeg");
                    } catch (Throwable t) {
                        throw new Exception("Upload error: " + t.getMessage(), t);
                    }

                    String url = AppwriteService.ENDPOINT
                            + "/storage/buckets/" + BUCKET_ID
                            + "/files/" + uploaded.getId()
                            + "/view?project=" + AppwriteService.PROJECT_ID;

                    uploadedUrls.add(url);
                    int done = doneCount.incrementAndGet();
                    Log.d(TAG, "Photo " + done + "/" + total + " → " + url);

                    if (done + failCount.get() == total) {
                        uploadDocThenSave(userId, new ArrayList<>(uploadedUrls));
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Photo upload error [" + idx + "]: " + e.getMessage(), e);
                    int fails = failCount.incrementAndGet();
                    runOnUiThread(() -> Toast.makeText(this,
                            "Photo " + (idx + 1) + " failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show());
                    if (doneCount.get() + fails == total) {
                        if (!uploadedUrls.isEmpty()) {
                            uploadDocThenSave(userId, new ArrayList<>(uploadedUrls));
                        } else {
                            runOnUiThread(() -> btnSubmit.setEnabled(true));
                        }
                    }
                }
            }).start();
        }
    }

    // ── Upload document then save ─────────────────────────────────────────────

    private void uploadDocThenSave(String userId, List<String> photoUrls) {
        if (documentUri == null || storage == null) {
            saveToDb(userId, photoUrls, "");
            return;
        }
        new Thread(() -> {
            String docUrl = "";
            try {
                byte[] bytes  = readBytes(documentUri);
                String fileId = UUID.randomUUID().toString().replace("-", "");
                String name   = getFileName(documentUri);

                io.appwrite.models.File uploaded;
                try {
                    uploaded = AppwriteHelper.uploadFileBlocking(
                            storage, BUCKET_ID, fileId, bytes, name, "application/pdf");
                } catch (Throwable t) {
                    throw new Exception("Doc upload error: " + t.getMessage(), t);
                }

                docUrl = AppwriteService.ENDPOINT
                        + "/storage/buckets/" + BUCKET_ID
                        + "/files/" + uploaded.getId()
                        + "/view?project=" + AppwriteService.PROJECT_ID;

            } catch (Exception e) {
                Log.e(TAG, "Document upload error: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(this,
                        "Document upload failed, saving report without it.",
                        Toast.LENGTH_SHORT).show());
            }
            saveToDb(userId, photoUrls, docUrl);
        }).start();
    }

    // ── Save to Appwrite DB ───────────────────────────────────────────────────
    // FIX: Use Role.user(userId) without empty string scope to avoid malformed
    //      permission strings like "user:abc123:" that Appwrite rejects.

    private void saveToDb(String userId, List<String> photoUrls, String documentUrl) {

        android.content.SharedPreferences prefs =
                getSharedPreferences("hoppe_prefs", MODE_PRIVATE);
        String reporterName = prefs.getString("logged_in_name", "");

        Map<String, Object> data = new HashMap<>();
        data.put("name",              safeText(etName));
        data.put("age",               Integer.parseInt(safeText(etAge)));
        data.put("gender",            safeText(etGender));
        data.put("missingSince",      safeText(etMissingSince));
        data.put("contact",           safeText(etContact));
        data.put("emergencyContact1", safeText(etEmergencyContact1));
        data.put("emergencyContact2", safeText(etEmergencyContact2));
        data.put("emergencyContact3", safeText(etEmergencyContact3));
        data.put("description",       safeText(etDescription));
        data.put("photoUrls",         photoUrls);
        data.put("userId",            userId != null ? userId : "");
        data.put("reportedBy",        userId != null ? userId : "");
        data.put("reporterName",      reporterName);
        data.put("status",            "pending");
        data.put("locationLat",       locationLat);
        data.put("locationLng",       locationLng);
        if (documentUrl != null && documentUrl.startsWith("http")) {
            data.put("documentUrl", documentUrl);
        }

        // ── FIXED PERMISSIONS ─────────────────────────────────────────────────
        // Old (broken): Role.Companion.user(userId, "")  →  produces "user:abc123:"
        // New (correct): Role.Companion.user(userId)     →  produces "user:abc123"
        // ─────────────────────────────────────────────────────────────────────
        // Use empty permissions list — rely on collection-level permissions in Appwrite console.
        // Document-level permission strings vary by SDK version and cause rejection errors.
        List<String> permissions = new ArrayList<>();

        if (databases == null) {
            runOnUiThread(() -> {
                Toast.makeText(this, "Databases not initialized",
                        Toast.LENGTH_LONG).show();
                btnSubmit.setEnabled(true);
            });
            return;
        }

        try {
            databases.createDocument(
                    DATABASE_ID,
                    COLLECTION_ID,
                    UUID.randomUUID().toString(),
                    data,
                    permissions,
                    new CoroutineCallback<>((result, error) -> runOnUiThread(() -> {
                        btnSubmit.setEnabled(true);
                        if (error != null) {
                            Log.e(TAG, "createDocument error: " + error.getMessage());
                            Toast.makeText(this,
                                    "Save failed: " + error.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this,
                                    "✅ Report submitted! Waiting for admin approval.",
                                    Toast.LENGTH_LONG).show();
                            finish();
                        }
                    })));
        } catch (Exception e) {
            Log.e(TAG, "createDocument exception: " + e.getMessage(), e);
            runOnUiThread(() -> {
                btnSubmit.setEnabled(true);
                Toast.makeText(this, "Save failed: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            });
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String safeText(android.widget.TextView t) {
        if (t == null) return "";
        CharSequence cs = t.getText();
        return cs == null ? "" : cs.toString().trim();
    }

    private byte[] readBytes(Uri uri) throws Exception {
        InputStream is = getContentResolver().openInputStream(uri);
        if (is == null) throw new Exception("Cannot open URI");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int len;
        while ((len = is.read(buf)) != -1) bos.write(buf, 0, len);
        is.close();
        return bos.toByteArray();
    }

    private String getFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (android.database.Cursor c = getContentResolver().query(uri,
                    new String[]{OpenableColumns.DISPLAY_NAME},
                    null, null, null)) {
                if (c != null && c.moveToFirst()) result = c.getString(0);
            } catch (Exception ignored) {}
        }
        if (result == null) {
            String path = uri.getPath();
            if (path == null) return "file";
            int idx = path.lastIndexOf('/');
            result = idx >= 0 ? path.substring(idx + 1) : path;
        }
        return result;
    }
}