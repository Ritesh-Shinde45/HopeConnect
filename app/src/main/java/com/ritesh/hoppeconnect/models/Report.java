// PATH: app/src/main/java/com/ritesh/hoppeconnect/models/Report.java
package com.ritesh.hoppeconnect.models;

import java.util.List;

/**
 * Schema for the "reports" Appwrite collection.
 * Original fields kept as-is. Admin fields added at the bottom.
 */
public class Report {

    // ── Original fields (unchanged) ───────────────────────────────────────────
    public String id;
    public String name;
    public String age;          // kept as String for backward compat
    public String location;     // maps to "lastSeenLocation" in Appwrite
    public String missingSince; // maps to "$createdAt"
    public String imageUrl;     // single image URL (legacy)

    // ── New fields required by admin module ───────────────────────────────────
    public String status;           // "pending" | "active" | "found" | "rejected"
    public String gender;
    public String ageGroup;         // "Children" | "Adults" | "Elderly"
    public String description;
    public String contactPhone;
    public double latitude;
    public double longitude;
    public String reportedBy;       // userId who filed the report
    public String approvedBy;       // adminId who approved
    public String rejectedReason;
    public List<String> photoIds;   // Storage file IDs (replaces single imageUrl)
    public String createdAt;        // ISO-8601 from $createdAt

    // ── Original constructor (kept for all existing code) ─────────────────────
    public Report(String id, String name, String age, String location,
                  String missingSince, String imageUrl) {
        this.id          = id;
        this.name        = name;
        this.age         = age;
        this.location    = location;
        this.missingSince= missingSince;
        this.imageUrl    = imageUrl;
        this.status      = "pending"; // default
    }

    // ── No-arg constructor for admin parsing ─────────────────────────────────
    public Report() {
        this.status = "pending";
    }

    // ── Getters used by admin adapters & bottom sheet ─────────────────────────

    public String getId()            { return id != null ? id : ""; }
    public String getName()          { return name != null ? name : ""; }
    public String getStatus()        { return status != null ? status : "pending"; }
    public String getGender()        { return gender != null ? gender : ""; }
    public String getAgeGroup()      { return ageGroup != null ? ageGroup : ""; }
    public String getDescription()   { return description != null ? description : ""; }
    public String getContactPhone()  { return contactPhone != null ? contactPhone : ""; }
    public String getReportedBy()    { return reportedBy != null ? reportedBy : ""; }
    public String getApprovedBy()    { return approvedBy != null ? approvedBy : ""; }
    public String getRejectedReason(){ return rejectedReason != null ? rejectedReason : ""; }
    public List<String> getPhotoIds(){ return photoIds; }
    public String getCreatedAt()     { return createdAt != null ? createdAt : ""; }
    public double getLatitude()      { return latitude; }
    public double getLongitude()     { return longitude; }

    /** Returns age as int — parses the String field safely. */
    public int getAgeInt() {
        if (age == null || age.isEmpty()) return 0;
        try { return Integer.parseInt(age.trim()); } catch (Exception e) { return 0; }
    }

    /** lastSeenLocation alias — admin code uses this name. */
    public String getLastSeenLocation() {
        return location != null ? location : "";
    }
}