<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="ja">
<head>
<meta charset="UTF-8">
<title>勤怠管理システム - ログイン</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/style.css">
</head>
<body>
   <div class = "container">
     <h1>勤怠管理システム</h1>
     <form action="login" method="post">
       <p>
       <label for="username">Your ID:</label>
       <input type="text" id="username" name="username" required>
       </p>
   
       <p>
       <label for="password">PassWord:</label>
       <input type="password" id="password" name = "password" required>
       </p>
   
       <div class = "button-group">
           <input type = "submit" value = "ログイン">
       </div>
     </form>
     
     <%-- ログイン失敗時のエラーメッセージ --%>
     <p class="error-message"><c:out value="${errorMessage}"/></p>
     
     
     <%-- 成功メッセージをセッションから取得して表示し、削除 --%>
     <c:if test="${not empty sessionScope.successMessage}">
         <p class="success-message">
         <c:out value="${sessionScope.successMessage}"/></p>
         <c:remove var="successMessage" scope="session"/>
     </c:if>
     
     <br>
     
     <div class="card">
          <h2>審査会用ID/PASSWORD</h2>
          <h4>ID:::admin1       PASSWORD:::Admin##11</h4>
          <h4>ID:::employee1    PASSWORD:::Empass%2%2 *出勤のままログアウト</h4>
          <h4>ID:::employee2    PASSWORD:::loYee33&& *管理者画面で勤怠手動追加/更新のテスト</h4>
          <h4>ID:::employee3    PASSWORD:::Jugyo4!!4Staff *無効化テスト用</h4>
          <h4>ID:::あああ        PASSWORD:::pass *ID/PASSWORDテスト用</h4>
     </div>
     
     <footer class="footer">
        <p>&copy; 2025 Hirotaka Tambo In/Out</p>
    </footer>

</div>

</body>
</html>
