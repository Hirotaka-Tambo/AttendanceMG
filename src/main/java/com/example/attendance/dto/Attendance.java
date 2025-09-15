package com.example.attendance.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Attendance {
    private int id;
    private String userId;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private LocalDateTime createdAt;

    // デフォルトコンストラクタ（通常は不要ですが、フレームワークによっては必要になる場合がある）
    public Attendance() {
    }

    // 勤怠記録を作成・取得するための主要なコンストラクタ
    public Attendance(String userId, LocalDateTime checkInTime, LocalDateTime checkOutTime) {
        this.userId = userId;
        this.checkInTime = checkInTime;
        this.checkOutTime = checkOutTime;
    }
    
    // 全てのフィールドを含むコンストラクタ（データベースから全データを取得する場合など）
    public Attendance(int id, String userId, LocalDateTime checkInTime, LocalDateTime checkOutTime, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.checkInTime = checkInTime;
        this.checkOutTime = checkOutTime;
        this.createdAt = createdAt;
    }
    
    // Getterメソッド
    public int getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public LocalDateTime getCheckInTime() {
        return checkInTime;
    }

    public LocalDateTime getCheckOutTime() {
        return checkOutTime;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // Setterメソッド
    public void setId(int id) {
        this.id = id;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setCheckInTime(LocalDateTime checkInTime) {
        this.checkInTime = checkInTime;
    }

    public void setCheckOutTime(LocalDateTime checkOutTime) {
        this.checkOutTime = checkOutTime;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    //グラフ用の取得
    
    public String getCheckInTimeStr() {
        if (this.checkInTime != null) {
            return this.checkInTime.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
        }
        return "-";
    }

    public String getCheckOutTimeStr() {
        if (this.checkOutTime != null) {
            return this.checkOutTime.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
        }
        return "-";
    }
}