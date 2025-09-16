<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8" isErrorPage="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="ja">
<head>
<meta charset="UTF-8">
<title>エラー</title>
</head>
<body>
   <h1>エラーが発生しました</h1>
   <p>申し訳ありませんが、処理中にエラーが発生しました。</p>
   <p>エラーメッセージ:<%= exception.getMessage() %></p>
   <br>
   <hr>
   <br>
   <p>修復しない場合は、お手数ですが開発者までご連絡ください</p>
   <p>メールアドレス:~~~~~~~~~~~~~~~~</p>
   <a href="../login.jsp">ログインページに戻る</a>

</body>
</html>
