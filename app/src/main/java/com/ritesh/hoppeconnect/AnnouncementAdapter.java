package com.ritesh.hoppeconnect;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AnnouncementAdapter extends RecyclerView.Adapter<AnnouncementAdapter.VH> {

    private final List<AnnouncementItem> items;

    public AnnouncementAdapter(List<AnnouncementItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_announcements, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        AnnouncementItem item = items.get(pos);
        h.tvTitle.setText(item.title);
        h.tvAudience.setText("To: " + item.audience);
        h.tvDate.setText(item.sentAt);
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvAudience, tvDate;

        VH(@NonNull View v) {
            super(v);
            tvTitle    = v.findViewById(R.id.tv_ann_title);
            tvAudience = v.findViewById(R.id.tv_ann_audience);
            tvDate     = v.findViewById(R.id.tv_ann_date);
        }
    }
}