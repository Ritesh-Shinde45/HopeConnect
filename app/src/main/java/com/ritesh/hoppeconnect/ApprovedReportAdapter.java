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

public class ApprovedReportAdapter extends RecyclerView.Adapter<ApprovedReportAdapter.VH> {

    public interface Listener {
        void onMarkResolved(AdminReportModel r, int pos);
        void onRevoke(AdminReportModel r, int pos);
    }

    private final Context ctx;
    private final List<AdminReportModel> items;
    private final Listener listener;

    public ApprovedReportAdapter(Context ctx, List<AdminReportModel> items, Listener listener) {
        this.ctx      = ctx;
        this.items    = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
        return new VH(LayoutInflater.from(ctx)
                .inflate(R.layout.item_admin_approved_report, p, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        AdminReportModel r = items.get(pos);
        h.tvName.setText(r.name);
        h.tvAge.setText(r.age + " yrs");
        h.tvLocation.setText(r.location);
        h.tvDate.setText(r.createdAt);
        h.tvStatus.setText(r.status.toUpperCase());

        if (!r.photoFileId.isEmpty()) {
            String url = AppwriteService.ENDPOINT + "/storage/buckets/"
                    + AppwriteService.USERS_BUCKET_ID + "/files/"
                    + r.photoFileId + "/view?project=" + AppwriteService.PROJECT_ID;
            Glide.with(ctx).load(url)
                    .placeholder(R.drawable.profile_placeholder)
                    .into(h.ivPhoto);
        }

        h.btnResolved.setOnClickListener(v -> listener.onMarkResolved(r, h.getAdapterPosition()));
        h.btnRevoke.setOnClickListener(v -> listener.onRevoke(r, h.getAdapterPosition()));
    }

    @Override
    public int getItemCount() { return items.size(); }

    public void remove(int pos) {
        items.remove(pos);
        notifyItemRemoved(pos);
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivPhoto;
        TextView tvName, tvAge, tvLocation, tvDate, tvStatus;
        Button btnResolved, btnRevoke;

        VH(View v) {
            super(v);
            ivPhoto     = v.findViewById(R.id.iv_approved_photo);
            tvName      = v.findViewById(R.id.tv_approved_name);
            tvAge       = v.findViewById(R.id.tv_approved_age);
            tvLocation  = v.findViewById(R.id.tv_approved_location);
            tvDate      = v.findViewById(R.id.tv_approved_date);
            tvStatus    = v.findViewById(R.id.tv_approved_status);
            btnResolved = v.findViewById(R.id.btn_mark_resolved);
            btnRevoke   = v.findViewById(R.id.btn_revoke_approval);
        }
    }
}