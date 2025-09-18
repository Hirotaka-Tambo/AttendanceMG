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
<script>
    window.onload = function() {
        // セッションスコープに "script" という名前の属性があるかチェック
        var script = "${sessionScope.script}";
        if (script && script.trim() !== "") {
            // alert()を実行
            eval(script);
            // ダイアログ表示後、メッセージが残らないようにセッションから削除
            <c:remove var="script" scope="session"/>
        }
    };

    
    function handleLogout() {
        // JSPのC:ifタグを使って、サーバーサイドから未退勤情報を取得する
        var isCheckedIn = <c:if test="${latestRecord != null && latestRecord.checkOutTime == null}">true</c:if><c:if test="${latestRecord == null || latestRecord.checkOutTime != null}">false</c:if>;

        if (isCheckedIn) {
            // 未退勤の場合、警告ダイアログを表示
            return confirm('退勤していませんが、ログアウトしますか？\nAre you sure you want to log out without checking out?');
        } else {
            // 退勤済みの場合、通常の確認ダイアログを表示
            return confirm('ログアウトしますか？\nLog out?');
        }
    }
    

     // 削除確認と完了メッセージを制御する関数
    function handleDeleteConfirmation() {
        const confirmed = confirm('本当にこの勤怠記録を削除しますか？');
        if (confirmed) {
            return true; // フォーム送信
        }
        return false; // 送信キャンセル
    }

     // 編集フォーム表示の関数
     function showEditForm(id, userId, checkInTime, checkOutTime) {
        // 出勤時刻と退勤時刻のフォーマットを調整
        // ISO 8601形式の 'YYYY-MM-DDTHH:mm' に変換
        const formattedCheckIn = checkInTime.substring(0, 16);
        const formattedCheckOut = checkOutTime ? checkOutTime.substring(0, 16) : '';

        document.getElementById('edit-id').value = id;
        document.getElementById('edit-userId').value = userId;
        document.getElementById('edit-checkInTime').value = formattedCheckIn;
        document.getElementById('edit-checkOutTime').value = formattedCheckOut;
        
     　　// フォーム要素を取得
        const editFormCard = document.getElementById('editFormCard');
        
        // フォームを表示
        editFormCard.style.display = 'block';

        // フォームまでスムーズにスクロールする
        editFormCard.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }

    // 編集フォームを非表示にする関数
    function hideEditForm() {
        document.getElementById('editFormCard').style.display = 'none';
    }
     
    // 手動追加の確認関数
    function handleManualAddConfirmation() {
        return confirm('この内容で勤怠記録を追加しますか？');
    }
