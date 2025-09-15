<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>ユーザー管理</title>
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
</script>
</head>
<body>
   <div class="container">
      <h1>ユーザー管理</h1>
      <p>ようこそ、${user.username}さん(管理者)</p>
      
      <div class="main-nav">
         <a href="attendance?action=filter">勤怠履歴管理</a>
         <a href="users?action=list">ユーザー管理</a>
         <a href="logout" class="danger">ログアウト</a>
      </div>
      
      <c:if test="${not empty successMessage}">
         <p class="success-message"><c:out value="${successMessage}"/></p>
         <c:remove var="successMessage" scope="session"/>
      </c:if>
      <c:if test="${not empty errorMessage}">
         <p class="error-message"><c:out value="${errorMessage}"/></p>
         <c:remove var="errorMessage" scope="session"/>
      </c:if>

      <c:if test="${not empty requestScope.script}">
          <script>
              <c:out value="${requestScope.script}" escapeXml="false"/>
          </script>
          <c:remove var="script" scope="session"/>
      </c:if>
      
    <h2>従業員一覧</h2> 
    <table class="table">
       <thead>
        <tr>
            <th>従業員ID</th>
            <th>役割</th>
            <th>アクション</th>
        </tr>
       </thead>
       <tbody>
          <c:forEach var="u" items="${userList}">
          <tr>
            <td>${u.username}</td>
            <td>
                <c:choose>
                   <c:when test="${u.role == 'admin'}">管理者</c:when>
                   <c:when test="${u.role == 'employee'}">従業員</c:when>
                   <c:otherwise>${u.role}</c:otherwise>
                </c:choose>
            </td>
            <td class="table-actions">
               <form action="users" method="get">
                  <input type="hidden" name="action" value="edit_user">
                  <input type="hidden" name="username" value="${u.username}">
                  <input type="submit" value="編集" class="button">
               </form>
               
                <form action="users" method="post" onsubmit="return confirm('本当にこのユーザーを削除しますか？');">
                    <input type="hidden" name="action" value="delete_user">
                    <input type="hidden" name="username" value="${u.username}">
                    <input type="submit" value="削除" class="button danger">
                </form>
            </td>
          </tr>
          </c:forEach>
        </tbody>
      </table>
    
    
    <h2>ユーザー追加</h2>
    <form action="users" method="post" onsubmit="return confirm('このユーザーを追加しますか?');">
        <input type="hidden" name="action" value="add_user">
        <p>
            <label for="username">ユーザーID:</label>
            <input type="text" id="username" name="username" required>
        </p>
        <p>
            <label for="password">パスワード:</label>
            <input type="password" id="password" name="password" required>
        </p>
        <p>
            <label for="role">役割:</label>
            <select id="role" name="role">
                <option value="employee">従業員</option>
                <option value="admin">管理者</option>
            </select>
        </p>
        <div class="button-group">
            <input type="submit" value="ユーザー追加" class="button">
        </div>
    </form>
     
</div>
</body>
</html>