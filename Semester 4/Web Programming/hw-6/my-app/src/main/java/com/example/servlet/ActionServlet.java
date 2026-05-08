package com.example.servlet;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.sql.*;
import com.example.db.Database;

public class ActionServlet extends HttpServlet {
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        String action = request.getParameter("action");
        
        try {
            if ("login".equals(action)) {
                handleLogin(request, response);
            } else if ("logout".equals(action)) {
                handleLogout(request, response);
            } else if ("selectCity".equals(action)) {
                ensureAuthAndRedirect(request, response, () -> handleSelectCity(request, response));
            } else if ("completeRoute".equals(action)) {
                ensureAuthAndRedirect(request, response, () -> handleCompleteRoute(request, response));
            } else if ("backToStep".equals(action)) {
                ensureAuthAndRedirect(request, response, () -> handleBackToStep(request, response));
            } else if ("newRoute".equals(action)) {
                ensureAuthAndRedirect(request, response, () -> handleNewRoute(request, response));
            }
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            if (!response.isCommitted()) {
                response.sendRedirect("route.jsp");
            }
        }
    }
    
    private void handleLogin(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String error = null;
        
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            error = "Username and password required";
        } else {
            try (Connection conn = Database.getConnection()) {
                try (PreparedStatement pstmt = conn.prepareStatement("SELECT id FROM users WHERE username = ? AND password = ?")) {
                    pstmt.setString(1, username);
                    pstmt.setString(2, password);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            int userId = rs.getInt("id");
                            HttpSession session = request.getSession();
                            session.setAttribute("userId", userId);
                            session.setAttribute("username", username);
                            response.sendRedirect("route.jsp");
                            return;
                        } else {
                            error = "Invalid credentials";
                        }
                    }
                }
            } catch (SQLException e) {
                error = "Database error: " + e.getMessage();
            }
        }
        
        request.setAttribute("error", error);
        request.getRequestDispatcher("login.jsp").forward(request, response);
    }
    
    private void handleLogout(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        request.getSession().invalidate();
        response.sendRedirect("login.jsp");
    }
    
    private void handleSelectCity(HttpServletRequest request, HttpServletResponse response) 
            throws SQLException, IOException {
        int userId = (Integer) request.getSession().getAttribute("userId");
        int nextCityId = Integer.parseInt(request.getParameter("nextCity"));
        
        try (Connection conn = Database.getConnection()) {
            int routeId = getOrCreateRoute(conn, userId);
            
            // Update current city
            try (PreparedStatement updateStmt = conn.prepareStatement("UPDATE routes SET current_city_id = ? WHERE id = ?")) {
                updateStmt.setInt(1, nextCityId);
                updateStmt.setInt(2, routeId);
                updateStmt.executeUpdate();
            }
            
            // Add step
            int nextStep = 0;
            try (PreparedStatement maxStmt = conn.prepareStatement("SELECT COALESCE(MAX(step_order), 0) as max_step FROM route_steps WHERE route_id = ?")) {
                maxStmt.setInt(1, routeId);
                try (ResultSet maxRs = maxStmt.executeQuery()) {
                    nextStep = (maxRs.next() ? maxRs.getInt("max_step") : 0) + 1;
                }
            }
            
            try (PreparedStatement insertStmt = conn.prepareStatement("INSERT INTO route_steps(route_id, city_id, step_order) VALUES(?, ?, ?)")) {
                insertStmt.setInt(1, routeId);
                insertStmt.setInt(2, nextCityId);
                insertStmt.setInt(3, nextStep);
                insertStmt.executeUpdate();
            }
        }
        response.sendRedirect("route.jsp");
    }
    
    private void handleCompleteRoute(HttpServletRequest request, HttpServletResponse response) 
            throws SQLException, IOException {
        int userId = (Integer) request.getSession().getAttribute("userId");
        try (Connection conn = Database.getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement("UPDATE routes SET completed = 1 WHERE user_id = ? AND completed = 0")) {
                pstmt.setInt(1, userId);
                pstmt.executeUpdate();
            }
        }
        response.sendRedirect("completed.jsp");
    }
    
    private void handleBackToStep(HttpServletRequest request, HttpServletResponse response) 
            throws SQLException, IOException {
        int userId = (Integer) request.getSession().getAttribute("userId");
        int stepToGoBackTo = Integer.parseInt(request.getParameter("step"));
        
        try (Connection conn = Database.getConnection()) {
            int routeId = getOrCreateRoute(conn, userId);
            
            // Get city at that step
            try (PreparedStatement getCityStmt = conn.prepareStatement("SELECT city_id FROM route_steps WHERE route_id = ? AND step_order = ?")) {
                getCityStmt.setInt(1, routeId);
                getCityStmt.setInt(2, stepToGoBackTo);
                try (ResultSet cityRs = getCityStmt.executeQuery()) {
                    if (cityRs.next()) {
                        int cityId = cityRs.getInt("city_id");
                        
                        // Update current city
                        try (PreparedStatement updateStmt = conn.prepareStatement("UPDATE routes SET current_city_id = ? WHERE id = ?")) {
                            updateStmt.setInt(1, cityId);
                            updateStmt.setInt(2, routeId);
                            updateStmt.executeUpdate();
                        }
                        
                        // Delete steps after this one
                        try (PreparedStatement deleteStmt = conn.prepareStatement("DELETE FROM route_steps WHERE route_id = ? AND step_order > ?")) {
                            deleteStmt.setInt(1, routeId);
                            deleteStmt.setInt(2, stepToGoBackTo);
                            deleteStmt.executeUpdate();
                        }
                    }
                }
            }
        }
        response.sendRedirect("route.jsp");
    }
    
    private void handleNewRoute(HttpServletRequest request, HttpServletResponse response) 
            throws SQLException, IOException {
        Integer userId = (Integer) request.getSession().getAttribute("userId");
        if (userId != null) {
            try (Connection conn = Database.getConnection()) {
                getOrCreateRoute(conn, userId);
                response.sendRedirect("route.jsp");
                return;
            }
        }
        response.sendRedirect("login.jsp");
    }
    
    private int getOrCreateRoute(Connection conn, int userId) throws SQLException {
        // Check if active route exists
        try (PreparedStatement checkStmt = conn.prepareStatement("SELECT id FROM routes WHERE user_id = ? AND completed = 0 LIMIT 1")) {
            checkStmt.setInt(1, userId);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        }
        
        // Create new route
        try (PreparedStatement insertStmt = conn.prepareStatement("INSERT INTO routes(user_id, current_city_id) VALUES(?, 1)")) {
            insertStmt.setInt(1, userId);
            insertStmt.executeUpdate();
        }
        
        // Get last inserted ID (SQLite compatible)
        try (PreparedStatement getIdStmt = conn.prepareStatement("SELECT last_insert_rowid() as id")) {
            try (ResultSet idRs = getIdStmt.executeQuery()) {
                if (idRs.next()) {
                    int routeId = idRs.getInt("id");
                    try (PreparedStatement stepStmt = conn.prepareStatement("INSERT INTO route_steps(route_id, city_id, step_order) VALUES(?, 1, 1)")) {
                        stepStmt.setInt(1, routeId);
                        stepStmt.executeUpdate();
                    }
                    return routeId;
                }
            }
        }
        return -1;
    }
    
    private void ensureAuthAndRedirect(HttpServletRequest request, HttpServletResponse response, 
            RouteAction action) throws SQLException, IOException {
        Integer userId = (Integer) request.getSession().getAttribute("userId");
        if (userId == null) {
            response.sendRedirect("login.jsp");
        } else {
            action.execute();
        }
    }
    
    @FunctionalInterface
    interface RouteAction {
        void execute() throws SQLException, IOException;
    }
}
