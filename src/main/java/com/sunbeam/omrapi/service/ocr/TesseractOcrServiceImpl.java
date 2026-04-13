package com.sunbeam.omrapi.service.ocr;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.Mat;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;

@Slf4j
@Service
@Profile("!test")
public class TesseractOcrServiceImpl implements OcrService {
    
    private Tesseract tesseract;
    private final OpenCVFrameConverter.ToMat matConverter = new OpenCVFrameConverter.ToMat();
    private final Java2DFrameConverter java2DConverter = new Java2DFrameConverter();

    public TesseractOcrServiceImpl() {
        try {
            tesseract = new Tesseract();
            tesseract.setDatapath("tessdata"); 
            tesseract.setLanguage("vie");
            tesseract.setPageSegMode(6);
        } catch (Throwable t) {
            log.error("Warning: Could not initialize Tesseract OCR. Missing native library (libtesseract). OCR features will be disabled.", t);
            tesseract = null;
        }
    }

    @Override
    public String extractText(Mat imageRegion) {
        if (tesseract == null) {
            return "OCR Disabled";
        }
        try {
            BufferedImage bufferedImage = java2DConverter.convert(matConverter.convert(imageRegion));
            if (bufferedImage == null) return "";
            return tesseract.doOCR(bufferedImage).trim();
        } catch (TesseractException e) {
            log.error("Tesseract OCR error: ", e);
            return "";
        } catch (Exception e) {
            log.error("Error converting Mat to BufferedImage: ", e);
            return "";
        }
    }
}
