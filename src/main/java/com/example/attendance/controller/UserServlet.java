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

        String script = (String) session.getAttribute("script");
        if (script != null) {
            request.setAttribute("script", script);
            session.removeAttribute("script");
        }
		
		if("list".equals(action) || action == null) {
			Collection<User> users = userDAO.getAllUsers();
			request.setAttribute("userList", users);
			RequestDispatcher rd = request.getRequestDispatcher("/jsp/user_management.jsp");
			rd.forward(request, response);
		} else if ("edit_user".equals(action)) {
            String username = request.getParameter("username");
            User userToEdit = userDAO.findByUsername(username);
            if (userToEdit != null) {
                request.setAttribute("userToEdit", userToEdit);
                RequestDispatcher rd = request.getRequestDispatcher("/jsp/edit_user.jsp");
                rd.forward(request, response);
            } else {
                session.setAttribute("errorMessage", "編集対象のユーザーが見つかりませんでした。");
                response.sendRedirect(request.getContextPath() + "/users?action=list");
            }
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
		
		String username = request.getParameter("username");
		
		try {
			if ("add_user".equals(action)) {
				String password = request.getParameter("password");
				String role = request.getParameter("role");
				
				if (userDAO.findByUsername(username) == null) {
					userDAO.addUser(username, password, role);
					session.setAttribute("script", "alert('ユーザー「" + username + "」を追加しました。');");
				} else {
					session.setAttribute("errorMessage", "ユーザーIDは既に存在します。");
				}
				
			} else if ("update_user".equals(action)) {
                String role = request.getParameter("role");
                
                User existingUser = userDAO.findByUsername(username);

                if (existingUser != null) {
                    existingUser.setRole(role);
                    userDAO.updateUser(existingUser);
                    session.setAttribute("script", "alert('ユーザー「" + username + "」の役割を更新しました。');");
                } else {
                    session.setAttribute("errorMessage", "ユーザーが見つかりませんでした。");
                }
            } else if ("reset_password".equals(action)) {
                String newPassword = request.getParameter("newPassword");
                
                userDAO.resetPassword(username, newPassword);
                session.setAttribute("script", "alert('ユーザー「" + username + "」のパスワードをリセットしました。');");
            } else if ("delete_user".equals(action)) {
				userDAO.deleteUser(username);
				session.setAttribute("script", "alert('ユーザー「" + username + "」を削除しました。');");
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			session.setAttribute("errorMessage", "操作中にエラーが発生しました: " + e.getMessage());
		}
		
		if ("update_user".equals(action) || "reset_password".equals(action)) {
			response.sendRedirect(request.getContextPath() + "/users?action=edit_user&username=" + username);
		} else {
			response.sendRedirect(request.getContextPath() + "/users?action=list");
		}
	}
}