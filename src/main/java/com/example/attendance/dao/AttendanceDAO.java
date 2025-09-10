package com.example.attendance.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServlet;

import com.example.attendance.dto.Attendance;
import com.example.attendance.util.DBUtils;


public class AttendanceDAO extends HttpServlet {
	
	public void checkIn(String userId) {
		String sql = "INSERT INTO attendance(user_id,check_in_time) VALUES(?,?)";
		try (Connection conn = DBUtils.getConnection();
			PreparedStatement pstmt = conn.prepareStatement(sql)){
			
			pstmt.setString(1, userId);
			pstmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
			pstmt.executeUpdate();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			throw new RuntimeException("出勤記録に失敗しました。",e);
		}
	}
	
	public void checkOut(String userId) {
		String sql = "UPDATE attendance SET check_out_time = ? WHERE user_id = ? AND check_out_time IS NULL";
		try (Connection conn = DBUtils.getConnection();
			PreparedStatement pstmt = conn.prepareStatement(sql)){
			
			pstmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
			pstmt.setString(2, userId);
			pstmt.executeUpdate();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			throw new RuntimeException("退勤記録に失敗しました。",e);
		}
	}
	
	
	
	public List<Attendance>findByUserId(String userId){
		List<Attendance> records = new ArrayList<Attendance>();
		String sql = "SELECT * FROM attendance WHERE user_id = ? ORDER BY check_in_time DESC";
		try (Connection conn = DBUtils.getConnection();
	             PreparedStatement pstmt = conn.prepareStatement(sql)) {
	            pstmt.setString(1, userId);
	            try (ResultSet rs = pstmt.executeQuery()) {
	                while (rs.next()) {
	                    records.add(createAttendanceFromResultSet(rs));
	                }
	            }
	        } catch (SQLException e) {
	            e.printStackTrace();
	            throw new RuntimeException("ユーザーの勤怠記録の取得に失敗しました。", e);
	        }
	        return records;
	}
	
