package com.ritesh.hoppeconnect;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.tensorflow.lite.Interpreter;

import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.appwrite.coroutines.CoroutineCallback;
import io.appwrite.models.DocumentList;
import io.appwrite.services.Databases;

public class MatchFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView emptyText;
    private com.ritesh.hoppeconnect.CaseAdapter adapter;
    private List<CaseModel> masterList = new ArrayList<>();

    private Databases databases;
    private Interpreter tflite;

    private static final String DATABASE_ID = "69a559b30025d6fa1396";
    private static final String COLLECTION_ID = "reports";
    private static final int INPUT_SIZE = 112;
    private static final int EMBEDDING_SIZE = 192;

    public MatchFragment() { }

    public static MatchFragment newInstance() {
        return new MatchFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_missed, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        recyclerView = view.findViewById(R.id.missedRecyclerView);
        emptyText = view.findViewById(R.id.emptyText);

        adapter = new com.ritesh.hoppeconnect.CaseAdapter(requireContext(), masterList);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        AppwriteService.INSTANCE.init(requireContext());
        databases = AppwriteService.INSTANCE.getDatabases();

        loadModel();
        fetchReportsFromAppwrite();
    }

    private void loadModel() {
        try {
            InputStream is = requireContext().getAssets().open("mobile_face_net.tflite");
            byte[] model = new byte[is.available()];
            is.read(model);
            ByteBuffer buffer = ByteBuffer.allocateDirect(model.length);
            buffer.order(ByteOrder.nativeOrder());
            buffer.put(model);
            tflite = new Interpreter(buffer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void fetchReportsFromAppwrite() {
        if (databases == null) return;

        try {
            databases.listDocuments(
                    DATABASE_ID,
                    COLLECTION_ID,
                    new ArrayList<>(),
                    new CoroutineCallback<DocumentList<Map<String, Object>>>((result, error) -> {
                        if (getActivity() == null) return;

                        if (error != null) {
                            error.printStackTrace();
                            return;
                        }

                        try {
                            masterList.clear();
                            List<io.appwrite.models.Document<Map<String, Object>>> docs = result.getDocuments();

                            for (io.appwrite.models.Document<Map<String, Object>> doc : docs) {
                                Map<String, Object> data = doc.getData();

                                CaseModel cm = new CaseModel();
                                cm.setId(doc.getId());
                                cm.setName(data.get("name") != null ? data.get("name").toString() : "");
                                cm.setDescription(data.get("description") != null ? data.get("description").toString() : "");
                                cm.setAge(data.get("age") != null ? data.get("age").toString() : "");
                                cm.setGender(data.get("gender") != null ? data.get("gender").toString() : "");
                                cm.setMissingSince(data.get("missingSince") != null ? data.get("missingSince").toString() : "");
                                cm.setContact(data.get("contact") != null ? data.get("contact").toString() : "");

                                Object photosObj = data.get("photoUrls");
                                if (photosObj instanceof List) {
                                    List<?> pL = (List<?>) photosObj;
                                    List<String> urls = new ArrayList<>();
                                    for (Object u : pL) {
                                        urls.add(u.toString() + "?project=" + AppwriteService.PROJECT_ID);
                                    }
                                    cm.setPhotoUrls(urls);
                                }

                                masterList.add(cm);
                            }

                            requireActivity().runOnUiThread(() -> {
                                adapter.notifyDataSetChanged();
                                if (emptyText != null) {
                                    emptyText.setVisibility(masterList.isEmpty() ? View.VISIBLE : View.GONE);
                                }
                            });

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    })
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private float[] getEmbedding(Bitmap bitmap) {
        Bitmap resized = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true);

        ByteBuffer input = ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3 * 4);
        input.order(ByteOrder.nativeOrder());

        for (int y = 0; y < INPUT_SIZE; y++) {
            for (int x = 0; x < INPUT_SIZE; x++) {
                int pixel = resized.getPixel(x, y);
                input.putFloat(((pixel >> 16) & 0xFF) / 255f);
                input.putFloat(((pixel >> 8) & 0xFF) / 255f);
                input.putFloat((pixel & 0xFF) / 255f);
            }
        }

        float[][] output = new float[1][EMBEDDING_SIZE];
        tflite.run(input, output);
        return output[0];
    }

    private float cosineSimilarity(float[] a, float[] b) {
        float dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return (float) (dot / (Math.sqrt(normA) * Math.sqrt(normB)));
    }

    public void matchWithDatabase(Bitmap capturedFace) {
        new Thread(() -> {
            try {
                float[] capturedEmbedding = getEmbedding(capturedFace);

                float bestScore = 0;
                CaseModel bestMatch = null;

                for (CaseModel model : masterList) {
                    if (model.getPhotoUrls() == null || model.getPhotoUrls().isEmpty()) continue;

                    String imageUrl = model.getPhotoUrls().get(0);
                    Bitmap dbBitmap = BitmapFactory.decodeStream(new URL(imageUrl).openStream());
                    float[] dbEmbedding = getEmbedding(dbBitmap);
                    float similarity = cosineSimilarity(capturedEmbedding, dbEmbedding);

                    if (similarity > bestScore) {
                        bestScore = similarity;
                        bestMatch = model;
                    }
                }

                if (bestScore > 0.6 && bestMatch != null) {
                    CaseModel finalMatch = bestMatch;
                    requireActivity().runOnUiThread(() -> {
                        masterList.clear();
                        masterList.add(finalMatch);
                        adapter.notifyDataSetChanged();
                        if (emptyText != null) emptyText.setVisibility(View.GONE);
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}