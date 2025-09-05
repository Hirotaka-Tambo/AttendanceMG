<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>ユーザー管理</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/style.css">
</head>
<body>
   <div class="container">
      <h1>ユーザー管理</h1>
      <p>ようこそ、${user.username}さん(管理者)</p>
      
      <div class="main-nav">
         <a href="attendance?action=filter">勤怠履歴管理</a>
         <a href="users?action=list">ユーザー管理</a>
         <a href="logout">ログアウト</a>
      </div>
      
      <c:if test="${not empty sessionScope.successMessage}">
         <p class="success-message"><c:out value="${sessionScope.successMessage}"/></p>
         <c:remove var="successMessage" scope="session"/>
      </c:if>
      
      <h2>ユーザー追加/編集</h2>
      <form action="users" method="post" class="user-form">
         <!-- action の分岐 -->
         <c:choose>
            <c:when test="${userToEdit != null}">
               <input type="hidden" name="action" value="update">
            </c:when>
            <c:otherwise>
               <input type="hidden" name="action" value="add">
            </c:otherwise>
         </c:choose>

         <label for="username">ユーザーID:</label>
         <input type="text" id="username" name="username"
                value="<c:out value='${userToEdit.username}'/>"
                <c:if test="${userToEdit != null}">readonly</c:if> required>

         <label for="password">パスワード:</label>
         <input type="password" id="password" name="password"
                <c:if test="${userToEdit == null}">required</c:if>>
         <c:if test="${userToEdit != null}">
            <p class="error-message">※編集時はパスワードは変更されません。リセットする場合は別途操作をしてください。</p>
         </c:if>
         
         <label for="role">役割:</label>
         <select id="role" name="role" required>
            <option value="employee" <c:if test="${userToEdit.role == 'employee'}">selected</c:if>>従業員</option>
            <option value="admin" <c:if test="${userToEdit.role == 'admin'}">selected</c:if>>管理者</option>
         </select>
         
         <p>
            <label for="enabled">アカウント有効:</label>
            <input type="checkbox" id="enabled" name="enabled" value="true"
                   <c:if test="${userToEdit == null || userToEdit.enabled}">checked</c:if>>
         </p>
         
         <div class="button-group">
            <input type="submit" value="<c:choose><c:when test='${userToEdit != null}'>更新</c:when><c:otherwise>追加</c:otherwise></c:choose>">
         </div>  
      </form>
   </div>
</body>
</html>
