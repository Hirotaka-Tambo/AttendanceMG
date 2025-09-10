package com.example.attendance.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.example.attendance.dao.AttendanceDAO;
import com.example.attendance.dao.UserDAO;
import com.example.attendance.dto.Attendance;
import com.example.attendance.dto.User;

@WebServlet("/attendance")
public class AttendanceServlet extends HttpServlet {
    
    private final AttendanceDAO attendanceDAO = new AttendanceDAO();  
    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User user = (User) session.getAttribute("user");
        
        if (user == null) {
            response.sendRedirect("login.jsp");
            return;
        }
        
        String successMessage = (String) session.getAttribute("successMessage");
        if (successMessage != null) {
            request.setAttribute("successMessage", successMessage);
            session.removeAttribute("successMessage");
        }
        
        String action = request.getParameter("action");
        
        if ("export_csv".equals(action) && "admin".equals(user.getRole())) {
            exportCsv(request, response);
            return;
        }
        
        if ("admin".equals(user.getRole())) {
            String filterUserId = request.getParameter("filterUserId");
            String startDateStr = request.getParameter("startDate");
            String endDateStr = request.getParameter("endDate");
            LocalDate startDate = null;
            LocalDate endDate = null;
            
            try {
                if (startDateStr != null && !startDateStr.isEmpty()) {
                    startDate = LocalDate.parse(startDateStr);
                }
                if (endDateStr != null && !endDateStr.isEmpty()) {
                    endDate = LocalDate.parse(endDateStr);
                }
            } catch (DateTimeParseException e) {
                request.setAttribute("errorMessage", "日付の形式が不正です。");
            }
            
            List<Attendance> allRecords = attendanceDAO.findFilteredRecords(filterUserId, startDate, endDate);
            request.setAttribute("allAttendanceRecords", allRecords);
            
            // 労働時間と出勤日数の集計は、DAOでデータベースから取得するよう修正済みのため、JSPに渡すだけで良い
            // ここでChronoUnitを使うのは非効率的で不正確
            // DAOで取得したデータをそのまま使用
            request.setAttribute("totalHoursByUser", attendanceDAO.getMonthlyWorkingHours(filterUserId));
            request.setAttribute("monthlyWorkingHours", attendanceDAO.getMonthlyWorkingHours(filterUserId));
            request.setAttribute("monthlyCheckInCounts", attendanceDAO.getMonthlyCheckInCounts(filterUserId));
            
            request.setAttribute("userList", userDAO.getAllUsers());
            
            RequestDispatcher rd = request.getRequestDispatcher("/jsp/admin_menu.jsp");
            rd.forward(request, response);
            
        } else {
            request.setAttribute("attendanceRecords", attendanceDAO.findByUserId(user.getUsername()));
            RequestDispatcher rd = request.getRequestDispatcher("/jsp/employee_menu.jsp");
            rd.forward(request, response);
        }
    }        

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User user = (User) session.getAttribute("user");
        
        if (user == null) {
            response.sendRedirect("login.jsp");
            return;
        }
        
        String action = request.getParameter("action");
        String targetUserId = request.getParameter("userId");
        
        try {
            if ("check_in".equals(action)) {
                attendanceDAO.checkIn(user.getUsername());
                session.setAttribute("successMessage", "出勤を記録しました");
            } else if ("check_out".equals(action)) {
                attendanceDAO.checkOut(user.getUsername());
                session.setAttribute("successMessage", "退勤を記録しました");
            } else if ("add_manual".equals(action) && "admin".equals(user.getRole())) {
                LocalDateTime checkIn = LocalDateTime.parse(request.getParameter("checkInTime"));
                LocalDateTime checkOut = request.getParameter("checkOutTime") != null && !request.getParameter("checkOutTime").isEmpty() ? LocalDateTime.parse(request.getParameter("checkOutTime")) : null;
                attendanceDAO.addManualAttendance(targetUserId, checkIn, checkOut);
                session.setAttribute("successMessage", "勤怠記録を手動で追加しました");
            } else if ("update_manual".equals(action) && "admin".equals(user.getRole())) {
                LocalDateTime oldCheckIn = LocalDateTime.parse(request.getParameter("oldCheckInTime"));
                LocalDateTime oldCheckOut = request.getParameter("oldCheckOutTime") != null && !request.getParameter("oldCheckOutTime").isEmpty() ? LocalDateTime.parse(request.getParameter("oldCheckOutTime")) : null; 
                LocalDateTime newCheckIn = LocalDateTime.parse(request.getParameter("newCheckInTime"));
                LocalDateTime newCheckOut = request.getParameter("newCheckOutTime") != null && !request.getParameter("newCheckOutTime").isEmpty() ? LocalDateTime.parse(request.getParameter("newCheckOutTime")) : null;
                
                if (attendanceDAO.updateManualAttendance(targetUserId, oldCheckIn, oldCheckOut, newCheckIn, newCheckOut)) {    
                    session.setAttribute("successMessage", "勤怠記録を手動で更新しました");
                } else {
                    session.setAttribute("errorMessage", "勤怠記録の更新に失敗しました");
                }
            } else if ("delete_manual".equals(action) && "admin".equals(user.getRole())) {
                LocalDateTime checkIn = LocalDateTime.parse(request.getParameter("checkInTime"));
                LocalDateTime checkOut = request.getParameter("checkOutTime") != null && !request.getParameter("checkOutTime").isEmpty() ? LocalDateTime.parse(request.getParameter("checkOutTime")) : null;
                
                if (attendanceDAO.deleteManualAttendance(targetUserId, checkIn, checkOut)) {
                    session.setAttribute("successMessage", "勤怠記録を削除しました");
                } else {
                    session.setAttribute("errorMessage", "勤怠記録の削除に失敗しました");
                }    
            }
        } catch (DateTimeParseException e) {
            session.setAttribute("errorMessage", "日付/時刻の形式が不正です。");
        } catch (Exception e) {
            session.setAttribute("errorMessage", "操作中にエラーが発生しました: " + e.getMessage());
        }
        
        if ("admin".equals(user.getRole())) {
            response.sendRedirect(request.getContextPath() + "/attendance?action=filter");
        } else {
            response.sendRedirect(request.getContextPath() + "/attendance");
        }
    }
        
    private void exportCsv(HttpServletRequest request, HttpServletResponse response) throws IOException {
        
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename=\"attendance_records.csv\"");
        
        PrintWriter writer = response.getWriter();
        writer.append("User ID,Check-in Time,Check-out Time\n");
        
        String filterUserId = request.getParameter("filterUserId");
        String startDatestr = request.getParameter("startDate");
        String endDatestr = request.getParameter("endDate");
        LocalDate startDate = null;
        LocalDate endDate = null;
        
        try {
            if (startDatestr != null && !startDatestr.isEmpty()) {
                startDate = LocalDate.parse(startDatestr);
            }
            if (endDatestr != null && !endDatestr.isEmpty()) {
                endDate = LocalDate.parse(endDatestr);
            }    
        } catch (DateTimeParseException e) {
            System.err.println("Invalid date format for CSV export: " + e.getMessage());
        }
        
        List<Attendance> records = attendanceDAO.findFilteredRecords(filterUserId, startDate, endDate);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        for (Attendance record : records) {
            writer.append(String.format("%s,%s,%s%n",
                record.getUserId(),
                record.getCheckInTime() != null ? record.getCheckInTime().format(formatter) : "",
                record.getCheckOutTime() != null ? record.getCheckOutTime().format(formatter) : ""));
        }
        writer.flush();
    }
}