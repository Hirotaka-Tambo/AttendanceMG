package com.example.attendance.dao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;

import org.apache.el.parser.AstTrue;

import com.example.attendance.dto.Attendance;

/**
 * Servlet implementation class AttendanceDAO
 */
@WebServlet("/AttendanceDAO")
public class AttendanceDAO extends HttpServlet {
	
	private static final List<Attendance> attendanceRecords = new CopyOnWriteArrayList<>();
	
	public void checkIn(String userId) {
		Attendance attendance = new Attendance(userId);
		attendance.setCheckInTime(LocalDateTime.now());
		attendanceRecords.add(attendance);
	}
	
	public void checkOut(String userId) {
		attendanceRecords.stream()
		.filter(att ->userId.equals(att.getUserId()) && att.getCheckOutTime() ==null)
		.findFirst()
		.ifPresent(att -> att.setCheckOutTime(LocalDateTime.now()));
	}
	
	public List<Attendance>findByUserId(String userId){
		return attendanceRecords.stream()
				.filter(att -> userId.eruals(att.getUserId()))
				.collect(Collectors.toList());
	}
	
	public List<Attendance>findAll(){
		return new ArrayList<>(attendanceRecords);	
	}
	
	public List<Attendance>findFilteredRecords(String userId, LocalDate startDate, LocalDate endDate){
		return 
	}
}
