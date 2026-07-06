package com.kechang.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kechang.common.PageResult;
import com.kechang.entity.Evaluation;
import com.kechang.entity.EvaluationResult;
import com.kechang.mapper.EvaluationMapper;
import com.kechang.mapper.EvaluationResultMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 评价服务
 */
@Service
@RequiredArgsConstructor
public class EvaluationService {

    private final EvaluationMapper evalMapper;
    private final EvaluationResultMapper resultMapper;

    public PageResult<Evaluation> list(int page, int size, Long courseId) {
        LambdaQueryWrapper<Evaluation> w = new LambdaQueryWrapper<>();
        if (courseId != null) {
            w.eq(Evaluation::getCourseId, courseId);
        }
        Page<Evaluation> p = evalMapper.selectPage(new Page<>(page, size), w);
        return PageResult.of(p.getRecords(), p.getTotal(), page, size);
    }

    public void create(Evaluation e) {
        evalMapper.insert(e);
    }

    public void submitResult(EvaluationResult r) {
        resultMapper.insert(r);
    }

    public List<EvaluationResult> getResults(Long evalId) {
        return resultMapper.selectList(new LambdaQueryWrapper<EvaluationResult>()
                .eq(EvaluationResult::getEvaluationId, evalId));
    }
}
