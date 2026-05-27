package com.scau.campusstudyroomreservationmanagementsystem;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 课设批量灌数：对正在运行的本地服务（MySQL）通过 HTTP API 批量注册学生、创建预约，
 * 数据保留在库中，可在管理端界面核对，并生成报告。
 *
 * 运行（先启动应用连接 MySQL）：
 * mvn test -Dtest=BulkCourseDataLiveTest -Dbulk.seed=true
 *
 * 可选参数：
 * -Dbulk.seed.base=http://localhost:8080
 * -Dbulk.seed.students=30  （注册学生数，默认 20）
 * -Dbulk.seed.reservations=3 （每学生预约条数，默认 2）
 */
@Tag("live")
@EnabledIfSystemProperty(named = "bulk.seed", matches = "true")
class BulkCourseDataLiveTest {

    private static final String BASE = System.getProperty("bulk.seed.base", "http://localhost:8080") + "/api";
    private static final int STUDENT_COUNT = Integer.parseInt(System.getProperty("bulk.seed.students", "20"));
    private static final int RESERVATIONS_PER_STUDENT = Integer.parseInt(System.getProperty("bulk.seed.reservations", "2"));

    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    void seedBulkCourseData() throws Exception {
        if (!serverAlive()) {
            fail("本地服务未启动，请先运行 Spring Boot 应用后再加 -Dbulk.seed=true");
        }
        String batch = "BULK-" + DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now());
        String superToken = adminLogin("superadmin", "super123");
        String adminToken = adminLogin("admin", "admin123");

        Long roomId = num(adminGet(superToken, "/admin/rooms").stream()
                .filter(r -> "LIB-01-A".equals(String.valueOf(r.get("room_code"))))
                .findFirst().orElse(adminGet(superToken, "/admin/rooms").get(0)).get("id"));

        List<String> studentNos = new ArrayList<>();
        List<Long> reservationIds = new ArrayList<>();
        int approved = 0;
        int reserved = 0;
        int cancelled = 0;

        for (int i = 1; i <= STUDENT_COUNT; i++) {
            String stuNoRaw = batch.replace("-", "") + String.format("%03d", i);
            final String stuNo = stuNoRaw.length() > 20 ? stuNoRaw.substring(0, 20) : stuNoRaw;
            try {
                publicPost("/auth/register", Map.of(
                        "studentNo", stuNo, "password", "123456",
                        "name", "【课设批量】" + i, "college", "计算机学院", "major", "软件工程", "grade", "2023"));
            } catch (Exception ignored) {
                // 可能已存在，继续
            }
            List<Map<String, Object>> users = adminGet(superToken, "/admin/users?keyword=" + stuNo);
            if (users.isEmpty()) continue;
            Long uid = num(users.stream().filter(u -> stuNo.equals(String.valueOf(u.get("student_no"))))
                    .findFirst().orElse(users.get(0)).get("id"));
            if (!"APPROVED".equals(String.valueOf(users.stream().filter(u -> stuNo.equals(String.valueOf(u.get("student_no"))))
                    .findFirst().orElse(users.get(0)).get("audit_status")))) {
                adminPost(superToken, "/admin/users/" + uid + "/approve", Map.of("remark", "课设批量审核-" + batch));
                approved++;
            }
            studentNos.add(stuNo);
            String stuToken = studentLogin(stuNo, "123456");

            for (int j = 0; j < RESERVATIONS_PER_STUDENT; j++) {
                LocalDate date = LocalDate.now().plusDays(1 + (i % 5));
                String start = j == 0 ? "09:00" : "14:00";
                String end = j == 0 ? "11:00" : "16:00";
                List<Map<String, Object>> seats = studentGet(stuToken,
                        "/seats/available?roomId=" + roomId + "&date=" + date + "&startTime=" + start + "&endTime=" + end);
                if (seats.isEmpty()) continue;
                Long seatId = num(seats.stream().filter(s -> bool(s.get("available"))).findFirst()
                        .orElse(seats.get(0)).get("id"));
                try {
                    Map<String, Object> res = studentPost(stuToken, "/reservations", Map.of(
                            "roomId", roomId, "seatId", seatId,
                            "reserveDate", date.toString(), "startTime", start, "endTime", end));
                    reservationIds.add(num(res.get("id")));
                    reserved++;
                    if (i % 5 == 0 && j == 0) {
                        studentPost(stuToken, "/reservations/" + res.get("id") + "/cancel", Map.of());
                        cancelled++;
                    }
                } catch (Exception ignored) {
                    // 座位冲突等跳过
                }
            }
        }

        Map<String, Object> reportAll = getJson("/admin/statistics/report?period=month", adminToken);
        Map<String, Object> reportRoom = getJson("/admin/statistics/report?period=month&roomId=" + roomId, adminToken);

