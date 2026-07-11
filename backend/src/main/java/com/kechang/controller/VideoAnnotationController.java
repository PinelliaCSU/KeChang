package com.kechang.controller;

import com.kechang.common.Result;
import com.kechang.service.VideoAnnotationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/video-annotations")
@RequiredArgsConstructor
public class VideoAnnotationController {

    private final VideoAnnotationService videoAnnotationService;

    @GetMapping
    public Result<List<Map<String, Object>>> list(@RequestParam String fileKey) {
        return Result.success(videoAnnotationService.list(getUserId(), fileKey));
    }

    @PutMapping
    public Result<Void> save(@RequestParam String fileKey, @RequestBody(required = false) List<Map<String, Object>> annotations) {
        videoAnnotationService.save(getUserId(), fileKey, annotations);
        return Result.success();
    }

    private Long getUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
