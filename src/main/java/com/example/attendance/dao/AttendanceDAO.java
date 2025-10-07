package com.example.attendance.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.attendance.dto.Attendance;
import com.example.attendance.exception.UserOperationException;
import com.example.attendance.util.DBUtils;

public class AttendanceDAO {

    // 従業員の出勤を記録
    public void checkIn(String userId) {
        String sql = "INSERT INTO attendance (user_id, check_in_time) VALUES (?, ?)";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            pstmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("出勤記録の追加に失敗しました。", e);
        }
    }

    // 従業員の退勤を記録
    public void checkOut(String userId) {
        String sql = "UPDATE attendance SET check_out_time = ? " +
                     "WHERE id = (SELECT id FROM attendance " +
                     "WHERE user_id = ? AND check_out_time IS NULL " +
                     "ORDER BY check_in_time DESC LIMIT 1)";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.setString(2, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("退勤記録の更新に失敗しました。", e);
        }
    }

    // ユーザーIDに基づいて勤怠記録を取得
    public List<Attendance> findByUserId(String userId) {
        List<Attendance> attendanceList = new ArrayList<>();
        String sql = "SELECT * FROM attendance WHERE user_id = ? ORDER BY check_in_time DESC";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                	int id = rs.getInt("id");
                    Timestamp checkInTimestamp = rs.getTimestamp("check_in_time");
                    Timestamp checkOutTimestamp = rs.getTimestamp("check_out_time");
                    
                    LocalDateTime checkInTime = (checkInTimestamp != null) ? checkInTimestamp.toLocalDateTime() : null;
                    LocalDateTime checkOutTime = (checkOutTimestamp != null) ? checkOutTimestamp.toLocalDateTime() : null;

                    attendanceList.add(new Attendance(id, rs.getString("user_id"), checkInTime, checkOutTime));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("勤怠記録の取得に失敗しました。", e);
        }
        return attendanceList;
    }
    
    // 期間とユーザーIDで勤怠記録をフィルタリング
    public List<Attendance> findFilteredRecords(String userId, LocalDate startDate, LocalDate endDate) {
        List<Attendance> records = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM attendance WHERE 1=1");
        
        if (userId != null && !userId.isEmpty()) {
            sql.append(" AND user_id = ?");
        }
        if (startDate != null) {
            sql.append(" AND DATE(check_in_time) >= ?");
        }
        if (endDate != null) {
            sql.append(" AND DATE(check_in_time) <= ?");
        }
        sql.append(" ORDER BY check_in_time DESC");

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            int paramIndex = 1;
            if (userId != null && !userId.isEmpty()) {
                pstmt.setString(paramIndex++, userId);
            }
            if (startDate != null) {
                pstmt.setDate(paramIndex++, java.sql.Date.valueOf(startDate));
            }
            if (endDate != null) {
                pstmt.setDate(paramIndex++, java.sql.Date.valueOf(endDate));
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                	int id = rs.getInt("id");
                    Timestamp checkInTimestamp = rs.getTimestamp("check_in_time");
                    Timestamp checkOutTimestamp = rs.getTimestamp("check_out_time");

                    LocalDateTime checkInTime = (checkInTimestamp != null) ? checkInTimestamp.toLocalDateTime() : null;
                    LocalDateTime checkOutTime = (checkOutTimestamp != null) ? checkOutTimestamp.toLocalDateTime() : null;
                    
                    records.add(new Attendance(id, rs.getString("user_id"), checkInTime, checkOutTime));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("勤怠記録のフィルタリングに失敗しました。", e);
        }
        return records;
    }

    // ユーザーごとの合計労働時間を取得（フィルター対応）
    public Map<String, Double> getTotalWorkingHoursByUsers(String userId, LocalDate startDate, LocalDate endDate) {
        Map<String, Double> totalHours = new HashMap<>();
        StringBuilder sql = new StringBuilder(
            "SELECT user_id, SUM(EXTRACT(EPOCH FROM (check_out_time - check_in_time)) / 3600) AS total_hours " +
            "FROM attendance WHERE check_out_time IS NOT NULL"
        );
        
        if (userId != null && !userId.isEmpty()) {
            sql.append(" AND user_id = ?");
        }
        if (startDate != null) {
            sql.append(" AND DATE(check_in_time) >= ?");
        }
        if (endDate != null) {
            sql.append(" AND DATE(check_in_time) <= ?");
        }
        sql.append(" GROUP BY user_id");
        
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            int paramIndex = 1;
            if (userId != null && !userId.isEmpty()) {
                pstmt.setString(paramIndex++, userId);
            }
            if (startDate != null) {
                pstmt.setDate(paramIndex++, java.sql.Date.valueOf(startDate));
            }
            if (endDate != null) {
                pstmt.setDate(paramIndex++, java.sql.Date.valueOf(endDate));
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    totalHours.put(rs.getString("user_id"), rs.getDouble("total_hours"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("ユーザーごとの合計労働時間の取得に失敗しました。", e);
        }
        return totalHours;
    }

    // 月ごとの合計労働時間を取得（フィルター対応）
    public Map<String, Double> getMonthlyWorkingHours(String userId, LocalDate startDate, LocalDate endDate) {
        Map<String, Double> monthlyHours = new HashMap<>();
        StringBuilder sql = new StringBuilder(
            "SELECT to_char(check_in_time, 'YYYY-MM') AS month, " +
            "SUM(EXTRACT(EPOCH FROM (check_out_time - check_in_time)) / 3600) AS total_hours " +
            "FROM attendance WHERE check_out_time IS NOT NULL"
        );
        
        if (userId != null && !userId.isEmpty()) {
            sql.append(" AND user_id = ?");
        }
        if (startDate != null) {
            sql.append(" AND DATE(check_in_time) >= ?");
        }
        if (endDate != null) {
            sql.append(" AND DATE(check_in_time) <= ?");
        }
        sql.append(" GROUP BY month ORDER BY month");

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            int paramIndex = 1;
            if (userId != null && !userId.isEmpty()) {
                pstmt.setString(paramIndex++, userId);
            }
            if (startDate != null) {
                pstmt.setDate(paramIndex++, java.sql.Date.valueOf(startDate));
            }
            if (endDate != null) {
                pstmt.setDate(paramIndex++, java.sql.Date.valueOf(endDate));
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    monthlyHours.put(rs.getString("month"), rs.getDouble("total_hours"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("月ごとの合計労働時間の取得に失敗しました。", e);
        }
        return monthlyHours;
    }
    
    // 月ごとの出勤日数を取得（フィルター対応）
    public Map<String, Long> getMonthlyCheckInCounts(String userId, LocalDate startDate, LocalDate endDate) {
        Map<String, Long> monthlyCounts = new HashMap<>();
        StringBuilder sql = new StringBuilder(
            "SELECT to_char(check_in_time, 'YYYY-MM') AS month, " +
            "COUNT(DISTINCT DATE(check_in_time)) AS daily_count " +
            "FROM attendance WHERE check_in_time IS NOT NULL"
        );

        if (userId != null && !userId.isEmpty()) {
            sql.append(" AND user_id = ?");
        }
        if (startDate != null) {
            sql.append(" AND DATE(check_in_time) >= ?");
        }
        if (endDate != null) {
            sql.append(" AND DATE(check_in_time) <= ?");
        }
        sql.append(" GROUP BY month ORDER BY month");

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            int paramIndex = 1;
            if (userId != null && !userId.isEmpty()) {
                pstmt.setString(paramIndex++, userId);
            }
            if (startDate != null) {
                pstmt.setDate(paramIndex++, java.sql.Date.valueOf(startDate));
            }
            if (endDate != null) {
                pstmt.setDate(paramIndex++, java.sql.Date.valueOf(endDate));
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    monthlyCounts.put(rs.getString("month"), rs.getLong("daily_count"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("月ごとの出勤日数の取得に失敗しました。", e);
        }
        return monthlyCounts;
    }
    
    
    // 勤怠記録の手動追加（管理者用）
    public void addManualAttendance(String userId, LocalDateTime checkIn, LocalDateTime checkOut) throws UserOperationException {
        try {
            if (hasTimeOverlap(userId, checkIn, checkOut)) {
                throw new UserOperationException("その時間帯は既に勤怠登録済みです。");
            }

            String sql = "INSERT INTO attendance (user_id, check_in_time, check_out_time) VALUES (?, ?, ?)";
            try (Connection conn = DBUtils.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, userId);
                pstmt.setTimestamp(2, Timestamp.valueOf(checkIn));
                pstmt.setTimestamp(3, checkOut != null ? Timestamp.valueOf(checkOut) : null);
                pstmt.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("手動での勤怠記録の追加に失敗しました。", e);
        }
    }
    
    // 勤怠記録更新（管理者用）
    public boolean updateManualAttendance(int attendanceId, String userId, LocalDateTime newCheckIn,
                                         LocalDateTime newCheckOut) throws UserOperationException {
        try {
            if (hasTimeOverlapForUpdate(attendanceId ,userId, newCheckIn, newCheckOut)) {
                throw new UserOperationException("更新後の時間帯が既存の勤怠と重複しています。");
            }

            String sql = "UPDATE attendance SET check_in_time = ?, check_out_time = ? WHERE id = ?";
            try (Connection conn = DBUtils.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setTimestamp(1, Timestamp.valueOf(newCheckIn));
                pstmt.setTimestamp(2, newCheckOut != null ? Timestamp.valueOf(newCheckOut) : null);
                pstmt.setInt(3, attendanceId);
                
                
                //デバッグでの確認
                System.out.println("更新対象ID: " + attendanceId);

                int rows = pstmt.executeUpdate();
                System.out.println("更新件数: " + rows);
                
                return rows > 0;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("手動での勤怠記録の更新に失敗しました。"+ e.getMessage(),e);
        }
    }

    // 勤怠記録の手動削除（管理者用)
    public boolean deleteManualAttendance(int attendanceId) {
        String sql = "DELETE FROM attendance WHERE id = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, attendanceId);
            int rowsAffected = pstmt.executeUpdate();
            
            return rowsAffected > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("勤怠記録の削除に失敗しました。", e);
        }
    }

    /**
     * 時間重複確認
     */
    public boolean hasTimeOverlap(String userId, LocalDateTime newCheckIn, LocalDateTime newCheckOut) {
        String sql = "SELECT COUNT(*) FROM attendance " +
                     "WHERE user_id = ? AND id != ?" +
                     "AND check_in_time < ? " +
                     "AND (check_out_time > ? OR check_out_time IS NULL)";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userId);
            pstmt.setTimestamp(2, Timestamp.valueOf(newCheckOut != null ? newCheckOut : LocalDateTime.MAX));
            pstmt.setTimestamp(3, Timestamp.valueOf(newCheckIn));

            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("時間重複の確認に失敗しました。", e);
        }
    }

   //勤怠記録を更新する際に、更新対象の記録を除いて時間重複がないか確認
   // 既存の時間と重複してもいいように、attendanceIdで取得してくる
    public boolean hasTimeOverlapForUpdate(int attendanceId, String userId,
                                         LocalDateTime newCheckIn, LocalDateTime newCheckOut) {
    	boolean result = false;
    	
    	String sql = "SELECT COUNT(*) FROM attendance " +
                "WHERE user_id = ? " +
                "AND id <> ? " + // 自分自身を除外
                "AND (" +
                "    (check_in_time < ? AND (check_out_time IS NULL OR check_out_time > ?))" +
                ")";
    	
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userId);
            pstmt.setInt(2, attendanceId);
            pstmt.setTimestamp(3, Timestamp.valueOf(newCheckOut != null ? newCheckOut : newCheckIn.plusHours(24)));
            pstmt.setTimestamp(4, Timestamp.valueOf(newCheckIn));
            pstmt.setTimestamp(5, Timestamp.valueOf(newCheckIn));

            try (ResultSet rs = pstmt.executeQuery()) {
            	if (rs.next()) {
                    result = rs.getInt(1) > 0;
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("更新時の時間重複の確認に失敗しました。"+ e.getMessage());
        }
        
        // デバッグログ
        System.out.println("時間重複チェック結果: " + result);
        return result;
    }

    /*
     * 指定されたユーザーIDの最新の勤怠記録を1件取得します。
     * @param userId ユーザーID
     * @return 最新の勤怠記録、見つからない場合はnull
     * 二重勤怠を防ぐため
     */
    public Attendance getLatestRecord(String userId) {
        String sql = "SELECT * FROM attendance WHERE user_id = ? ORDER BY check_in_time DESC LIMIT 1";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Attendance(
                        rs.getInt("id"),
                        rs.getString("user_id"),
                        rs.getTimestamp("check_in_time").toLocalDateTime(),
                        rs.getTimestamp("check_out_time") != null ? rs.getTimestamp("check_out_time").toLocalDateTime() : null
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * 勤務中のすべてのユーザーを取得する。
     * @return 勤務中のAttendanceオブジェクトのリスト
     * @throws SQLException
     */
    public List<Attendance> findWorkingUsers() throws SQLException {
        List<Attendance> workingUsers = new ArrayList<>();
        // punch_out_timeがNULLのレコードを検索
        String sql = "SELECT * FROM attendance WHERE check_out_time IS NULL";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                // Attendanceオブジェクトを生成し、リストに追加
                Attendance attendance = new Attendance();
                attendance.setId(rs.getInt("id"));
                attendance.setUserId(rs.getString("user_id")); 
                attendance.setCheckInTime(rs.getTimestamp("check_in_time").toLocalDateTime());
                // punch_out_timeはNULLなので設定しない
                workingUsers.add(attendance);
            }
        }
        return workingUsers;
    }
    
}