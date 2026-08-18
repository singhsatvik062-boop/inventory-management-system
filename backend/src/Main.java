import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

public class Main {

    private static final String DB_URL =
            "jdbc:mysql://localhost:3306/inventory_db";
    private static final String DB_USER = "root";

    // PUT YOUR MYSQL ROOT PASSWORD HERE
    private static final String DB_PASSWORD = "satvik@12";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    public static void main(String[] args) throws Exception {

        // Test MySQL
        try (Connection conn = getConnection()) {
            System.out.println("MySQL Connected Successfully!");
        }

        HttpServer server =
                HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/api/products", Main::handleProducts);
        server.createContext("/api/add", Main::handleAdd);
        server.createContext("/api/update", Main::handleUpdate);
        server.createContext("/api/delete", Main::handleDelete);
        server.createContext("/api/search", Main::handleSearch);

        server.setExecutor(null);
        server.start();

        System.out.println("======================================");
        System.out.println(" Inventory Management System");
        System.out.println(" Server running at:");
        System.out.println(" http://localhost:8080");
        System.out.println("======================================");
    }

    // GET /api/products
    private static void handleProducts(HttpExchange exchange)
            throws IOException {

        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            send(exchange, "Method Not Allowed", 405);
            return;
        }

        StringBuilder json = new StringBuilder("[");
        boolean first = true;

