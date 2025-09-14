package com.example.attendance.controller;

import java.io.IOException;
import java.util.Collection;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.example.attendance.dao.UserDAO;
import com.example.attendance.dto.User;

@WebServlet("/users")
public class UserServlet extends HttpServlet {
	private final UserDAO userDAO = new UserDAO();
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String action = request.getParameter("action");
		HttpSession session = request.getSession(false);
		User currentUser = (User) session.getAttribute("user");
		
		if(currentUser == null || !"admin".equals(currentUser.getRole())) {
			response.sendRedirect("login.jsp");
			return;
		}
		
		String successMessage = (String) session.getAttribute("successMessage");
		if(successMessage != null) {
			request.setAttribute("successMessage", successMessage);
			session.removeAttribute("successMessage");
		}
		
		String errorMessage = (String) session.getAttribute("errorMessage");
		if (errorMessage != null) {
			request.setAttribute("errorMessage", errorMessage);
			session.removeAttribute("errorMessage");
		}
		
		if("list".equals(action) || action == null) {
			Collection<User> users = userDAO.getAllUsers();
			request.setAttribute("users", users);
			RequestDispatcher rd = request.getRequestDispatcher("/jsp/user_manegement.jsp");
			rd.forward(request, response);
		} else {
			response.sendRedirect(request.getContextPath() + "/users?action=list");
		}
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		request.setCharacterEncoding("UTF-8");
		String action = request.getParameter("action");
		HttpSession session = request.getSession(false);
		User currentUser = (User) session.getAttribute("user");
		
		if(currentUser == null || !"admin".equals(currentUser.getRole())) {
			response.sendRedirect("login.jsp");
			return;
		}
		
		try {
			if ("add".equals(action)) {
				String username = request.getParameter("username");
				String password = request.getParameter("password");
				String role = request.getParameter("role");
				
				if (userDAO.findByUsername(username) == null) {
					userDAO.addUser(username, password, role);
					session.setAttribute("successMessage", "ユーザーを追加しました");
				} else {
					session.setAttribute("errorMessage", "ユーザーIDは既に存在します");
				}
				
			} else if ("update".equals(action)) {
				String username = request.getParameter("username");
				String role = request.getParameter("role");
				boolean enabled = request.getParameter("enabled") != null;
				
				User existingUser = userDAO.findByUsername(username);
				
				if (existingUser != null) {
					existingUser.setRole(role);
					existingUser.setEnabled(enabled);
					userDAO.updateUser(existingUser);
					session.setAttribute("successMessage", "ユーザー情報を更新しました");
				}
				
			} else if ("delete".equals(action)) {
				String username = request.getParameter("username");
				userDAO.deleteUser(username);
				session.setAttribute("successMessage", "ユーザーを削除しました");
				
			} else if ("reset_password".equals(action)) {
				String username = request.getParameter("username");
				String newPassword = request.getParameter("newPassword");
				userDAO.resetPassword(username, newPassword);
				session.setAttribute("successMessage", username + "のパスワードをリセットしました。(デフォルトパスワード:" + newPassword + ")");
				
			} else if ("toggle_enabled".equals(action)) {
				String username = request.getParameter("username");
				boolean enabled = "true".equals(request.getParameter("enabled"));
				userDAO.toggleUserEnabled(username, enabled);
				session.setAttribute("successMessage", username + "のアカウントを" + (enabled ? "有効" : "無効") + "にしました。");
			}
			
		} catch (Exception e) {
			session.setAttribute("errorMessage", "操作中にエラーが発生しました: " + e.getMessage());
		}
		
		response.sendRedirect(request.getContextPath() + "/users?action=list");
	}
}