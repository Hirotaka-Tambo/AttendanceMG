package com.example.attendance.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

@WebServlet("/attendance")
public class AttendanceServlet extends HttpServlet {
	
	private final AttendanceDAO attendanceDAO = new AttendanceDAO();
	private final UserDAO userDAO = new UserDAO();

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		HttpSession session = request.getSession(false);
		User user = (User) session.getAttribute("user");
		
		if (user == null) {
			response.sendRedirect("login.jsp");
			return;
		}
		
		String action = request.getParameter("action");
		
		if ("export_csv".equals(action) && "admin".equals(user.getRole())) {
			exportCsv(request, response);
			return;
		}
		
		if ("admin".equals(user.getRole())) {
			String filterUserId = request.getParameter("filterUserId");
			String startDateStr = request.getParameter("startDate");
			String endDateStr = request.getParameter("endDate");
			LocalDate startDate = null;
			LocalDate endDate = null;
			
			try {
				if (startDateStr != null && !startDateStr.isEmpty()) {
					startDate = LocalDate.parse(startDateStr);
				}
				if (endDateStr != null && !endDateStr.isEmpty()) {
					endDate = LocalDate.parse(endDateStr);
				}
			} catch (DateTimeParseException e) {
				request.setAttribute("errorMessage", "日付の形式が不正です。");
			}
			
			List<Attendance> allRecords = attendanceDAO.findFilteredRecords(filterUserId, startDate, endDate);
			request.setAttribute("allAttendanceRecords", allRecords);
			
			// 管理者メニュー用のデータを取得（フィルター対応）
			Map<String, Double> totalHoursByUser = attendanceDAO.getTotalWorkingHoursByUsers(filterUserId, startDate, endDate);
			Map<String, Double> monthlyWorkingHours = attendanceDAO.getMonthlyWorkingHours(filterUserId, startDate, endDate);
			Map<String, Long> monthlyCheckInCounts = attendanceDAO.getMonthlyCheckInCounts(filterUserId, startDate, endDate);
			
			request.setAttribute("totalHoursByUser", totalHoursByUser);
			request.setAttribute("monthlyWorkingHours", monthlyWorkingHours);
			request.setAttribute("monthlyCheckInCounts", monthlyCheckInCounts);
			
			double maxHours = monthlyWorkingHours.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
			long maxCount = monthlyCheckInCounts.values().stream().mapToLong(Long::longValue).max().orElse(1L);

			request.setAttribute("maxHours", maxHours);
			request.setAttribute("maxCount", maxCount);
			
			// 標準値を定義
			double standardHours = 160.0; // 月間標準労働時間
			long standardDays = 20L;      // 月間標準出勤日数

			// パーセンテージ計算用のマップを作成
			Map<String, Double> hoursPercentage = new HashMap<>();
			Map<String, Double> daysPercentage = new HashMap<>();

			for (Map.Entry<String, Double> entry : monthlyWorkingHours.entrySet()) {
			    double percentage = (entry.getValue() / standardHours) * 100;
			    hoursPercentage.put(entry.getKey(), percentage);
			}

			for (Map.Entry<String, Long> entry : monthlyCheckInCounts.entrySet()) {
			    double percentage = (entry.getValue().doubleValue() / standardDays) * 100;
			    daysPercentage.put(entry.getKey(), percentage);
			}

			// JSPに送信
			request.setAttribute("hoursPercentage", hoursPercentage);
			request.setAttribute("daysPercentage", daysPercentage);
			request.setAttribute("standardHours", standardHours);
			request.setAttribute("standardDays", standardDays);

			// デバッグ出力
			System.out.println("労働時間パーセンテージ: " + hoursPercentage);
			System.out.println("出勤日数パーセンテージ: " + daysPercentage);
			
			// 各月の計算された高さも出力
			for (Map.Entry<String, Double> entry : monthlyWorkingHours.entrySet()) {
			    double height = maxHours > 0 ? (entry.getValue() / maxHours) * 150 : 5;
			    
			    // コンソール上の処理(デバッグ用)
			    System.out.println("月: " + entry.getKey() + ", 時間: " + entry.getValue() + ", 計算高さ: " + height + "px");
			}
			
			request.setAttribute("userList", userDAO.getAllUsers());
			
			RequestDispatcher rd = request.getRequestDispatcher("/jsp/admin_menu.jsp");
			rd.forward(request, response);
			
		} else { // 従業員ユーザーの場合
			String userId = user.getUsername();
			List<Attendance> userRecords = attendanceDAO.findByUserId(user.getUsername());
			
			// 最新の勤怠記録を取得
			Attendance latestRecord = null;
			if (!userRecords.isEmpty()) {
				latestRecord = userRecords.get(0);
			}
			
	        LocalDate endDate = LocalDate.now();
	        LocalDate startDate = endDate.minusMonths(5).withDayOfMonth(1); 
			
		    Map<String, Double> monthlyWorkingHours = attendanceDAO.getMonthlyWorkingHours(userId, startDate, endDate);
		    Map<String, Long> monthlyCheckInCounts = attendanceDAO.getMonthlyCheckInCounts(userId, startDate, endDate);
		    
		    double standardHours = 160.0;
		    long standardDays = 20L;
			
		    Map<String, Double> hoursPercentage = new HashMap<>();
		    Map<String, Double> daysPercentage = new HashMap<>();
		    
		    for (Map.Entry<String, Double> entry : monthlyWorkingHours.entrySet()) {
		        double percentage = (entry.getValue() / standardHours) * 100;
		        hoursPercentage.put(entry.getKey(), percentage);
		    }
		    
		    for (Map.Entry<String, Long> entry : monthlyCheckInCounts.entrySet()) {
		        double percentage = (entry.getValue().doubleValue() / standardDays) * 100;
		        daysPercentage.put(entry.getKey(), percentage);
		    }
			
			request.setAttribute("attendanceRecords", userRecords);
			request.setAttribute("latestRecord", latestRecord);
			
		    request.setAttribute("monthlyWorkingHours", monthlyWorkingHours);
		    request.setAttribute("monthlyCheckInCounts", monthlyCheckInCounts);
		    request.setAttribute("hoursPercentage", hoursPercentage);
		    request.setAttribute("daysPercentage", daysPercentage);
		    request.setAttribute("standardHours", standardHours);
		    request.setAttribute("standardDays", standardDays);
			
			RequestDispatcher rd = request.getRequestDispatcher("/jsp/employee_menu.jsp");
			rd.forward(request, response);
		}
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		HttpSession session = request.getSession(false);
		User user = (User) session.getAttribute("user");
		
