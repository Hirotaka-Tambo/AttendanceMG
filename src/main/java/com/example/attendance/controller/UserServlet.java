package com.example.attendance.controller;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.UUID;

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
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");
        HttpSession session = request.getSession(false);
        User currentUser = (User) session.getAttribute("user");

        // 管理者チェック
        if (currentUser == null || !"admin".equals(currentUser.getRole())) {
            response.sendRedirect("login.jsp");
            return;
        }

        // セッションメッセージの取り出し
        String successMessage = (String) session.getAttribute("successMessage");
        if (successMessage != null) {
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

        // アクションごとの処理
        if ("list".equals(action) || action == null) {
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
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");
        HttpSession session = request.getSession(false);
        User currentUser = (User) session.getAttribute("user");

        // 管理者チェック
        if (currentUser == null || !"admin".equals(currentUser.getRole())) {
            response.sendRedirect("login.jsp");
            return;
        }

        String username = request.getParameter("username");

        try {
            if ("add_user".equals(action)) {
                String password = request.getParameter("password");
                String role = request.getParameter("role");

                // --- バリデーションチェック ---
                // 1. ユーザー名
                if (username == null || username.trim().isEmpty() || username.length() > 10) {
                    session.setAttribute("errorMessage", "ユーザー名は1〜10文字で入力してください。");
                    response.sendRedirect(request.getContextPath() + "/users?action=list");
                    return;
                }
                if (!username.matches("^[a-zA-Z0-9]+$")) {
                    session.setAttribute("errorMessage", "ユーザー名は半角英数字のみ使用できます。");
                    response.sendRedirect(request.getContextPath() + "/users?action=list");
                    return;
                }

                // 2. パスワード
                if (password == null || password.length() < 5 || password.length() > 10) {
                    session.setAttribute("errorMessage", "パスワードは5〜10文字で入力してください。");
                    response.sendRedirect(request.getContextPath() + "/users?action=list");
                    return;
                }
                if (!password.matches("^[a-zA-Z]+$")) {
                    session.setAttribute("errorMessage", "パスワードは半角アルファベットのみ使用できます。");
                    response.sendRedirect(request.getContextPath() + "/users?action=list");
                    return;
                }

                // 3. ユーザー名重複チェック
                if (userDAO.findByUsername(username) != null) {
                    session.setAttribute("errorMessage", "ユーザーIDは既に存在します。");
                    response.sendRedirect(request.getContextPath() + "/users?action=list");
                    return;
                }

                // --- パスワードのハッシュ化とソルト ---
                String salt = UUID.randomUUID().toString(); // ランダムなソルトを生成
                String hashedPassword = hashPassword(password, salt);

                // DAO呼び出し（ハッシュ済みパスワード＋ソルト）
                userDAO.addUser(username, hashedPassword, salt, role);

                session.setAttribute("script", "alert('ユーザー「" + username + "」を追加しました。');");

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

        // 画面遷移
        if ("update_user".equals(action) || "reset_password".equals(action)) {
            response.sendRedirect(request.getContextPath() + "/users?action=edit_user&username=" + username);
        } else {
            response.sendRedirect(request.getContextPath() + "/users?action=list");
        }
    }

    /**
     * パスワード＋ソルトをSHA-256でハッシュ化
     */
    private String hashPassword(String password, String salt) {
        try {
            String combinedString = password + salt;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = md.digest(combinedString.getBytes());

            StringBuilder sb = new StringBuilder();
            for (byte b : hashedBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("パスワードのハッシュ化に失敗しました。", e);
        }
    }
}
