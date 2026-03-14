package com.ritesh.hoppeconnect;

import java.util.List;

public class CaseModel {

    private String id;
    private String name;
    private String photoId;
    private String lastSeenLocation;
    private String city;
    private String age;
    private String gender;
    private String status;
    private String description;
    private String missingSince;
    private String contact;
    private List<String> photoUrls;

    public CaseModel() { }

    public CaseModel(String id, String name, String photoId,
                     String lastSeenLocation, String city,
                     String age, String gender, String status) {
        this.id = id;
        this.name = name;
        this.photoId = photoId;
        this.lastSeenLocation = lastSeenLocation;
        this.city = city;
        this.age = age;
        this.gender = gender;
        this.status = status;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhotoId() { return photoId; }
    public void setPhotoId(String photoId) { this.photoId = photoId; }

    public String getLastSeenLocation() { return lastSeenLocation; }
    public void setLastSeenLocation(String lastSeenLocation) { this.lastSeenLocation = lastSeenLocation; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getAge() { return age; }
    public void setAge(String age) { this.age = age; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getMissingSince() { return missingSince; }
    public void setMissingSince(String missingSince) { this.missingSince = missingSince; }

    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }

    public List<String> getPhotoUrls() { return photoUrls; }
    public void setPhotoUrls(List<String> photoUrls) { this.photoUrls = photoUrls; }
}