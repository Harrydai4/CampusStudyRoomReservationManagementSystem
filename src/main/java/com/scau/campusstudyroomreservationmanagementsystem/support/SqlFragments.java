package com.scau.campusstudyroomreservationmanagementsystem.support;

/**
 * SQL 动态片段安全拼接，避免 Java 文本块与变量拼接时出现 wherer、andr、1group 等粘连。
 */
public final class SqlFragments {

    private SqlFragments() {
    }

    public static String join(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part == null) {
                continue;
            }
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(trimmed);
        }
        return sb.toString();
    }

    /** 检测常见 SQL 拼接错误（用于测试与日志） */
    public static boolean hasGlueBug(String sql) {
        if (sql == null) {
            return false;
        }
        String s = sql.toLowerCase();
        return s.contains("wherer.") || s.contains("andr.")
                || s.matches("(?s).*manager_id=\\d+group\\s+by.*")
                || s.matches("(?s).*\\d+group\\s+by.*");
    }
}
