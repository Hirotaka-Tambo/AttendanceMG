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

        HttpSession session = request.getSession(false);
        User currentUser = session != null ? (User) session.getAttribute("user") : null;

        // 管理者チェック
        if (currentUser == null || !"admin".equals(currentUser.getRole())) {
            response.sendRedirect("login.jsp");
            return;
        }

        // セッションメッセージの取り出し
        transferSessionMessageToRequest(session, request);

        String action = request.getParameter("action");

        try {
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

        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("errorMessage", "予期せぬシステムエラーが発生しました。");
            RequestDispatcher rd = request.getRequestDispatcher("/jsp/error.jsp");
            rd.forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false);
        User currentUser = session != null ? (User) session.getAttribute("user") : null;

        if (currentUser == null || !"admin".equals(currentUser.getRole())) {
            response.sendRedirect("login.jsp");
            return;
        }

        String action = request.getParameter("action");
        String username = request.getParameter("username");

        try {
            if (username == null || username.isEmpty()) {
                session.setAttribute("errorMessage", "ユーザー名が指定されていません。");
                response.sendRedirect(request.getContextPath() + "/users?action=list");
                return;
            }

            switch (action) {
                case "add_user":
                    handleAddUser(request, session, username);
                    break;
                case "update_user":
                    handleUpdateUser(request, session, username);
                    break;
                case "reset_password":
                    handleResetPassword(request, session, username);
                    break;
                case "delete_user":
                    handleDeleteUser(request, session, username);
                    break;
                default:
                    session.setAttribute("errorMessage", "不明な操作です。");
            }

        } catch (Exception e) {
            // ユーザーが直せないエラーは error.jsp に遷移
            e.printStackTrace();
            request.setAttribute("errorMessage", "予期せぬシステムエラーが発生しました: " + e.getMessage());
            RequestDispatcher rd = request.getRequestDispatcher("/jsp/error.jsp");
            rd.forward(request, response);
            return;
        }

        // 画面遷移
        if ("update_user".equals(action) || "reset_password".equals(action)) {
            response.sendRedirect(request.getContextPath() + "/users?action=edit_user&username=" + username);
        } else {
            response.sendRedirect(request.getContextPath() + "/users?action=list");
        }
    }

    /** ユーザー追加処理 */
    private void handleAddUser(HttpServletRequest request, HttpSession session, String username) throws Exception {
        String password = request.getParameter("password");
        String role = request.getParameter("role");

        // --- バリデーション ---
        if (!isValidUsername(username)) {
            session.setAttribute("errorMessage", "ユーザー名は1〜10文字の半角英数字で入力してください。");
            return;
        }
        if (!isValidPassword(password)) {
            session.setAttribute("errorMessage", "パスワードは5〜10文字の半角アルファベットで入力してください。");
            return;
        }
        if (userDAO.findByUsername(username) != null) {
            session.setAttribute("errorMessage", "ユーザーIDは既に存在します。");
            return;
        }

        // --- パスワードのハッシュ化とソルト ---
        String salt = UUID.randomUUID().toString();
        String hashedPassword = hashPassword(password, salt);

        userDAO.addUser(username, hashedPassword, salt, role);
        session.setAttribute("script", "alert('ユーザー「" + username + "」を追加しました。');");
    }

    /** ユーザー更新処理 */
    private void handleUpdateUser(HttpServletRequest request, HttpSession session, String username) throws Exception {
        String role = request.getParameter("role");
        User existingUser = userDAO.findByUsername(username);

        if (existingUser != null) {
            existingUser.setRole(role);
            userDAO.updateUser(existingUser);
            session.setAttribute("script", "alert('ユーザー「" + username + "」の役割を更新しました。');");
        } else {
            session.setAttribute("errorMessage", "ユーザーが見つかりませんでした。");
        }
    }

    /** パスワードリセット処理 */
    private void handleResetPassword(HttpServletRequest request, HttpSession session, String username) throws Exception {
        String newPassword = request.getParameter("newPassword");
        if (!isValidPassword(newPassword)) {
            session.setAttribute("errorMessage", "新しいパスワードは5〜10文字の半角アルファベットで入力してください。");
            return;
        }
        userDAO.resetPassword(username, newPassword);
        session.setAttribute("script", "alert('ユーザー「" + username + "」のパスワードをリセットしました。');");
    }

    /** ユーザー削除処理 */
    private void handleDeleteUser(HttpServletRequest request, HttpSession session, String username) throws Exception {
        userDAO.deleteUser(username);
        session.setAttribute("script", "alert('ユーザー「" + username + "」を削除しました。');");
    }

    /** ユーザー名バリデーション */
    private boolean isValidUsername(String username) {
        return username != null && username.length() <= 10 && username.matches("^[a-zA-Z0-9]+$");
    }

    /** パスワードバリデーション */
    private boolean isValidPassword(String password) {
        return password != null && password.length() >= 5 && password.length() <= 10 && password.matches("^[a-zA-Z]+$");
    }

    /** パスワード＋ソルトをSHA-256でハッシュ化 */
    private String hashPassword(String password, String salt) {
        try {
            String combined = password + salt;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashed = md.digest(combined.getBytes());

            StringBuilder sb = new StringBuilder();
            for (byte b : hashed) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("パスワードのハッシュ化に失敗しました。", e);
        }
    }

    /** セッションのメッセージを request に移す */
    private void transferSessionMessageToRequest(HttpSession session, HttpServletRequest request) {
        if (session == null) return;

        Object successMessage = session.getAttribute("successMessage");
        if (successMessage != null) {
            request.setAttribute("successMessage", successMessage);
            session.removeAttribute("successMessage");
        }

        Object errorMessage = session.getAttribute("errorMessage");
        if (errorMessage != null) {
            request.setAttribute("errorMessage", errorMessage);
            session.removeAttribute("errorMessage");
        }

        Object script = session.getAttribute("script");
        if (script != null) {
            request.setAttribute("script", script);
            session.removeAttribute("script");
        }
    }
}
