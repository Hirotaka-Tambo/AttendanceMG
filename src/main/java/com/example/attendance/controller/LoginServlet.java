package com.example.attendance.controller;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.example.attendance.dao.UserDAO;
import com.example.attendance.dto.User;

/**
 * Servlet implementation class LoginServlet
 */
@WebServlet("/login")
public class LoginServlet extends HttpServlet {
	//セキュリティ強化のために、hash+saltで強固に
	
	private final UserDAO userDAO = new UserDAO();
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
	            // 管理者の場合、AttendanceServletにリダイレクト
	            response.sendRedirect(request.getContextPath() + "/attendance");
	        } else {
	            // 一般ユーザーの場合、AttendanceServletにリダイレクト
	            response.sendRedirect(request.getContextPath() + "/attendance");
	        }

	    } else {
	        // ログイン失敗時
	        request.setAttribute("errorMessage", "ユーザーIDまたはパスワードが不正、もしくはアカウントが無効です");
	        request.getRequestDispatcher("/login.jsp").forward(request, response);
	    }
	}
}