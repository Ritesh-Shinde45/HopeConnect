
package com.ritesh.hoppeconnect.models;

import java.util.List;


public class Report {

   
    public String id;
    public String name;
    public String age;         
    public String location;    
    public String missingSince;
    public String imageUrl;    

   
    public String status;          
    public String gender;
    public String ageGroup;        
    public String description;
    public String contactPhone;
    public double latitude;
    public double longitude;
    public String reportedBy;      
    public String approvedBy;      
    public String rejectedReason;
    public List<String> photoIds;  
    public String createdAt;       

   
    public Report(String id, String name, String age, String location,
                  String missingSince, String imageUrl) {
        this.id          = id;
        this.name        = name;
        this.age         = age;
        this.location    = location;
        this.missingSince= missingSince;
        this.imageUrl    = imageUrl;
        this.status      = "pending";
    }

   
    public Report() {
        this.status = "pending";
    }

   

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

    
    public int getAgeInt() {
        if (age == null || age.isEmpty()) return 0;
        try { return Integer.parseInt(age.trim()); } catch (Exception e) { return 0; }
    }

    
    public String getLastSeenLocation() {
        return location != null ? location : "";
    }
}