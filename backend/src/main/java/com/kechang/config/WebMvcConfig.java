package com.kechang.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${kechang.upload.path:./uploads}")
    private String uploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Paths.get(uploadPath).toAbsolutePath().normalize().toUri().toString();
        if (!location.endsWith("/")) {
            location += "/";
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }

    /**
     * 确保 /uploads/** 下的文件以 inline 方式提供（浏览器直接打开而非下载）
     */
    @Bean
    public FilterRegistrationBean<Filter> uploadsInlineFilter() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter((request, response, chain) -> {
            chain.doFilter(request, response);
            if (response instanceof HttpServletResponse httpResponse) {
                String header = httpResponse.getHeader("Content-Disposition");
                if (header == null || !header.toLowerCase().contains("attachment")) {
                    httpResponse.setHeader("Content-Disposition", "inline");
                }
            }
        });
        registration.addUrlPatterns("/uploads/*");
        registration.setOrder(0);
        return registration;
    }
}
