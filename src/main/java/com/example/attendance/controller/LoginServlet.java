package com.example.attendance.controller;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.servlet.RequestDispatcher;
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
@WebServlet("/LoginServlet")
public class LoginServlet extends HttpServlet {
	
	private final UserDAO userDAO = new UserDAO();
	private final AttendanceDAO attendanceDAO = new AttendanceDAO();	

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		
		String username = request.getParameter("username");
		String password = request.getParameter("password");
		User user = userDAO.findByUsername(username);
		if(user != null && user.isEnabled()&& userDAO.verifyPassword(username, password)) {
			HttpSession session = request.getSession();
			session.setAttribute("user", user);
			session.setAttribute("successMessage", "ログインしました");
			
			if("admin".equals(username.getRole())) {
				request.setAttribute("allAtendanceRecords", attendanceDAO.findAll());
				
				Map<String, Long>totalHoursByUser = attendanceDAO.findAll().stream().collect(Collectors.groupingBy(com.example.attendance.dto.Attendance:: getUserId,Collectors.summingLong(att ->{
					if(att.getCheckInTime() != null && att.getCheckOutTime() != null) {
						return java.time.temporal.ChronoUnit.HOURS.between(att.getCheckInTime(), att.getCheckOutTime());
					}
					return 0L;
					
				})));
				
		     request.setAttribute("totalHoursByUser", totalHoursByUser);
		     RequestDispatcher rd = request.getRequestDispatcher("/jsp/admin_menu.jsp");
		     rd.forward(request,response);
			}else {
				request.setAttribute("errorMessage","ユーザーIDまたはパスワードが不正、もしくはアカウントが無効です");
				RequestDispatcher rd = request.getRequestDispatcher("/login.jsp");rd.forward(request, response);
				}

		}
	}

}
