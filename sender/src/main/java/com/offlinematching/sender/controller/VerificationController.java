package com.offlinematching.sender.controller;

import java.util.Base64;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.futronictech.AnsiSDKLib;
import com.offlinematching.sender.payload.FingerModel;
import com.offlinematching.sender.payload.VerificationResponse;

@RestController
@RequestMapping("api/v1/fingerverify")
public class VerificationController {

    @PostMapping
    public ResponseEntity<VerificationResponse> fingerVerify(@RequestBody FingerModel model) {
        AnsiSDKLib ansiSDKLib = new AnsiSDKLib();
        VerificationResponse verificationResponse = new VerificationResponse();

        float[] score = new float[1];
        if (model.getFirstFinger().length() > 100 && model.getSecondFinger().length() > 100) {

            byte[] fFinger = Base64.getDecoder().decode(model.getFirstFinger());
            byte[] sFinger = Base64.getDecoder().decode(model.getSecondFinger());
            ansiSDKLib.MatchTemplates(fFinger, sFinger, score);

            if (score[0] > AnsiSDKLib.FTR_ANSISDK_MATCH_SCORE_HIGH_MEDIUM) {
                verificationResponse.setFlag("Y");
                verificationResponse.setMessage("Matched!");
                verificationResponse.setScore(score[0]);
            } else {
                verificationResponse.setFlag("N");
                verificationResponse.setMessage("Not Matched!");
                verificationResponse.setScore(score[0]);
            }
        } else {
            verificationResponse.setFlag("N/A");
            verificationResponse.setMessage("Invalid Fingerprint Template!");
            verificationResponse.setScore(00f);
        }

        return new ResponseEntity<VerificationResponse>(verificationResponse, HttpStatus.OK);

    }

}