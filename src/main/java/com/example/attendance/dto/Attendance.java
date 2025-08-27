package com.example.attendance.dto;

import java.time.LocalDateTime;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;

/**
 * Servlet implementation class Attendance
 */
@WebServlet("/Attendance")
public class Attendance extends HttpServlet {
	
	private String userId;
	private LocalDateTime checkInTime;
	private LocalDateTime checkOutTime;
	
	public Attendance(String userId) {
		this.userId = userId;
	}
	
	
	public String getUserId() {
		return userId;
	}
	
	public void setUserId(String userId) {
		this.userId = userId;
	}
	
	
	public LocalDateTime getCheckInTime() {
		return checkInTime;
	}
	
	public void setCheckInTime(LocalDateTime checkInTime) {
		this.checkInTime = checkInTime;
	}
	
	public LocalDateTime getCheckOutTime() {
		return checkOutTime;
	}
	
	public void setCheckOutTime(LocalDateTime checkOutTime) {
		this.checkOutTime = checkOutTime;
	}
	
	
	
}
