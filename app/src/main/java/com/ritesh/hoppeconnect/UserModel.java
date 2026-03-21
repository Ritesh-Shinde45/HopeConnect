package com.ritesh.hoppeconnect;

import java.util.Map;

public class UserModel {
    public String id, name, email, username, status, joinDate;
    public int reportCount;

    public UserModel(String id, String name, String email,
                     String username, String status, String joinDate, int reportCount) {
        this.id = id;
        this.name = name != null ? name : "Unknown";
        this.email = email != null ? email : "";
        this.username = username != null ? username : "";
        this.status = status != null ? status : "active";
        this.joinDate = joinDate != null ? joinDate : "";
        this.reportCount = reportCount;
    }

    @SuppressWarnings("unchecked")
    public static UserModel fromDocument(io.appwrite.models.Document<?> doc) {
        Map<String, Object> d = (Map<String, Object>) doc.getData();
        String iso = doc.getCreatedAt();
        String date = (iso != null && iso.length() >= 10) ? iso.substring(0, 10) : "—";
        return new UserModel(
                doc.getId(),
                s(d, "name"), s(d, "email"), s(d, "username"),
                s(d, "status", "active"), date, 0
        );
    }

    private static String s(Map<String, Object> m, String k) { return s(m, k, ""); }
    private static String s(Map<String, Object> m, String k, String def) {
        Object v = m.get(k); return v != null ? v.toString() : def;
    }
}