		if (user == null) {
			response.sendRedirect("login.jsp");
			return;
		}
		
		String action = request.getParameter("action");
		String targetUserId = request.getParameter("targetUserId");
		if (targetUserId == null || targetUserId.isEmpty()) {
		    targetUserId = user.getUsername();
		}
		
		
		
		try {
			if ("check_in".equals(action)) {
				attendanceDAO.checkIn(user.getUsername());
				session.setAttribute("script", "alert('出勤を記録しました');");
			
			} else if ("check_out".equals(action)) {
				attendanceDAO.checkOut(user.getUsername());
				session.setAttribute("script", "alert('退勤を記録しました');");
			
			} else if ("add_manual".equals(action) && "admin".equals(user.getRole())) {
				String checkInStr = request.getParameter("checkInTime");
				String checkOutStr = request.getParameter("checkOutTime");

				// バリデーションチェック
				if (checkInStr == null || checkInStr.isEmpty()) {
					session.setAttribute("script", "alert('出勤時間は必須です。');");
					response.sendRedirect(request.getContextPath() + "/attendance?action=filter");
					return;
				}
				
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
				LocalDateTime checkIn = LocalDateTime.parse(checkInStr, formatter);
				LocalDateTime checkOut = (checkOutStr != null && !checkOutStr.isEmpty())
				    ? LocalDateTime.parse(checkOutStr, formatter)
				    : null;

				
				if (checkOut != null && checkIn.isAfter(checkOut)) {
					session.setAttribute("script", "alert('退勤時間は出勤時間より後である必要があります。');");
					response.sendRedirect(request.getContextPath() + "/attendance?action=filter");
					return;
				}
				
				// 時間重複チェック (DAOにメソッドを実装する必要があります)
				if (attendanceDAO.hasTimeOverlap(targetUserId, checkIn, checkOut)) {
					session.setAttribute("script", "alert('入力された期間にすでに勤怠記録が存在します。');");
					response.sendRedirect(request.getContextPath() + "/attendance?action=filter");
					return;
				}
				
				 System.out.println("add_manual 受信: userId=" + targetUserId);
				 System.out.println("checkIn=" + checkIn);
				 System.out.println("checkOut=" + checkOut);
				
				attendanceDAO.addManualAttendance(targetUserId, checkIn, checkOut);
				session.setAttribute("script", "alert('勤怠記録を手動で追加しました。');");
			
			} else if ("update_manual".equals(action) && "admin".equals(user.getRole())) {
				String oldCheckInStr = request.getParameter("oldCheckInTime");
				String oldCheckOutStr = request.getParameter("oldCheckOutTime");
				String newCheckInStr = request.getParameter("newCheckInTime");
				String newCheckOutStr = request.getParameter("newCheckOutTime");
				
				// バリデーションチェック
				if (newCheckInStr == null || newCheckInStr.isEmpty()) {
					session.setAttribute("script", "alert('新しい出勤時間は必須です。');");
					response.sendRedirect(request.getContextPath() + "/attendance?action=filter");
					return;
				}
				
				LocalDateTime oldCheckIn = LocalDateTime.parse(oldCheckInStr);
				LocalDateTime oldCheckOut = oldCheckOutStr != null && !oldCheckOutStr.isEmpty() ? LocalDateTime.parse(oldCheckOutStr) : null;
				LocalDateTime newCheckIn = LocalDateTime.parse(newCheckInStr);
				LocalDateTime newCheckOut = newCheckOutStr != null && !newCheckOutStr.isEmpty() ? LocalDateTime.parse(newCheckOutStr) : null;

				if (newCheckOut != null && newCheckIn.isAfter(newCheckOut)) {
					session.setAttribute("script", "alert('新しい退勤時間は出勤時間より後である必要があります。');");
					response.sendRedirect(request.getContextPath() + "/attendance?action=filter");
					return;
				}

				// 時間重複チェック（更新対象の記録自体は除外）
				if (attendanceDAO.hasTimeOverlapForUpdate(targetUserId, oldCheckIn, newCheckIn, newCheckOut)) {
					session.setAttribute("script", "alert('入力された期間にすでに他の勤怠記録が存在します。');");
					response.sendRedirect(request.getContextPath() + "/attendance?action=filter");
					return;
				}
				
				if (attendanceDAO.updateManualAttendance(targetUserId, oldCheckIn, oldCheckOut, newCheckIn, newCheckOut)) {
					session.setAttribute("script", "alert('勤怠記録を手動で更新しました。');");
				} else {
					session.setAttribute("script", "alert('勤怠記録の更新に失敗しました。');");
				}
			} else if ("delete_manual".equals(action) && "admin".equals(user.getRole())) {
				LocalDateTime checkIn = LocalDateTime.parse(request.getParameter("checkInTime"));
				LocalDateTime checkOut = request.getParameter("checkOutTime") != null && !request.getParameter("checkOutTime").isEmpty() ? LocalDateTime.parse(request.getParameter("checkOutTime")) : null;
				
				if (attendanceDAO.deleteManualAttendance(targetUserId, checkIn, checkOut)) {
					session.setAttribute("script", "alert('勤怠記録を削除しました。');");
				} else {
					session.setAttribute("script", "alert('勤怠記録の削除に失敗しました。');");
				}
			}
		} catch (DateTimeParseException e) {
			session.setAttribute("script", "alert('日付/時刻の形式が不正です。');");
			System.err.println("日付/時刻の形式が不正です。: " + e.getMessage());
		} catch (Exception e) {
			session.setAttribute("script", "alert('操作中にエラーが発生しました: ' + e.getMessage() + '');");
			System.err.println("操作中にエラーが発生しました: " + e.getMessage());
			e.printStackTrace();
		}
		
