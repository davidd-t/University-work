<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="java.sql.*" %>
<%@ page import="com.example.db.Database" %>
<%
    Integer userId = (Integer) session.getAttribute("userId");
    String username = (String) session.getAttribute("username");
    
    if (userId == null) {
        response.sendRedirect("login.jsp");
        return;
    }
    
    try (Connection conn = Database.getConnection()) {
        PreparedStatement routeStmt = conn.prepareStatement("SELECT id FROM routes WHERE user_id = ? AND completed = 1 ORDER BY id DESC LIMIT 1");
        routeStmt.setInt(1, userId);
        ResultSet routeRs = routeStmt.executeQuery();
        
        if (routeRs.next()) {
            int routeId = routeRs.getInt("id");
            PreparedStatement stepsStmt = conn.prepareStatement("SELECT city_id FROM route_steps WHERE route_id = ? ORDER BY step_order");
            stepsStmt.setInt(1, routeId);
            ResultSet stepsRs = stepsStmt.executeQuery();
%>
<!DOCTYPE html>
<html>
<head>
    <title>Route Completed</title>
    <link rel="stylesheet" href="style.css">
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>Route Complete!</h1>
            <div class="user-info">
                <form method="POST" action="action" style="display:inline;">
                    <input type="hidden" name="action" value="logout">
                    <button type="submit" class="btn btn-logout">Logout</button>
                </form>
            </div>
        </div>
        
        <div class="section">
            <div class="route-path" style="font-weight: bold; font-size: 16px; padding: 15px; background: #e8f5e9;">
                <% 
                    String separator = "";
                    while (stepsRs.next()) {
                        int cityId = stepsRs.getInt("city_id");
                        PreparedStatement cityStmt = conn.prepareStatement("SELECT name FROM cities WHERE id = ?");
                        cityStmt.setInt(1, cityId);
                        ResultSet cityRs = cityStmt.executeQuery();
                        String cityName = cityRs.next() ? cityRs.getString("name") : "?";
                %>
                    <%= separator %><%= cityName %>
                <%
                        separator = " → ";
                    }
                %>
            </div>
            <p style="margin-top: 15px;">
                <form method="POST" action="action" style="display:inline;">
                    <input type="hidden" name="action" value="newRoute">
                    <button type="submit" class="btn">Start New Route</button>
                </form>
            </p>
        </div>
    </div>
</body>
</html>
<%
        } else {
            out.println("<div class='container'><p>No completed routes.</p></div>");
        }
    } catch (SQLException e) {
        out.println("Error: " + e.getMessage());
    }
%>
