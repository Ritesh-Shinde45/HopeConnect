package com.ritesh.hoppeconnect;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

// If you are using Glide or Picasso for image loading, add the import
// import com.bumptech.glide.Glide;

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
        // Inflate the layout for a single list item
        View view = LayoutInflater.from(context).inflate(R.layout.item_case, parent, false);
        return new CaseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CaseViewHolder holder, int position) {
        // Get the data for the current position
        CaseModel model = caseList.get(position);

        // Bind the data to the views in your item layout
        holder.nameTextView.setText(model.getName());
        holder.locationTextView.setText(model.getLastSeenLocation());

        // Example for loading an image using a library like Glide or Picasso
        // Uncomment this if you have an ImageView and a URL in your model
        /*
        if (model.getPhotoUrl() != null && !model.getPhotoUrl().isEmpty()) {
            Glide.with(context)
                 .load(model.getPhotoUrl())
                 .placeholder(R.drawable.ic_placeholder) // A default placeholder image
                 .into(holder.personImageView);
        }
        */
    }

    @Override
    public int getItemCount() {
        return caseList.size();
    }

    // Method to update the list for the search filter
    public void updateList(List<CaseModel> filteredList) {
        this.caseList = filteredList;
        notifyDataSetChanged();
    }


    // The ViewHolder class holds the views for a single list item
    public static class CaseViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        TextView locationTextView;
        ImageView personImageView; // Example ImageView

        public CaseViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.person_name);
            locationTextView = itemView.findViewById(R.id.person_location);
            personImageView = itemView.findViewById(R.id.person_image); // Example ImageView
        }
    }
}
