package com.scau.campusstudyroomreservationmanagementsystem.support;

public record CurrentUser(Long id, String username, String role, String displayName) {
    public boolean isStudent() {
        return "STUDENT".equals(role);
    }

    public boolean isAdmin() {
        return "ADMIN".equals(role) || "SUPER_ADMIN".equals(role);
    }

    public boolean isSuperAdmin() {
        return "SUPER_ADMIN".equals(role);
    }
}
