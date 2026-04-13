# OMR Vision API

A robust Optical Mark Recognition (OMR) system built with **Java (Spring Boot, JavaCV, Tesseract OCR)** for robust automatic exam grading. This pipeline works flawlessly with both clean scanner images and photos taken by smartphones (coping well with shadows and skewed perspectives).

## Features
- **Perspective Alignment:** Uses OpenCV `findContours` and `warpPerspective` to locate document corners and normalize any tilted or skewed images to a 1000x1400 canvas.
- **Adaptive Thresholding:** Handles uneven lighting and shadows using Gaussian Adaptive Thresholding.
- **Intensity Mapping:** Automatically filters out "empty/hollow" circles using OpenCV `meanIntensity` region calculations, reliably extracting only filled answers.
- **Handwriting OCR:** Uses `Tess4j` (Tesseract) to extract handwritten student names and class data.
- **Automatic Grading:** Compares extracted OMR bubbles to a master answer key uploaded via CSV and calculates student percentage/score.

## Tech Stack
- **Java 25**
- **Spring Boot 3.4.x**
- **JavaCV (OpenCV)** (`org.bytedeco:javacpp-presets`)
- **Tess4j (Tesseract OCR)**
- **OpenCSV**
- **Lombok**

## Getting Started

### Prerequisites
- **Java Development Kit (JDK) 25** or higher.
- **Tesseract OCR Engine** and language files natively installed on the host OS. 
  - Ensure you download `vie.traineddata` (or `eng.traineddata`) and place it inside `tessdata/` root directory to activate the text extraction logic without errors.

### Build and Test
```bash
./mvnw clean compile test
```
The test suite validates multiple types of OMR documents (both flat scans and skewed phone photos) checking for 100% extraction accuracy on real-world edge cases.

### Run Locally
```bash
./mvnw spring-boot:run
```

## API Endpoint
The service currently exposes the following grading endpoint:

```http
POST /api/v1/grade
Content-Type: multipart/form-data
```
**Form Data Parameters:**
- `image`: The image file (JPEG/PNG) of the student's answer sheet.
- `answers`: The CSV file containing the Answer Key. Format: `QuestionNumber, CorrectAnswer`.

**Response JSON Example:**
```json
{
  "studentName": "Nguyen Van A",
  "className": "10A1",
  "score": 8.5,
  "correctCount": 34,
  "totalQuestions": 40,
  "incorrectQuestions": [3, 7, 12, 19, 21, 35],
  "studentAnswers": {
    "1": "A",
    "2": "B"
  }
}
```

## How It Works
1. **Endpoint `ProcessController`** receives the image and keys.
2. **`ImageProcessingService`** uses JavaCV to identify corner markers, warp the image flat, compute adaptive thresholds, extract the bounding boxes of filled bubbles, and map coordinates to the A, B, C, D choices.
3. **`TesseractOcrServiceImpl`** extracts the handwriting strings for the name.
4. **`GradingService`** cross-checks the OCR payload against the Answer Key CSV and calculates the score.

## License
MIT License
