package com.ritesh.hoppeconnect.models;

import java.util.Date;
import java.util.List;

public class MissingPerson {

   
    private String id;

    private String name;
    private String age;
    private String gender;
    private String missingSince;
    private String contactNumber;
    private String emergencyContact1;
    private String emergencyContact2;
    private String emergencyContact3;
    private String description;
    private List<String> imageUrls;
    private String reporterId;

   
    private Date createdAt;

   
    public MissingPerson() {}

    public MissingPerson(String name, String age, String gender, String missingSince,
                         String contactNumber, String emergencyContact1,
                         String emergencyContact2, String emergencyContact3,
                         String description, List<String> imageUrls,
                         String reporterId) {

        this.name = name;
        this.age = age;
        this.gender = gender;
        this.missingSince = missingSince;
        this.contactNumber = contactNumber;
        this.emergencyContact1 = emergencyContact1;
        this.emergencyContact2 = emergencyContact2;
        this.emergencyContact3 = emergencyContact3;
        this.description = description;
        this.imageUrls = imageUrls;
        this.reporterId = reporterId;
    }

   
    public String getId() { return id; }
    public String getName() { return name; }
    public String getAge() { return age; }
    public String getGender() { return gender; }
    public String getMissingSince() { return missingSince; }
    public String getContactNumber() { return contactNumber; }
    public String getEmergencyContact1() { return emergencyContact1; }
    public String getEmergencyContact2() { return emergencyContact2; }
    public String getEmergencyContact3() { return emergencyContact3; }
    public String getDescription() { return description; }
    public List<String> getImageUrls() { return imageUrls; }
    public String getReporterId() { return reporterId; }
    public Date getCreatedAt() { return createdAt; }

   
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setAge(String age) { this.age = age; }
    public void setGender(String gender) { this.gender = gender; }
    public void setMissingSince(String missingSince) { this.missingSince = missingSince; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }
    public void setEmergencyContact1(String emergencyContact1) { this.emergencyContact1 = emergencyContact1; }
    public void setEmergencyContact2(String emergencyContact2) { this.emergencyContact2 = emergencyContact2; }
    public void setEmergencyContact3(String emergencyContact3) { this.emergencyContact3 = emergencyContact3; }
    public void setDescription(String description) { this.description = description; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }
    public void setReporterId(String reporterId) { this.reporterId = reporterId; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}