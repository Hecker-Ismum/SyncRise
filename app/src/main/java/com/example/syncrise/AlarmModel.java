package com.example.syncrise;

public class AlarmModel {
    private String id;
    private String time;
    private boolean isCycleAlarm;
    private double cycleDurationHours;
    private String missionType;
    private boolean isEnabled;

    // No-argument constructor required for Firebase deserialization
    public AlarmModel() {
    }

    // Parameterized constructor
    public AlarmModel(String id, String time, boolean isCycleAlarm, double cycleDurationHours, String missionType, boolean isEnabled) {
        this.id = id;
        this.time = time;
        this.isCycleAlarm = isCycleAlarm;
        this.cycleDurationHours = cycleDurationHours;
        this.missionType = missionType;
        this.isEnabled = isEnabled;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public boolean isCycleAlarm() {
        return isCycleAlarm;
    }

    public void setCycleAlarm(boolean cycleAlarm) {
        isCycleAlarm = cycleAlarm;
    }

    public double getCycleDurationHours() {
        return cycleDurationHours;
    }

    public void setCycleDurationHours(double cycleDurationHours) {
        this.cycleDurationHours = cycleDurationHours;
    }

    public String getMissionType() {
        return missionType;
    }

    public void setMissionType(String missionType) {
        this.missionType = missionType;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }
}