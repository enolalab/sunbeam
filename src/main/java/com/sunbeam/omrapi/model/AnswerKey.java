package com.sunbeam.omrapi.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnswerKey {
    private int questionNumber;
    private String correctAnswer;
}
