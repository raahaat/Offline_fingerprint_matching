package com.offlinematching.sender.payload;

import lombok.Data;

@Data
public class VerificationResponse {
    String message;
    String flag;
    Float score;
}
