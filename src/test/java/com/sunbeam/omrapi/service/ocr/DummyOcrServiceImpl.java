package com.sunbeam.omrapi.service.ocr;

import org.bytedeco.opencv.opencv_core.Mat;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("test")
public class DummyOcrServiceImpl implements OcrService {
    @Override
    public String extractText(Mat imageRegion) {
        return "Dummy Student";
    }
}
