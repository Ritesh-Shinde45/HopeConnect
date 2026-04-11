package com.ritesh.hoppeconnect;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.appwrite.Query;
import io.appwrite.coroutines.CoroutineCallback;
import io.appwrite.models.DocumentList;

public class NotificationActivity extends AppCompatActivity {

    private static final String TAG = "NotificationActivity";

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private ImageView ivBack;

    private NotifAdapter adapter;
    private final List<NotifItem> items = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        AppwriteService.init(this);

        ivBack       = findViewById(R.id.ivBack);
        recyclerView = findViewById(R.id.notifRecyclerView);
        progressBar  = findViewById(R.id.progressBar);
        tvEmpty      = findViewById(R.id.emptyText);

        ivBack.setOnClickListener(v -> finish());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotifAdapter(items);
        recyclerView.setAdapter(adapter);

        loadRecentReports();
    }

    
    private void loadRecentReports() {
        progressBar.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                List<String> queries = new ArrayList<>();
                queries.add(Query.Companion.orderDesc("$createdAt"));
                queries.add(Query.Companion.limit(20));

                AppwriteService.getDatabases().listDocuments(
                        AppwriteService.DB_ID,
                        "reports",
                        queries,
                        new CoroutineCallback<DocumentList<Map<String, Object>>>(
                                (result, error) -> {
                                    runOnUiThread(() -> progressBar.setVisibility(View.GONE));

                                    if (error != null) {
                                        Log.e(TAG, "load error: " + error.getMessage(), error);
                                        runOnUiThread(() -> tvEmpty.setVisibility(View.VISIBLE));
                                        return;
                                    }

                                    items.clear();
                                    for (io.appwrite.models.Document<Map<String, Object>> doc
                                            : result.getDocuments()) {
                                        ReportModel rm = MissedFragment.parseDocument(
                                                doc.getId(), doc.getData());

                                        NotifItem item = new NotifItem();
                                        item.reportId  = rm.id;
                                        item.title     = "New missing person report";
                                        item.body      = rm.name + " • " +
                                                (rm.missingSince.isEmpty()
                                                        ? "Recently reported"
                                                        : "Since " + rm.missingSince);
                                        item.photoUrl  = (rm.photoUrls != null &&
                                                !rm.photoUrls.isEmpty())
                                                ? rm.photoUrls.get(0) : null;
                                        item.timestamp = rm.createdAt;
                                        items.add(item);
                                    }

                                    runOnUiThread(() -> {
                                        adapter.notifyDataSetChanged();
                                        tvEmpty.setVisibility(
                                                items.isEmpty() ? View.VISIBLE : View.GONE);
                                    });
                                })
                );

            } catch (Exception e) {
                Log.e(TAG, "exception: " + e.getMessage(), e);
                runOnUiThread(() -> progressBar.setVisibility(View.GONE));
            }
        }).start();
    }

   

    static class NotifItem {
        String reportId;
        String title;
        String body;
        String photoUrl;
        long   timestamp;
    }

   

    class NotifAdapter extends RecyclerView.Adapter<NotifAdapter.VH> {

        private final List<NotifItem> data;

        NotifAdapter(List<NotifItem> data) { this.data = data; }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_notification, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH h, int pos) {
            NotifItem item = data.get(pos);
            h.tvTitle.setText(item.title);
            h.tvBody.setText(item.body);

            if (item.timestamp > 0) {
                String time = new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                        .format(new Date(item.timestamp));
                h.tvTime.setText(time);
            } else {
                h.tvTime.setText("");
            }

            if (item.photoUrl != null) {
                Glide.with(h.ivPhoto.getContext())
                        .load(item.photoUrl)
                        .circleCrop()
                        .placeholder(R.drawable.person_placeholder)
                        .into(h.ivPhoto);
            } else {
                h.ivPhoto.setImageResource(R.drawable.person_placeholder);
            }

            h.itemView.setOnClickListener(v -> {
                if (item.reportId != null) {
                    Intent intent = new Intent(NotificationActivity.this,
                            MissedPersonDetailActivity.class);
                    intent.putExtra(MissedPersonDetailActivity.EXTRA_REPORT_ID, item.reportId);
                    startActivity(intent);
                }
            });
        }

        @Override
        public int getItemCount() { return data.size(); }

        class VH extends RecyclerView.ViewHolder {
            ImageView ivPhoto;
            TextView  tvTitle, tvBody, tvTime;
            VH(View v) {
                super(v);
                ivPhoto  = v.findViewById(R.id.ivPhoto);
                tvTitle  = v.findViewById(R.id.tvTitle);
                tvBody   = v.findViewById(R.id.tvBody);
                tvTime   = v.findViewById(R.id.tvTime);
            }
        }
    }
}