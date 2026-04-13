package com.sunbeam.omrapi.service.ocr;

import org.bytedeco.opencv.opencv_core.Mat;

public interface OcrService {
    String extractText(Mat imageRegion);
}
