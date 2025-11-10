package com.example.AuthService.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(ResponseStatusException ex) {
        Map<String, Object> response = new LinkedHashMap<>();

        response.put("success", false);
        response.put("code", ex.getStatusCode().value());
        response.put("message", ex.getReason());

        // ======== DEBUG OBJECT ==========
        Map<String, Object> debug = new LinkedHashMap<>();
        debug.put("headers", new HashMap<>());
        debug.put("original", buildOriginalDebug(ex));
        debug.put("exception", null);
        response.put("debug", debug);
        // =================================

        return new ResponseEntity<>(response, ex.getStatusCode());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        Map<String, Object> response = new LinkedHashMap<>();

        response.put("success", false);
        response.put("code", HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.put("message", "Internal Server Error");

        Map<String, Object> debug = new LinkedHashMap<>();
        debug.put("headers", new HashMap<>());
        debug.put("original", buildOriginalDebug(ex));
        debug.put("exception", null);
        response.put("debug", debug);

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private Map<String, Object> buildOriginalDebug(Exception ex) {
        Map<String, Object> original = new LinkedHashMap<>();
        original.put("message", ex.getMessage());
        original.put("exception", ex.getClass().getName());

        // Lấy file và dòng lỗi
        if (ex.getStackTrace().length > 0) {
            StackTraceElement element = ex.getStackTrace()[0];
            original.put("file", element.getFileName());
            original.put("line", element.getLineNumber());
        } else {
            original.put("file", null);
            original.put("line", null);
        }

        // Convert stack trace thành mảng string
        List<String> trace = new ArrayList<>();
        for (StackTraceElement element : ex.getStackTrace()) {
            trace.add(element.toString());
        }
        original.put("trace", trace);

        return original;
    }
}
