package com.ritesh.hoppeconnect;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import java.util.List;

public class CaseAdapter extends RecyclerView.Adapter<CaseAdapter.CaseViewHolder> {

    private Context context;
    private List<CaseModel> caseList;

    public CaseAdapter(Context context, List<CaseModel> caseList) {
        this.context = context;
        this.caseList = caseList;
    }

    @NonNull
    @Override
    public CaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
       
        View view = LayoutInflater.from(context).inflate(R.layout.item_case, parent, false);
        return new CaseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CaseViewHolder holder, int position) {
       
        CaseModel model = caseList.get(position);

       
        holder.nameTextView.setText(model.getName());
        holder.locationTextView.setText(model.getLastSeenLocation());

       
       
        
    }

    @Override
    public int getItemCount() {
        return caseList.size();
    }

   
    public void updateList(List<CaseModel> filteredList) {
        this.caseList = filteredList;
        notifyDataSetChanged();
    }


   
    public static class CaseViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        TextView locationTextView;
        ImageView personImageView;

        public CaseViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.person_name);
            locationTextView = itemView.findViewById(R.id.person_location);
            personImageView = itemView.findViewById(R.id.person_image);
        }
    }
}
