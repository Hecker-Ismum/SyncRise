package com.example.syncrise;

public class SleepLogModel {
    private long startTimeMillis;
    private long endTimeMillis;
    private long durationMillis;
    private int cyclesCompleted;
    private int status;

    public SleepLogModel() {}

    public SleepLogModel(long startTimeMillis, long endTimeMillis, long durationMillis, int cyclesCompleted, int status) {
        this.startTimeMillis = startTimeMillis;
        this.endTimeMillis = endTimeMillis;
        this.durationMillis = durationMillis;
        this.cyclesCompleted = cyclesCompleted;
        this.status = status;
    }

    public long getStartTimeMillis() {
        return startTimeMillis;
    }

    public void setStartTimeMillis(long startTimeMillis) {
        this.startTimeMillis = startTimeMillis;
    }

    public long getEndTimeMillis() {
        return endTimeMillis;
    }

    public void setEndTimeMillis(long endTimeMillis) {
        this.endTimeMillis = endTimeMillis;
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    public void setDurationMillis(long durationMillis) {
        this.durationMillis = durationMillis;
    }

    public int getCyclesCompleted() {
        return cyclesCompleted;
    }

    public void setCyclesCompleted(int cyclesCompleted) {
        this.cyclesCompleted = cyclesCompleted;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}