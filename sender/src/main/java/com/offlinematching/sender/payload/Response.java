package com.offlinematching.sender.payload;

import lombok.Data;

@Data
public class Response {
    String message;
    String customerNumber;
    String token;
    
}
