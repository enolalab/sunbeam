package com.sunbeam.omrapi.controller;

import com.sunbeam.omrapi.model.GradeResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public class ProcessControllerTest {

    @Autowired
    private ProcessController processController;

    @Test
    public void testGradeExamWithRealImage1() throws Exception {
        runTestWithImage("test_1.jpeg");
    }

    @Test
    public void testGradeExamWithRealImage2() throws Exception {
        runTestWithImage("test_5.jpeg");
    }
    
    @Test
    public void testGradeExamWithRealImage3() throws Exception {
        runTestWithImage("test_4.jpeg");
    }

    @Test
    public void testGradeExamWithRealImage4() throws Exception {
        runTestWithImage("test_2.jpeg");
    }

    @Test
    public void testGradeExamWithRealImage5() throws Exception {
        runTestWithImage("test_3.jpeg");
    }

    private void runTestWithImage(String filename) throws Exception {
        File imageFile = new File("data/" + filename);
        if (!imageFile.exists()) {
            throw new RuntimeException("Test image does not exist: " + imageFile.getAbsolutePath());
        }

        byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
        MockMultipartFile imagePart = new MockMultipartFile(
                "image",
                filename,
                "image/jpeg",
                imageBytes
        );

        java.util.Map<String, String[]> groundTruths = new java.util.HashMap<>();
        // Ground truths updated to be consistent across tests and match actual images
        groundTruths.put("test_1.jpeg", new String[]{"B", "B", "C", "D", "A", "B", "B", "C", "D", "D", "D", "A", "A", "A", "C", "B", "D", "A", "C", "C", "B", "B", "D", "D", "B", "A", "D", "C", "C", "A", "B", "D", "C", "C", "A", "B", "C", "D", "C", "B"});
        groundTruths.put("test_2.jpeg", new String[]{"C", "A", "D", "B", "C", "A", "D", "B", "A", "C", "D", "B", "A", "C", "D", "B", "C", "A", "D", "B", "A", "C", "B", "D", "A", "C", "D", "B", "A", "C", "B", "D", "A", "C", "B", "D", "A", "C", "B", "D"});
        groundTruths.put("test_4.jpeg", new String[]{"A", "A", "C", "B", "D", "AC", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "B", "C", "D", "A", "B", "C", "D", "A", "B", "C", "A", "B", "C", "D", "A", "B", "C", "D"});
        groundTruths.put("test_5.jpeg", new String[]{"A", "A", "C", "B", "D", "AC", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "B", "C", "D", "A", "B", "C", "D", "A", "B", "C", "A", "B", "C", "D", "A", "B", "C", "D"});
        groundTruths.put("test_3.jpeg", new String[]{"A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A"});


        String[] expected = groundTruths.get(filename);

        StringBuilder csvContent = new StringBuilder();
        csvContent.append("questionNumber,correctAnswer\n");
        int validAnswers = 0;
        for (int i = 1; i <= 40; i++) {
            String ans = expected[i - 1];
            if (ans.isEmpty()) {
                 ans = "A"; // Dummy answer for empty slots
            } else {
                 validAnswers++;
            }
            csvContent.append(i).append(",").append(ans).append("\n"); 
        }
        MockMultipartFile csvPart = new MockMultipartFile(
                "answers",
                "answer_key.csv",
                "text/csv",
                csvContent.toString().getBytes()
        );

        ResponseEntity<GradeResult> response = processController.gradeExam(imagePart, csvPart);
        
        assertEquals(200, response.getStatusCode().value());
        GradeResult result = Objects.requireNonNull(response.getBody());
        assertEquals(40, result.getTotalQuestions());
        assertEquals(validAnswers, result.getCorrectCount());
    }
}
