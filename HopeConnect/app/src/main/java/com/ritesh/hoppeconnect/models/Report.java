package com.ritesh.hoppeconnect.models;

import java.util.List;

public class Report {
    public String id;
    public String name;
    public String age;
    public String location;
    public String missingSince;
    public String imageUrl;

    public Report(String id, String name, String age, String location, String missingSince, String imageUrl) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.location = location;
        this.missingSince = missingSince;
        this.imageUrl = imageUrl;
    }
}