<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>従業員メニュー</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/style.css">
<script>
window.onload = function() {
    var script = "${sessionScope.script}";
    if (script && script.trim() !== "") {
        eval(script);
        // ダイアログ表示後、メッセージが残らないようにセッションから削除
        <c:remove var="script" scope="session"/>
    }
};

function handleLogout() {
    // JSPのC:ifタグを使って、サーバーサイドから未退勤情報を取得する
    var isCheckedIn = <c:if test="${latestRecord != null && latestRecord.checkOutTime == null}">true</c:if><c:if test="${latestRecord == null || latestRecord.checkOutTime != null}">false</c:if>;

    if (isCheckedIn) {
        // 未退勤の場合、警告ダイアログを表示
        return confirm('退勤していませんが、ログアウトしますか？\nAre you sure you want to log out without clocking out?');
    } else {
        // 退勤済みの場合、通常の確認ダイアログを表示
        return confirm('ログアウトしますか？\nLog out?');
    }
}
</script>
</head>
<body>
  <div class="container">
     <h1>従業員メニュー　/　Your Menu</h1>
     <p>ようこそ、${user.username}さん　　/　　Welcome　${user.username}!!</p>
     
     <c:if test="${not empty sessionScope.successMessage}">
         <p class="success-message"><c:out value="${sessionScope.successMessage}"/></p>
         <c:remove var="successMessage" scope="session"/>
     </c:if>
     
     <div class="button-group">
        <c:if test="${latestRecord != null && latestRecord.checkOutTime == null}">
            <form action="attendance" method="post" class="inline-form" onsubmit="return confirm('退勤しますか？\nClock out?');">
                <input type="hidden" name="action" value="check_out">
                <input type="submit" value="退勤　/　Check Out" class="button check-out">
            </form>
        </c:if>
        
        <c:if test="${latestRecord == null || latestRecord.checkOutTime != null}">
            <form action="attendance" method="post" class="inline-form" onsubmit="return confirm('出勤しますか？\nClock in?');">
                <input type="hidden" name="action" value="check_in">
                <input type="submit" value="出勤　/　Check In" class="button check-in">
            </form>
        </c:if>
     </div>
     
     <h2>あなたの勤怠履歴　/　Your Attendance History</h2>
     <table>
        <thead>
           <tr>
              <th>出勤時刻 /　Clock In</th>
              <th>退勤時刻 /　Clock Out</th>
           </tr>
        </thead>
        
        <tbody>
           <c:forEach var="att" items="${attendanceRecords}">
              <tr>
                 <td>
                    <%
                       com.example.attendance.dto.Attendance currentAtt = (com.example.attendance.dto.Attendance) pageContext.getAttribute("att");
                       if (currentAtt.getCheckInTime() != null) {
                           out.print(currentAtt.getCheckInTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")));
                       }
                    %>
                 </td>
                 <td>
                    <%
                       if (currentAtt.getCheckOutTime() != null) {
                           out.print(currentAtt.getCheckOutTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")));
                       }
                    %>
                 </td>
              </tr>
           </c:forEach>
           
           <c:if test="${empty attendanceRecords}">
              <tr><td colspan="2">勤怠記録がありません。</td></tr>
           </c:if>
        </tbody>
     </table>
     
     <h3>月別勤怠グラフ</h3>
    <div class="chart-container">
        <div class="chart-section">
            <h4>月別労働時間（標準: ${standardHours}時間/1人）</h4>
            <div class="bar-chart">
                <c:forEach var="entry" items="${monthlyWorkingHours}">
                   <div class="bar-container">
                       <div class="bar hour-bar" 
                            style="height: ${entry.value > standardHours ? 150 : (entry.value / standardHours) * 150}px;"></div>
                       <span class="value">
                           <fmt:formatNumber value="${entry.value}" pattern="0.0" />h<br>
                           <small>(<fmt:formatNumber value="${hoursPercentage[entry.key]}" pattern="0" />%)</small>
                       </span>
                       <span class="label">${entry.key}</span>
                    </div>
                </c:forEach>
                <c:if test="${empty monthlyWorkingHours}">
                    <div class="no-data">データがありません。</div>
                </c:if>
            </div>
        </div>

        <div class="chart-section">
            <h4>月別出勤日数（標準: ${standardDays}日/1人）</h4>
            <div class="bar-chart">
                <c:forEach var="entry" items="${monthlyCheckInCounts}">
                   <div class="bar-container">
                       <div class="bar count-bar" 
                            style="height: ${entry.value > standardDays ? 150 : (entry.value / standardDays) * 150}px;"></div>
                       <span class="value">
                           ${entry.value}日<br>
                           <small>(<fmt:formatNumber value="${daysPercentage[entry.key]}" pattern="0" />%)</small>
                       </span>
                       <span class="label">${entry.key}</span>
                    </div>
                </c:forEach>
                <c:if test="${empty monthlyCheckInCounts}">
                    <div class="no-data">データがありません。</div>
                </c:if>
            </div>
        </div>
    </div>
     
     <div class="button-group">
        <a href="logout" class="button danger" onclick="return handleLogout();">ログアウト</a>
    </div>
     
  </div>
</body>
</html>