package com.ritesh.hoppeconnect;

import java.util.HashMap;
import java.util.Map;

/**
 * Lightweight in-memory cache to pass ReportModel between activities
 * without serialization, avoiding the need for Parcelable/JSON.
 */
public class ReportModelCache {

    private static final Map<String, ReportModel> cache = new HashMap<>();

    public static void put(ReportModel model) {
        if (model != null && model.id != null) {
            cache.put(model.id, model);
        }
    }

    public static ReportModel get(String id) {
        return id != null ? cache.get(id) : null;
    }

    public static void remove(String id) {
        if (id != null) cache.remove(id);
    }
}