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

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();
    
    // 明示的にdogetを記述・sessionMessageをremoveするため(デフォルトのget処理ではerrorMessageが残ってしまうため)
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // メッセージをセッションから削除する共通処理
        clearSessionMessages(request.getSession(false));
        // login.jspに転送
        request.getRequestDispatcher("/login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // セッションを取得し、以前のエラーメッセージの削除
    	clearSessionMessages(request.getSession(false));
    	
    	String username = request.getParameter("username");
        String password = request.getParameter("password");

        try {
            User user = userDAO.findByUsername(username);

            if (user != null && user.isEnabled() && userDAO.verifyPassword(password, user.getPasswordHash(), user.getSalt())) {
                HttpSession session = request.getSession();
                session.setAttribute("user", user);
                session.setAttribute("successMessage", "ログインしました");
                response.sendRedirect(request.getContextPath() + "/attendance");
            } else {
            	request.setAttribute("errorMessage", "ユーザーIDまたはパスワードが不正、もしくはアカウントが無効です");
            	request.getRequestDispatcher("/login.jsp").forward(request, response);
            }

        } catch (Exception e) {
            // ユーザーが直せない例外
        	request.setAttribute("errorMessage", "システムエラーが発生しました");
        	request.getRequestDispatcher("/jsp/error.jsp").forward(request, response);
        }
    }
    
    // sessionMessageをremoveするためのメソッド
    private void clearSessionMessages(HttpSession session) {
        if (session != null) {
            session.removeAttribute("errorMessage");
            session.removeAttribute("successMessage");
        }
    }
    
}
