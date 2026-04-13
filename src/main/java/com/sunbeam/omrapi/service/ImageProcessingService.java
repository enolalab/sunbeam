package com.sunbeam.omrapi.service;

import com.sunbeam.omrapi.service.ocr.OcrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageProcessingService {

    private final OcrService ocrService;

    public Map<String, Object> processExamSheet(MultipartFile imageFile) throws Exception {
        File tempFile = File.createTempFile("exam-", ".jpg");
        imageFile.transferTo(tempFile);

        try (Mat src = opencv_imgcodecs.imread(tempFile.getAbsolutePath())) {
            if (src.empty()) {
                throw new RuntimeException("Cannot read image using OpenCV");
            }

            // Step 1: Perspective Warp
            try (Mat alignedImg = alignImage(src)) {
                // Step 2: Crop OCR region and extract text (Placeholder logic)
                try (Mat nameRoi = new Mat(alignedImg, new Rect(150, 100, 300, 50));
                     Mat classRoi = new Mat(alignedImg, new Rect(500, 100, 150, 50))) {

                    String studentName = ocrService.extractText(nameRoi);
                    String className = ocrService.extractText(classRoi);
                    String imgName = imageFile.getOriginalFilename() != null ? imageFile.getOriginalFilename() : "unknown";

                    // Step 3: Extract OMR bubble answers
                    Map<Integer, String> studentAnswers = extractAnswers(alignedImg, imgName);

                    Map<String, Object> result = new HashMap<>();
                    result.put("studentName", studentName);
                    result.put("className", className);
                    result.put("answers", studentAnswers);

                    return result;
                }
            }
        } finally {
            tempFile.delete();
        }
    }

    public Mat alignImage(Mat src) {
        log.info("Applying perspective alignment...");
        
        try (Mat gray = new Mat();
             Mat blurred = new Mat();
             Mat thresh = new Mat();
             Mat kernel = opencv_imgproc.getStructuringElement(opencv_imgproc.MORPH_RECT, new Size(5, 5));
             MatVector contours = new MatVector()) {

            Mat resized = new Mat();
            double ratio = 1000.0 / src.cols();
            opencv_imgproc.resize(src, resized, new Size(1000, (int) (src.rows() * ratio)));

            opencv_imgproc.cvtColor(resized, gray, opencv_imgproc.COLOR_BGR2GRAY);
            opencv_imgproc.GaussianBlur(gray, blurred, new Size(5, 5), 0);
            opencv_imgproc.adaptiveThreshold(blurred, thresh, 255, opencv_imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, opencv_imgproc.THRESH_BINARY_INV, 51, 15);
            opencv_imgproc.morphologyEx(thresh, thresh, opencv_imgproc.MORPH_OPEN, kernel);            
            
            opencv_imgproc.findContours(thresh, contours, new Mat(), opencv_imgproc.RETR_EXTERNAL, opencv_imgproc.CHAIN_APPROX_SIMPLE);

            java.util.List<Point> centers = new java.util.ArrayList<>();
            for (int i = 0; i < contours.size(); i++) {
                try (Mat contour = contours.get(i)) {
                    double area = opencv_imgproc.contourArea(contour);
                    Rect rect = opencv_imgproc.boundingRect(contour);
                    
                    if (area <= 100 || area >= 3000) continue;
                    
                    double aspect = (double) rect.width() / rect.height();
                    if (aspect <= 0.7 || aspect >= 1.3) continue;
                    
                    double extent = area / (rect.width() * rect.height());
                    if (extent <= 0.6) continue;
                    
                    int cx = rect.x() + rect.width() / 2;
                    int cy = rect.y() + rect.height() / 2;
                    centers.add(new Point(cx, cy));
                }
            }
            
            if (centers.size() >= 4) {
                Point tl = centers.get(0), br = centers.get(0), tr = centers.get(0), bl = centers.get(0);
                int minSum = Integer.MAX_VALUE, maxSum = Integer.MIN_VALUE;
                int minDiff = Integer.MAX_VALUE, maxDiff = Integer.MIN_VALUE;

                for (Point pt : centers) {
                    int sum = pt.x() + pt.y();
                    int diff = pt.x() - pt.y();
                    if (sum < minSum) {
                        minSum = sum;
                        tl = pt;
                    }
                    if (sum > maxSum) {
                        maxSum = sum;
                        br = pt;
                    }
                    if (diff < minDiff) {
                        minDiff = diff;
                        bl = pt;
                    }
                    if (diff > maxDiff) {
                        maxDiff = diff;
                        tr = pt;
                    }
                }

                try (Mat srcPts = new Mat(4, 1, opencv_core.CV_32FC2);
                     Mat dstPts = new Mat(4, 1, opencv_core.CV_32FC2)) {
                    
                    org.bytedeco.javacpp.indexer.FloatIndexer srcIdx = srcPts.createIndexer();
                    srcIdx.put(0, 0, 0, tl.x()).put(0, 0, 1, tl.y());
                    srcIdx.put(1, 0, 0, tr.x()).put(1, 0, 1, tr.y());
                    srcIdx.put(2, 0, 0, br.x()).put(2, 0, 1, br.y());
                    srcIdx.put(3, 0, 0, bl.x()).put(3, 0, 1, bl.y());

                    org.bytedeco.javacpp.indexer.FloatIndexer dstIdx = dstPts.createIndexer();
                    dstIdx.put(0, 0, 0, 50).put(0, 0, 1, 50);
                    dstIdx.put(1, 0, 0, 750).put(1, 0, 1, 50);
                    dstIdx.put(2, 0, 0, 750).put(2, 0, 1, 1150);
                    dstIdx.put(3, 0, 0, 50).put(3, 0, 1, 1150);

                    try (Mat perspectiveTransform = opencv_imgproc.getPerspectiveTransform(srcPts, dstPts)) {
                        Mat warped = new Mat();
                        opencv_imgproc.warpPerspective(resized, warped, perspectiveTransform, new Size(800, 1200));
                        resized.close();
                        return warped;
                    }
                }
            }

            log.warn("Could not find 4 corners. Returning center cropped region.");
            Mat fallback = new Mat();
            opencv_imgproc.resize(src, fallback, new Size(800, 1200));
            resized.close();
            return fallback;
        }
    }

    public Map<Integer, String> extractAnswers(Mat alignedImg, String imgName) {
        Map<Integer, String> answers = new HashMap<>();
        String[] ansArr = new String[40];
        for (int i = 0; i < 40; i++) ansArr[i] = "";

        try (Mat gray = new Mat();
             Mat blurred = new Mat();
             Mat thresh = new Mat();
             MatVector bubbleContours = new MatVector();
             Mat debugImg = alignedImg.clone()) {

            opencv_imgproc.cvtColor(alignedImg, gray, opencv_imgproc.COLOR_BGR2GRAY);
            opencv_imgproc.GaussianBlur(gray, blurred, new Size(5, 5), 0);
            opencv_imgproc.adaptiveThreshold(blurred, thresh, 255, opencv_imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, opencv_imgproc.THRESH_BINARY_INV, 51, 15);

            opencv_imgproc.findContours(thresh, bubbleContours, new Mat(), opencv_imgproc.RETR_LIST, opencv_imgproc.CHAIN_APPROX_SIMPLE);

            for (long i = 0; i < bubbleContours.size(); i++) {
                try (Mat contour = bubbleContours.get(i)) {
                    double area = opencv_imgproc.contourArea(contour);
                    Rect rect = opencv_imgproc.boundingRect(contour);
                    double aspect = (double) rect.width() / rect.height();
                    int cx = rect.x() + rect.width() / 2;
                    int cy = rect.y() + rect.height() / 2;

                    if (area < 80 || area > 2000) continue;
                    if (aspect < 0.5 || aspect > 2.0) continue;
                    if (cy < 300 || cy > 1000) continue;

                    int col = -1;
                    if (cx > 50 && cx < 230) col = 0;
                    else if (cx > 230 && cx < 410) col = 1;
                    else if (cx > 410 && cx < 590) col = 2;
                    else if (cx > 590 && cx < 800) col = 3;
                    if (col == -1) continue;

                    try (Mat roiGray = new Mat(blurred, rect)) {
                        Scalar meanScalar = opencv_core.mean(roiGray);
                        double meanIntensity = meanScalar.get(0);
                        
                        if (meanIntensity < 165) {
                            double rowF = (cy - 380.0) / 51.5;
                            int row = (int) Math.round(rowF);
                            if (row < 0 || row > 9) continue;
                            if (Math.abs(rowF - row) > 0.45) continue;

                            double[] colXStarts = {93.0, 275.0, 461.0, 645.0};
                            double optF = (cx - colXStarts[col]) / 32.0;
                            int opt = (int) Math.round(optF);
                            if (opt < 0 || opt > 3) continue;
                            if (Math.abs(optF - opt) > 0.48) continue;

                            int qNum = col * 10 + row;
                            char optChar = (char) ('A' + opt);
                            if (ansArr[qNum].isEmpty()) {
                                ansArr[qNum] = String.valueOf(optChar);
                            } else if (!ansArr[qNum].contains(String.valueOf(optChar))) {
                                ansArr[qNum] = ansArr[qNum] + optChar;
                            }
                            opencv_imgproc.rectangle(debugImg, rect, new Scalar(0, 255, 0, 0), 2, 8, 0);
                        }
                    }
                }
            }

            for (int q = 0; q < 40; q++) {
                String ans = ansArr[q];
                if (ans.length() > 1) {
                    char[] chars = ans.toCharArray();
                    java.util.Arrays.sort(chars);
                    ans = new String(chars);
                }
                answers.put(q + 1, ans);
            }

            try {
                opencv_imgcodecs.imwrite("./data/debug/debug_grid_" + imgName + "_" + System.currentTimeMillis() + ".jpeg", debugImg);
            } catch (Exception e) {}

        } catch (Exception e) {
            log.error("Error extracting answers", e);
        }
        return answers;
    }
}
