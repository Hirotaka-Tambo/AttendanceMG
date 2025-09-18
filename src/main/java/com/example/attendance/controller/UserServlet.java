package com.example.attendance.controller;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
import com.fasterxml.jackson.databind.ObjectMapper;

@WebServlet("/users")
public class UserServlet extends HttpServlet {
    private final UserDAO userDAO = new UserDAO();
    private final AttendanceDAO attendanceDAO = new AttendanceDAO();

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
                
             // --- リアルタイム勤怠情報の取得と設定（追加部分） ---
             List<Attendance> workingUsers = attendanceDAO.findWorkingUsers();
             request.setAttribute("workingUsers", workingUsers);
                
                
                RequestDispatcher rd = request.getRequestDispatcher("/jsp/user_management.jsp");
                rd.forward(request, response);
                
            }else if ("get_working_status".equals(action)) { 
                handleGetWorkingStatus(response);
            }  else if ("edit_user".equals(action)) {
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
                case "toggle_enabled":
                	handleToggleEnabled(request, session, username);
                	break;
                default:
                    session.setAttribute("errorMessage", "不明な操作です。");
                    break;
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
            session.setAttribute("errorMessage", "パスワードは8〜20文字で、大文字・小文字・数字・記号を含めてください。");
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
            session.setAttribute("errorMessage", 
                "新しいパスワードは8〜20文字で、大文字・小文字・数字・記号を含めてください。");
            return;
        }

        userDAO.resetPassword(username, newPassword);
        session.setAttribute("script", 
            "alert('ユーザー「" + username + "」のパスワードをリセットしました。');");
    }

    /* ユーザー削除処理 */
    private void handleDeleteUser(HttpServletRequest request, HttpSession session, String username) throws Exception {
        userDAO.deleteUser(username);
        session.setAttribute("script", "alert('ユーザー「" + username + "」を削除しました。');");
    }
    
    /* ユーザー無効化/有効化の切替 */
    private void handleToggleEnabled(HttpServletRequest request, HttpSession session, String username) throws Exception {
    	User user = userDAO.findByUsername(username);

        if (user != null) {
            boolean newEnabledState = !user.isEnabled(); // 現在の状態を反転させる
            
            // DAOメソッドを呼び出してデータベースを更新
            userDAO.toggleUserEnabled(username, newEnabledState);

            // 成功メッセージをセット
            String message = "ユーザー「" + username + "」を" + (newEnabledState ? "有効化" : "無効化") + "しました。";
            session.setAttribute("script", "alert('" + message + "');");
        } else {
            session.setAttribute("errorMessage", "有効/無効を切り替えるユーザーが見つかりませんでした。");
        }
    }

    /* ユーザー名バリデーション */
    private boolean isValidUsername(String username) {
        return username != null && username.length() <= 10 && username.matches("^[a-zA-Z0-9]+$");
    }

    /* パスワードバリデーション */
    private boolean isValidPassword(String password) {
        if (password == null) return false;

        // 8〜20文字
        if (password.length() < 8 || password.length() > 20) return false;

        // 大文字・小文字・数字・記号を最低1種類ずつ含む
        boolean hasUpper = password.matches(".*[A-Z].*");
        boolean hasLower = password.matches(".*[a-z].*");
        boolean hasDigit = password.matches(".*[0-9].*");
        boolean hasSymbol = password.matches(".*[^a-zA-Z0-9].*");

        return hasUpper && hasLower && hasDigit && hasSymbol;
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
    
    /* 勤務状況をJSON形式で返す新しいメソッド */
    private void handleGetWorkingStatus(HttpServletResponse response) throws IOException {
        // 空のリストを作成(例外処理対応のため)
        List<Attendance> workingUsers = null;
        try {
        	//データベースから取り寄せ(DAOでの処理)
        	workingUsers = attendanceDAO.findWorkingUsers();
			
		} catch (SQLException e) {
			e.printStackTrace();
			//JSON形式でエラーレスポンスを返す
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> error = new HashMap<>();
            error.put("error", "データベースエラーが発生しました。");
            mapper.writeValue(response.getWriter(), error);
            return;
		}

        // JSON形式に変換してクライアントに返す
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(response.getWriter(), workingUsers);
    }
}
