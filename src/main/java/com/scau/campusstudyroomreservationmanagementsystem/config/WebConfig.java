package com.scau.campusstudyroomreservationmanagementsystem.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 静态资源映射：将 uploads 目录暴露为 /uploads/**，供座位分布图等材料访问。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final UploadStorage uploadStorage;

    public WebConfig(UploadStorage uploadStorage) {
        this.uploadStorage = uploadStorage;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadStorage.resourceLocation());
    }
}
