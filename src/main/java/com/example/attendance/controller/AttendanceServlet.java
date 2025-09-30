package com.example.attendance.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
import com.example.attendance.exception.UserOperationException;

@WebServlet("/attendance")
public class AttendanceServlet extends HttpServlet {

    private final AttendanceDAO attendanceDAO = new AttendanceDAO();
    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        User user = session != null ? (User) session.getAttribute("user") : null;

        if (user == null) {
            response.sendRedirect("login.jsp");
            return;
        }

        try {
            String action = request.getParameter("action");
            if ("export_csv".equals(action) && "admin".equals(user.getRole())) {
                exportCsv(request, response);
                return;
            }

            if ("admin".equals(user.getRole())) {
                handleAdminGet(request, response);
            } else {
                handleEmployeeGet(request, response, user);
            }
        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("errorMessage", "予期せぬシステムエラーが発生しました: " + e.getMessage());
            RequestDispatcher rd = request.getRequestDispatcher("/jsp/error.jsp");
            rd.forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        User user = session != null ? (User) session.getAttribute("user") : null;

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
            switch (action) {
                case "check_in":
                    attendanceDAO.checkIn(user.getUsername());
                    session.setAttribute("script", "alert('出勤を記録しました');");
                    break;

                case "check_out":
                    attendanceDAO.checkOut(user.getUsername());
                    session.setAttribute("script", "alert('退勤を記録しました');");
                    break;

                case "add_manual":
                    if (!"admin".equals(user.getRole())) break;
                    handleAddManual(request, session, targetUserId);
                    break;

                case "update_manual":
                    if (!"admin".equals(user.getRole())) break;
                    handleUpdateManual(request, session, targetUserId);
                    break;

                case "delete_manual":
                    if (!"admin".equals(user.getRole())) break;

                    String attendanceIdStr = request.getParameter("attendanceId");
                    if (attendanceIdStr == null || attendanceIdStr.isEmpty()) {
                        session.setAttribute("script", "alert('削除する勤怠記録が指定されていません。');");
                        break;
                    }

                    int attendanceId = Integer.parseInt(attendanceIdStr);
                    boolean deleted = handleDeleteManual(attendanceId);
                    if (deleted) {
                        session.setAttribute("script", "alert('勤怠記録を削除しました。');");
                    } else {
                        session.setAttribute("script",
                                "alert('勤怠記録の削除に失敗しました。IDが見つからないか、データベースエラーです。');");
                    }
                    break;

                default:
                    session.setAttribute("errorMessage", "不明な操作です。");
            }
            Attendance latestRecord = attendanceDAO.getLatestRecord(user.getUsername());
            session.setAttribute("latestRecord", latestRecord);
            
        } catch (DateTimeParseException e) {
            session.setAttribute("script", "alert('日付/時刻の形式が不正です。');");
        } catch (UserOperationException e) {
            session.setAttribute("script", "alert('" + e.getMessage() + "');");
        } catch (NumberFormatException e) {
            session.setAttribute("script", "alert('勤怠記録IDが不正です。');");
        } catch (Exception e) {
            e.printStackTrace();
            session.setAttribute("errorMessage", "予期せぬシステムエラーが発生しました: " + e.getMessage());
            
        }
       
        
        if ("admin".equals(user.getRole())) {
            // 現在のフィルタリング情報をセッションに保存
            session.setAttribute("filterUserId", request.getParameter("filterUserId"));
            session.setAttribute("startDate", request.getParameter("startDate"));
            session.setAttribute("endDate", request.getParameter("endDate"));

            String redirectUrl = request.getContextPath() + "/attendance";
            response.sendRedirect(redirectUrl);
        } else {
            response.sendRedirect(request.getContextPath() + "/attendance");
        }
    }
    
    

    // 管理者/従業員 メソッド処理

    private void handleAdminGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
    	
    	HttpSession session = request.getSession(false);
    	User user = (User) session.getAttribute("user");
    	
    	//管理者自身の出退勤
        Attendance latestRecord = attendanceDAO.getLatestRecord(user.getUsername());
        session.setAttribute("latestRecord", latestRecord); 
        request.setAttribute("latestRecord", latestRecord);

        String filterUserId = request.getParameter("filterUserId");
        String startDateStr = request.getParameter("startDate");
        String endDateStr = request.getParameter("endDate");

        if (filterUserId == null && startDateStr == null && endDateStr == null) {
            // URLパラメータがない場合、セッションからフィルタリング情報を取得
            filterUserId = (String) session.getAttribute("filterUserId");
            startDateStr = (String) session.getAttribute("startDate");
            endDateStr = (String) session.getAttribute("endDate");
        } else {
            // URLパラメータが存在する場合、その情報をセッションに保存
            session.setAttribute("filterUserId", filterUserId);
            session.setAttribute("startDate", startDateStr);
            session.setAttribute("endDate", endDateStr);
        }

        
        LocalDate startDate = null;
        LocalDate endDate = null;

        try {
            if (startDateStr != null && !startDateStr.isEmpty()) startDate = LocalDate.parse(startDateStr);
            if (endDateStr != null && !endDateStr.isEmpty()) endDate = LocalDate.parse(endDateStr);
        } catch (DateTimeParseException e) {
            request.setAttribute("errorMessage", "日付の形式が不正です。");
            RequestDispatcher rd = request.getRequestDispatcher("/jsp/admin_menu.jsp");
            rd.forward(request, response);
            return;
        }

        List<Attendance> allRecords = attendanceDAO.findFilteredRecords(filterUserId, startDate, endDate);
        request.setAttribute("allAttendanceRecords", allRecords);
        

        Map<String, Double> totalHoursByUser = attendanceDAO.getTotalWorkingHoursByUsers(filterUserId, startDate, endDate);

        Map<String, Double> monthlyWorkingHours =
                new TreeMap<>(attendanceDAO.getMonthlyWorkingHours(filterUserId, startDate, endDate));
        Map<String, Long> monthlyCheckInCounts =
                new TreeMap<>(attendanceDAO.getMonthlyCheckInCounts(filterUserId, startDate, endDate));

        
        // ユーザーごとの月別労働時間を取得するロジックを追加
        Map<String, Map<String, Double>> monthlyHoursByUser = new HashMap<>();
        Collection<User> allUsersCollection = userDAO.getAllUsers();
        List<User> userList = new ArrayList<>(allUsersCollection);
        
        for (User u : userList) {
            Map<String, Double> userMonthlyHours = new TreeMap<>(attendanceDAO.getMonthlyWorkingHours(u.getUsername(), startDate, endDate));
            if (!userMonthlyHours.isEmpty()) {
                monthlyHoursByUser.put(u.getUsername(), userMonthlyHours);
            }
        }
        
        request.setAttribute("totalHoursByUser", totalHoursByUser);
        request.setAttribute("monthlyWorkingHours", monthlyWorkingHours);
        request.setAttribute("monthlyCheckInCounts", monthlyCheckInCounts);
        request.setAttribute("hoursPercentage", calculatePercentage(monthlyWorkingHours, 160.0));
        request.setAttribute("daysPercentage", calculatePercentageLong(monthlyCheckInCounts, 20L));
        request.setAttribute("userList", userDAO.getAllUsers());
        request.setAttribute("monthlyHoursByUser", monthlyHoursByUser);

        double standardHours = 160.0;
        long standardDays = 20L;
        request.setAttribute("standardHours", standardHours);
        request.setAttribute("standardDays", standardDays);
        request.setAttribute("hoursPercentage", calculatePercentage(monthlyWorkingHours, standardHours));
        request.setAttribute("daysPercentage", calculatePercentageLong(monthlyCheckInCounts, standardDays));
        
        //JSPにフォワード(受け渡し)
        RequestDispatcher rd = request.getRequestDispatcher("/jsp/admin_menu.jsp");
        rd.forward(request, response);
    }

    private void handleEmployeeGet(HttpServletRequest request, HttpServletResponse response, User user)
            throws ServletException, IOException {
    	HttpSession session = request.getSession(false);
        String userId = user.getUsername();
        
        // 最新の勤怠記録(保存先はsession)
        Attendance latestRecord = attendanceDAO.getLatestRecord(userId);
        session.setAttribute("latestRecord", latestRecord);
        request.setAttribute("latestRecord", latestRecord);

        String startDateStr = request.getParameter("startDate");
        String endDateStr = request.getParameter("endDate");

        if (startDateStr == null && endDateStr == null) {
            startDateStr = (String) session.getAttribute("emp_startDate");
            endDateStr = (String) session.getAttribute("emp_endDate");
        } else {
            session.setAttribute("emp_startDate", startDateStr);
            session.setAttribute("emp_endDate", endDateStr);
        }
        
        LocalDate startDate = null;
        LocalDate endDate = null;

        try {
            if (startDateStr != null && !startDateStr.isEmpty()) startDate = LocalDate.parse(startDateStr);
            if (endDateStr != null && !endDateStr.isEmpty()) endDate = LocalDate.parse(endDateStr);
        } catch (DateTimeParseException e) {
            request.setAttribute("errorMessage", "日付の形式が不正です。");
            RequestDispatcher rd = request.getRequestDispatcher("/jsp/employee_menu.jsp");
            rd.forward(request, response);
            return;
        }

        List<Attendance> userRecords = attendanceDAO.findFilteredRecords(userId, startDate, endDate);

        LocalDate graphEndDate = LocalDate.now();
        LocalDate graphStartDate = graphEndDate.minusMonths(5).withDayOfMonth(1);

        Map<String, Double> monthlyWorkingHours =
                new TreeMap<>(attendanceDAO.getMonthlyWorkingHours(userId, graphStartDate, graphEndDate));
        Map<String, Long> monthlyCheckInCounts =
                new TreeMap<>(attendanceDAO.getMonthlyCheckInCounts(userId, graphStartDate, graphEndDate));
        
        
        // 個人の月ごと(ユーザー絞り込み前の月別労働時間の表を表示するため)
        Map<String, Map<String, Double>> monthlyHoursByUser = new HashMap<>();
        
        // UserDAO.getAllUsers()の戻り値の型をCollectionで受け取る
        Collection<User> allUsersCollection = userDAO.getAllUsers();
        // CollectionをListに変換
        List<User> userList = new ArrayList<>(allUsersCollection);

        
        for (User u : userList) {
         // ユーザーIDごとに月別労働時間を取得
         Map<String, Double> userMonthlyHours = new TreeMap<>(attendanceDAO.getMonthlyWorkingHours(u.getUsername(), startDate, endDate));
         if (!userMonthlyHours.isEmpty()) {
             monthlyHoursByUser.put(u.getUsername(), userMonthlyHours);
         }
     }

        request.setAttribute("attendanceRecords", userRecords);
        request.setAttribute("monthlyWorkingHours", monthlyWorkingHours);
        request.setAttribute("monthlyCheckInCounts", monthlyCheckInCounts);
        request.setAttribute("monthlyHoursByUser", monthlyHoursByUser);
        request.setAttribute("userList", userList);

        double standardHours = 160.0;
        long standardDays = 20L;
        request.setAttribute("standardHours", standardHours);
        request.setAttribute("standardDays", standardDays);
        request.setAttribute("hoursPercentage", calculatePercentage(monthlyWorkingHours, standardHours));
        request.setAttribute("daysPercentage", calculatePercentageLong(monthlyCheckInCounts, standardDays));

        
        RequestDispatcher rd = request.getRequestDispatcher("/jsp/employee_menu.jsp");
        rd.forward(request, response);
    }

    // 計算メソッド(グラフ用)

    private Map<String, Double> calculatePercentage(Map<String, Double> data, double standard) {
        Map<String, Double> result = new HashMap<>();
        for (Map.Entry<String, Double> e : data.entrySet()) {
            result.put(e.getKey(), (e.getValue() / standard) * 100);
        }
        return result;
    }

    private Map<String, Double> calculatePercentageLong(Map<String, Long> data, long standard) {
        Map<String, Double> result = new HashMap<>();
        for (Map.Entry<String, Long> e : data.entrySet()) {
            result.put(e.getKey(), (e.getValue().doubleValue() / standard) * 100);
        }
        return result;
    }

    // 手動勤怠追加
    private void handleAddManual(HttpServletRequest request, HttpSession session, String targetUserId)
            throws UserOperationException {

        String checkInStr = request.getParameter("checkInTime");
        String checkOutStr = request.getParameter("checkOutTime");

        if (checkInStr == null || checkInStr.isEmpty()) {
            session.setAttribute("script", "alert('出勤時間は必須です。');");
            return;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
        LocalDateTime checkIn = LocalDateTime.parse(checkInStr, formatter);
        LocalDateTime checkOut =
                (checkOutStr != null && !checkOutStr.isEmpty()) ? LocalDateTime.parse(checkOutStr, formatter) : null;

        // バリデーションチェック&&ダイアログ表示の分岐
        if (checkOut != null && checkIn.isAfter(checkOut)) {
            session.setAttribute("script", "alert('退勤時間は出勤時間より後である必要があります。');");
        } else if (attendanceDAO.hasTimeOverlap(targetUserId, checkIn, checkOut)) {
            session.setAttribute("script",
                    "alert('追加できませんでした。入力された期間にすでに勤怠記録が存在します。');");
        } else {
            // 全てのバリデーションを通過した場合のみ実行
            attendanceDAO.addManualAttendance(targetUserId, checkIn, checkOut);
            session.setAttribute("script", "alert('勤怠記録を手動で追加しました。');");
        }
    }

    // 手動による更新 
    // *エラーが出るよ(旧:10:00~13:00/新:09:00~14:00)
    private void handleUpdateManual(HttpServletRequest request, HttpSession session, String targetUserId)
            throws UserOperationException {

    	// oldは必要ないけれど、念のため残しておく
        String oldCheckInStr = request.getParameter("oldCheckInTime");
        String oldCheckOutStr = request.getParameter("oldCheckOutTime");
        String newCheckInStr = request.getParameter("newCheckInTime");
        String newCheckOutStr = request.getParameter("newCheckOutTime");
        int attendanceId = Integer.parseInt(request.getParameter("attendanceId"));

        LocalDateTime oldCheckIn = LocalDateTime.parse(oldCheckInStr);
        LocalDateTime oldCheckOut =
                oldCheckOutStr != null && !oldCheckOutStr.isEmpty() ? LocalDateTime.parse(oldCheckOutStr) : null;
        LocalDateTime newCheckIn = LocalDateTime.parse(newCheckInStr);
        LocalDateTime newCheckOut =
                newCheckOutStr != null && !newCheckOutStr.isEmpty() ? LocalDateTime.parse(newCheckOutStr) : null;

        if (newCheckOut != null && newCheckIn.isAfter(newCheckOut)) {
            session.setAttribute("script", "alert('新しい退勤時間は出勤時間より後である必要があります。');");
            return;
        }

        if (attendanceDAO.hasTimeOverlapForUpdate(attendanceId, targetUserId, newCheckIn, newCheckOut)) {
            session.setAttribute("script",
                    "alert('更新できませんでした。入力された期間にすでに他の勤怠記録が存在します。');");
            return;
        }

        if (attendanceDAO.updateManualAttendance(attendanceId,targetUserId, newCheckIn, newCheckOut)) {
            session.setAttribute("script", "alert('勤怠記録を手動で更新しました。');");
        } else {
            session.setAttribute("script", "alert('勤怠記録の更新に失敗しました。');");
        }
    }

    private boolean handleDeleteManual(int attendanceId) {
        return attendanceDAO.deleteManualAttendance(attendanceId);
    }

    // CSV出力

    private void exportCsv(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename=\"attendance_records.csv\"");

        String filterUserId = request.getParameter("filterUserId");
        LocalDate startDate = null;
        LocalDate endDate = null;

        try {
            String startStr = request.getParameter("startDate");
            String endStr = request.getParameter("endDate");
            if (startStr != null && !startStr.isEmpty()) startDate = LocalDate.parse(startStr);
            if (endStr != null && !endStr.isEmpty()) endDate = LocalDate.parse(endStr);
        } catch (DateTimeParseException e) {
            System.err.println("Invalid date format for CSV export: " + e.getMessage());
        }

        List<Attendance> records = attendanceDAO.findFilteredRecords(filterUserId, startDate, endDate);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        Map<String, Long> checkInCounts = new HashMap<>();

        try (PrintWriter writer = response.getWriter()) {
            writer.append("User ID,Date,Check-in Time,Check-out Time,Working Hours\n");
            for (Attendance record : records) {
                writer.append(String.format("\"%s\",\"%s\",\"%s\",\"%s\",%.2f%n",
                        record.getUserId(),
                        record.getCheckInTime() != null ? record.getCheckInTime().toLocalDate().toString() : "",
                        record.getCheckInTime() != null ? record.getCheckInTime().format(formatter) : "",
                        record.getCheckOutTime() != null ? record.getCheckOutTime().format(formatter) : "",
                        record.getWorkingHours()));

                if (record.getUserId() != null && record.getCheckInTime() != null) {
                    checkInCounts.put(record.getUserId(), checkInCounts.getOrDefault(record.getUserId(), 0L) + 1);
                }
            }

            writer.append("\n---\n");
            writer.append("User ID,Total Working Days\n");
            for (Map.Entry<String, Long> entry : checkInCounts.entrySet()) {
                writer.append(String.format("\"%s\",%d%n", entry.getKey(), entry.getValue()));
            }
        }
    }
}
