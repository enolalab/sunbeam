package com.sunbeam.omrapi.service;

import com.opencsv.bean.CsvToBeanBuilder;
import com.sunbeam.omrapi.model.AnswerKey;
import com.sunbeam.omrapi.model.GradeResult;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class GradingService {

    public List<AnswerKey> parseCsv(MultipartFile csvFile) throws Exception {
        try (Reader reader = new InputStreamReader(csvFile.getInputStream())) {
            return new CsvToBeanBuilder<AnswerKey>(reader)
                    .withType(AnswerKey.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build()
                    .parse();
        }
    }

    public GradeResult grade(Map<Integer, String> studentAnswers, List<AnswerKey> answerKeys, String studentName, String className) {
        int correctCount = 0;
        List<Integer> incorrectQuestions = new ArrayList<>();
        
        for (AnswerKey key : answerKeys) {
            String studentAns = studentAnswers.get(key.getQuestionNumber());
            if (studentAns != null && studentAns.equalsIgnoreCase(key.getCorrectAnswer())) {
                correctCount++;
            } else {
                incorrectQuestions.add(key.getQuestionNumber());
            }
        }

        int totalQuestions = answerKeys.size();
        double score = totalQuestions > 0 ? ((double) correctCount / totalQuestions) * 10.0 : 0.0;

        return GradeResult.builder()
                .studentName(studentName)
                .className(className)
                .correctCount(correctCount)
                .totalQuestions(totalQuestions)
                .score(Math.round(score * 100.0) / 100.0)
                .incorrectQuestions(incorrectQuestions)
                .studentAnswers(studentAnswers)
                .build();
    }
}