	public List<Attendance> findAll() {
        List<Attendance> records = new ArrayList<>();
        String sql = "SELECT * FROM attendance ORDER BY check_in_time DESC";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                records.add(createAttendanceFromResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("全勤怠記録の取得に失敗しました。", e);
        }
        return records;
    }
	
	
	public List<Attendance> findFilteredRecords(String userId, LocalDate startDate, LocalDate endDate) {
        List<Attendance> records = new ArrayList<>();
        StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM attendance WHERE 1=1 ");
        if (userId != null && !userId.isEmpty()) {
            sqlBuilder.append("AND user_id = ? ");
        }
        if (startDate != null) {
            sqlBuilder.append("AND check_in_time >= ? ");
        }
        if (endDate != null) {
            sqlBuilder.append("AND check_in_time < ? ");
        }
        sqlBuilder.append("ORDER BY check_in_time DESC");

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {
            int paramIndex = 1;
            if (userId != null && !userId.isEmpty()) {
                pstmt.setString(paramIndex++, userId);
            }
            if (startDate != null) {
                pstmt.setTimestamp(paramIndex++, Timestamp.valueOf(startDate.atStartOfDay()));
            }
            if (endDate != null) {
                pstmt.setTimestamp(paramIndex++, Timestamp.valueOf(endDate.plusDays(1).atStartOfDay()));
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    records.add(createAttendanceFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("フィルタリングされた勤怠記録の取得に失敗しました。", e);
        }
        return records;
    }
	
	
	public Map<YearMonth, Long> getMonthlyWorkingHours(String userId) {
        Map<YearMonth, Long> monthlyHours = new HashMap<>();
        StringBuilder sqlBuilder = new StringBuilder(
            "SELECT TO_CHAR(check_in_time, 'YYYY-MM') AS month, " +
            "SUM(EXTRACT(EPOCH FROM (check_out_time - check_in_time))) AS total_seconds " +
            "FROM attendance WHERE check_out_time IS NOT NULL ");
        if (userId != null && !userId.isEmpty()) {
            sqlBuilder.append("AND user_id = ? ");
        }
        sqlBuilder.append("GROUP BY month ORDER BY month");

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {
            int paramIndex = 1;
            if (userId != null && !userId.isEmpty()) {
                pstmt.setString(paramIndex++, userId);
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    YearMonth month = YearMonth.parse(rs.getString("month"));
                    long totalSeconds = (long) rs.getDouble("total_seconds");
                    long totalHours = totalSeconds / 3600;
                    monthlyHours.put(month, totalHours);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("月別労働時間の取得に失敗しました。", e);
        }
        return monthlyHours;
    }
	
	public Map<YearMonth, Long> getMonthlyCheckInCounts(String userId) {
        Map<YearMonth, Long> monthlyCounts = new HashMap<>();
        StringBuilder sqlBuilder = new StringBuilder(
            "SELECT TO_CHAR(check_in_time, 'YYYY-MM') AS month, COUNT(*) AS count " +
            "FROM attendance WHERE check_in_time IS NOT NULL ");
        if (userId != null && !userId.isEmpty()) {
            sqlBuilder.append("AND user_id = ? ");
        }
        sqlBuilder.append("GROUP BY month ORDER BY month");

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {
            int paramIndex = 1;
            if (userId != null && !userId.isEmpty()) {
                pstmt.setString(paramIndex++, userId);
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    YearMonth month = YearMonth.parse(rs.getString("month"));
                    monthlyCounts.put(month, rs.getLong("count"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("月別出勤日数の取得に失敗しました。", e);
        }
        return monthlyCounts;
    }
	
	public void addManualAttendance(String userId, LocalDateTime checkIn, LocalDateTime checkOut) {
        String sql = "INSERT INTO attendance (user_id, check_in_time, check_out_time) VALUES (?, ?, ?)";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            pstmt.setTimestamp(2, Timestamp.valueOf(checkIn));
            if (checkOut != null) {
                pstmt.setTimestamp(3, Timestamp.valueOf(checkOut));
            } else {
                pstmt.setNull(3, java.sql.Types.TIMESTAMP);
            }
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("手動による勤怠記録の追加に失敗しました。", e);
        }
    }
	
	public boolean updateManualAttendance(String userId, LocalDateTime oldCheckIn, LocalDateTime oldCheckOut,LocalDateTime newCheckIn, LocalDateTime newCheckOut) {
		String sql = "UPDATE attendance SET user_id = ?, check_in_time = ?, check_out_time = ? WHERE user_id = ? AND check_in_time = ? AND check_out_time " +
            (oldCheckOut == null ? "IS NULL" : "= ?");
		try (Connection conn = DBUtils.getConnection();
				PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, userId);
			pstmt.setTimestamp(2, Timestamp.valueOf(newCheckIn));
			if (newCheckOut != null) {
				pstmt.setTimestamp(3, Timestamp.valueOf(newCheckOut));
				} else {
					pstmt.setNull(3, java.sql.Types.TIMESTAMP);
					}
			pstmt.setString(4, userId);
			pstmt.setTimestamp(5, Timestamp.valueOf(oldCheckIn));
			if (oldCheckOut != null) {
				pstmt.setTimestamp(6, Timestamp.valueOf(oldCheckOut));
				}
			return pstmt.executeUpdate() > 0;
			} catch (SQLException e) {
				e.printStackTrace();
				throw new RuntimeException("手動による勤怠記録の更新に失敗しました。", e);
				}
		}
	
	
	public boolean deleteManualAttendance(String userId, LocalDateTime checkIn, LocalDateTime checkOut) {
        String sql = "DELETE FROM attendance WHERE user_id = ? AND check_in_time = ? AND check_out_time " +
                     (checkOut == null ? "IS NULL" : "= ?");
        
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            pstmt.setTimestamp(2, Timestamp.valueOf(checkIn));
            if (checkOut != null) {
                pstmt.setTimestamp(3, Timestamp.valueOf(checkOut));
            }
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("手動による勤怠記録の削除に失敗しました。", e);
        }
    }
	
	private Attendance createAttendanceFromResultSet(ResultSet rs) throws SQLException {
        Attendance attendance = new Attendance(rs.getString("user_id"));
        attendance.setCheckInTime(rs.getTimestamp("check_in_time").toLocalDateTime());
        Timestamp checkOutTime = rs.getTimestamp("check_out_time");
        if (checkOutTime != null) {
            attendance.setCheckOutTime(checkOutTime.toLocalDateTime());
        }
        return attendance;
    }
}
	
