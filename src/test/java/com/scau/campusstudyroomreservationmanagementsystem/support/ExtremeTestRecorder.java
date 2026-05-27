package com.scau.campusstudyroomreservationmanagementsystem.support;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 极端测试用例记录器：汇总每条操作结果并生成 Markdown/JSON 报告。
 */
public class ExtremeTestRecorder {
    public record CaseResult(
            int no,
            String category,
            String action,
            String status,
            String detail,
            String verifyHint
    ) {}

    private final String batchId;
    private final List<CaseResult> results = new ArrayList<>();
    private final Map<String, Object> artifacts = new LinkedHashMap<>();
    private int pass;
    private int expectedFail;
    private int unexpectedFail;

    public ExtremeTestRecorder(String batchId) {
        this.batchId = batchId;
        artifacts.put("batchId", batchId);
        artifacts.put("createdAt", LocalDateTime.now().toString());
    }

    public String batchId() {
        return batchId;
    }

    public void artifact(String key, Object value) {
        artifacts.put(key, value);
    }

    public Map<String, Object> artifacts() {
        return artifacts;
    }

    public void pass(int no, String category, String action, String detail) {
        pass(no, category, action, detail, "");
    }

    public void pass(int no, String category, String action, String detail, String verifyHint) {
        results.add(new CaseResult(no, category, action, "PASS", detail, verifyHint));
        pass++;
    }

    public void expectedFail(int no, String category, String action, String detail) {
        results.add(new CaseResult(no, category, action, "EXPECTED_FAIL", detail, ""));
        expectedFail++;
    }

    public void fail(int no, String category, String action, String detail) {
        results.add(new CaseResult(no, category, action, "FAIL", detail, ""));
        unexpectedFail++;
    }

    public int unexpectedFailCount() {
        return unexpectedFail;
    }

    public void writeReports(Path mdPath, Path jsonPath) throws IOException {
        Files.createDirectories(mdPath.getParent());
        Files.writeString(mdPath, buildMarkdown(), StandardCharsets.UTF_8);
        Files.writeString(jsonPath, buildJson(), StandardCharsets.UTF_8);
    }

    private String buildMarkdown() {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        StringBuilder sb = new StringBuilder();
        sb.append("# 校园自习室系统 — 极端全角度测试报告\n\n");
        sb.append("- **批次号**: `").append(batchId).append("`\n");
        sb.append("- **生成时间**: ").append(ts).append("\n");
        sb.append("- **通过**: ").append(pass).append(" | **预期失败(负例)**: ").append(expectedFail)
                .append(" | **意外失败**: ").append(unexpectedFail).append("\n");
        sb.append("- **总用例**: ").append(results.size()).append("\n\n");

        if (!artifacts.isEmpty()) {
            sb.append("## 可在界面核对的新建/变更实体\n\n");
            sb.append("| 键 | 值 |\n|---|---|\n");
            for (Map.Entry<String, Object> e : artifacts.entrySet()) {
                if ("batchId".equals(e.getKey()) || "createdAt".equals(e.getKey())) continue;
                sb.append("| ").append(e.getKey()).append(" | `").append(String.valueOf(e.getValue())).append("` |\n");
            }
            sb.append("\n");
        }

        sb.append("## 用例明细\n\n");
        sb.append("| # | 分类 | 操作 | 结果 | 说明 | 界面核对提示 |\n");
        sb.append("|---:|---|---|---|---|---|\n");
        for (CaseResult r : results) {
            sb.append("| ").append(r.no()).append(" | ").append(r.category()).append(" | ")
                    .append(escape(r.action())).append(" | **").append(r.status()).append("** | ")
                    .append(escape(r.detail())).append(" | ").append(escape(r.verifyHint())).append(" |\n");
        }
        sb.append("\n");
        if (unexpectedFail == 0) {
            sb.append("> ✅ 结论：**全部非负例用例通过**，系统在当前批次下行为符合预期。\n");
        } else {
            sb.append("> ❌ 结论：存在 **").append(unexpectedFail).append("** 条意外失败，请查看 FAIL 行。\n");
        }
        return sb.toString();
    }

    private String buildJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"batchId\": \"").append(batchId).append("\",\n");
        sb.append("  \"pass\": ").append(pass).append(",\n");
        sb.append("  \"expectedFail\": ").append(expectedFail).append(",\n");
        sb.append("  \"unexpectedFail\": ").append(unexpectedFail).append(",\n");
        sb.append("  \"artifacts\": ").append(mapToJson(artifacts)).append(",\n");
        sb.append("  \"cases\": [\n");
        for (int i = 0; i < results.size(); i++) {
            CaseResult r = results.get(i);
            sb.append("    {\"no\":").append(r.no())
                    .append(",\"category\":\"").append(escapeJson(r.category()))
                    .append("\",\"action\":\"").append(escapeJson(r.action()))
                    .append("\",\"status\":\"").append(r.status())
                    .append("\",\"detail\":\"").append(escapeJson(r.detail()))
                    .append("\",\"verifyHint\":\"").append(escapeJson(r.verifyHint()))
                    .append("\"}");
            if (i < results.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ]\n}\n");
        return sb.toString();
    }

    private static String mapToJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        int i = 0;
        for (Map.Entry<String, Object> e : map.entrySet()) {
            if (i++ > 0) sb.append(",");
            sb.append("\"").append(escapeJson(e.getKey())).append("\":");
            Object v = e.getValue();
            if (v instanceof Number || v instanceof Boolean) {
                sb.append(v);
            } else {
                sb.append("\"").append(escapeJson(String.valueOf(v))).append("\"");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("|", "\\|").replace("\n", " ");
    }

    private static String escapeJson(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
