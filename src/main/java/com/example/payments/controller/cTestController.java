// NicepayPaymentController.java (Service 없이 처리)
package com.example.payments.controller;

import org.apache.commons.codec.binary.Hex;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;

@Controller
@RequestMapping("/nicepay")
public class cTestController {

    private final String merchantKey = "EYzu8jGGMfqaDEp76gSckuvnaHHu+bC4opsSN6lHv3b2lurNYkVXrZ7Z1AoqQnXI3eLuaUFyoRNC6FkrzVjceg==";
    private final String mid = "nicepay00m";
    private final String moid = "nicepay_api_3.0_test";

    @PostMapping("/cancel")
    public String cancelPayment(@RequestParam Map<String, String> params, Model model) throws Exception {
        String tid = params.get("TID");
        String cancelAmt = params.get("CancelAmt");
        String partialCancelCode = params.get("PartialCancelCode");
        String cancelMsg = "고객요청";

        String ediDate = getyyyyMMddHHmmss();
        String signData = encryptSHA256(mid + cancelAmt + ediDate + merchantKey);

        String requestData = "TID=" + tid +
                "&MID=" + mid +
                "&Moid=" + moid +
                "&CancelAmt=" + cancelAmt +
                "&CancelMsg=" + URLEncoder.encode(cancelMsg, "euc-kr") +
                "&PartialCancelCode=" + partialCancelCode +
                "&EdiDate=" + ediDate +
                "&CharSet=utf-8" +
                "&SignData=" + signData;

        String resultJsonStr = connectToServer(requestData, "https://pg-api.nicepay.co.kr/webapi/cancel_process.jsp");

        Map<String, String> resultData = new HashMap<>();
        if ("9999".equals(resultJsonStr)) {
            resultData.put("ResultCode", "9999");
            resultData.put("ResultMsg", "통신실패");
        } else {
            resultData = jsonStringToMap(resultJsonStr);
        }

        model.addAllAttributes(resultData);
        return "nicepay/cancelResult";
    }

    private String encryptSHA256(String strData) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(strData.getBytes());
        byte[] raw = md.digest();
        return new String(Hex.encodeHex(raw));
    }

    private String getyyyyMMddHHmmss() {
        return new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
    }

    private String connectToServer(String data, String reqUrl) {
        HttpURLConnection conn = null;
        BufferedReader resultReader = null;
        PrintWriter pw = null;
        StringBuilder recvBuffer = new StringBuilder();

        try {
            URL url = new URL(reqUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(25000);
            conn.setDoOutput(true);

            pw = new PrintWriter(conn.getOutputStream());
            pw.write(data);
            pw.flush();

            resultReader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
            String temp;
            while ((temp = resultReader.readLine()) != null) {
                recvBuffer.append(temp);
            }
            return recvBuffer.toString().trim();
        } catch (Exception e) {
            return "9999";
        } finally {
            try { if (resultReader != null) resultReader.close(); } catch (Exception ignored) {}
            try { if (pw != null) pw.close(); } catch (Exception ignored) {}
            try { if (conn != null) conn.disconnect(); } catch (Exception ignored) {}
        }
    }

    private Map<String, String> jsonStringToMap(String str) {
        Map<String, String> dataMap = new HashMap<>();
        try {
            JSONParser parser = new JSONParser();
            JSONObject jsonObject = (JSONObject) parser.parse(str);
            for (Object keyObj : jsonObject.keySet()) {
                String key = (String) keyObj;
                dataMap.put(key, String.valueOf(jsonObject.get(key)));
            }
        } catch (Exception ignored) {
        }
        return dataMap;
    }
}
