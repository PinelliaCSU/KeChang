package com.kechang.controller.exam;

import com.kechang.common.Result;
import com.kechang.dto.ParticipationAwardDTO;
import com.kechang.dto.RollCallDTO;
import com.kechang.service.ParticipationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/participation")
public class ParticipationController {

    private final ParticipationService participationService;

    private Long getUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @PostMapping("/roll-call")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Map<String, Object>> rollCall(@RequestBody RollCallDTO dto) {
        return Result.success(participationService.rollCall(dto, getUserId()));
    }

    @PostMapping("/award")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Map<String, Object>> award(@RequestBody ParticipationAwardDTO dto) {
        return Result.success(participationService.award(dto, getUserId()));
    }

    @GetMapping("/courses/{courseId}/scoreboard")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<List<Map<String, Object>>> scoreboard(@PathVariable Long courseId) {
        return Result.success(participationService.scoreboard(courseId, getUserId()));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    public Result<Map<String, Object>> myOverview() {
        return Result.success(participationService.myOverview(getUserId()));
    }
}
