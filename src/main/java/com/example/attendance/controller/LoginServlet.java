package com.example.attendance.controller;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.example.attendance.dao.AttendanceDAO;
import com.example.attendance.dao.UserDAO;
import com.example.attendance.dto.User;

/**
 * Servlet implementation class LoginServlet
 */
@WebServlet("/login")
public class LoginServlet extends HttpServlet {
	
	private final UserDAO userDAO = new UserDAO();
	private final AttendanceDAO attendanceDAO = new AttendanceDAO();	

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	    String username = request.getParameter("username");
	    String password = request.getParameter("password");
	    User user = userDAO.findByUsername(username);

	    if (user != null && user.isEnabled() && userDAO.verifyPassword(username, password)) {
	        HttpSession session = request.getSession();
	        session.setAttribute("user", user);
	        session.setAttribute("successMessage", "ログインしました");

	        if ("admin".equals(user.getRole())) {
	            // 管理者の場合
	            request.setAttribute("allAtendanceRecords", attendanceDAO.findAll());

	            Map<String, Long> totalHoursByUser = attendanceDAO.findAll().stream()
	                .collect(Collectors.groupingBy(
	                    com.example.attendance.dto.Attendance::getUserId,
	                    Collectors.summingLong(att -> {
	                        if (att.getCheckInTime() != null && att.getCheckOutTime() != null) {
	                            return java.time.temporal.ChronoUnit.HOURS
	                                .between(att.getCheckInTime(), att.getCheckOutTime());
	                        }
	                        return 0L;
	                    })
	                ));

	            request.setAttribute("totalHoursByUser", totalHoursByUser);
	            request.getRequestDispatcher("/jsp/admin_menu.jsp").forward(request, response);

	        } else {
	            // 一般ユーザー用の画面にフォワード
	            request.getRequestDispatcher("/jsp/employee_menu.jsp").forward(request, response);
	        }

	    } else {
	        // ログイン失敗時
	        request.setAttribute("errorMessage", "ユーザーIDまたはパスワードが不正、もしくはアカウントが無効です");
	        request.getRequestDispatcher("/login.jsp").forward(request, response);
	    }
	}
}