package com.example.payments.service;

import java.util.Map;

public interface service {
    Map<String, Object> handleAuthResult(Map<String, String> params);
    Map<String, Object> handleCancel(Map<String, String> params);
}
