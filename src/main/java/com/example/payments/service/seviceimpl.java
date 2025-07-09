package com.example.payments.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;
import org.springframework.stereotype.Service;

@Service
public class seviceimpl implements service{
    private final String merchantKey = "EYzu8jGGMfqaDEp76gSckuvnaHHu+bC4opsSN6lHv3b2lurNYkVXrZ7Z1AoqQnXI3eLuaUFyoRNC6FkrzVjceg==";
    private final String cancelUrl = "https://pg-api.nicepay.co.kr/webapi/cancel_process.jsp";

    @Override
    public Map<String, Object> handleAuthResult(Map<String, String> params) {
        // TODO: 기존 인증/승인 처리 코드 작성 (앞서 제공된 JSP 로직 기반)
        // 이 부분은 이전에 제공한 auth 로직 참고하여 작성
        return new HashMap<>();
    }

    @Override
    public Map<String, Object> handleCancel(Map<String, String> params) {
        Map<String, Object> resultMap = new HashMap<>();
        try {
            String mid = "nicepay00m";
            String moid = "nicepay_api_3.0_test";
            String cancelMsg = "고객요청";
            String ediDate = getyyyyMMddHHmmss();

            String tid = params.get("TID");
            String cancelAmt = params.get("CancelAmt");
            String partialCancelCode = params.get("PartialCancelCode");

            String signData = encrypt(mid + cancelAmt + ediDate + merchantKey);

            StringBuilder requestData = new StringBuilder();
            requestData.append("TID=").append(tid).append("&")
                    .append("MID=").append(mid).append("&")
                    .append("Moid=").append(moid).append("&")
                    .append("CancelAmt=").append(cancelAmt).append("&")
                    .append("CancelMsg=").append(URLEncoder.encode(cancelMsg, "euc-kr")).append("&")
                    .append("PartialCancelCode=").append(partialCancelCode).append("&")
                    .append("EdiDate=").append(ediDate).append("&")
                    .append("CharSet=utf-8&")
                    .append("SignData=").append(signData);

            String jsonResponse = connectToServer(requestData.toString(), cancelUrl);

            if ("9999".equals(jsonResponse)) {
                resultMap.put("ResultCode", "9999");
                resultMap.put("ResultMsg", "통신실패");
            } else {
                resultMap.putAll(jsonStringToHashMap(jsonResponse));
            }

        } catch (Exception e) {
            resultMap.put("ResultCode", "9999");
            resultMap.put("ResultMsg", e.getMessage());
        }
        return resultMap;
    }

    private String getyyyyMMddHHmmss() {
        return new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
    }

    private String encrypt(String strData) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(strData.getBytes());
        byte[] raw = md.digest();

        StringBuilder sb = new StringBuilder();
        for (byte b : raw) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private String connectToServer(String data, String reqUrl) throws Exception {
        HttpURLConnection conn = null;
        BufferedReader reader = null;
        OutputStreamWriter writer = null;
        try {
            URL url = new URL(reqUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(25000);
            
            writer = new OutputStreamWriter(conn.getOutputStream(), "utf-8");
            writer.write(data);
            writer.flush();

            int statusCode = conn.getResponseCode();
            if (statusCode != HttpURLConnection.HTTP_OK) {
                throw new Exception("HTTP Error: " + statusCode);
            }

            reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            return response.toString();
        } finally {
            if (writer != null) writer.close();
            if (reader != null) reader.close();
            if (conn != null) conn.disconnect();
        }
    }

    private Map<String, Object> jsonStringToHashMap(String jsonStr) throws Exception {
        Map<String, Object> dataMap = new HashMap<>();
        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject) parser.parse(jsonStr);

        for (Object key : json.keySet()) {
            dataMap.put((String) key, json.get(key));
        }
        return dataMap;
    }
}
