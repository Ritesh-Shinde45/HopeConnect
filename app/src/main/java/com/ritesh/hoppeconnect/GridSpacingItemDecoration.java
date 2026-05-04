package com.ritesh.hoppeconnect;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {

    private final int spacing;

    public GridSpacingItemDecoration(int spacing) {
        this.spacing = spacing;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                               @NonNull RecyclerView parent,
                               @NonNull RecyclerView.State state) {

        int position = parent.getChildAdapterPosition(view);
        int column = position % 2;

        if (column == 0) {
            outRect.left = spacing;
            outRect.right = spacing / 2;
        } else {
            outRect.left = spacing / 2;
            outRect.right = spacing;
        }

        if (position >= 2) {
            outRect.top = spacing;
        } else {
            outRect.top = spacing / 2;
        }

        outRect.bottom = spacing;
    }
}
