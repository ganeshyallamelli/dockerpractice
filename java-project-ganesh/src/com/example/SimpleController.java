package com.example;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class SimpleController {
    private static final Path DB_PATH = Paths.get("project-db.json");
    private final Map<Integer, Member> members = Collections.synchronizedMap(new HashMap<Integer, Member>() {{
        put(1, new Member(1, "Ganesh", "Developer"));
        put(2, new Member(2, "Sarath", "Developer"));
    }});
    private final AtomicInteger nextId = new AtomicInteger(3);
    private String projectName = "AI Native Observability";
    private String projectDescription = "AI-driven observability for modern systems.";

    public SimpleController() {
        loadDatabase();
    }

    public void handleRequest(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        String response;
        int status = 200;

        if ("/".equals(path) || path.isEmpty()) {
            response = jsonMessage("AI Native Observability API is running");
        } else if ("/project".equals(path)) {
            if ("GET".equalsIgnoreCase(method)) {
                response = projectJson();
            } else if ("PUT".equalsIgnoreCase(method)) {
                response = updateProject(exchange);
                status = response.startsWith("{\"error\"") ? 400 : 200;
            } else {
                response = errorJson("Method not allowed");
                status = 405;
            }
        } else if ("/members".equals(path) || "/members/".equals(path)) {
            switch (method) {
                case "GET":
                    response = listMembers();
                    break;
                case "POST":
                    response = createMember(exchange);
                    status = response.startsWith("{\"error\"") ? 400 : 201;
                    break;
                default:
                    response = errorJson("Method not allowed");
                    status = 405;
                    break;
            }
        } else if (path.startsWith("/members/")) {
            int memberId = parseId(path);
            if (memberId < 0) {
                response = errorJson("Invalid member id");
                status = 404;
            } else {
                switch (method) {
                    case "GET":
                        response = getMember(memberId);
                        status = response.startsWith("{\"error\"") ? 404 : 200;
                        break;
                    case "PUT":
                        response = updateMember(memberId, exchange);
                        status = response.startsWith("{\"error\"") ? 404 : 200;
                        break;
                    case "DELETE":
                        response = deleteMember(memberId);
                        status = response.startsWith("{\"error\"") ? 404 : 204;
                        break;
                    default:
                        response = errorJson("Method not allowed");
                        status = 405;
                        break;
                }
            }
        } else {
            response = errorJson("Not Found");
            status = 404;
        }

        sendJson(exchange, response, status);
    }

    private void loadDatabase() {
        try {
            if (Files.exists(DB_PATH)) {
                String raw = Files.readString(DB_PATH, StandardCharsets.UTF_8);
                parseDatabase(raw);
            } else {
                seedDatabase();
                saveDatabase();
            }
        } catch (IOException e) {
            // If loading fails, continue with defaults and allow saves later.
        }
    }

    private void seedDatabase() {
        members.clear();
        members.put(1, new Member(1, "Ganesh", "Developer"));
        members.put(2, new Member(2, "Sarath", "Developer"));
        nextId.set(3);
    }

    private void parseDatabase(String raw) {
        String name = extractJsonValue(raw, "projectName");
        if (name != null) {
            projectName = name;
        }
        String description = extractJsonValue(raw, "projectDescription");
        if (description != null) {
            projectDescription = description;
        }

        String memberArray = extractJsonArray(raw, "members");
        if (memberArray != null) {
            List<String> objects = splitJsonObjects(memberArray);
            for (String object : objects) {
                int id = extractJsonInt(object, "id", -1);
                String memberName = extractJsonValue(object, "name");
                String role = extractJsonValue(object, "role");
                if (id > 0 && memberName != null) {
                    members.put(id, new Member(id, memberName, role == null ? "" : role));
                    nextId.set(Math.max(nextId.get(), id + 1));
                }
            }
        }
    }

    private String projectJson() {
        return "{\"projectName\":\"" + escapeJson(projectName) + "\"," +
                "\"projectDescription\":\"" + escapeJson(projectDescription) + "\"," +
                "\"memberCount\":" + members.size() + "}";
    }

    private String updateProject(HttpExchange exchange) throws IOException {
        String body = readBody(exchange);
        String name = extractJsonValue(body, "projectName");
        String description = extractJsonValue(body, "projectDescription");
        if ((name == null || name.isBlank()) && (description == null || description.isBlank())) {
            return errorJson("projectName or projectDescription is required");
        }
        if (name != null && !name.isBlank()) {
            projectName = name.trim();
        }
        if (description != null && !description.isBlank()) {
            projectDescription = description.trim();
        }
        saveDatabase();
        return projectJson();
    }

    private String listMembers() {
        StringBuilder builder = new StringBuilder("[");
        synchronized (members) {
            boolean first = true;
            for (Member member : members.values()) {
                if (!first) {
                    builder.append(',');
                }
                builder.append(member.toJson());
                first = false;
            }
        }
        builder.append(']');
        return builder.toString();
    }

    private String getMember(int memberId) {
        Member member = members.get(memberId);
        if (member == null) {
            return errorJson("Member not found");
        }
        return member.toJson();
    }

    private String createMember(HttpExchange exchange) throws IOException {
        String body = readBody(exchange);
        String name = extractJsonValue(body, "name");
        String role = extractJsonValue(body, "role");
        if (name == null || name.isBlank()) {
            return errorJson("Member name is required");
        }
        int id = nextId.getAndIncrement();
        Member member = new Member(id, name.trim(), role == null ? "Developer" : role.trim());
        members.put(id, member);
        saveDatabase();
        return member.toJson();
    }

    private String updateMember(int memberId, HttpExchange exchange) throws IOException {
        Member member = members.get(memberId);
        if (member == null) {
            return errorJson("Member not found");
        }
        String body = readBody(exchange);
        String name = extractJsonValue(body, "name");
        String role = extractJsonValue(body, "role");
        if ((name == null || name.isBlank()) && (role == null || role.isBlank())) {
            return errorJson("name or role is required");
        }
        if (name != null && !name.isBlank()) {
            member.name = name.trim();
        }
        if (role != null && !role.isBlank()) {
            member.role = role.trim();
        }
        saveDatabase();
        return member.toJson();
    }

    private String deleteMember(int memberId) {
        Member removed = members.remove(memberId);
        if (removed == null) {
            return errorJson("Member not found");
        }
        saveDatabase();
        return jsonMessage("Member deleted");
    }

    private int parseId(String path) {
        String[] parts = path.split("/");
        if (parts.length != 3) {
            return -1;
        }
        try {
            return Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void saveDatabase() {
        StringBuilder builder = new StringBuilder();
        builder.append("{\n");
        builder.append("  \"projectName\": \"").append(escapeJson(projectName)).append("\",\n");
        builder.append("  \"projectDescription\": \"").append(escapeJson(projectDescription)).append("\",\n");
        builder.append("  \"members\": [\n");
        synchronized (members) {
            Iterator<Member> iterator = members.values().iterator();
            while (iterator.hasNext()) {
                builder.append("    ").append(iterator.next().toJson());
                if (iterator.hasNext()) {
                    builder.append(",\n");
                } else {
                    builder.append("\n");
                }
            }
        }
        builder.append("  ]\n");
        builder.append("}");
        try {
            Files.writeString(DB_PATH, builder.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            // ignore persistence failure; API still works in-memory
        }
    }

    private String readBody(HttpExchange exchange) throws IOException {
        try (InputStream input = exchange.getRequestBody()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String extractJsonValue(String json, String key) {
        String token = "\"" + key + "\"";
        int index = json.indexOf(token);
        if (index < 0) {
            return null;
        }
        int colon = json.indexOf(':', index + token.length());
        if (colon < 0) {
            return null;
        }
        int start = json.indexOf('"', colon + 1);
        if (start < 0) {
            return null;
        }
        int end = json.indexOf('"', start + 1);
        if (end < 0) {
            return null;
        }
        return json.substring(start + 1, end);
    }

    private String extractJsonArray(String json, String key) {
        String token = "\"" + key + "\"";
        int index = json.indexOf(token);
        if (index < 0) {
            return null;
        }
        int bracket = json.indexOf('[', index + token.length());
        if (bracket < 0) {
            return null;
        }
        int depth = 1;
        int pos = bracket + 1;
        while (pos < json.length() && depth > 0) {
            char c = json.charAt(pos);
            if (c == '[') {
                depth++;
            } else if (c == ']') {
                depth--;
            }
            pos++;
        }
        if (depth != 0) {
            return null;
        }
        return json.substring(bracket + 1, pos - 1).trim();
    }

    private int extractJsonInt(String json, String key, int fallback) {
        String token = "\"" + key + "\"";
        int index = json.indexOf(token);
        if (index < 0) {
            return fallback;
        }
        int colon = json.indexOf(':', index + token.length());
        if (colon < 0) {
            return fallback;
        }
        int start = colon + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
            start++;
        }
        int end = start;
        while (end < json.length() && Character.isDigit(json.charAt(end))) {
            end++;
        }
        if (start == end) {
            return fallback;
        }
        try {
            return Integer.parseInt(json.substring(start, end));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private List<String> splitJsonObjects(String arrayBody) {
        List<String> objects = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < arrayBody.length(); i++) {
            char c = arrayBody.charAt(i);
            if (c == '{') {
                if (depth == 0) {
                    start = i;
                }
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    objects.add(arrayBody.substring(start, i + 1));
                }
            }
        }
        return objects;
    }

    private String errorJson(String message) {
        return "{\"error\":\"" + escapeJson(message) + "\"}";
    }

    private String jsonMessage(String message) {
        return "{\"message\":\"" + escapeJson(message) + "\"}";
    }

    private String escapeJson(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private void sendJson(HttpExchange exchange, String response, int status) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private class Member {
        int id;
        String name;
        String role;

        Member(int id, String name) {
            this(id, name, "Developer");
        }

        Member(int id, String name, String role) {
            this.id = id;
            this.name = name;
            this.role = role;
        }

        String toJson() {
            return "{\"id\":" + id + ",\"name\":\"" + escapeJson(name) + "\",\"role\":\"" + escapeJson(role) + "\"}";
        }
    }
}
