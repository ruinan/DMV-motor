package com.dmvmotor.api.content.controller;

import com.dmvmotor.api.common.ApiResponse;
import com.dmvmotor.api.content.domain.Exam;
import com.dmvmotor.api.content.infrastructure.ExamRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Exam catalog — the (state × license type) options a learner can prepare for.
 * Public (no auth): the picker needs it before/at sign-in. Today it returns the
 * single seeded CA-M1 exam; more are added as content is sourced.
 */
@RestController
@RequestMapping("/api/v1/exams")
public class ExamController {

    private final ExamRepository examRepo;

    public ExamController(ExamRepository examRepo) {
        this.examRepo = examRepo;
    }

    @GetMapping
    public ApiResponse<?> list(@RequestParam(required = false) String language) {
        boolean zh = "zh".equalsIgnoreCase(language);
        List<ExamDto> exams = examRepo.findAllActive().stream()
                .map(e -> ExamDto.from(e, zh)).toList();
        return ApiResponse.ok(new ExamListDto(exams));
    }

    record ExamListDto(List<ExamDto> exams) {}

    record ExamDto(String id, String stateCode, String licenseClass, String name) {
        static ExamDto from(Exam e, boolean zh) {
            return new ExamDto(String.valueOf(e.id()), e.stateCode(), e.licenseClass(),
                    zh ? e.nameZh() : e.nameEn());
        }
    }
}
