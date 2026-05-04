package com.ritesh.hoppeconnect;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class PendingReportAdapter extends RecyclerView.Adapter<PendingReportAdapter.VH> {

    public interface Listener {
        void onApprove(AdminReportModel r, int pos);
        void onFlagFake(AdminReportModel r, int pos);
        void onClick(AdminReportModel r);
    }

    private final Context ctx;
    private final List<AdminReportModel> items;
    private final Listener listener;

    public PendingReportAdapter(Context ctx, List<AdminReportModel> items, Listener listener) {
        this.ctx      = ctx;
        this.items    = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
        return new VH(LayoutInflater.from(ctx)
                .inflate(R.layout.item_admin_pending_report, p, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        AdminReportModel r = items.get(pos);
        h.tvName.setText(r.name);
        h.tvAge.setText(r.age + " yrs");
        h.tvLocation.setText(r.location);
        h.tvDate.setText(r.createdAt);
        h.tvReporter.setText("By: " + r.reporterName);

        if (!r.photoFileId.isEmpty()) {
            String url = AppwriteService.ENDPOINT + "/storage/buckets/"
                    + AppwriteService.USERS_BUCKET_ID + "/files/"
                    + r.photoFileId + "/view?project=" + AppwriteService.PROJECT_ID;
            Glide.with(ctx).load(url)
                    .placeholder(R.drawable.profile_placeholder)
                    .into(h.ivPhoto);
        }

        h.btnApprove.setOnClickListener(v -> listener.onApprove(r, h.getAdapterPosition()));
        h.btnFake.setOnClickListener(v -> listener.onFlagFake(r, h.getAdapterPosition()));
        h.itemView.setOnClickListener(v -> listener.onClick(r));
    }

    @Override
    public int getItemCount() { return items.size(); }

    public void remove(int pos) {
        items.remove(pos);
        notifyItemRemoved(pos);
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivPhoto;
        TextView tvName, tvAge, tvLocation, tvDate, tvReporter;
        Button btnApprove, btnFake;

        VH(View v) {
            super(v);
            ivPhoto    = v.findViewById(R.id.iv_report_photo);
            tvName     = v.findViewById(R.id.tv_report_name);
            tvAge      = v.findViewById(R.id.tv_report_age);
            tvLocation = v.findViewById(R.id.tv_report_location);
            tvDate     = v.findViewById(R.id.tv_report_date);
            tvReporter = v.findViewById(R.id.tv_report_reporter);
            btnApprove = v.findViewById(R.id.btn_approve_report);
            btnFake    = v.findViewById(R.id.btn_fake_report);
        }
    }
}