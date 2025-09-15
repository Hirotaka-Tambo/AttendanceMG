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
            <form action="attendance" method="post" class="inline-form">
                <input type="hidden" name="action" value="check_out">
                <input type="submit" value="退勤" class="button check-out">
            </form>
        </c:if>
        
        <c:if test="${latestRecord == null || latestRecord.checkOutTime != null}">
            <form action="attendance" method="post" class="inline-form">
                <input type="hidden" name="action" value="check_in">
                <input type="submit" value="出勤" class="button check-in">
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
     
     <div class="button-group">
        <a href="logout" class="button danger">ログアウト</a>
     </div>
     
  </div>
</body>
</html>