        String sql = "SELECT * FROM products ORDER BY id";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {

                if (!first) {
                    json.append(",");
                }

                json.append("{");
                json.append("\"id\":").append(rs.getInt("id")).append(",");
                json.append("\"name\":\"")
                        .append(escape(rs.getString("name"))).append("\",");
                json.append("\"category\":\"")
                        .append(escape(rs.getString("category"))).append("\",");
                json.append("\"quantity\":")
                        .append(rs.getInt("quantity")).append(",");
                json.append("\"price\":")
                        .append(rs.getBigDecimal("price"));
                json.append("}");

                first = false;
            }

        } catch (SQLException e) {
            send(exchange, "{\"error\":\"Database error\"}", 500);
            return;
        }

        json.append("]");

        sendJson(exchange, json.toString());
    }

    // POST /api/add
    private static void handleAdd(HttpExchange exchange)
            throws IOException {

        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            send(exchange, "Method Not Allowed", 405);
            return;
        }

        Map<String, String> data =
                parseForm(readBody(exchange));

        String sql =
                "INSERT INTO products (name, category, quantity, price) " +
                "VALUES (?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, data.get("name"));
            stmt.setString(2, data.get("category"));
            stmt.setInt(3, Integer.parseInt(data.get("quantity")));
            stmt.setBigDecimal(4,
                    new java.math.BigDecimal(data.get("price")));

            stmt.executeUpdate();

            sendJson(exchange,
                    "{\"success\":true,\"message\":\"Product added successfully\"}");

        } catch (Exception e) {

            sendJson(exchange,
                    "{\"success\":false,\"message\":\"" +
                            escape(e.getMessage()) + "\"}");
        }
    }

    // POST /api/update
    private static void handleUpdate(HttpExchange exchange)
            throws IOException {

        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            send(exchange, "Method Not Allowed", 405);
            return;
        }

        Map<String, String> data =
                parseForm(readBody(exchange));

        String sql =
                "UPDATE products SET name=?, category=?, quantity=?, price=? " +
                "WHERE id=?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, data.get("name"));
            stmt.setString(2, data.get("category"));
            stmt.setInt(3, Integer.parseInt(data.get("quantity")));
            stmt.setBigDecimal(4,
                    new java.math.BigDecimal(data.get("price")));
            stmt.setInt(5, Integer.parseInt(data.get("id")));

            int rows = stmt.executeUpdate();

            if (rows > 0) {
                sendJson(exchange,
                        "{\"success\":true,\"message\":\"Product updated successfully\"}");
            } else {
                sendJson(exchange,
                        "{\"success\":false,\"message\":\"Product not found\"}");
            }

        } catch (Exception e) {

            sendJson(exchange,
                    "{\"success\":false,\"message\":\"" +
                            escape(e.getMessage()) + "\"}");
        }
    }

    // POST /api/delete
    private static void handleDelete(HttpExchange exchange)
            throws IOException {

        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            send(exchange, "Method Not Allowed", 405);
            return;
        }

        Map<String, String> data =
                parseForm(readBody(exchange));

        String sql = "DELETE FROM products WHERE id=?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, Integer.parseInt(data.get("id")));

            int rows = stmt.executeUpdate();

            if (rows > 0) {
                sendJson(exchange,
                        "{\"success\":true,\"message\":\"Product deleted successfully\"}");
            } else {
                sendJson(exchange,
                        "{\"success\":false,\"message\":\"Product not found\"}");
            }

        } catch (Exception e) {

            sendJson(exchange,
                    "{\"success\":false,\"message\":\"" +
                            escape(e.getMessage()) + "\"}");
        }
    }

    // GET /api/search?query=laptop
    private static void handleSearch(HttpExchange exchange)
            throws IOException {

        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            send(exchange, "Method Not Allowed", 405);
            return;
        }

        String query = "";

        String request = exchange.getRequestURI().getQuery();

        if (request != null && request.startsWith("query=")) {
            query = URLDecoder.decode(
                    request.substring(6),
                    StandardCharsets.UTF_8);
        }

        StringBuilder json = new StringBuilder("[");
        boolean first = true;

        String sql =
                "SELECT * FROM products " +
                "WHERE name LIKE ? OR category LIKE ? " +
                "ORDER BY id";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String value = "%" + query + "%";

            stmt.setString(1, value);
            stmt.setString(2, value);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {

                if (!first) {
                    json.append(",");
                }

                json.append("{");
                json.append("\"id\":").append(rs.getInt("id")).append(",");
                json.append("\"name\":\"")
                        .append(escape(rs.getString("name"))).append("\",");
                json.append("\"category\":\"")
                        .append(escape(rs.getString("category"))).append("\",");
                json.append("\"quantity\":")
                        .append(rs.getInt("quantity")).append(",");
                json.append("\"price\":")
                        .append(rs.getBigDecimal("price"));
                json.append("}");

                first = false;
            }

        } catch (SQLException e) {

            sendJson(exchange,
                    "{\"error\":\"Database error\"}");

            return;
        }

        json.append("]");

        sendJson(exchange, json.toString());
    }

    private static String readBody(HttpExchange exchange)
            throws IOException {

        InputStream input = exchange.getRequestBody();

        return new String(
                input.readAllBytes(),
                StandardCharsets.UTF_8);
    }

    private static Map<String, String> parseForm(String body)
            throws UnsupportedEncodingException {

        Map<String, String> map = new HashMap<>();

        if (body == null || body.isEmpty()) {
            return map;
        }

        String[] pairs = body.split("&");

        for (String pair : pairs) {

            String[] parts = pair.split("=", 2);

            String key = URLDecoder.decode(
                    parts[0],
                    StandardCharsets.UTF_8);

            String value = "";

            if (parts.length > 1) {
                value = URLDecoder.decode(
                        parts[1],
                        StandardCharsets.UTF_8);
            }

            map.put(key, value);
        }

        return map;
    }

    private static void sendJson(
            HttpExchange exchange,
            String response)
            throws IOException {

        exchange.getResponseHeaders()
                .set("Content-Type", "application/json");

        exchange.getResponseHeaders()
                .set("Access-Control-Allow-Origin", "*");

        byte[] bytes =
                response.getBytes(StandardCharsets.UTF_8);

        exchange.sendResponseHeaders(200, bytes.length);

        try (OutputStream output =
                     exchange.getResponseBody()) {

            output.write(bytes);
        }
    }

    private static void send(
            HttpExchange exchange,
            String response,
            int status)
            throws IOException {

        byte[] bytes =
                response.getBytes(StandardCharsets.UTF_8);

        exchange.sendResponseHeaders(status, bytes.length);

        try (OutputStream output =
                     exchange.getResponseBody()) {

            output.write(bytes);
        }
    }

    private static String escape(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}