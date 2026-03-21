package com.ritesh.hoppeconnect;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.VH> {

    public interface OnClickListener {
        void onClick(NotificationModel n);
    }

    private final Context ctx;
    private final List<NotificationModel> items;
    private final OnClickListener listener;

    public NotificationAdapter(Context ctx, List<NotificationModel> items,
                               OnClickListener listener) {
        this.ctx      = ctx;
        this.items    = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(ctx)
                .inflate(R.layout.item_notification, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        NotificationModel n = items.get(pos);

        h.tvTitle.setText(n.title);
        h.tvBody.setText(n.body);
        h.tvTime.setText(n.time);
        h.dot.setVisibility(n.isRead ? View.INVISIBLE : View.VISIBLE);
        h.itemView.setAlpha(n.isRead ? 0.7f : 1.0f);

        // Badge color + label by type
        switch (n.type) {
            case NotificationModel.TYPE_ANNOUNCEMENT:
                h.tvBadge.setText("ANNOUNCEMENT");
                h.tvBadge.setBackgroundColor(Color.parseColor("#D97706"));
                break;
            case NotificationModel.TYPE_MY_REPORT:
                h.tvBadge.setText("YOUR REPORT");
                h.tvBadge.setBackgroundColor(Color.parseColor("#16A34A"));
                break;
            default: // TYPE_REPORT
                h.tvBadge.setText("REPORT");
                h.tvBadge.setBackgroundColor(Color.parseColor("#2563EB"));
                break;
        }

        // Photo
        if (n.photoUrl != null && !n.photoUrl.isEmpty()) {
            Glide.with(ctx)
                    .load(n.photoUrl)
                    .placeholder(R.drawable.person_placeholder)
                    .error(R.drawable.person_placeholder)
                    .centerCrop()
                    .into(h.ivPhoto);
        } else {
            h.ivPhoto.setImageResource(
                    n.type == NotificationModel.TYPE_ANNOUNCEMENT
                            ? R.drawable.ic_dashboard
                            : R.drawable.person_placeholder);
        }

        h.itemView.setOnClickListener(v -> {
            n.isRead = true;
            notifyItemChanged(pos);
            if (listener != null) listener.onClick(n);
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivPhoto;
        TextView tvTitle, tvBody, tvTime, tvBadge;
        View dot;

        VH(View v) {
            super(v);
            ivPhoto  = v.findViewById(R.id.ivPhoto);
            tvTitle  = v.findViewById(R.id.tvTitle);
            tvBody   = v.findViewById(R.id.tvBody);
            tvTime   = v.findViewById(R.id.tvTime);
            tvBadge  = v.findViewById(R.id.tvBadge);
            dot      = v.findViewById(R.id.dotIndicator);
        }
    }
}