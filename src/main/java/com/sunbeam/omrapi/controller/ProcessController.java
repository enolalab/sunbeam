package com.sunbeam.omrapi.controller;

import com.sunbeam.omrapi.model.AnswerKey;
import com.sunbeam.omrapi.model.GradeResult;
import com.sunbeam.omrapi.service.GradingService;
import com.sunbeam.omrapi.service.ImageProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ProcessController {

    private final ImageProcessingService imageProcessingService;
    private final GradingService gradingService;

    @PostMapping("/grade")
    public ResponseEntity<GradeResult> gradeExam(
            @RequestParam("image") MultipartFile imageFile,
            @RequestParam("answers") MultipartFile csvFile) {
        try {
            List<AnswerKey> answerKeys = gradingService.parseCsv(csvFile);
            Map<String, Object> processedData = imageProcessingService.processExamSheet(imageFile);
            
            String studentName = (String) processedData.get("studentName");
            String className = (String) processedData.get("className");
            @SuppressWarnings("unchecked")
            Map<Integer, String> studentAnswers = (Map<Integer, String>) processedData.get("answers");

            GradeResult result = gradingService.grade(studentAnswers, answerKeys, studentName, className);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error during API image processing", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
