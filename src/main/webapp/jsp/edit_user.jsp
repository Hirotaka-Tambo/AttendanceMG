<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>ユーザー情報編集</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/style.css">
<script>
    window.onload = function() {
        var script = "${sessionScope.script}";
        if (script && script.trim() !== "") {
            eval(script);
            <c:remove var="script" scope="session"/>
        }
    };
</script>
</head>
<body>
<div class="container">
    <h1>ユーザー情報編集</h1>
    <p>ユーザーID: <c:out value="${userToEdit.username}"/></p>
    
    <div class="main-nav">
        <a href="attendance?action=filter">勤怠履歴管理</a>
        <a href="users?action=list">ユーザー管理</a>
        <a href="logout" class="danger" onclick="return confirm('ログアウトしますか？');">ログアウト</a>
    </div>
    
    <c:if test="${not empty requestScope.script}">
        <script>
            <c:out value="${requestScope.script}" escapeXml="false"/>
        </script>
        <c:remove var="script" scope="session"/>
    </c:if>

    <c:if test="${not empty errorMessage}">
        <p class="error-message"><c:out value="${errorMessage}"/></p>
    </c:if>

    <div class ="card">
    <h2>役割の変更</h2>
    <form action="users" method="post" onsubmit="return confirm('ユーザー「${userToEdit.username}」の役割を更新しますか？');">
        <input type="hidden" name="action" value="update_user">
        <input type="hidden" name="username" value="${userToEdit.username}">
        <p>
            <label for="role">新しい役割:</label>
            <select id="role" name="role">
                <option value="employee" <c:if test="${userToEdit.role == 'employee'}">selected</c:if>>従業員</option>
                <option value="admin" <c:if test="${userToEdit.role == 'admin'}">selected</c:if>>管理者</option>
            </select>
        </p>
        <div class="button-group">
            <input type="submit" value="役割を更新" class="button">
        </div>
    </form>
    </div>
    
    <hr>
    
    <div class ="card">
       <h2>パスワードのリセット</h2>
       <form action="users" method="post" onsubmit="return validatePasswordReset();">
       <input type="hidden" name="action" value="reset_password">
       <input type="hidden" name="username" value="${userToEdit.username}">
      <p>
        <label for="newPassword">新しいパスワード:</label>
        <input type="password" id="newPassword" name="newPassword" required
        pattern="^(?=.*[A-Z])(?=.*[a-z])(?=.*\d)(?=.*[^a-zA-Z0-9]).{8,20}$"
        title="8〜20文字で、大文字・小文字・数字・記号をすべて含む必要があります。"
        placeholder="8~20文字かつ大文字・小文字・数字・記号の全てを含んでください。 例: Abcd1234!">
        
        <span id="newPasswordError" class="error-message"></span>
      </p>
    <div class="button-group">
        <input type="submit" value="パスワードをリセット" class="button danger">
    </div>
    </form>
    </div>
    
    <hr>

    <div class="button-group">
        <a href="users?action=list" class="button denger">ユーザー管理に戻る</a>
    </div>
    
    <footer class="footer">
        <p>&copy; 2025 Hirotaka Tambo In/Out</p>
    </footer>

</div>

<script>
    // パスワードリセットフォームのみを対象。
    function validatePasswordReset() {
        document.getElementById('newPasswordError').innerText = '';

        var newPassword = document.getElementById('newPassword').value;
        var isValid = true;

        if (newPassword === '') {
            document.getElementById('newPasswordError').innerText = '! パスワードを入力してください。';
            isValid = false;
        } else if (newPassword.length < 8 || newPassword.length > 20) {
            document.getElementById('newPasswordError').innerText = 'パスワードは8〜20文字で入力してください。';
            isValid = false;
        } else if (!/[A-Z]/.test(newPassword)) {
            document.getElementById('newPasswordError').innerText = 'パスワードには大文字を含めてください。';
            isValid = false;
        } else if (!/[a-z]/.test(newPassword)) {
            document.getElementById('newPasswordError').innerText = 'パスワードには小文字を含めてください。';
            isValid = false;
        } else if (!/[0-9]/.test(newPassword)) {
            document.getElementById('newPasswordError').innerText = 'パスワードには数字を含めてください。';
            isValid = false;
        } else if (!/[^a-zA-Z0-9]/.test(newPassword)) {
            document.getElementById('newPasswordError').innerText = 'パスワードには記号を含めてください。';
            isValid = false;
        }
                

        if (isValid) {
            return confirm('本当にパスワードをリセットしますか？');
        } else {
            return false;
        }
    }
</script>
</body>
</html>