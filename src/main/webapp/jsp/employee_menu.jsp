<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>従業員メニュー</title>
</head>
<body>
  <div class="container">
     <h1>従業員メニュー</h1>
     <p>ようこそ、${user.username}さん</p>
     
     <c:if test="{$not empty sessionScope.successMessage}">
         <p class="success-message"><c:out value="${sessionScope.successMessage}"/></p>
         <c:remove var="seuccessMessage" scope="session"/>
     </c:if>
  </div>

</body>
</html>
