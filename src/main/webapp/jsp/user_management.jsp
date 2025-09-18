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
      <h3>ようこそ、${user.username}さん(管理者)</h3>
      
      <div class="main-nav">
         <a href="attendance?action=filter">勤怠履歴管理</a>
         <a href="users?action=list">ユーザー管理</a>
         <a href="logout" class="danger" onclick="return confirm('ログアウトしますか？');">ログアウト</a>
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
      
   <div class="card">
    <h2>従業員一覧</h2> 
    <table class="table">
       <thead>
        <tr>
            <th>従業員ID</th>
            <th>役割</th>
            <th>アカウント</th>
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
            
            <td>
                <c:choose>
                    <c:when test="${u.enabled}">
                        <span style="color: green; font-weight: bold;">有効</span>
                    </c:when>
                    <c:otherwise>
                        <span style="color: red; font-weight: bold;">無効</span>
                    </c:otherwise>
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
                
                <c:if test="${u.role != 'admin'}">
                <form action="users" method="post" style="display:inline;" onsubmit="return confirm('ユーザー「${u.username}」を${u.enabled ? '無効化' : '有効化'}しますか？');">
                    <input type="hidden" name="action" value="toggle_enabled">
                    <input type="hidden" name="username" value="${u.username}">
                    <input type="hidden" name="enabled" value="${u.enabled}">
                    <input type="submit" value="${u.enabled ? '無効化' : '有効化'}" class="button ${u.enabled ? 'danger' : ''}">
                </form>
                </c:if>
            </td>
          </tr>
          </c:forEach>
        </tbody>
      </table>
     </div>
     
    <div class="card">
      <h2>ユーザー追加</h2>
      <form action="users" method="post" onsubmit="return validateForm();">
         <input type="hidden" name="action" value="add_user">
          <p>
           <label for="username">ユーザーID:</label>
           <input type="text" id="username" name="username" required maxlength="10" placeholder="1〜10文字の半角英数字で入力してください。 例: user123">
           <span id="usernameError" class="error-message"></span>
          </p>
    
        <p>
           <label for="password">パスワード:</label>
           <input type="password" id="password" name="password" required placeholder="8~20文字かつ大文字・小文字・数字・記号の全てを含んでください。 例: Abcd1234!">
           <span id="passwordError" class="error-message"></span>
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
     
</div>

<script>
    function validateForm() {
        // エラーメッセージをクリア
        document.getElementById('usernameError').innerText = '';
        document.getElementById('passwordError').innerText = '';

        var username = document.getElementById('username').value;
        var password = document.getElementById('password').value;
        var isValid = true;

        // ユーザー名のバリデーション
        if (username.trim() === '') {
            document.getElementById('usernameError').innerText = '! ユーザーIDを入力してください。';
            isValid = false;
        } else if (username.length > 10) {
            document.getElementById('usernameError').innerText = 'ユーザーIDは10文字以内で入力してください。';
            isValid = false;
        } else if (!/^[a-zA-Z0-9]+$/.test(username)) {
            document.getElementById('usernameError').innerText = 'ユーザーIDは半角英数字のみ使用できます。';
            isValid = false;
        }

        // パスワードのバリデーション
        if (password === '') {
            document.getElementById('passwordError').innerText = '! パスワードを入力してください。';
            isValid = false;
        } else if (password.length < 8 || password.length > 20) {
            document.getElementById('passwordError').innerText = 'パスワードは8〜20文字で入力してください。';
            isValid = false;
        } else if (!/[A-Z]/.test(password)) {
            document.getElementById('passwordError').innerText = 'パスワードには大文字を含めてください。';
            isValid = false;
        } else if (!/[a-z]/.test(password)) {
            document.getElementById('passwordError').innerText = 'パスワードには小文字を含めてください。';
            isValid = false;
        } else if (!/[0-9]/.test(password)) {
            document.getElementById('passwordError').innerText = 'パスワードには数字を含めてください。';
            isValid = false;
        } else if (!/[^a-zA-Z0-9]/.test(password)) {
            document.getElementById('passwordError').innerText = 'パスワードには記号を含めてください。';
            isValid = false;
        }
        

        // バリデーションが全て成功したらtrueを返す
        if (isValid) {
            return confirm('このユーザーを追加しますか?');
        } else {
            return false;
        }
    }
</script>

</body>
</html>