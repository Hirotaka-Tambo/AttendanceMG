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

/**
 * Servlet implementation class UserServlet
 */
@WebServlet("/users")
public class UserServlet extends HttpServlet {
	private final UserDAO userDAO = new UserDAO();
	
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		String action = request.getParameter("action");
		HttpSession session = request.getSession(false);
		User currentUser = (User) session.getAttribute("user");
		
		if(currentUser == null || !"admin".equals(currentUser.getRole())) {
			response.sendRedirect("login.jsp");
			return;
		}
		
		String message = (String) session.getAttribute("successMessage");
		if(message != null) {
			request.setAttribute("successMessage",message);
			session.removeAttribute("successMessage");
		}
		
		if("list".equals(action) || action == null) {
			Collection<User> users = userDAO.getAllUsers();
			request.setAttribute("users", users);
			RequestDispatcher rd = request.getRequestDispatcher("/jsp/user_manegement.jsp");
			rd.forward(request, response);
		}else {
			response.sendRedirect("users?action=list");
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		request.setCharacterEncoding("UTF-8");
		String action = request.getParameter("action");
		HttpSession session = request.getSession(false);
		User currentUser = (User) session.getAttribute("user");
		
		if(currentUser == null || "admin".equals(currentUser.getRole())) {
			response.sendRedirect("login.jsp");
			return;
		}
		
		if("add".equals(action)) {
			String username = request.getParameter("username");
			String password = request.getParameter("password");
			String role = request.getParameter("role");
			
			if(userDAO.findByUsername(username)== null) {
				userDAO.addUser(new User(username, UserDAO.hashPassword(password), role));
				session.setAttribute("successMessage", "ユーザーを追加しました");
			}else {
				request.setAttribute("errorMessage", "ユーザーIDは既に存在します");
			}
		}else if("update".equals(action)) {
			String username = request.getParameter("username");
			String role = request.getParameter("role");
			boolean enabled = request.getParameter("enabled") != null;
			
			User existingUser = userDAO.findByUsername(username);
			if(existingUser != null) {
				userDAO.updateUser(new User(username, existingUser.getPassword(), role,enabled));
				
				session.setAttribute("successMessage", "ユーザー情報を更新しました");
			}
		}else if("delete".equals(action)) {
			String username = request.getParameter("username");
			userDAO.deleteUer(username);
			session.setAttribute("successMessage", "ユーザーを削除しました");
		}else if("reset_password".equals(action)){
			String username = request.getParameter("username");
			String newPassword = request.getParameter("newPassword");
			userDAO.resetPassword(username, newPassword);
			session.setAttribute("successMessage", username + "のパスワードをリセットしました。(デフォルトパスワード:"+newPassword+")");
		}else if("toggle_enabled".equals(action)) {
			String username = request.getParameter("username");
			boolean enabled = Boolean.parseBoolean(request.getParameter("enabled"));
			userDAO.toggleUserEnabled(username, enabled);
			session.setAttribute("successMessage", username + "のアカウントを" + (enabled ? "有効" : "無効") + "にしました。");
			
			
		}
		
		response.sendRedirect("users?action=list");
		
	}

}
