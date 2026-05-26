package com.scau.campusstudyroomreservationmanagementsystem.controller;

import com.scau.campusstudyroomreservationmanagementsystem.support.ApiResponse;
import com.scau.campusstudyroomreservationmanagementsystem.support.BusinessException;
import com.scau.campusstudyroomreservationmanagementsystem.support.CurrentUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 文件上传：注册材料、自习室座位分布图等，保存到 app.upload.dir 并通过 /uploads 访问。
 */
@RestController
@RequestMapping("/api")
public class UploadController {
    private static final Set<String> ALLOWED = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif", "application/pdf");

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

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
        String contentType = file.getContentType() == null ? "" : file.getContentType();
        if (!ALLOWED.contains(contentType)) {
            throw new BusinessException(400, "仅支持 JPG/PNG/WEBP/GIF/PDF 格式");
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new BusinessException(400, "文件大小不能超过 10MB");
        }
        String ext = switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            case "application/pdf" -> ".pdf";
            default -> "";
        };
        Path dir = Paths.get(uploadDir, sanitize(category));
        Files.createDirectories(dir);
        String filename = UUID.randomUUID() + ext;
        Path target = dir.resolve(filename);
        file.transferTo(target.toFile());
        String url = "/uploads/" + sanitize(category) + "/" + filename;
        return ApiResponse.ok(Map.of("url", url, "filename", filename));
    }

    private String sanitize(String category) {
        return category.replaceAll("[^a-zA-Z0-9_-]", "");
    }
}
