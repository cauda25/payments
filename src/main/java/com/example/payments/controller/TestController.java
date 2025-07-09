package com.example.payments.controller;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.apache.commons.codec.binary.Hex;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;


@Controller
public class TestController {

        String merchantKey 		= "EYzu8jGGMfqaDEp76gSckuvnaHHu+bC4opsSN6lHv3b2lurNYkVXrZ7Z1AoqQnXI3eLuaUFyoRNC6FkrzVjceg=="; // 상점키


    @GetMapping("/testRequest")
    public String getMethodName(Model model) {
        String mID 		= "nicepay00m"; 				// 상점아이디
        String goodsName 		= "나이스페이"; 					// 결제상품명
        String amt 			= "1004"; 						// 결제상품금액	
        String buyerName 		= "나이스"; 						// 구매자명
        String buyerTel 		= "01000000000"; 				// 구매자연락처
        String buyerEmail 		= "happy@day.co.kr"; 			// 구매자메일주소
        String moid 			= "mnoid1234567890"; 			// 상품주문번호	
      

        DataEncrypt sha256Enc 	= new DataEncrypt();
        String ediDate 			= getyyyyMMddHHmmss();	
        String hashString 		= sha256Enc.encrypt(ediDate + mID + amt + merchantKey);

        model.addAttribute("GoodsName", goodsName);
        model.addAttribute("Amt", amt);
        model.addAttribute("MID", mID);
        model.addAttribute("Moid", moid);
        model.addAttribute("BuyerName", buyerName);
        model.addAttribute("BuyerEmail", buyerEmail);
        model.addAttribute("BuyerTel", buyerTel);

        model.addAttribute("EdiDate", ediDate);
        model.addAttribute("SignData", hashString);

        return "/testRequest";

    }


    public final synchronized String getyyyyMMddHHmmss(){
        SimpleDateFormat yyyyMMddHHmmss = new SimpleDateFormat("yyyyMMddHHmmss");
        return yyyyMMddHHmmss.format(new Date());
    }
    // SHA-256 형식으로 암호화
    public class DataEncrypt{
        MessageDigest md;
        String strSRCData = "";
        String strENCData = "";
        String strOUTData = "";
        
        public DataEncrypt(){ }
        public String encrypt(String strData){
            String passACL = null;
            MessageDigest md = null;
            try{
                md = MessageDigest.getInstance("SHA-256");
                md.reset();
                md.update(strData.getBytes());
                byte[] raw = md.digest();
                passACL = encodeHex(raw);
            }catch(Exception e){
                System.out.print("암호화 에러" + e.toString());
            }
            return passACL;
        }
        
        public String encodeHex(byte [] b){
            char [] c = Hex.encodeHex(b);
            return new String(c);
        }
    }

      @PostMapping("/result")
    public String handlePaymentResult(HttpServletRequest request, Model model) {
        String authResultCode = request.getParameter("AuthResultCode");
        String authResultMsg = request.getParameter("AuthResultMsg");
        String authToken = request.getParameter("AuthToken");
        String payMethod = request.getParameter("PayMethod");
        String mid = request.getParameter("MID");
        String amt = request.getParameter("Amt");
        String txTid = request.getParameter("TxTid");
        String nextAppURL = request.getParameter("NextAppURL");

        // 위변조 방지용 데이터 생성
        String ediDate = getyyyyMMddHHmmss();
        String signData = encrypt(authToken + mid + amt + ediDate + merchantKey);

        // 승인 요청 (Server to Server)
        String reqBody = "TID=" + txTid +
                "&AuthToken=" + authToken +
                "&MID=" + mid +
                "&Amt=" + amt +
                "&EdiDate=" + ediDate +
                "&CharSet=utf-8" +
                "&SignData=" + signData;

        Map<String, Object> resultMap = new HashMap<>();
        try {
            String response = connectToServer(reqBody, nextAppURL);
            resultMap = parseJsonToMap(response); // JSON 응답 → Map 변환
        } catch (Exception e) {
            resultMap.put("ResultCode", "9999");
            resultMap.put("ResultMsg", "서버 통신 실패");
        }

        model.addAttribute("result", resultMap);
        return "payResult"; // → payResult.html
    }

    // ---- 유틸 메서드 ----

    // private String getyyyyMMddHHmmss() {
    //     return new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
    // }

