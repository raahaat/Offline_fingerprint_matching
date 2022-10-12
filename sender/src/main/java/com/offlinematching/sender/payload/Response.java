package com.offlinematching.sender.payload;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class Response {
    String message;
    String customerNumber;
    String token;
    
}
