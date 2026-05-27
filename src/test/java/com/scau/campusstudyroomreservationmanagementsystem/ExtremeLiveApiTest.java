package com.scau.campusstudyroomreservationmanagementsystem;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scau.campusstudyroomreservationmanagementsystem.support.ExtremeTestRecorder;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 对正在运行的本地服务（默认 http://localhost:8080）执行 live 极端测试。
 * 新建数据保留在 MySQL 中，便于界面核对。
 *
 * 运行：mvn test -Dtest=ExtremeLiveApiTest -Dextreme.live=true
 */
@Tag("live")
@EnabledIfSystemProperty(named = "extreme.live", matches = "true")
class ExtremeLiveApiTest {

    private static final String BASE = System.getProperty("extreme.live.base", "http://localhost:8080") + "/api";
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    void runLiveExtremeSuite() throws Exception {
        if (!serverAlive()) {
            fail("本地服务未启动，请先运行应用后再加 -Dextreme.live=true");
        }
        String batch = "LIVE-" + DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now());
        String liveRoomCode = "L" + DateTimeFormatter.ofPattern("MMddHHmmss").format(LocalDateTime.now());
        ExtremeTestRecorder rec = new ExtremeTestRecorder(batch);
        AtomicInteger n = new AtomicInteger(0);

        String superToken = adminLogin("superadmin", "super123");
        String adminToken = adminLogin("admin", "admin123");
        rec.artifact("liveBaseUrl", BASE.replace("/api", ""));

        Long adminId = num(adminGet(superToken, "/admin/admins").stream()
                .filter(a -> "admin".equals(a.get("account"))).findFirst().orElseThrow().get("id"));

        // 1 新建自习室（保留不删）
        Map<String, Object> roomBody = new HashMap<>();
        roomBody.put("roomCode", liveRoomCode);
        roomBody.put("name", "【极端测试-LIVE】自习室-" + batch);
        roomBody.put("location", "Live测试区A栋");
        roomBody.put("floor", "8楼");
        roomBody.put("rowCount", 3);
        roomBody.put("colCount", 4);
        roomBody.put("managerId", adminId);
        roomBody.put("facilities", "空调,WiFi,Live测试");
        roomBody.put("openTime", "07:00:00");
        roomBody.put("closeTime", "22:30:00");
        roomBody.put("status", "OPEN");
        Map<String, Object> room = adminPost(superToken, "/admin/rooms", roomBody);
        Long roomId = num(room.get("id"));
        rec.artifact("liveRoomId", roomId);
        rec.artifact("liveRoomName", room.get("name"));
        rec.pass(n.incrementAndGet(), "LIVE", "新建自习室", "id=" + roomId,
                "管理端→自习室，名称含【极端测试-LIVE】");

        // 2 修改行列触发网格同步
        Map<String, Object> roomUpdate = new HashMap<>(roomBody);
        roomUpdate.put("rowCount", 2);
        roomUpdate.put("colCount", 5);
        roomUpdate.put("name", "【极端测试-LIVE】自习室-2x5-" + batch);
        adminPut(superToken, "/admin/rooms/" + roomId, roomUpdate);
        rec.pass(n.incrementAndGet(), "LIVE", "修改行列2x5", "OK", "管理端→座位管理选该自习室");

        // 3 新建公告
        Map<String, Object> ann = adminPost(superToken, "/admin/announcements", Map.of(
                "title", "【极端测试-LIVE】公告-" + batch,
                "content", "Live 极端测试公告，请管理员与学生端公告页核对。",
                "type", "SYSTEM", "pinned", true, "scope", "GLOBAL", "status", "PUBLISHED"));
        rec.artifact("liveAnnouncementId", ann.get("id"));
        rec.pass(n.incrementAndGet(), "LIVE", "新建公告", "id=" + ann.get("id"), "管理端→公告");

        // 4 新建管理员
        String liveAdminAcc = "live_ext_" + batch.replace("-", "").toLowerCase();
        Map<String, Object> newAdmin = adminPost(superToken, "/admin/admins", Map.of(
                "account", liveAdminAcc, "password", "admin123", "name", "【极端测试-LIVE】管理员", "phone", "13700000000"));
        rec.artifact("liveAdminAccount", liveAdminAcc);
        rec.pass(n.incrementAndGet(), "LIVE", "新建管理员", liveAdminAcc, "管理端→管理员管理");

        // 5 注册+审核学生
        String liveStu = batch.replace("-", "") + "99";
        publicPost("/auth/register", Map.of(
                "studentNo", liveStu, "password", "123456", "name", "【极端测试-LIVE】学生",
                "college", "Live学院", "major", "测试", "grade", "2025"));
        Long stuId = num(jdbcLikeQuery(superToken, liveStu));
        adminPost(superToken, "/admin/users/" + stuId + "/approve", Map.of("remark", "Live极端测试通过"));
        rec.artifact("liveStudentNo", liveStu);
        rec.pass(n.incrementAndGet(), "LIVE", "注册并审核学生", liveStu, "管理端→用户管理");