    private String encrypt(String str) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(str.getBytes(StandardCharsets.UTF_8));
            return Hex.encodeHexString(hash);
        } catch (Exception e) {
            return null;
        }
    }

    private String connectToServer(String data, String urlStr) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(25000);
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");

        try (PrintWriter out = new PrintWriter(conn.getOutputStream())) {
            out.write(data);
        }

        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) result.append(line);
        }

        return result.toString();
    }

    private Map<String, Object> parseJsonToMap(String json) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, new TypeReference<Map<String, Object>>() {});
    }

    private final NicepayService nicepayService;

    public NicepayPaymentController(NicepayService nicepayService) {
        this.nicepayService = nicepayService;
    }

    @PostMapping("/auth")
    public String authResult(@RequestParam Map<String, String> params, Model model) {
        Map<String, Object> result = nicepayService.handleAuthResult(params);
        model.addAllAttributes(result);
        return "nicepay/payResult"; // resources/templates/nicepay/payResult.html
    }

    @PostMapping("/cancel")
    public String cancelPayment(@RequestParam Map<String, String> params, Model model) {
        Map<String, Object> result = nicepayService.handleCancel(params);
        model.addAllAttributes(result);
        return "nicepay/cancelResult"; // resources/templates/nicepay/cancelResult.html
    }
}



     @PostMapping("/nicepay/auth")
    public String handleAuthResult(
            @RequestParam String AuthResultCode,
            @RequestParam String AuthResultMsg,
            @RequestParam String NextAppURL,
            @RequestParam String TxTid,
            @RequestParam String AuthToken,
            @RequestParam String PayMethod,
            @RequestParam String MID,
            @RequestParam String Moid,
            @RequestParam String Amt,
            @RequestParam(required = false) String ReqReserved,
            @RequestParam String NetCancelURL,
            Model model
    ) throws Exception {

        DataEncrypt sha256Enc = new DataEncrypt();
        String merchantKey = "EYzu8jGGMfqaDEp76gSckuvnaHHu+bC4opsSN6lHv3b2lurNYkVXrZ7Z1AoqQnXI3eLuaUFyoRNC6FkrzVjceg==";

        String ResultCode = "";
        String ResultMsg = "";
        String payMethod = "";
        String goodsName = "";
        String amount = "";
        String tid = "";
        String resultJsonStr = "";

        if ("0000".equals(AuthResultCode)) {
            String ediDate = getyyyyMMddHHmmss();
            String signData = sha256Enc.encrypt(AuthToken + MID + Amt + ediDate + merchantKey);

            StringBuilder requestData = new StringBuilder();
            requestData.append("TID=").append(TxTid).append("&")
                    .append("AuthToken=").append(AuthToken).append("&")
                    .append("MID=").append(MID).append("&")
                    .append("Amt=").append(Amt).append("&")
                    .append("EdiDate=").append(ediDate).append("&")
                    .append("CharSet=utf-8&SignData=").append(signData);

            resultJsonStr = HttpClientUtil.post(NextAppURL, requestData.toString());

            if ("9999".equals(resultJsonStr)) {
                requestData.append("&NetCancel=1");
                String cancelResultJsonStr = HttpClientUtil.post(NetCancelURL, requestData.toString());
                HashMap<String, String> cancelResultData = JsonUtil.toHashMap(cancelResultJsonStr);
                ResultCode = cancelResultData.get("ResultCode");
                ResultMsg = cancelResultData.get("ResultMsg");
            } else {
                HashMap<String, String> resultData = JsonUtil.toHashMap(resultJsonStr);
                ResultCode = resultData.get("ResultCode");
                ResultMsg = resultData.get("ResultMsg");
                payMethod = resultData.get("PayMethod");
                goodsName = resultData.get("GoodsName");
                amount = resultData.get("Amt");
                tid = resultData.get("TID");
            }
        } else {
            ResultCode = AuthResultCode;
            ResultMsg = AuthResultMsg;
        }

        model.addAttribute("ResultCode", ResultCode);
        model.addAttribute("ResultMsg", ResultMsg);
        model.addAttribute("PayMethod", payMethod);
        model.addAttribute("GoodsName", goodsName);
        model.addAttribute("Amt", amount);
        model.addAttribute("TID", tid);

        return "payResult";
    }

   public class JsonUtil {

    public static HashMap<String, String> toHashMap(String jsonStr) throws Exception {
        HashMap<String, String> map = new HashMap<>();
        JSONParser parser = new JSONParser();

        Object obj = parser.parse(jsonStr);
        JSONObject jsonObject = (JSONObject) obj;

        Iterator<?> keys = jsonObject.keySet().iterator();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            Object value = jsonObject.get(key);
            map.put(key, value != null ? value.toString() : "");
        }

        return map;
    }
}
public class HttpClientUtil {

    public static String post(String requestUrl, String requestData) {
        HttpURLConnection conn = null;
        BufferedReader reader = null;
        PrintWriter writer = null;
        StringBuilder result = new StringBuilder();

        try {
            URL url = new URL(requestUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(5000);
            conn.setDoOutput(true);

            writer = new PrintWriter(new OutputStreamWriter(conn.getOutputStream(), "UTF-8"));
            writer.write(requestData);
            writer.flush();

            reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line).append("\n");
            }

            return result.toString().trim();
        } catch (Exception e) {
            return "9999";
        } finally {
            try {
                if (writer != null) writer.close();
                if (reader != null) reader.close();
                if (conn != null) conn.disconnect();
            } catch (Exception ignored) {}
        }
    }
}

    
}
    
    

