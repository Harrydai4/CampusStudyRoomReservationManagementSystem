package com.scau.campusstudyroomreservationmanagementsystem.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

/**
 * 统一管理上传目录：使用绝对路径，避免 Tomcat 临时工作目录导致 FileNotFoundException。
 * 默认保存到「启动命令所在目录」/uploads（即项目根目录下 uploads 文件夹）。
 */
@Component
public class UploadStorage {
    private static final Logger log = LoggerFactory.getLogger(UploadStorage.class);
    private static final Set<String> CATEGORIES = Set.of("material", "layout", "common");

    private final Path root;

    public UploadStorage(@Value("${app.upload.dir:uploads}") String configuredDir) {
        Path path = Paths.get(configuredDir.trim());
        if (!path.isAbsolute()) {
            path = Paths.get(System.getProperty("user.dir")).resolve(path).normalize();
        }
        this.root = path.toAbsolutePath();
    }

    @PostConstruct
    void init() throws IOException {
        Files.createDirectories(root);
        for (String category : CATEGORIES) {
            Files.createDirectories(root.resolve(category));
        }
        log.info("文件上传目录: {}", root);
    }

    public Path root() {
        return root;
    }

    /** Spring 静态资源映射地址，必须以 / 结尾 */
    public String resourceLocation() {
        String uri = root.toUri().toString();
        return uri.endsWith("/") ? uri : uri + "/";
    }

    public String sanitizeCategory(String category) {
        String safe = category == null ? "common" : category.replaceAll("[^a-zA-Z0-9_-]", "");
        return safe.isBlank() ? "common" : safe;
    }

    /**
     * 保存上传文件。
     *
     * @return 可写入数据库、供前端访问的 URL 路径，如 /uploads/layout/xxx.png
     */
    public String save(InputStream input, String category, String ext) throws IOException {
        String cat = sanitizeCategory(category);
        Path dir = root.resolve(cat);
        Files.createDirectories(dir);
        String filename = UUID.randomUUID() + ext;
        Path target = dir.resolve(filename);
        Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        return "/uploads/" + cat + "/" + filename;
    }
}
