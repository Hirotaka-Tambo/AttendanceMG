<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>管理者メニュー</title>
<link rel="stylesheet" href="../style.css">
</head>
<body>
<div class="container">
  <h1>管理者メニュー</h1>
  <p>ようこそ、${user.username}さん(管理者)</p>
  
  <div class = "main-nav">
      <a href = "attendance?action=filter">勤怠履歴管理</a>
      <a href = "users?action=list">ユーザー管理</a>
      <a href = "logout">ログアウト</a>
  </div>
  
  <c:if test="${not empty sessionScope.successMessage}">
      <p class="success-message"><c:out value="${sessionScope.successMessage}"/></p> 
      <c:remove var="successMessage" scope="session"/>
  </c:if>
  
  <h2>勤怠履歴</h2>
  
  <form action="attendance" method= "get" class="filter-form">
       <input type="hidden" name="action" value="filter">
       <div>
          <label for="filterUserId">ユーザーID：</label>
          <input type = "text" id="filterUserId" name="filterUserId" value="<c:outvalue="${param.filterUserId}"/>"> 
       </div>
  </form>
  
</div>

</body>
</html>
