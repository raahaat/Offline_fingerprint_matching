package com.offlinematching.sender.service;

import org.json.JSONObject;
import org.springframework.stereotype.Service;

import com.futronictech.AnsiSDKLib;

import java.util.Map;

@Service
public class TwoCustsMatching {

    public JSONObject checkTwoCustomers(Map<String, byte[]> custOne,
            Map<String, byte[]> custTwo, String custNoOne, String custNoTwo) {
        AnsiSDKLib lib = new AnsiSDKLib();
        JSONObject res = new JSONObject();
        float score[] = new float[1];

        for (String custOnekey : custOne.keySet()) {
            if (custOne.get(custOnekey) != null) {

                for (String custTwoKey : custTwo.keySet()) {
                    if (custTwo.get(custTwoKey) != null) {
                        lib.MatchTemplates(custOne.get(custOnekey), custTwo.get(custTwoKey), score);
                    }
                    res.put(custNoOne + "-" + custOnekey + "---" + custNoTwo + "-" + custTwoKey,
                            score[0]);

                }
            }
        }

        return res;
    }


}
