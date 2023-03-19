package com.offlinematching.sender.controller;

import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.futronictech.AnsiSDKLib;
import com.offlinematching.sender.payload.CustvCust;
import com.offlinematching.sender.payload.FingerModel;
import com.offlinematching.sender.payload.VerificationResponse;
import com.offlinematching.sender.service.TwoCustsMatching;

import io.github.cdimascio.dotenv.Dotenv;

@RestController
@RequestMapping("api/v1/fingerverify")
public class VerificationController {

    Dotenv dotenv = Dotenv.load();
    @Autowired
    TwoCustsMatching twoCustsMatching;

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

    @PostMapping("/custvcust")
    public ResponseEntity<String> checkTwoCustomers(@RequestBody CustvCust customers)
            throws ClassNotFoundException, SQLException {

        JSONObject res = new JSONObject();
        Connection con;
        Map<String, byte[]> custOneFingers = new HashMap<>();
        Map<String, byte[]> custTwoFingers = new HashMap<>();


        Class.forName("oracle.jdbc.OracleDriver");
        con = DriverManager.getConnection(
                dotenv.get("cust_db_url"), dotenv.get("cust_db_user"), dotenv.get("cust_db_password"));
        Statement selectCustOneFingers = con.createStatement();
        ResultSet custOneFinger = selectCustOneFingers
                .executeQuery("select * from fp_enroll where cust_no=" + customers.getCustOne());
        while (custOneFinger.next()) {
            custOneFingers.put("RTHUMB", getByteDataFromBlob(custOneFinger.getBlob("RTHUMB")));
            custOneFingers.put("RINDEX", getByteDataFromBlob(custOneFinger.getBlob("RINDEX")));
            custOneFingers.put("RMIDDLE", getByteDataFromBlob(custOneFinger.getBlob("RMIDDLE")));
            custOneFingers.put("RRING", getByteDataFromBlob(custOneFinger.getBlob("RRING")));
            custOneFingers.put("RLITTLE", getByteDataFromBlob(custOneFinger.getBlob("RLITTLE")));
            custOneFingers.put("LTHUMB", getByteDataFromBlob(custOneFinger.getBlob("LTHUMB")));
            custOneFingers.put("LINDEX", getByteDataFromBlob(custOneFinger.getBlob("LINDEX")));
            custOneFingers.put("LMIDDLE", getByteDataFromBlob(custOneFinger.getBlob("LMIDDLE")));
            custOneFingers.put("LRING", getByteDataFromBlob(custOneFinger.getBlob("LRING")));
            custOneFingers.put("LLITTLE", getByteDataFromBlob(custOneFinger.getBlob("LLITTLE")));
        }
        custOneFinger.close();
        selectCustOneFingers.close();
        Statement selectCustTwoFingers = con.createStatement();
        ResultSet custTwoFinger = selectCustTwoFingers
                .executeQuery("select * from fp_enroll where cust_no=" + customers.getCustTwo());

        while (custTwoFinger.next()) {
            custTwoFingers.put("RTHUMB", getByteDataFromBlob(custTwoFinger.getBlob("RTHUMB")));
            custTwoFingers.put("RINDEX", getByteDataFromBlob(custTwoFinger.getBlob("RINDEX")));
            custTwoFingers.put("RMIDDLE", getByteDataFromBlob(custTwoFinger.getBlob("RMIDDLE")));
            custTwoFingers.put("RRING", getByteDataFromBlob(custTwoFinger.getBlob("RRING")));
            custTwoFingers.put("RLITTLE", getByteDataFromBlob(custTwoFinger.getBlob("RLITTLE")));
            custTwoFingers.put("LTHUMB", getByteDataFromBlob(custTwoFinger.getBlob("LTHUMB")));
            custTwoFingers.put("LINDEX", getByteDataFromBlob(custTwoFinger.getBlob("LINDEX")));
            custTwoFingers.put("LMIDDLE", getByteDataFromBlob(custTwoFinger.getBlob("LMIDDLE")));
            custTwoFingers.put("LRING", getByteDataFromBlob(custTwoFinger.getBlob("LRING")));
            custTwoFingers.put("LLITTLE", getByteDataFromBlob(custTwoFinger.getBlob("LLITTLE")));
        }
        custTwoFinger.close();
        selectCustTwoFingers.close();
        con.close();
        System.out.println(custOneFingers);
        System.out.println(custTwoFingers);

        res = twoCustsMatching.checkTwoCustomers(custOneFingers, custTwoFingers, customers.getCustOne(), customers.getCustTwo());
        System.out.println(res);

        return new ResponseEntity<String> (res.toString(), HttpStatus.OK);
    }

    public byte[] getByteDataFromBlob(Blob blob) {
        if (blob != null) {
            try {
                return blob.getBytes(1, (int) blob.length());
            } catch (SQLException ex) {
                System.out.println(ex);
            }
        }
        return null;
    }

}