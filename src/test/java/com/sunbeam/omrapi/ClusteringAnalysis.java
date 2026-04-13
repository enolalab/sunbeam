package com.sunbeam.omrapi;

import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.Size;
import com.sunbeam.omrapi.service.ImageProcessingService;

import java.io.File;

public class ClusteringAnalysis {
    public static void main(String[] args) throws Exception {
        ImageProcessingService omr = new ImageProcessingService(null);
        
        File imgDir = new File("data");
        File[] files = imgDir.listFiles((dir, name) -> name.endsWith(".jpeg"));
        if (files == null) return;
        
        for (File f : files) {
            System.out.println("====== " + f.getName() + " ======");
            Mat rawImg = opencv_imgcodecs.imread(f.getAbsolutePath());
            Mat fixedImg = omr.alignImage(rawImg);
            
            Mat gray = new Mat();
            opencv_imgproc.cvtColor(fixedImg, gray, opencv_imgproc.COLOR_BGR2GRAY);
            Mat blurred = new Mat();
            opencv_imgproc.GaussianBlur(gray, blurred, new Size(5, 5), 0);
            Mat thresh = new Mat();
            opencv_imgproc.threshold(blurred, thresh, 0, 255, opencv_imgproc.THRESH_BINARY_INV | opencv_imgproc.THRESH_OTSU);
            
            org.bytedeco.opencv.opencv_core.MatVector bubbleContours = new org.bytedeco.opencv.opencv_core.MatVector();
            opencv_imgproc.findContours(thresh.clone(), bubbleContours, new Mat(), opencv_imgproc.RETR_LIST, opencv_imgproc.CHAIN_APPROX_SIMPLE);

            java.util.List<Integer> cxList = new java.util.ArrayList<>();
            java.util.List<Integer> cyList = new java.util.ArrayList<>();
            
            for (long i = 0; i < bubbleContours.size(); i++) {
                Mat contour = bubbleContours.get(i);
                double area = opencv_imgproc.contourArea(contour);
                Rect rect = opencv_imgproc.boundingRect(contour);
                double aspect = (double) rect.width() / rect.height();
                int cx = rect.x() + rect.width() / 2;
                int cy = rect.y() + rect.height() / 2;
                
                if (area < 150 || area > 2000) continue; 
                if (aspect < 0.6 || aspect > 1.6) continue;
                if (cy < 350 || cy > 950) continue;
                
                cxList.add(cx);
                cyList.add(cy);
            }
            
            java.util.Collections.sort(cxList);
            java.util.Collections.sort(cyList);
            
            System.out.println("----- CY CLUSTERS -----");
            int lastCy = -100;
            java.util.List<Integer> currentCluster = new java.util.ArrayList<>();
            int rowIdx = 0;
            for (int cy : cyList) {
                if (cy - lastCy > 20 && !currentCluster.isEmpty()) {
                    double avg = currentCluster.stream().mapToInt(Integer::intValue).average().orElse(0);
                    System.out.println("Row " + rowIdx + " Center: " + avg);
                    rowIdx++;
                    currentCluster.clear();
                }
                currentCluster.add(cy);
                lastCy = cy;
            }
            if (!currentCluster.isEmpty()) {
                double avg = currentCluster.stream().mapToInt(Integer::intValue).average().orElse(0);
                System.out.println("Row " + rowIdx + " Center: " + avg);
            }
            
            System.out.println("----- CX CLUSTERS -----");
            int lastCx = -100;
            currentCluster.clear();
            int optIdx = 0;
            for (int cx : cxList) {
                if (cx - lastCx > 15 && !currentCluster.isEmpty()) {
                    double avg = currentCluster.stream().mapToInt(Integer::intValue).average().orElse(0);
                    System.out.println("Col/Opt " + optIdx + " Center: " + avg);
                    optIdx++;
                    currentCluster.clear();
                }
                currentCluster.add(cx);
                lastCx = cx;
            }
            if (!currentCluster.isEmpty()) {
                double avg = currentCluster.stream().mapToInt(Integer::intValue).average().orElse(0);
                System.out.println("Col/Opt " + optIdx + " Center: " + avg);
            }
        }
    }
}