</script>
</head>
<body>
<div class="container">
    <h1>勤怠履歴管理</h1>
    <h3>ようこそ、${user.username}さん(管理者)</h3>

    <div class="main-nav">
        <a href="attendance?action=filter">勤怠履歴管理</a>
        <a href="users?action=list">ユーザー管理</a>
        <a href="logout" class="danger" onclick="return handleLogout();">ログアウト</a>
    </div>

    <%-- 成功メッセージとエラーメッセージをセッションスコープから取得して表示 --%>
    <c:if test="${not empty sessionScope.successMessage}">
        <p class="success-message">${sessionScope.successMessage}</p>
        <c:remove var="successMessage" scope="session"/>
    </c:if>
    
    <c:if test="${not empty sessionScope.errorMessage}">
        <p class="error-message">${sessionScope.errorMessage}</p>
        <c:remove var="errorMessage" scope="session"/>
    </c:if>
    
    <div class = "card">
    <h2>管理者自身の出勤/退勤ボタン</h2>
    <div class="button-group">
        <c:if test="${latestRecord != null && latestRecord.checkOutTime == null}">
            <form action="attendance" method="post" class="inline-form" onsubmit="return confirm('退勤しますか？');">
                <input type="hidden" name="action" value="check_out">
                <input type="submit" value="退勤" class="button check-out">
            </form>
        </c:if>
        
        <c:if test="${latestRecord == null || latestRecord.checkOutTime != null}">
            <form action="attendance" method="post" class="inline-form" onsubmit="return confirm('出勤しますか？');">
                <input type="hidden" name="action" value="check_in">
                <input type="submit" value="出勤" class="button check-in">
            </form>
        </c:if>
     </div>
    </div>
    
    
   <div class="card">
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

    <a href="attendance?action=export_csv&filterUserId=${param.filterUserId}&startDate=${param.startDate}&endDate=${param.endDate}" class="button">
        勤怠履歴をエクスポート
    </a>
  </div>

  <div class="card">
    <h2>勤怠サマリー</h2>
    <table class="summary-table">
        <thead>
            <tr>
                <th>ユーザーID</th>
                <th>合計労働時間(時間)</th>
                <c:if test="${not empty monthlyWorkingHours}">
                    <c:forEach var="month" items="${monthlyWorkingHours}">
                        <th>${month.key}月</th>
                    </c:forEach>
                </c:if>
            </tr>
        </thead>
        <tbody>
            <c:choose>
                <c:when test="${not empty totalHoursByUser}">
                    <c:forEach var="entry" items="${totalHoursByUser}">
                        <tr>
                            <td>${entry.key}</td>
                            <td><fmt:formatNumber value="${entry.value}" pattern="#.##"/></td>
                            <c:set var="userMonthlyHours" value="${monthlyHoursByUser[entry.key]}" />
                            <c:forEach var="month" items="${monthlyWorkingHours}">
                                <td>
                                    <c:if test="${userMonthlyHours != null && userMonthlyHours[month.key] != null}">
                                        <fmt:formatNumber value="${userMonthlyHours[month.key]}" pattern="#.##"/>
                                    </c:if>
                                </td>
                            </c:forEach>
                        </tr>
                    </c:forEach>
                </c:when>
                <c:otherwise>
                    <tr><td colspan="<c:out value="${monthlyWorkingHours.size() + 2}"/>">データがありません</td></tr>
                </c:otherwise>
            </c:choose>
        </tbody>
    </table>
   </div>
    
       
    
   <div class="card"> 
    <h2>月別勤怠グラフ</h2>
    <div class="chart-container">
      <div class="chart-section">
        <h4>月別労働時間（標準: ${standardHours}時間/1人）</h4>
        <hr>
        <div class="bar-chart">
            <c:forEach var="entry" items="${monthlyWorkingHours}">
               <div class="bar-container">
                   <div class="bar hour-bar" 
                        style="height: ${(entry.value / standardHours) * 150}px;"></div>
                   <span class="value">
                       <fmt:formatNumber value="${entry.value}" pattern="0.0" />h<br>
                       <small>(<fmt:formatNumber value="${hoursPercentage[entry.key]}" pattern="0" />%)</small>
                   </span>
                   <span class="label">${entry.key}月</span>
                </div>
            </c:forEach>
            <c:if test="${empty monthlyWorkingHours}">
                <div class="no-data">データがありません。</div>
            </c:if>
        </div>
    </div>

    <div class="chart-section">
        <h4>月別出勤日数（標準: ${standardDays}日/1人）</h4>
        <hr>
        <div class="bar-chart">
            <c:forEach var="entry" items="${monthlyCheckInCounts}">
               <div class="bar-container">
                   <div class="bar count-bar" 
                        style="height: ${(entry.value / standardDays) * 150}px;"></div>
                   <span class="value">
                       ${entry.value}日<br>
                       <small>(<fmt:formatNumber value="${daysPercentage[entry.key]}" pattern="0" />%)</small>
                   </span>
                   <span class="label">${entry.key}</span>
                </div>
            </c:forEach>
            <c:if test="${empty monthlyCheckInCounts}">
                <div class="no-data">データがありません。</div>
            </c:if>
         </div>
      </div>
     </div>
   </div>
   
   <div class="card">
    <h2>詳細勤怠履歴</h2>
    <table class="summary-table">
        <thead>
            <tr>
                <th>従業員ID</th>
                <th>出勤時刻</th>
                <th>退勤時刻</th>
                <th>労働時間</th>
                <th>操作</th>
            </tr>
        </thead>
        <tbody>
            <c:forEach var="att" items="${allAttendanceRecords}">
                <tr>
                    <td>${att.userId}</td>
                    <td>${att.checkInTimeStr}</td>
                    <td>${att.checkOutTimeStr}</td>
                    <td>
                    <c:if test="${att.checkOutTime != null}">
                        <fmt:formatNumber value="${att.workingHours}" pattern="#.##" />
                    </c:if>
                    </td>
                    <td class="table-actions">
                    <button class="button edit-button" onclick="showEditForm('${att.id}', '${att.userId}', '${att.checkInTime}', '${att.checkOutTime}')">
                    更新
                    </button>
                     <form action="attendance" method="post" style="display:inline;" onsubmit="return handleDeleteConfirmation();">
                       <input type="hidden" name="action" value="delete_manual">
                       <input type="hidden" name="attendanceId" value="${att.id}">
                       <input type="hidden" name="checkInTime" value="${att.checkInTime}">
                       <input type="hidden" name="checkOutTime" value="${att.checkOutTime}">
                       <input type="submit" value="削除" class="button danger">
                     </form>
                    </td>
                </tr>
            </c:forEach>
            <c:if test="${empty allAttendanceRecords}">
                <tr><td colspan="5">データがありません</td></tr>
            </c:if>
        </tbody>
    </table>
  </div>

  <div class = "card">
    <h2>勤怠記録の手動追加</h2>
    <form action="attendance" method="post" onsubmit="return handleManualAddConfirmation();">
        <input type="hidden" name="action" value="add_manual">
        <p>
            <label for="manualUserId">ユーザーID:</label>
            <input type="text" id="manualUserId" name="targetUserId" required>
        </p>
        <p>
            <label for="manualCheckInTime">出勤時刻:</label>
            <input type="datetime-local" id="manualCheckInTime" name="checkInTime" required>
        </p>
        <p>
            <label for="manualCheckOutTime">退勤時刻:</label>
            <input type="datetime-local" id="manualCheckOutTime" name="checkOutTime">
        </p>
        <div class="button-group">
            <input type="submit" value="追加">
        </div>
    </form>
   </div> 
   
   <div class="card" id="editFormCard" style="display:none;">
    <h2>勤怠記録の手動更新</h2>
    <form action="attendance" method="post">
        <input type="hidden" name="action" value="update_manual">
        <input type="hidden" name="attendanceId" id="edit-id">
        <input type="hidden" name="targetUserId" id="edit-userId">
        <p>
            <label>出勤時刻:</label>
            <input type="datetime-local" name="checkInTime" id="edit-checkInTime" required>
        </p>
        <p>
            <label>退勤時刻:</label>
            <input type="datetime-local" name="checkOutTime" id="edit-checkOutTime">
        </p>
        <div class="button-group">
            <input type="submit" value="更新" class="button">
            <button type="button" class="button" onclick="hideEditForm()">キャンセル</button>
        </div>
    </form>
   </div>
   
   <footer class="footer">
        <p>&copy; 2025 Hirotaka Tambo In/Out</p>
    </footer>
    
</div>
</body>
</html>