        StringBuilder md = new StringBuilder();
        md.append("# 课设批量灌数报告\n\n");
        md.append("- 批次：`").append(batch).append("`\n");
        md.append("- 注册/审核学生：").append(studentNos.size()).append("（新审核 ").append(approved).append("）\n");
        md.append("- 成功预约：").append(reserved).append("\n");
        md.append("- 主动取消（扣50分）：").append(cancelled).append("\n");
        md.append("- 统计自习室 roomId：").append(roomId).append("\n");
        md.append("- 月度汇总 totalReserve：").append(summary(reportAll, "totalReserve")).append("\n");
        md.append("- 单室月度 totalReserve：").append(summary(reportRoom, "totalReserve")).append("\n\n");
        md.append("## 界面核对\n\n");
        md.append("1. 管理端 → 用户管理，搜索 `").append(batch.replace("-", "")).append("`\n");
        md.append("2. 管理端 → 预约，筛选对应学生\n");
        md.append("3. 管理端 → 统计，选择自习室查看单室数据\n");
        md.append("4. 学生端 → 信用积分，查看取消扣分（红色 -50）\n\n");
        md.append("## 学号样例\n\n");
        studentNos.stream().limit(5).forEach(no -> md.append("- `").append(no).append("` / 123456\n"));

        Path out = Path.of("target", "bulk-seed-report.md");
        Files.writeString(out, md.toString());
        System.out.println("批量灌数报告: " + out.toAbsolutePath());
        assertTrue(reserved > 0, "应至少成功创建一条预约");
    }

    private Map<String, Object> postJson(String path, Map<String, Object> body, String token) throws Exception {
        return requestJson("POST", path, body, token);
    }

    private int summary(Map<String, Object> report, String key) {
        Object s = report.get("summary");
        if (s instanceof Map<?, ?> m) {
            return num(m.get(key)).intValue();
        }
        return 0;
    }

    private boolean serverAlive() {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(BASE.replace("/api", "/"))).GET().build();
            return http.send(req, HttpResponse.BodyHandlers.discarding()).statusCode() < 500;
        } catch (Exception e) {
            return false;
        }
    }

    private String adminLogin(String account, String password) throws Exception {
        return String.valueOf(postJson("/admin/auth/login", Map.of("account", account, "password", password), null).get("token"));
    }

    private String studentLogin(String username, String password) throws Exception {
        return String.valueOf(postJson("/auth/login", Map.of("username", username, "password", password), null).get("token"));
    }


    private Map<String, Object> adminPost(String token, String path, Map<String, Object> body) throws Exception {
        return postJson(path, body, token);
    }

    private Map<String, Object> studentPost(String token, String path, Map<String, Object> body) throws Exception {
        return postJson(path, body, token);
    }

    private Map<String, Object> publicPost(String path, Map<String, Object> body) throws Exception {
        return postJson(path, body, null);
    }

    private List<Map<String, Object>> adminGet(String token, String path) throws Exception {
        Map<String, Object> data = getJson(path, token);
        if (data.get("data") instanceof List<?> list) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> cast = (List<Map<String, Object>>) list;
            return cast;
        }
        return List.of();
    }

    private List<Map<String, Object>> studentGet(String token, String path) throws Exception {
        return adminGet(token, path);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getJson(String path, String token) throws Exception {
        return requestJson("GET", path, null, token);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> requestJson(String method, String path, Map<String, Object> body, String token) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(BASE + path))
                .header("Content-Type", "application/json");
        if (token != null) b.header("Authorization", "Bearer " + token);
        if ("GET".equals(method)) {
            b.GET();
        } else {
            b.method(method, HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body == null ? Map.of() : body)));
        }
        HttpResponse<String> res = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
        Map<String, Object> envelope = json.readValue(res.body(), new TypeReference<>() {});
        int code = ((Number) envelope.getOrDefault("code", 500)).intValue();
        if (code != 200) {
            throw new IllegalStateException(method + " " + path + " => " + code + " " + envelope.get("message"));
        }
        Object data = envelope.get("data");
        if (data == null) return Map.of();
        if (data instanceof Map<?, ?> m) return (Map<String, Object>) m;
        return envelope;
    }

    private Long num(Object v) {
        if (v == null) return 0L;
        if (v instanceof Number n) return n.longValue();
        return Long.parseLong(String.valueOf(v));
    }

    private boolean bool(Object v) {
        if (v instanceof Boolean b) return b;
        if (v instanceof Number n) return n.intValue() != 0;
        return "1".equals(String.valueOf(v)) || "true".equalsIgnoreCase(String.valueOf(v));
    }
}
