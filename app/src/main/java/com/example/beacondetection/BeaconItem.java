package com.example.beacondetection;

public class BeaconItem {
    private String uuid;
    private int major;
    private int minor;
    private double distance;

    public BeaconItem(String uuid, int major, int minor, double distance) {
        this.uuid = uuid;
        this.major = major;
        this.minor = minor;
        this.distance = distance;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public int getMajor() {
        return major;
    }

    public void setMajor(int major) {
        this.major = major;
    }

    public int getMinor() {
        return minor;
    }

    public void setMinor(int minor) {
        this.minor = minor;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }
}

