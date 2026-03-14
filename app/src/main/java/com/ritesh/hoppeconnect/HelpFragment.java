package com.ritesh.hoppeconnect;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class HelpFragment extends Fragment implements SearchableFragment {

    private RecyclerView recyclerView;
    private TextView emptyText;
    private com.ritesh.hoppeconnect.CaseAdapter adapter;
    private List<CaseModel> masterList;
    private List<CaseModel> displayedList;
    private String currentSearchQuery = "";

    public HelpFragment() { }

    public static HelpFragment newInstance() {
        return new HelpFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_missed, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView = view.findViewById(R.id.missedRecyclerView);
        emptyText = view.findViewById(R.id.emptyText);

        masterList = new ArrayList<>();
        displayedList = new ArrayList<>();
        adapter = new com.ritesh.hoppeconnect.CaseAdapter(requireContext(), displayedList);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        subscribeHelpReports();
    }

    private void subscribeHelpReports() {
        // TODO: Implement Appwrite database listing logic
        applySearchFilter(currentSearchQuery);
    }

    @Override
    public void onSearch(String query) {
        currentSearchQuery = query == null ? "" : query.trim();
        applySearchFilter(currentSearchQuery);
    }

    private void applySearchFilter(String q) {
        displayedList.clear();
        if (TextUtils.isEmpty(q)) {
            displayedList.addAll(masterList);
        } else {
            String lower = q.toLowerCase();
            for (CaseModel item : masterList) {
                boolean matched = false;
                if (item.getName() != null && item.getName().toLowerCase().contains(lower)) matched = true;
                if (!matched && item.getCity() != null && item.getCity().toLowerCase().contains(lower)) matched = true;
                if (matched) displayedList.add(item);
            }
        }
        adapter.updateList(displayedList);
        emptyText.setVisibility(displayedList.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
