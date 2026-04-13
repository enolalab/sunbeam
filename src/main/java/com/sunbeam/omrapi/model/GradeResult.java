package com.sunbeam.omrapi.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class GradeResult {
    private String studentName;
    private String className;
    private double score;
    private int correctCount;
    private int totalQuestions;
    private List<Integer> incorrectQuestions;
    private Map<Integer, String> studentAnswers;
}
