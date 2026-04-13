package com.sunbeam.omrapi.service;

import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.opencv_core.Mat;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ImageProcessingServiceTest {

    @Test
    public void testDetectStudentAnswers() {
        ImageProcessingService service = new ImageProcessingService(null);

        File imgFile = new File("data/test_3.jpeg");
        Mat src = opencv_imgcodecs.imread(imgFile.getAbsolutePath());
        assertNotNull(src, "Cannot read image");
        System.out.println("Original size: " + src.cols() + "x" + src.rows());

        Mat warped = service.alignImage(src);

        Map<Integer, String> parsedAnswers = service.extractAnswers(warped, imgFile.getName());

        System.out.println("Parsed answers from service for test_3.jpeg:");
        for (int i = 1; i <= 40; i++) {
            System.out.print(i + ":" + parsedAnswers.get(i) + "  ");
            if (i % 10 == 0) System.out.println();
        }
    }

    @Test
    void testFullOMRExtraction() throws Exception {
        ImageProcessingService service = new ImageProcessingService(null);
        Map<String, String[]> groundTruths = new HashMap<>();
        
        groundTruths.put("test_1.jpeg", new String[]{"B", "B", "C", "D", "A", "B", "B", "C", "D", "D", "D", "A", "A", "A", "C", "B", "D", "A", "C", "C", "B", "B", "D", "D", "B", "A", "D", "C", "C", "A", "B", "D", "C", "C", "A", "B", "C", "D", "C", "B"});
        groundTruths.put("test_2.jpeg", new String[]{"C", "A", "D", "B", "C", "A", "D", "B", "A", "C", "D", "B", "A", "C", "D", "B", "C", "A", "D", "B", "A", "C", "B", "D", "A", "C", "D", "B", "A", "C", "B", "D", "A", "C", "B", "D", "A", "C", "B", "D"});
        groundTruths.put("test_3.jpeg", new String[]{"A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A"});
        groundTruths.put("test_4.jpeg", new String[]{"A", "A", "C", "B", "D", "AC", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "B", "C", "D", "A", "B", "C", "D", "A", "B", "C", "A", "B", "C", "D", "A", "B", "C", "D"});
        groundTruths.put("test_5.jpeg", new String[]{"A", "A", "C", "B", "D", "AC", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "B", "C", "D", "A", "B", "C", "D", "A", "B", "C", "A", "B", "C", "D", "A", "B", "C", "D"});

        File dataDir = new File("data");
        File[] files = dataDir.listFiles((dir, name) -> (name.endsWith(".jpeg") || name.endsWith(".jpg")) && !name.startsWith("debug_"));
        assertNotNull(files);

        List<Executable> assertions = new ArrayList<>();
        for (File f : files) {
            System.out.println("\nValidating " + f.getName());
            try (Mat rawImg = opencv_imgcodecs.imread(f.getAbsolutePath())) {
                try (Mat warped = service.alignImage(rawImg)) {
                    Map<Integer, String> answers = service.extractAnswers(warped, f.getName());
                    
                    String[] expected = groundTruths.get(f.getName());
                    assertNotNull(expected, "No Ground Truth for " + f.getName());
                    
                    System.out.println("Extracted answers for " + f.getName() + ":");
                    for (int i = 1; i <= 40; i++) {
                        String ans = answers.getOrDefault(i, "");
                        System.out.print(i + ":" + ans + "  ");
                        if (i % 10 == 0) System.out.println();
                        
                        final int qNum = i;
                        final String exp = expected[i - 1];
                        final String act = ans;
                        assertions.add(() -> assertEquals(exp, act, "Mismatch at Q" + qNum + " on " + f.getName()));
                    }
                }
            }
        }
        Assertions.assertAll("OMR Extraction Results", assertions);
    }
}
