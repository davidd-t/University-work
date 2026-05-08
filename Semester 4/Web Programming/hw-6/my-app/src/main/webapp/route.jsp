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
        // Get or create active route
        String routeQuery = "SELECT id, current_city_id FROM routes WHERE user_id = ? AND completed = 0 LIMIT 1";
        PreparedStatement routeStmt = conn.prepareStatement(routeQuery);
        routeStmt.setInt(1, userId);
        ResultSet routeRs = routeStmt.executeQuery();
        
        if (!routeRs.next()) {
            // Create new route
            PreparedStatement insertStmt = conn.prepareStatement("INSERT INTO routes(user_id, current_city_id) VALUES(?, 1)", Statement.RETURN_GENERATED_KEYS);
            insertStmt.setInt(1, userId);
            insertStmt.executeUpdate();
            ResultSet generatedKeys = insertStmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                int routeId = generatedKeys.getInt(1);
                PreparedStatement stepStmt = conn.prepareStatement("INSERT INTO route_steps(route_id, city_id, step_order) VALUES(?, 1, 1)");
                stepStmt.setInt(1, routeId);
                stepStmt.executeUpdate();
            }
            routeStmt = conn.prepareStatement(routeQuery);
            routeStmt.setInt(1, userId);
            routeRs = routeStmt.executeQuery();
            routeRs.next();
        }
        
        int routeId = routeRs.getInt("id");
        int currentCityId = routeRs.getInt("current_city_id");
        
        // Get current city name
        PreparedStatement cityStmt = conn.prepareStatement("SELECT name FROM cities WHERE id = ?");
        cityStmt.setInt(1, currentCityId);
        ResultSet cityRs = cityStmt.executeQuery();
        String currentCity = cityRs.next() ? cityRs.getString("name") : "";
        
        // Get neighboring cities
        PreparedStatement neighborStmt = conn.prepareStatement("SELECT id, name FROM cities WHERE id IN (SELECT to_city_id FROM connections WHERE from_city_id = ?)");
        neighborStmt.setInt(1, currentCityId);
        ResultSet neighborRs = neighborStmt.executeQuery();
%>
<!DOCTYPE html>
<html>
<head>
    <title>Transportation Route System</title>
    <link rel="stylesheet" href="style.css">
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>Transportation Route</h1>
            <div class="user-info">
                <span><%= username %></span>
                <form method="POST" action="action" style="display:inline;">
                    <input type="hidden" name="action" value="logout">
                    <button type="submit" class="btn btn-logout">Logout</button>
                </form>
            </div>
        </div>
        
        <div class="section">
            <h2>Current Station</h2>
            <div class="current-station"><%= currentCity %></div>
        </div>
        
        <div class="section">
            <h2>Next Destination</h2>
            <div class="neighbors">
                <% while (neighborRs.next()) { %>
                    <form method="POST" action="action" style="display:inline;">
                        <input type="hidden" name="action" value="selectCity">
                        <input type="hidden" name="nextCity" value="<%= neighborRs.getInt("id") %>">
                        <button type="submit" class="btn"><%= neighborRs.getString("name") %></button>
                    </form>
                <% } %>
            </div>
        </div>
        
        <div class="section">
            <h2>Your Route</h2>
            <div class="route-path">
                <%
                    String stepsQuery = "SELECT city_id, step_order FROM route_steps WHERE route_id = ? ORDER BY step_order";
                    PreparedStatement stepsStmt = conn.prepareStatement(stepsQuery);
                    stepsStmt.setInt(1, routeId);
                    ResultSet stepsRs = stepsStmt.executeQuery();
                    String separator = "";
                    while (stepsRs.next()) {
                        int stepCityId = stepsRs.getInt("city_id");
                        int stepOrder = stepsRs.getInt("step_order");
                        PreparedStatement scStmt = conn.prepareStatement("SELECT name FROM cities WHERE id = ?");
                        scStmt.setInt(1, stepCityId);
                        ResultSet scRs = scStmt.executeQuery();
                        String stepCityName = scRs.next() ? scRs.getString("name") : "?";
                %>
                    <%= separator %><%= stepCityName %>
                    <form method="POST" action="action" style="display:inline; margin: 0 5px;">
                        <input type="hidden" name="action" value="backToStep">
                        <input type="hidden" name="step" value="<%= stepOrder %>">
                        <button type="submit" class="btn btn-small" onclick="return confirm('Go back to <%= stepCityName %>?')">↶</button>
                    </form>
                <%
                        separator = " → ";
                    }
                %>
            </div>
        </div>
        
        <div class="section">
            <form method="POST" action="action">
                <input type="hidden" name="action" value="completeRoute">
                <button type="submit" class="btn" onclick="return confirm('Complete route?')">✓ Complete Route</button>
            </form>
        </div>
    </div>
</body>
</html>
<%
    } catch (SQLException e) {
        out.println("Error: " + e.getMessage());
    }
%>
