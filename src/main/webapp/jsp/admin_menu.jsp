<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>管理者メニュー</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/style.css">
</head>
<body>
<div class="container">
    <h1>勤怠履歴管理</h1>
    <p>ようこそ、${user.username}さん(管理者)</p>

    <div class="main-nav">
        <a href="attendance?action=filter">勤怠履歴管理</a>
        <a href="users?action=list">ユーザー管理</a>
        <a href="logout" class="danger">ログアウト</a>
    </div>

    <c:if test="${not empty sessionScope.successMessage}">
        <p class="success-message">${sessionScope.successMessage}</p>
        <c:remove var="successMessage" scope="session"/>
    </c:if>

    <h2>勤怠履歴</h2>
    <form action="attendance" method="get" class="filter-form">
        <input type="hidden" name="action" value="filter">
        <div>
            <label for="filterUserId">ユーザーID:</label>
            <input type="text" id="filterUserId" name="filterUserId" value="${param.filterUserId}">
        </div>
        <div>
            <label for="startDate">開始日：</label>
            <input type="date" id="startDate" name="startDate" value="${param.startDate}">
        </div>
        <div>
            <label for="endDate">終了日：</label>
            <input type="date" id="endDate" name="endDate" value="${param.endDate}">
        </div>
        <button type="submit" class="button">フィルタ</button>
    </form>

    <p class="error-message">${errorMessage}</p>

    <a href="attendance?action=export_csv&filterUserId=${param.filterUserId}&startDate=${param.startDate}&endDate=${param.endDate}" class="button">
        勤怠履歴をエクスポート
    </a>

    <h3>勤怠サマリー(合計労働時間)</h3>
    <table class="summary-table">
        <thead>
            <tr>
                <th>ユーザーID</th>
                <th>合計労働時間(時間)</th>
            </tr>
        </thead>
        <tbody>
            <c:forEach var="entry" items="${totalHoursByUser}">
                <tr>
                    <td>${entry.key}</td>
                    <td><fmt:formatNumber value="${entry.value}" pattern="#.##"/></td>
                </tr>
            </c:forEach>
            <c:if test="${empty totalHoursByUser}">
                <tr><td colspan="2">データがありません</td></tr>
            </c:if>
        </tbody>
    </table>

    <c:set var="maxHours" value="0"/>
    <c:forEach var="entry" items="${monthlyWorkingHours}">
        <c:if test="${entry.value > maxHours}">
            <c:set var="maxHours" value="${entry.value}"/>
        </c:if>
    </c:forEach>

    <c:set var="maxCount" value="0"/>
    <c:forEach var="entry" items="${monthlyCheckInCounts}">
        <c:if test="${entry.value > maxCount}">
            <c:set var="maxCount" value="${entry.value}"/>
        </c:if>
    </c:forEach>
    
   <h3>月別勤怠グラフ</h3>
    <div style="display: flex; gap: 40px; justify-content: center;">
        <div>
            
            <div class="bar-chart" style="height: 150px;">
                <c:forEach var="entry" items="${monthlyWorkingHours}">
                   <div class="bar-container">
                       <div class="bar hour-bar" style="height: ${maxHours > 0 ? (entry.value / maxHours) * 150 : 0}px;"></div>
                       <span class="value"><fmt:formatNumber value="${entry.value}" pattern="0.00" />時間</span>
                       <span class="label">${entry.key}</span>
                    </div>
                </c:forEach>
                <c:if test="${empty monthlyWorkingHours}">データがありません。</c:if>
            </div>
            <h4>月別労働時間</h4>
        </div>

        <div>
            <div class="bar-chart" style="height: 150px;">
                <c:forEach var="entry" items="${monthlyCheckInCounts}">
                   <div class="bar-container">
                       <div class="bar count-bar" style="height: ${maxCount > 0 ? (entry.value / maxCount) * 150 : 0}px;"></div>
                       <span class="value">${entry.value}日</span>
                       <span class="label">${entry.key}</span>
                    </div>
                </c:forEach>
                <c:if test="${empty monthlyCheckInCounts}">データがありません。</c:if>
            </div>
            <h4>月別出勤日数</h4>
        </div>
    </div>
    
    <c:if test="${empty monthlyWorkingHours and empty monthlyCheckInCounts}">
        <div class="no-data">データがありません。</div>
    </c:if>

    <h3>詳細勤怠履歴</h3>
    <table>
        <thead>
            <tr>
                <th>従業員ID</th>
                <th>出勤時刻</th>
                <th>退勤時刻</th>
                <th>操作</th>
            </tr>
        </thead>
        <tbody>
            <c:forEach var="att" items="${allAttendanceRecords}">
                <tr>
                    <td>${att.userId}</td>
                    <td>${att.checkInTimeStr}</td>
                    <td>${att.checkOutTimeStr}</td>
                    <td class="table-actions">
                        <form action="attendance" method="post" style="display:inline;">
                            <input type="hidden" name="action" value="delete_manual">
                            <input type="hidden" name="userId" value="${att.userId}">
                            <input type="hidden" name="checkInTime" value="${att.checkInTime}">
                            <input type="hidden" name="checkOutTime" value="${att.checkOutTime}">
                            <input type="submit" value="削除" class="button danger"
                                   onclick="return confirm('本当にこの勤怠記録を削除しますか?');">
                        </form>
                    </td>
                </tr>
            </c:forEach>
            <c:if test="${empty allAttendanceRecords}">
                <tr><td colspan="4">データがありません</td></tr>
            </c:if>
        </tbody>
    </table>

    <h2>勤怠記録の手動追加</h2>
    <form action="attendance" method="post">
        <input type="hidden" name="action" value="add_manual">
        <p>
            <label for="manualUserId">ユーザーID:</label>
            <input type="text" id="manualUserId" name="userId" required>
        </p>
        <p>
            <label for="manualCheckInTime">出勤時刻:</label>
            <input type="datetime-local" id="manualCheckInTime" name="checkInTime" required>
        </p>
        <p>
            <label for="manualCheckOutTime">退勤時刻 (任意):</label>
            <input type="datetime-local" id="manualCheckOutTime" name="checkOutTime">
        </p>
        <div class="button-group">
            <input type="submit" value="追加">
        </div>
    </form>
</div>
</body>
</html>