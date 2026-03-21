package com.ritesh.hoppeconnect;

import java.util.Map;

public class AdminReportModel {
    public String id, name, age, gender, location, status;
    public String reportedBy, reporterName, contact, photoFileId, description, createdAt;

    public AdminReportModel(String id, String name, String age, String gender,
                            String location, String status, String reportedBy,
                            String reporterName, String contact,
                            String photoFileId, String description, String createdAt) {
        this.id           = id;
        this.name         = name         != null ? name         : "Unknown";
        this.age          = age          != null ? age          : "—";
        this.gender       = gender       != null ? gender       : "—";
        this.location     = location     != null ? location     : "—";
        this.status       = status       != null ? status       : "pending";
        this.reportedBy   = reportedBy   != null ? reportedBy   : "";
        this.reporterName = reporterName != null ? reporterName : "Unknown";
        this.contact      = contact      != null ? contact      : "—";
        this.photoFileId  = photoFileId  != null ? photoFileId  : "";
        this.description  = description  != null ? description  : "";
        this.createdAt    = createdAt    != null ? createdAt    : "";
    }

    @SuppressWarnings("unchecked")
    public static AdminReportModel fromDocument(io.appwrite.models.Document<?> doc) {
        Map<String, Object> d = (Map<String, Object>) doc.getData();
        String iso  = doc.getCreatedAt();
        String date = (iso != null && iso.length() >= 10) ? iso.substring(0, 10) : "—";
        return new AdminReportModel(
                doc.getId(),
                s(d, "name"),         s(d, "age"),       s(d, "gender"),
                s(d, "location"),     s(d, "status", "pending"),
                s(d, "reportedBy"),   s(d, "reporterName"),
                s(d, "contact"),      s(d, "photoFileId"),
                s(d, "description"),  date
        );
    }

    private static String s(Map<String, Object> m, String key) {
        return s(m, key, "");
    }

    private static String s(Map<String, Object> m, String key, String def) {
        Object v = m.get(key);
        return v != null ? v.toString() : def;
    }
}