        String stuToken = studentLogin(liveStu, "123456");
        List<Map<String, Object>> seats = studentGet(stuToken, "/seats/available?roomId=" + roomId
                + "&date=" + java.time.LocalDate.now().plusDays(1) + "&startTime=14:00&endTime=16:00");
        assertFalse(seats.isEmpty());
        Long seatId = num(seats.stream().filter(s -> bool(s.get("is_seat"))).findFirst().orElseThrow().get("id"));
        Map<String, Object> reservation = studentPost(stuToken, "/reservations", Map.of(
                "roomId", roomId, "seatId", seatId,
                "reserveDate", java.time.LocalDate.now().plusDays(1).toString(),
                "startTime", "14:00", "endTime", "16:00"));
        rec.artifact("liveReservationId", reservation.get("id"));
        rec.pass(n.incrementAndGet(), "LIVE", "学生创建预约", "id=" + reservation.get("id"), "学生端→我的预约");

        // 6 反馈
        Map<String, Object> fb = studentPost(stuToken, "/feedback", Map.of(
                "content", "【极端测试-LIVE】反馈-" + batch, "severity", "HIGH", "type", "SUGGESTION", "roomId", roomId));
        adminPut(adminToken, "/admin/feedback/" + fb.get("id"), Map.of(
                "handleResult", "Live测试已处理-" + batch, "status", "DONE"));
        rec.pass(n.incrementAndGet(), "LIVE", "反馈提交与处理", "id=" + fb.get("id"), "管理端→反馈");

        // 7 统计接口冒烟
        for (String p : List.of("day", "week", "month")) {
            adminGet(superToken, "/admin/statistics/report?period=" + p);
            rec.pass(n.incrementAndGet(), "LIVE", "统计报表-" + p, "OK", "");
        }

        rec.artifact("note", "Live 测试数据已保留，请在前端搜索【极端测试-LIVE】或批次 " + batch);
        Path md = Path.of("target", "extreme-live-report.md");
        Path js = Path.of("target", "extreme-live-artifacts.json");
        rec.writeReports(md, js);
        System.out.println("Live 报告: " + md.toAbsolutePath());
        assertEquals(0, rec.unexpectedFailCount());
    }

    private boolean serverAlive() {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(BASE.replace("/api", "/"))).GET().build();
            HttpResponse<Void> res = http.send(req, HttpResponse.BodyHandlers.discarding());
            return res.statusCode() < 500;
        } catch (Exception e) {
            return false;
        }
    }

    private String adminLogin(String account, String password) throws Exception {
        Map<String, Object> body = Map.of("account", account, "password", password);
        Map<String, Object> data = postJson("/admin/auth/login", body, null);
        return String.valueOf(data.get("token"));
    }

    private String studentLogin(String username, String password) throws Exception {
        Map<String, Object> data = postJson("/auth/login", Map.of("username", username, "password", password), null);
        return String.valueOf(data.get("token"));
    }

    private Map<String, Object> adminPost(String token, String path, Map<String, Object> body) throws Exception {
        return postJson(path, body, token);
    }

    private Map<String, Object> adminPut(String token, String path, Map<String, Object> body) throws Exception {
        return requestJson("PUT", path, body, token);
    }

    private Map<String, Object> publicPost(String path, Map<String, Object> body) throws Exception {
        return postJson(path, body, null);
    }

    private Map<String, Object> studentPost(String token, String path, Map<String, Object> body) throws Exception {
        return postJson(path, body, token);
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

    private Map<String, Object> postJson(String path, Map<String, Object> body, String token) throws Exception {
        return requestJson("POST", path, body, token);
    }

    private Map<String, Object> getJson(String path, String token) throws Exception {
        return requestJson("GET", path, null, token);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> requestJson(String method, String path, Map<String, Object> body, String token) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(BASE + path))
                .header("Content-Type", "application/json");
        if (token != null) {
            b.header("Authorization", "Bearer " + token);
        }
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
        if (data == null) {
            return Map.of();
        }
        if (data instanceof Map<?, ?> m) {
            return (Map<String, Object>) m;
        }
        return envelope;
    }

    /** 通过管理端用户列表查 pending 用户 id */
    private Object jdbcLikeQuery(String token, String studentNo) throws Exception {
        List<Map<String, Object>> users = adminGet(token, "/admin/users?keyword=" + studentNo);
        return users.stream().filter(u -> studentNo.equals(String.valueOf(u.get("student_no"))))
                .findFirst().orElseThrow().get("id");
    }

    private static Long num(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        return Long.parseLong(String.valueOf(o));
    }

    private static boolean bool(Object o) {
        if (o instanceof Boolean b) return b;
        if (o instanceof Number n) return n.intValue() != 0;
        return "1".equals(String.valueOf(o)) || "true".equalsIgnoreCase(String.valueOf(o));
    }
}
