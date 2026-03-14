package com.ritesh.hoppeconnect;

public interface SearchableFragment {
    /**
     * Called by ExploreActivity when user types in search box.
     * Implementations should filter their list and update the UI.
     */
    void onSearch(String query);
}