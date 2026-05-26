package com.scau.campusstudyroomreservationmanagementsystem.controller;

import com.scau.campusstudyroomreservationmanagementsystem.config.UploadStorage;
import com.scau.campusstudyroomreservationmanagementsystem.support.ApiResponse;
import com.scau.campusstudyroomreservationmanagementsystem.support.BusinessException;
import com.scau.campusstudyroomreservationmanagementsystem.support.CurrentUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * 文件上传：注册材料、自习室座位分布图等，保存到项目 uploads 目录并通过 /uploads 访问。
 */
@RestController
@RequestMapping("/api")
public class UploadController {
    private static final Set<String> ALLOWED = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif", "application/pdf");

    private final UploadStorage uploadStorage;

    public UploadController(UploadStorage uploadStorage) {
        this.uploadStorage = uploadStorage;
    }

    @PostMapping("/auth/register/upload")
    public ApiResponse<Map<String, String>> registerUpload(@RequestParam("file") MultipartFile file) throws IOException {
        return uploadInternal(file, "material");
    }

    @PostMapping("/upload")
    public ApiResponse<Map<String, String>> upload(@AuthenticationPrincipal CurrentUser user,
                                                   @RequestParam("file") MultipartFile file,
                                                   @RequestParam(value = "category", defaultValue = "common") String category) throws IOException {
        if (user == null) {
            throw new BusinessException(401, "请先登录");
        }
        return uploadInternal(file, category);
    }

    private ApiResponse<Map<String, String>> uploadInternal(MultipartFile file, String category) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "请选择要上传的文件");
        }
        String contentType = resolveContentType(file);
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new BusinessException(400, "文件大小不能超过 10MB");
        }
        String ext = extensionFor(contentType);
        String url;
        try (var in = file.getInputStream()) {
            url = uploadStorage.save(in, category, ext);
        }
        String filename = url.substring(url.lastIndexOf('/') + 1);
        return ApiResponse.ok(Map.of("url", url, "filename", filename));
    }

    private String resolveContentType(MultipartFile file) {
        String contentType = file.getContentType() == null ? "" : file.getContentType();
        if (ALLOWED.contains(contentType)) {
            return contentType;
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (original.endsWith(".jpg") || original.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (original.endsWith(".png")) {
            return "image/png";
        }
        if (original.endsWith(".webp")) {
            return "image/webp";
        }
        if (original.endsWith(".gif")) {
            return "image/gif";
        }
        if (original.endsWith(".pdf")) {
            return "application/pdf";
        }
        throw new BusinessException(400, "仅支持 JPG/PNG/WEBP/GIF/PDF 格式");
    }

    private String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            case "application/pdf" -> ".pdf";
            default -> "";
        };
    }
}
