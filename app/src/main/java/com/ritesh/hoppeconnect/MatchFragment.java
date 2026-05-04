package com.ritesh.hoppeconnect;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class MatchFragment extends Fragment {

   
    private Interpreter tflite;
    private static final float MATCH_THRESHOLD = 0.50f;

   
    private float[] queryEmbedding = null;
    private Bitmap  selectedBitmap = null;
    private int     searchScope    = 3;
    private String  filterAge      = "Any";
    private String  filterGender   = "Any";
    private String  locationInput  = "";
    private boolean isMatching     = false;

   
    private ImageView        ivSelectedPhoto;
    private MaterialCardView cardPhoto;
    private Button           btnSelectPhoto, btnStartMatch, btnOptions;
    private ProgressBar      progressBar;
    private TextView         tvStatus, tvProgress, tvResultCount, tvEmptyHint;
    private RecyclerView     rvResults;
    private LinearLayout     layoutEmpty, layoutResult;

   
    private final List<FaceMatchResult> resultList = new ArrayList<>();
    private FaceMatchResultAdapter resultAdapter;

   
    private FaceDetector faceDetector;

   
    private ActivityResultLauncher<Intent> photoPickerLauncher;

   
    private ExecutorService executor    = Executors.newFixedThreadPool(3);
    private final Handler   mainHandler = new Handler(Looper.getMainLooper());

    public MatchFragment() {}
    public static MatchFragment newInstance() { return new MatchFragment(); }

   
   
   

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        if (executor == null || executor.isShutdown())
            executor = Executors.newFixedThreadPool(3);
        super.onCreate(savedInstanceState);

        photoPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK
                            && result.getData() != null)
                        handleSelectedImage(result.getData().getData());
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_match, container, false);
        bindViews(root);
        setupRecyclerView();
        initFaceDetector();
        loadTFLiteModel();

        btnSelectPhoto.setOnClickListener(v -> openPhotoPicker());

        btnStartMatch.setOnClickListener(v -> {
            if (queryEmbedding == null) {
                Toast.makeText(getContext(), "Select a photo first", Toast.LENGTH_SHORT).show();
                return;
            }
            showOptionsDialog();
        });

        btnOptions.setOnClickListener(v -> showOptionsDialog());

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isMatching = false;
        executor.shutdown();
        executor = Executors.newFixedThreadPool(3);
        tvStatus        = null;
        progressBar     = null;
        btnStartMatch   = null;
        ivSelectedPhoto = null;
        cardPhoto       = null;
        tvProgress      = null;
        tvResultCount   = null;
        tvEmptyHint     = null;
        rvResults       = null;
        layoutEmpty     = null;
        layoutResult    = null;
    }

   
   
   

    private void bindViews(View root) {
        ivSelectedPhoto = root.findViewById(R.id.ivSelectedPhoto);
        cardPhoto       = root.findViewById(R.id.cardPhoto);
        btnSelectPhoto  = root.findViewById(R.id.btnSelectPhoto);
        btnStartMatch   = root.findViewById(R.id.btnStartMatch);
        btnOptions      = root.findViewById(R.id.btnOptions);
        progressBar     = root.findViewById(R.id.progressBar);
        tvStatus        = root.findViewById(R.id.tvStatus);
        tvProgress      = root.findViewById(R.id.tvProgress);
        tvResultCount   = root.findViewById(R.id.tvResultCount);
        tvEmptyHint     = root.findViewById(R.id.tvEmptyHint);
        rvResults       = root.findViewById(R.id.rvResults);
        layoutEmpty     = root.findViewById(R.id.layoutEmpty);
        layoutResult    = root.findViewById(R.id.layoutResult);
    }

    private void setupRecyclerView() {
        resultAdapter = new FaceMatchResultAdapter(
                requireContext(), resultList,
                model -> {
                    ReportModelCache.put(model);
                    Intent i = new Intent(getContext(), MissedPersonDetailActivity.class);
                    i.putExtra(MissedPersonDetailActivity.EXTRA_REPORT_ID, model.id);
                    startActivity(i);
                });
        rvResults.setLayoutManager(new LinearLayoutManager(getContext()));
        rvResults.setNestedScrollingEnabled(false);
        rvResults.setAdapter(resultAdapter);
    }

   
   
   

    private void showOptionsDialog() {
        if (!isAdded()) return;

        BottomSheetDialog sheet = new BottomSheetDialog(requireContext());
        View dv = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_face_match_options, null);
        sheet.setContentView(dv);
        sheet.getBehavior().setPeekHeight(900);

       
        ImageView dialogPhoto       = dv.findViewById(R.id.dialogPhoto);
        Button    dialogSelectPhoto = dv.findViewById(R.id.dialogSelectPhoto);
        TextView  tvFaceDetected    = dv.findViewById(R.id.tvFaceDetected);

        if (selectedBitmap != null) dialogPhoto.setImageBitmap(selectedBitmap);
        tvFaceDetected.setVisibility(queryEmbedding != null ? View.VISIBLE : View.GONE);
        dialogSelectPhoto.setOnClickListener(v -> { sheet.dismiss(); openPhotoPicker(); });

       
        ChipGroup chipAge     = dv.findViewById(R.id.chipGroupAge);
        Chip chipAgeAny       = dv.findViewById(R.id.chipAgeAny);
        Chip chipAgeChild     = dv.findViewById(R.id.chipAgeChild);
        Chip chipAgeAdult     = dv.findViewById(R.id.chipAgeAdult);
        Chip chipAgeElderly   = dv.findViewById(R.id.chipAgeElderly);
        chipAgeAny.setChecked("Any".equals(filterAge));
        chipAgeChild.setChecked("Child".equals(filterAge));
        chipAgeAdult.setChecked("Adult".equals(filterAge));
        chipAgeElderly.setChecked("Elderly".equals(filterAge));

       
        ChipGroup chipGenderGroup = dv.findViewById(R.id.chipGroupGender);
        Chip chipGenderAny        = dv.findViewById(R.id.chipGenderAny);
        Chip chipGenderMale       = dv.findViewById(R.id.chipGenderMale);
        Chip chipGenderFemale     = dv.findViewById(R.id.chipGenderFemale);
        chipGenderAny.setChecked("Any".equals(filterGender));
        chipGenderMale.setChecked("Male".equals(filterGender));
        chipGenderFemale.setChecked("Female".equals(filterGender));

       
        RadioGroup   radioScope     = dv.findViewById(R.id.radioScope);
        LinearLayout layoutLocation = dv.findViewById(R.id.layoutLocationInput);
        EditText     etLocation     = dv.findViewById(R.id.etLocation);
        Button       btnStart       = dv.findViewById(R.id.btnStartMatching);

        if      (searchScope == 0) radioScope.check(R.id.radioLocation);
        else if (searchScope == 1) radioScope.check(R.id.radioState);
        else if (searchScope == 2) radioScope.check(R.id.radioCountry);
        else                       radioScope.check(R.id.radioAll);

        layoutLocation.setVisibility(searchScope == 0 ? View.VISIBLE : View.GONE);
        if (!locationInput.isEmpty()) etLocation.setText(locationInput);

        radioScope.setOnCheckedChangeListener((rg, checkedId) ->
                layoutLocation.setVisibility(
                        checkedId == R.id.radioLocation ? View.VISIBLE : View.GONE));

        btnStart.setOnClickListener(v -> {
            if (queryEmbedding == null) {
                Toast.makeText(getContext(),
                        "Select a photo first", Toast.LENGTH_SHORT).show();
                return;
            }
            searchScope   = scopeCodeFor(radioScope.getCheckedRadioButtonId());
            filterAge     = selectedChipText(chipAge,
                    new Chip[]{chipAgeAny, chipAgeChild, chipAgeAdult, chipAgeElderly},
                    new String[]{"Any", "Child", "Adult", "Elderly"});
            filterGender  = selectedChipText(chipGenderGroup,
                    new Chip[]{chipGenderAny, chipGenderMale, chipGenderFemale},
                    new String[]{"Any", "Male", "Female"});
            locationInput = etLocation.getText().toString().trim();
            sheet.dismiss();
            startFaceMatching();
        });

        sheet.show();
    }

    private int scopeCodeFor(int id) {
        if (id == R.id.radioLocation) return 0;
        if (id == R.id.radioState)    return 1;
        if (id == R.id.radioCountry)  return 2;
        return 3;
    }

    private String selectedChipText(ChipGroup g, Chip[] chips, String[] labels) {
        for (int i = 0; i < chips.length; i++) if (chips[i].isChecked()) return labels[i];
        return labels[0];
    }

   
   
   

    private void startFaceMatching() {
        if (isMatching) return;
        isMatching = true;

        resultList.clear();
        if (resultAdapter != null) resultAdapter.notifyDataSetChanged();
        setVisible(layoutEmpty,  false);
        setVisible(layoutResult, false);
        setVisible(progressBar,  true);
        setText(tvStatus,      "Fetching reports…");
        setText(tvProgress,    "");
        setText(tvResultCount, "0 matched");

        executor.execute(() -> {
            try {
                AppwriteService.init(requireContext());
                io.appwrite.services.Databases db = AppwriteService.getDatabases();
                List<ReportModel> reports = fetchAllReports(db);
                int total = reports.size();

                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    setText(tvStatus, "Scanning " + total + " reports…");
                });

                AtomicInteger scanned = new AtomicInteger(0);
                AtomicInteger matched = new AtomicInteger(0);
                List<FaceMatchResult> tmp =
                        Collections.synchronizedList(new ArrayList<>());

                for (ReportModel report : reports) {
                    if (!isAdded() || !isMatching) break;

                    int idx = scanned.incrementAndGet();
                    mainHandler.post(() -> setText(tvProgress,
                            idx + "/" + total + "  •  " + matched.get() + " matched"));

                    if (report.photoUrls == null || report.photoUrls.isEmpty()) continue;

                    float sim = matchFaceFromUrl(report.photoUrls.get(0));

                    if (sim >= MATCH_THRESHOLD) {
                        matched.incrementAndGet();
                        tmp.add(new FaceMatchResult(report, sim));

                        List<FaceMatchResult> snap = new ArrayList<>(tmp);
                        Collections.sort(snap,
                                (a, b) -> Float.compare(b.similarity, a.similarity));
                        mainHandler.post(() -> {
                            if (!isAdded()) return;
                            resultList.clear();
                            resultList.addAll(snap);
                            if (resultAdapter != null) resultAdapter.notifyDataSetChanged();
                            setVisible(layoutResult, true);
                            setText(tvResultCount, matched.get() + " matched");
                        });
                    }
                }

                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    isMatching = false;
                    setVisible(progressBar, false);
                    setText(tvProgress, "");
                    if (resultList.isEmpty()) {
                        setText(tvStatus, "No matches found");
                        setVisible(layoutEmpty, true);
                        setText(tvEmptyHint,
                                "No face matched above "
                                        + Math.round(MATCH_THRESHOLD * 100)
                                        + "% similarity.\nTry a clearer front-facing photo.");
                    } else {
                        setText(tvStatus,
                                "✅ Done — " + resultList.size() + " match(es) found");
                    }
                });

            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    isMatching = false;
                    setVisible(progressBar, false);
                    setText(tvStatus, "Error: " + e.getMessage());
                });
            }
        });
    }

   
   
   

    private List<ReportModel> fetchAllReports(
            io.appwrite.services.Databases db) throws Exception {

        List<ReportModel> all = new ArrayList<>();
        int page = 0;
        final int PS = 25, MAX = 500;

        final String cityF = (searchScope == 0
                && locationInput != null
                && !locationInput.trim().isEmpty())
                ? locationInput.trim().toLowerCase() : null;

        while (all.size() < MAX) {
            List<String> q = new ArrayList<>();
            q.add(io.appwrite.Query.Companion.limit(PS));
            q.add(io.appwrite.Query.Companion.offset(page * PS));

            List<? extends io.appwrite.models.Document<?>> docs;
            try {
                docs = AppwriteHelper.listDocuments(
                        db, AppwriteService.DB_ID,
                        AppwriteService.COL_REPORTS, q).getDocuments();
            } catch (Exception e) {
                android.util.Log.e("MatchFragment", "Query failed: " + e.getMessage());
                break;
            }

            if (docs.isEmpty()) break;

            for (io.appwrite.models.Document<?> doc : docs) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) doc.getData();

               
               
                if (cityF != null &&
                        !str(data, "city").toLowerCase().contains(cityF)) continue;

               
                if ("Child".equals(filterAge)   && intVal(data, "age") >= 18) continue;
                if ("Adult".equals(filterAge))  {
                    int a = intVal(data, "age");
                    if (a < 18 || a > 60) continue;
                }
                if ("Elderly".equals(filterAge) && intVal(data, "age") <= 60) continue;

               
                if (!"Any".equals(filterGender) &&
                        !str(data, "gender").equalsIgnoreCase(filterGender)) continue;

               
                ReportModel m = new ReportModel();
                m.id                = doc.getId();
                m.name              = str(data, "name");
                m.age               = intVal(data, "age");
                m.gender            = str(data, "gender");
                m.status            = str(data, "status");
                m.description       = str(data, "description");
                m.contact           = str(data, "contact");
                m.emergencyContact1 = str(data, "emergencyContact1");
                m.missingSince      = str(data, "missingSince");

               
                Object rawUrls = data.get("photoUrls");
                List<String> urls = new ArrayList<>();
                if (rawUrls instanceof List) {
                    for (Object item : (List<?>) rawUrls) {
                        if (item == null) continue;
                        String val = item.toString().trim();
                        if (!val.isEmpty()) urls.add(resolveUrl(val));
                    }
                } else if (rawUrls instanceof String
                        && !((String) rawUrls).trim().isEmpty()) {
                    for (String part : ((String) rawUrls).trim().split(",")) {
                        String val = part.trim();
                        if (!val.isEmpty()) urls.add(resolveUrl(val));
                    }
                }
                m.photoUrls = urls;

               
                Object latObj = data.get("locationLat");
                Object lngObj = data.get("locationLng");
                if (latObj instanceof String) m.locationLat = (String) latObj;
                if (lngObj instanceof String) m.locationLng = (String) lngObj;
                if (latObj instanceof Number)
                    m.locationLat = String.valueOf(((Number) latObj).doubleValue());
                if (lngObj instanceof Number)
                    m.locationLng = String.valueOf(((Number) lngObj).doubleValue());

                all.add(m);
            }

            if (docs.size() < PS) break;
            page++;
        }
        return all;
    }


    private String resolveUrl(String val) {
        if (!val.startsWith("http")) return buildViewUrl(val);
        if (!val.contains("project="))
            return val + (val.contains("?") ? "&" : "?")
                    + "project=" + AppwriteService.PROJECT_ID;
        return val;
    }

    private String buildViewUrl(String fileId) {
        return AppwriteService.ENDPOINT
                + "/storage/buckets/" + AppwriteService.USERS_BUCKET_ID
                + "/files/" + fileId
                + "/view?project=" + AppwriteService.PROJECT_ID;
    }

   
   
   

    private float matchFaceFromUrl(String url) {
        try {
            Bitmap bmp = Glide.with(requireContext())
                    .asBitmap()
                    .load(url)
                    .submit(600, 600)
                    .get(15, java.util.concurrent.TimeUnit.SECONDS);
            if (bmp == null) return 0f;
            float[] emb = extractFaceEmbedding(bmp);
            return emb == null ? 0f : cosineSimilarity(queryEmbedding, emb);
        } catch (Exception e) {
            android.util.Log.e("FaceMatch", "matchFaceFromUrl error: " + e.getMessage());
            return 0f;
        }
    }

   
   
   

    private float[] extractFaceEmbedding(Bitmap bitmap) {
        if (tflite == null) return null;

        final float[][] result = {null};
        final Object    lock   = new Object();
        final boolean[] done   = {false};

        InputImage img = InputImage.fromBitmap(bitmap, 0);
        faceDetector.process(img)
                .addOnSuccessListener(faces -> {
                    Bitmap target = bitmap;
                    if (!faces.isEmpty()) {
                        android.graphics.Rect box = faces.get(0).getBoundingBox();
                        int pw = (int)(box.width()  * 0.2f);
                        int ph = (int)(box.height() * 0.2f);
                        int l  = Math.max(0, box.left   - pw);
                        int t  = Math.max(0, box.top    - ph);
                        int r  = Math.min(bitmap.getWidth(),  box.right  + pw);
                        int b  = Math.min(bitmap.getHeight(), box.bottom + ph);
                        if (r > l && b > t)
                            target = Bitmap.createBitmap(bitmap, l, t, r - l, b - t);
                    }
                    result[0] = runInference(target);
                    synchronized (lock) { done[0] = true; lock.notifyAll(); }
                })
                .addOnFailureListener(e -> {
                    result[0] = runInference(bitmap);
                    synchronized (lock) { done[0] = true; lock.notifyAll(); }
                });

        synchronized (lock) {
            try { if (!done[0]) lock.wait(10_000); }
            catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        }
        return result[0];
    }

    private float[] runInference(Bitmap bmp) {
        if (bmp == null || tflite == null) return null;

        Bitmap scaled = Bitmap.createScaledBitmap(bmp, 112, 112, true);
        ByteBuffer buf = ByteBuffer.allocateDirect(4 * 112 * 112 * 3);
        buf.order(ByteOrder.nativeOrder());

        int[] px = new int[112 * 112];
        scaled.getPixels(px, 0, 112, 0, 0, 112, 112);
        for (int p : px) {
            buf.putFloat(((p >> 16 & 0xFF) - 127.5f) / 127.5f);
            buf.putFloat(((p >>  8 & 0xFF) - 127.5f) / 127.5f);
            buf.putFloat(((p       & 0xFF) - 127.5f) / 127.5f);
        }
        buf.rewind();

        float[][] out = new float[1][192];
        try {
            tflite.run(buf, out);
        } catch (Exception e) {
            int[] sh = tflite.getOutputTensor(0).shape();
            out = new float[1][sh[1]];
            tflite.run(buf, out);
        }
        return l2Normalize(out[0]);
    }

    private float[] l2Normalize(float[] v) {
        float s = 0f;
        for (float x : v) s += x * x;
        float n = (float) Math.sqrt(s);
        if (n < 1e-10f) return v;
        float[] o = new float[v.length];
        for (int i = 0; i < v.length; i++) o[i] = v[i] / n;
        return o;
    }

    private void extractQueryEmbedding(Bitmap bitmap) {
        setText(tvStatus, "Detecting face…");
        setVisible(progressBar, true);

        executor.execute(() -> {
            float[] emb = extractFaceEmbedding(bitmap);
            mainHandler.post(() -> {
                if (!isAdded()) return;
                setVisible(progressBar, false);
                if (emb != null) {
                    queryEmbedding = emb;
                    setText(tvStatus, "✅ Face detected — ready to match");
                    if (btnStartMatch != null) {
                        btnStartMatch.setEnabled(true);
                        btnStartMatch.setAlpha(1f);
                    }
                } else {
                    queryEmbedding = null;
                    setText(tvStatus, "⚠️ No face detected. Try a clearer photo.");
                    if (btnStartMatch != null) {
                        btnStartMatch.setEnabled(false);
                        btnStartMatch.setAlpha(0.45f);
                    }
                }
            });
        });
    }

    private float cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) return 0f;
        float dot = 0f;
        for (int i = 0; i < a.length; i++) dot += a[i] * b[i];
        return Math.max(0f, dot);
    }

   
   
   

    private void openPhotoPicker() {
        Intent i = new Intent(Intent.ACTION_PICK);
        i.setType("image/*");
        photoPickerLauncher.launch(i);
    }

    private void handleSelectedImage(Uri uri) {
        try {
            InputStream is  = requireContext().getContentResolver().openInputStream(uri);
            Bitmap      bmp = BitmapFactory.decodeStream(is);
            if (bmp == null) {
                Toast.makeText(getContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
                return;
            }
            selectedBitmap = bmp;
            if (ivSelectedPhoto != null) ivSelectedPhoto.setImageBitmap(bmp);
            if (cardPhoto       != null) cardPhoto.setVisibility(View.VISIBLE);
            extractQueryEmbedding(bmp);
        } catch (Exception e) {
            Toast.makeText(getContext(),
                    "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

   
   
   

    private void initFaceDetector() {
        faceDetector = FaceDetection.getClient(
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .setMinFaceSize(0.1f)
                        .build());
    }

    private void loadTFLiteModel() {
        try {
            AssetFileDescriptor afd =
                    requireContext().getAssets().openFd("MobileFaceNet.tflite");
            MappedByteBuffer model =
                    new FileInputStream(afd.getFileDescriptor()).getChannel()
                            .map(FileChannel.MapMode.READ_ONLY,
                                    afd.getStartOffset(), afd.getDeclaredLength());
            Interpreter.Options opts = new Interpreter.Options();
            opts.setNumThreads(2);
            tflite = new Interpreter(model, opts);
        } catch (Exception e) {
            Toast.makeText(getContext(),
                    "Model load error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

   
   
   

    private void setText(TextView tv, String t)   { if (tv != null) tv.setText(t); }
    private void setVisible(View v, boolean show) {
        if (v != null) v.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private static String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v != null ? v.toString() : "";
    }

    private static int intVal(Map<String, Object> m, String k) {
        Object v = m.get(k);
        if (v instanceof Number) return ((Number) v).intValue();
        try { return Integer.parseInt(String.valueOf(v)); }
        catch (Exception e) { return 0; }
    }

   
   
   

    public static class FaceMatchResult {
        public final ReportModel report;
        public final float       similarity;

        public FaceMatchResult(ReportModel r, float s) {
            report = r; similarity = s;
        }

        public String percentageLabel() {
            return Math.round(similarity * 100) + "%";
        }
    }

   
   
   

    static class FaceMatchResultAdapter
            extends RecyclerView.Adapter<FaceMatchResultAdapter.VH> {

        interface OnClick { void onClick(ReportModel m); }

        private final Context               ctx;
        private final List<FaceMatchResult> data;
        private final OnClick               listener;

        FaceMatchResultAdapter(Context c, List<FaceMatchResult> d, OnClick l) {
            ctx = c; data = d; listener = l;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(ctx)
                    .inflate(R.layout.item_face_match_result, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            FaceMatchResult item  = data.get(pos);
            ReportModel     m     = item.report;
            float           pct   = item.similarity;

            h.tvName.setText(m.name != null ? m.name : "Unknown");
            h.tvAge.setText(m.age > 0 ? m.age + " yrs" : "Age N/A");
            h.tvGender.setText(m.gender != null ? m.gender : "");
            h.tvPercent.setText(Math.round(pct * 100) + "%");

            int color;
            if      (pct >= 0.75f) color = android.graphics.Color.parseColor("#4CAF50");
            else if (pct >= 0.50f) color = android.graphics.Color.parseColor("#FF9800");
            else                   color = android.graphics.Color.parseColor("#F44336");

            h.tvPercent.setTextColor(color);
            h.progressMatch.setProgressTintList(
                    android.content.res.ColorStateList.valueOf(color));
            h.progressMatch.setProgress(Math.round(pct * 100));

            if (m.photoUrls != null && !m.photoUrls.isEmpty()) {
                Glide.with(ctx)
                        .load(m.photoUrls.get(0))
                        .placeholder(R.drawable.person_placeholder)
                        .error(R.drawable.person_placeholder)
                        .centerCrop()
                        .into(h.ivPhoto);
            } else {
                h.ivPhoto.setImageResource(R.drawable.person_placeholder);
            }

            h.tvRank.setText("#" + (pos + 1));
            h.itemView.setOnClickListener(v -> listener.onClick(m));
        }

        @Override public int getItemCount() { return data.size(); }

        static class VH extends RecyclerView.ViewHolder {
            ImageView   ivPhoto;
            TextView    tvName, tvAge, tvGender, tvPercent, tvRank;
            ProgressBar progressMatch;

            VH(@NonNull View v) {
                super(v);
                ivPhoto       = v.findViewById(R.id.ivPhoto);
                tvName        = v.findViewById(R.id.tvName);
                tvAge         = v.findViewById(R.id.tvAge);
                tvGender      = v.findViewById(R.id.tvGender);
                tvPercent     = v.findViewById(R.id.tvPercent);
                tvRank        = v.findViewById(R.id.tvRank);
                progressMatch = v.findViewById(R.id.progressMatch);
            }
        }
    }
}