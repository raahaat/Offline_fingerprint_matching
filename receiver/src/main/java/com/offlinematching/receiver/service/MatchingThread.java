package com.offlinematching.receiver.service;

import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import com.futronictech.AnsiSDKLib;

import io.github.cdimascio.dotenv.Dotenv;

public class MatchingThread implements Runnable {

    Connection con;
    Connection logCon;
    Map<String, byte[]> customerFingers = new HashMap<>();
    int startIndex;
    int chunk;
    String customerNumber;
    String token;
    static int checkedItem = 0;

    public MatchingThread() {
    }

    public MatchingThread(String token, String customerNumber, Connection con,Connection logCon,
            Map<String, byte[]> customerFingers,
            int startIndex, int chunk) {
        this.con = con;
        this.customerFingers = customerFingers;
        this.startIndex = startIndex;
        this.chunk = chunk;
        this.customerNumber = customerNumber;
        this.token = token;
        this.logCon = logCon;
    }

    @Override
    public void run() {
        try {
            fingerMatching();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void fingerMatching() throws SQLException {
        float[] score = new float[1];
        AnsiSDKLib ansiSDKLib = new AnsiSDKLib();
        Statement stmt = con.createStatement();
        Dotenv queries = Dotenv.configure().filename(".queries")
                .load();
        ResultSet rs = stmt.executeQuery(queries.get("random_customer") + " OFFSET " + startIndex
                + " ROWS FETCH FIRST " + chunk + " ROWS ONLY");
        while (rs.next()) {
            Map<String, byte[]> singleFingerData = new HashMap<>();
            singleFingerData.put("RTHUMB", getByteDataFromBlob(rs.getBlob("RTHUMB")));
            singleFingerData.put("RINDEX", getByteDataFromBlob(rs.getBlob("RINDEX")));
            singleFingerData.put("RMIDDLE", getByteDataFromBlob(rs.getBlob("RMIDDLE")));
            singleFingerData.put("RRING", getByteDataFromBlob(rs.getBlob("RRING")));
            singleFingerData.put("RLITTLE", getByteDataFromBlob(rs.getBlob("RLITTLE")));
            singleFingerData.put("LTHUMB", getByteDataFromBlob(rs.getBlob("LTHUMB")));
            singleFingerData.put("LINDEX", getByteDataFromBlob(rs.getBlob("LINDEX")));
            singleFingerData.put("LMIDDLE", getByteDataFromBlob(rs.getBlob("LMIDDLE")));
            singleFingerData.put("LRING", getByteDataFromBlob(rs.getBlob("LRING")));
            singleFingerData.put("LLITTLE", getByteDataFromBlob(rs.getBlob("LLITTLE")));
            String custFromDB = rs.getString("cust_no");

            for (String custKey : customerFingers.keySet()) {
                if (customerFingers.get(custKey) != null) {
                    for (String randomKey : singleFingerData.keySet()) {
                        if (singleFingerData.get(randomKey) != null) {
                            ansiSDKLib.MatchTemplates(customerFingers.get(custKey), singleFingerData.get(randomKey),
                                    score);
                            checkedItem++;
                            if (score[0] > AnsiSDKLib.FTR_ANSISDK_MATCH_SCORE_HIGH_MEDIUM) {
                                System.out.println("Token: " + token);
                                System.out.println("Matched with score: " + score[0]);
                                System.out.println(customerNumber + "--" + custKey + "----with----" + custFromDB + "--"
                                        + randomKey);

                                PreparedStatement pstmt = logCon.prepareStatement(queries.get("matching_log"));
                                pstmt.setString(1, customerNumber);
                                pstmt.setString(2, custKey);
                                pstmt.setString(3, custFromDB);
                                pstmt.setString(4, randomKey);
                                pstmt.setString(5, token);
                                pstmt.execute();
                                pstmt.close();
                            } 

                        }

                    }
                }
            }
        }

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
