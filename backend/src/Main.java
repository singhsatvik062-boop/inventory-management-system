import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Main {

    private static final List<Map<String, Object>> products = new ArrayList<>();
    private static int nextId = 1;

    public static void main(String[] args) throws Exception {

        int port = Integer.parseInt(
                System.getenv().getOrDefault("PORT", "8080"));

        HttpServer server =
                HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/api/products", Main::handleProducts);
        server.createContext("/api/add", Main::handleAdd);
        server.createContext("/api/update", Main::handleUpdate);
        server.createContext("/api/delete", Main::handleDelete);
        server.createContext("/api/search", Main::handleSearch);

        server.setExecutor(null);
        server.start();

        System.out.println("Server running on port " + port);
    }

    private static void handleProducts(HttpExchange exchange)
            throws IOException {

        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            send(exchange, "Method Not Allowed", 405);
            return;
        }

        sendJson(exchange, productsToJson(products));
    }

    private static void handleAdd(HttpExchange exchange)
            throws IOException {

        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            send(exchange, "Method Not Allowed", 405);
            return;
        }

        Map<String, String> data =
                parseForm(readBody(exchange));

        Map<String, Object> p = new HashMap<>();

        p.put("id", nextId++);
        p.put("name", data.getOrDefault("name", ""));
        p.put("category", data.getOrDefault("category", ""));
        p.put("quantity",
                Integer.parseInt(data.getOrDefault("quantity", "0")));
        p.put("price",
                Double.parseDouble(data.getOrDefault("price", "0")));

        products.add(p);

        sendJson(exchange,
                "{\"success\":true,\"message\":\"Product added successfully\"}");
    }

    private static void handleUpdate(HttpExchange exchange)
            throws IOException {

        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            send(exchange, "Method Not Allowed", 405);
            return;
        }

        Map<String, String> data =
                parseForm(readBody(exchange));

        int id =
                Integer.parseInt(data.get("id"));

        for (Map<String, Object> p : products) {

            if ((int) p.get("id") == id) {

                p.put("name", data.get("name"));
                p.put("category", data.get("category"));
                p.put("quantity",
                        Integer.parseInt(data.get("quantity")));
                p.put("price",
                        Double.parseDouble(data.get("price")));

                sendJson(exchange,
                        "{\"success\":true,\"message\":\"Product updated successfully\"}");
                return;
            }
        }

        sendJson(exchange,
                "{\"success\":false,\"message\":\"Product not found\"}");
    }

    private static void handleDelete(HttpExchange exchange)
            throws IOException {

        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            send(exchange, "Method Not Allowed", 405);
            return;
        }

        Map<String, String> data =
                parseForm(readBody(exchange));

        int id =
                Integer.parseInt(data.get("id"));

        boolean removed =
                products.removeIf(p -> (int) p.get("id") == id);

        if (removed) {
            sendJson(exchange,
                    "{\"success\":true,\"message\":\"Product deleted successfully\"}");
        } else {
            sendJson(exchange,
                    "{\"success\":false,\"message\":\"Product not found\"}");
        }
    }

    private static void handleSearch(HttpExchange exchange)
            throws IOException {

        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            send(exchange, "Method Not Allowed", 405);
            return;
        }

        String query = "";

        String request =
                exchange.getRequestURI().getQuery();

        if (request != null && request.startsWith("query=")) {

            query = URLDecoder.decode(
                    request.substring(6),
                    StandardCharsets.UTF_8);
        }

        query = query.toLowerCase();

        List<Map<String, Object>> result =
                new ArrayList<>();

        for (Map<String, Object> p : products) {

            String name =
                    p.get("name").toString().toLowerCase();

            String category =
                    p.get("category").toString().toLowerCase();

            if (name.contains(query) ||
                    category.contains(query)) {

                result.add(p);
            }
        }

        sendJson(exchange, productsToJson(result));
    }

    private static String productsToJson(
            List<Map<String, Object>> list) {

        StringBuilder json =
                new StringBuilder("[");

        boolean first = true;

        for (Map<String, Object> p : list) {

            if (!first) {
                json.append(",");
            }

            json.append("{");
            json.append("\"id\":").append(p.get("id")).append(",");
            json.append("\"name\":\"")
                    .append(escape(p.get("name").toString()))
                    .append("\",");
            json.append("\"category\":\"")
                    .append(escape(p.get("category").toString()))
                    .append("\",");
            json.append("\"quantity\":")
                    .append(p.get("quantity")).append(",");
            json.append("\"price\":")
                    .append(p.get("price"));
            json.append("}");

            first = false;
        }

        json.append("]");

        return json.toString();
    }

    private static String readBody(HttpExchange exchange)
            throws IOException {

        return new String(
                exchange.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8);
    }

    private static Map<String, String> parseForm(String body)
            throws UnsupportedEncodingException {

        Map<String, String> map =
                new HashMap<>();

        if (body == null || body.isEmpty()) {
            return map;
        }

        for (String pair : body.split("&")) {

            String[] parts =
                    pair.split("=", 2);

            String key =
                    URLDecoder.decode(parts[0],
                            StandardCharsets.UTF_8);

            String value =
                    parts.length > 1
                            ? URLDecoder.decode(parts[1],
                            StandardCharsets.UTF_8)
                            : "";

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

        try (OutputStream out =
                     exchange.getResponseBody()) {

            out.write(bytes);
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

        try (OutputStream out =
                     exchange.getResponseBody()) {

            out.write(bytes);
        }
    }

    private static String escape(String value) {

        return value == null
                ? ""
                : value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
