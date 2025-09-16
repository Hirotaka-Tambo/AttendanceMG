package com.example.attendance.dao;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.example.attendance.dto.User;
import com.example.attendance.exception.UserOperationException;
import com.example.attendance.util.DBUtils;

public class UserDAO {
	
	// UserServletからの呼び出しに合わせてaddUserメソッドを修正
	public void addUser(String username, String hashedPassword, String salt, String role) throws UserOperationException {
	    // まずユーザーが既に存在するかチェック
	    if (findByUsername(username) != null) {
	        throw new UserOperationException("ユーザー名「" + username + "」は既に使用されています。");
	    }

	    String sql = "INSERT INTO users(username, password_hash, salt, user_role, enabled) VALUES(?,?,?,?,?)";

	    try(Connection conn = DBUtils.getConnection();
	        PreparedStatement pstmt = conn.prepareStatement(sql)){
	        pstmt.setString(1, username);
	        pstmt.setString(2, hashedPassword);
	        pstmt.setString(3, salt);
	        pstmt.setString(4, role);
	        pstmt.setBoolean(5, true); // 新規ユーザーはデフォルトで有効
	        pstmt.executeUpdate();
	    } catch (SQLException e) {
	        e.printStackTrace();
	        throw new RuntimeException("ユーザーの追加に失敗しました", e);
	    }
	}

	
	// ユーザーの検索
	public User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getString("username"),
                            rs.getString("password_hash"),
                            rs.getString("salt"),
                            rs.getString("user_role"),
                            rs.getBoolean("enabled"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("ユーザーの検索に失敗しました。", e);
        }
        return null;
    }
	
	// パスワードの真偽検証
	public boolean verifyPassword(String inputPassword, String storedHash, String storedSalt) {
		String hashedInputPassword = hashPassword(inputPassword, storedSalt);
		return storedHash.equals(hashedInputPassword);
	}
	
	// 全ユーザーの取得
	public Collection<User> getAllUsers() {
        List<User> userList = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY username";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                userList.add(new User(
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getString("salt"),
                        rs.getString("user_role"),
                        rs.getBoolean("enabled")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("全ユーザーの取得に失敗しました。", e);
        }
        return userList;
    }
	
	// ユーザー情報の更新
	public void updateUser(User user) throws UserOperationException{
		String sql = "UPDATE users SET user_role = ?, enabled = ? WHERE username = ?";
		try (Connection connection = DBUtils.getConnection();
			     PreparedStatement pstmt = connection.prepareStatement(sql)) {
			    pstmt.setString(1, user.getRole());
			    pstmt.setBoolean(2, user.isEnabled());
			    pstmt.setString(3, user.getUsername());
			    
			    int updated = pstmt.executeUpdate();  
			    if (updated == 0) {                  
			        throw new UserOperationException("更新対象のユーザーが存在しません。");
			    }

			} catch (SQLException e) {
			    e.printStackTrace();
			    throw new RuntimeException("ユーザー情報の更新に失敗しました。", e);
			}
	}
	
	// ユーザーの削除
	public void deleteUser(String username) throws UserOperationException {
	    String sql = "DELETE FROM users WHERE username = ?";
	    try (Connection conn = DBUtils.getConnection();
	         PreparedStatement pstmt = conn.prepareStatement(sql)) {
	        pstmt.setString(1, username);
	        boolean deleted = pstmt.executeUpdate() > 0;
	        if (!deleted) {
	            throw new UserOperationException("削除対象のユーザーが存在しません。");
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	        throw new RuntimeException("ユーザーの削除に失敗しました。", e);
	    }
	}

	// パスワードのリセット
	public void resetPassword(String username, String newPassword) throws UserOperationException {
	    String salt = UUID.randomUUID().toString();
	    String hashedPassword = hashPassword(newPassword, salt);
	    String sql = "UPDATE users SET password_hash = ?, salt = ? WHERE username = ?";

	    try (Connection conn = DBUtils.getConnection();
	         PreparedStatement pstmt = conn.prepareStatement(sql)) {
	        pstmt.setString(1, hashedPassword);
	        pstmt.setString(2, salt);
	        pstmt.setString(3, username);

	        int updated = pstmt.executeUpdate();  // ← 更新行数をチェック
	        if (updated == 0) {
	            throw new UserOperationException("パスワードをリセットする対象のユーザーが存在しません。");
	        }

	    } catch (SQLException e) {
	        e.printStackTrace();
	        throw new RuntimeException("パスワードのリセットに失敗しました。", e);
	    }
	}

	// ユーザーアカウントの有効/無効切り替え
	public void toggleUserEnabled(String username, boolean enabled) throws UserOperationException {
	    String sql = "UPDATE users SET enabled = ? WHERE username = ?";

	    try (Connection conn = DBUtils.getConnection();
	         PreparedStatement pstmt = conn.prepareStatement(sql)) {
	        pstmt.setBoolean(1, enabled);
	        pstmt.setString(2, username);

	        int updated = pstmt.executeUpdate();  // ← 更新行数をチェック
	        if (updated == 0) {
	            throw new UserOperationException("有効/無効切り替えの対象ユーザーが存在しません。");
	        }

	    } catch (SQLException e) {
	        e.printStackTrace();
	        throw new RuntimeException("アカウント有効状態の切り替えに失敗しました。", e);
	    }
	}

   
	
    // パスワードをハッシュ化して、ソルトを追加する
    private String hashPassword(String password, String salt) {
        try {
            String combinedString = password + salt;	
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = md.digest(combinedString.getBytes());
            
            StringBuilder sb = new StringBuilder();
            for(byte b: hashedBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch(NoSuchAlgorithmException e){
            e.printStackTrace();
            throw new RuntimeException("パスワードのハッシュ化に失敗しました。", e);
        }
    }
}