		// ユーザーの役割に基づいてリダイレクト先を振り分ける
		if ("admin".equals(user.getRole())) {
		    response.sendRedirect(request.getContextPath() + "/attendance?action=filter");
		} else {
		    response.sendRedirect(request.getContextPath() + "/attendance");
		}
	}
	
	private void exportCsv(HttpServletRequest request, HttpServletResponse response) throws IOException {
		
		response.setContentType("text/csv; charset=UTF-8");
		response.setHeader("Content-Disposition", "attachment;filename=\"attendance_records.csv\"");
		
		PrintWriter writer = response.getWriter();
		writer.append("User ID,Date,Check-in Time,Check-out Time,Working Hours\n");
		
		String filterUserId = request.getParameter("filterUserId");
		String startDatestr = request.getParameter("startDate");
		String endDatestr = request.getParameter("endDate");
		LocalDate startDate = null;
		LocalDate endDate = null;
		
		try {
			if (startDatestr != null && !startDatestr.isEmpty()) {
				startDate = LocalDate.parse(startDatestr);
			}
			if (endDatestr != null && !endDatestr.isEmpty()) {
				endDate = LocalDate.parse(endDatestr);
			}
		} catch (DateTimeParseException e) {
			System.err.println("Invalid date format for CSV export: " + e.getMessage());
		}
		
		List<Attendance> records = attendanceDAO.findFilteredRecords(filterUserId, startDate, endDate);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		
		// 合計出勤日数を計算するためのマップ
	    Map<String, Long> checkInCounts = new HashMap<>();

	    
		for (Attendance record : records) {
	        // 出力する値の数を修正
	        writer.append(String.format("%s,%s,%s,%s,%.2f%n",
	            record.getUserId(),
	            record.getCheckInTime() != null ? record.getCheckInTime().toLocalDate().toString() : "",
	            record.getCheckInTime() != null ? record.getCheckInTime().format(formatter) : "",
	            record.getCheckOutTime() != null ? record.getCheckOutTime().format(formatter) : "",
	            record.getWorkingHours())); // 労働時間をCSVの要素として追加
	     // ユーザーごとの出勤日数をカウント
	        String userId = record.getUserId();
	        if (userId != null && !userId.isEmpty() && record.getCheckInTime() != null) {
	            checkInCounts.put(userId, checkInCounts.getOrDefault(userId, 0L) + 1);
	        }
	    }
		// 合計出勤日数のヘッダーを追加
	    writer.append("\n---\n");
	    writer.append("User ID,Total Working Days\n");

	    // 各ユーザーの合計出勤日数をCSVに出力
	    for (Map.Entry<String, Long> entry : checkInCounts.entrySet()) {
	        writer.append(String.format("%s,%d%n", entry.getKey(), entry.getValue()));
	    }
		writer.flush();
	}
	
}