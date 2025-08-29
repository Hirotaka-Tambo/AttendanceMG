package com.example.attendance.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.apache.naming.java.javaURLContextFactory;

import com.example.attendance.dao.AttendanceDAO;
import com.example.attendance.dto.Attendance;
import com.example.attendance.dto.User;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servlet implementation class AttendanceServlet
 */
@WebServlet("/AttendanceServlet")
public class AttendanceServlet extends HttpServlet {
	
	private final AttendanceDAO attendanceDAO = new AttendanceDAO();  

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	
		String action = request.getParameter("action");
		HttpSession session = request.getSession(false);
		User user = (User) session.getAttribute("user");
		
		if(user == null) {
			response.sendRedirect("login.jsp");
			return;
		}
		
		String message = (String) session.getAttribute("successMessage");
		if(message != null) {
			request.setAttribute("successMessage", message);
			session.removeAttribute("successMessage");
		}
		
		if("export_csv".equals(action)&& "admin".equals(user.getRole())) {
			exportCsv(request,response);
		}else if("filter".equals(action) && "admin".equals(user.getRole())) {
			String filterUserId = request.getParameter("filterUserId");
			String  startDateStr = request.getParameter("startDate");
			String endDateStr = request.getParameter("endDate");
			LocalDate startDate = null;
			LocalDate endDate = null;
			
			try {
				if(startDateStr != null && !startDateStr.isEmpty()) {
					startDate = LocalDate.parse(startDateStr);
				}
				if(endDateStr != null && !endDateStr.isEmpty()) {
					endDate = LocalDate.parse(endDateStr);
				}
				
			} catch (DateTimeParseException e) {
				// TODO: handle exception
				request.setAttribute("errorMessage", "日付の形式が不正です。");
			}
			
			List<Attendance> filteredRecords = attendanceDAO.findFilteredRecords(filterUserId, startDate, endDate);
				request.setAttribute("allAttendanceRecords", filteredRecords);
				
			Map<String, Long> totalHoursByUserMap = filteredRecords.stream().collect(Collectors.groupingBy(Attendance::getUserId,Collectors.summingLong(att->{
					if(attendanceDAO.getCheckInTime() != null && attendanceDAO.getCheckOutTime() != null) {
						return java.Time.Temporal.ChronoUnit.HOURS.between(att.getCheckInTime(), att.getCheckOutTime());
					}
					return 0L;
				})));
			request.setAttribute("totalHoursByUser", totalHoursByUser);
			
			request.setAttribute("monthlyWorkingHours",attendanceDAO.getMonthlyWorkingHours(filterUserId));
			request.setAttribute("monthlyCheckInCounts", attendanceDAO.getMonthlyCheckInCounts(filterUserId));
			
			RequestDispatcher rd = request.getRequestDispatcher("/jsp/admin_menu.jsp");
			rd.forward(request, response);
			
			}else {
				if("admin".equals(user.getRole())) {
					request.setAttribute("allAtendanceRecords", attendanceDAO.findAll());
					Map<String, Long> totalHoursByUser = attendanceDAO.findAll().stream().collect(Collectors.groupingBy(Attendance::getUserId,Collectors.summingLong(att ->{
						if(attendanceDAO.getCheckInTime() != null && attendanceDAO.getCheckOutTime() != null) {
							return java.time.temporal.ChronoUnit.HOURS.between(att.getCheckInTime(), att.getCheckOutTIme());
						}
						return 0L;
									
					})));
					
					request.setAttribute("totalHoursByUser", totalHoursByUser);
					request.setAttribute("monthlyWorkingHours", attendanceDAO.getMonthlyWorkingHours(null));
					request.setAttribute("monthlyCheckInCounts", attendanceDAO.getMonthlyCheckInCounts(null));
					
					RequestDispatcher rd = request.getRequestDispatcher("/jsp/admin_menu.jsp");
					rd.forward(request, response);
				}else {
					request.setAttribute("attendanceRecords", attendanceDAO.findByUserId(user.getUsername()));
					RequestDispatcher rd = request.getRequestDispatcher("/jsp/employee_menu.jsp");
					rd.forward(request, response);
				}
			}
	}

	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		HttpSession session = request.getSession(false);
		User user = (User) session.getAttribute("user");
		
		if(user == null) {
			response.sendRedirect("login.jsp");
			return;
		}
		
		String action = request.getParameter("action");
		
		if("check_in".equals(action)) {
			attendanceDAO.checkIn(user.getUsername());
			session.setAttribute("successMessage", "出勤を記録しました");
		}else if("check_out".equals(action)) {
			attendanceDAO.checkOut(user.getUsername());
			session.setAttribute("successMessage", "退勤を記録しました");
		}else if("add_manual".equals(action) && "adimin".equals(user.getRole())) {
			String userId = request.getParameter("userId");
			String checkInStr = request.getParameter("checkInTime");
			String checkOutStr = request.getParameter("checkOutTime");
			
			try {
				LocalDateTime checkIn = LocalDateTime.parse(checkInStr);
				LocalDateTime checkOut = checkOutStr != null && !checkOutStr.isEmpty()? LocalDateTime.parse(checkOutStr):null;
				attendanceDAO.addManualAttendance(userId, checkIn, checkOut);
				session.setAttribute("successMessage", "勤怠記録を手動で追加しました");
			} catch (DateTimeParseException e) {
				// TODO: handle exception
				session.setAttribute("errorMessage", "日付/時刻の形式が不正です。");
			}
		}else if("update_manual".equals(action)&& "admin".equals(user.getRole())) {
			String userId = request.getParameter("userId");
			
			LocalDateTime oldCheckIn = LocalDateTime.parse(request.getParameter("oldcheckInTime"));
			LocalDateTime oldCheckOut = request.getParameter("oldCheckOutTime") != null && !request.getParameter("oldCheckOutTime").isEmpty()? LocalDateTime.parse(request.getParameter("oldCheckOutTime")): null; 
			
			LocalDateTime newCheckIn = LocalDateTime.parse(request.getParameter("newCheckIntTime"));
			LocalDateTime newCheckOut = request.getParameter("newCheckOutTime") != null && !request.getParameter("newCheckOutTime").isEmpty()? LocalDateTime.parse(request.getParameter("newCheckOutTime")) : null;
			
			if(attendanceDAO.updateManualAttendance(userId, oldCheckIn, oldCheckOut, newCheckIn, newCheckOut)) {	
				session.setAttribute("successMessage", "勤怠記録を手動で更新しました");
			}else {
				session.setAttribute("errorMessage", "勤怠記録の更新に失敗しました");
			}
		}else if("delete_manual".equals(action)&& "admin".equals(user.getRole())) {
			String userId = request.getParameter("userId");
			LocalDateTime checkIn = LocalDateTime.parse(request.getParameter("checkInTime"));
			LocalDateTime checkOut = request.getParameter("checkOutTime") != null && !request.getParameter("checkOutTime").isEmpty() ? LocalDateTime.parse(request.getParameter("checkOutTime")) : null;
			
			if(attendanceDAO.deleteManualAttendance(userId, checkIn, checkOut)) {
				session.setAttribute("successMessage", "勤怠記録を削除しました");
			}else {
				session.setAttribute("errorMessage", "勤怠記録の削除に失敗しました");
			}	
		}
		
		if("admin".equals(user.getRole())) {
			response.sendRedirect("attendance?action=filter&filterUserId="+
		(request.getParameter("filterUserId") != null ? request.getParameter("filterUserId") : "")+
		"&startDate=" + (request.getParameter("startDate") != null ? request.getParameter("startDate") : ""+)
		"&endDate=" +(request.getParameter("endDate" != null ? request.getParameter("endDate") : ""));
		}else {
			response.sendRedirect("attendance");
		}
	}
	
	private void exportCsv(HttpServletRequest req, HttpServletResponse resp)throws IOException {
		
		resp.setContentType("text/csv: charset=UTF-8");
		resp.setHeader("Content-Disposition", "attachment;filename=\"attendance_recordscsv\"");
		
		PrintWriter writer = resp.getWriter();
		writer.append("User ID,Check-in Time,Check-out Time\n");
		
		String filterUserId = req.getParameter("filterUserId");
		String startDatestr = req.getParameter("startDate");
		String endDatestr = req.getParameter("endDate");
		LocalDate startDate = null;
		LocalDate endDate = null;
		
		try {
			if(startDatestr != null && !startDatestr.isEmpty()) {
				startDate = LocalDate.parse(startDatestr);
			}
			if(endDatestr != null && !endDatestr.isEmpty()) {
				endDate = LocalDate.parse(endDatestr);
			}	
		} catch (DateTimeParseException e) {
			// TODO: handle exception
			System.err.println("Invalid date format for CSV export:" + e.getMessage());
		}
		
		List<Attendance> records = attendanceDAO.findFilteredRecords(filterUserId, startDate, endDate);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM--dd HH:mm:ss");
		
		for(Attendance record:records) {
			writer.append(String.format("%s,%s,%s%n",record.getUserId(),record.getCheckInTime()!=null ? record.getCheckInTime().format(formatter):"",
			record.getCheckOutTime() != null ? record.getCheckOutTime().format(formatter):""));
		}
		writer.flush();
	}
		
	

}
