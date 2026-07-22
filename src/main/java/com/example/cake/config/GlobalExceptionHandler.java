
package com.example.cake.config;

import com.example.cake.response.ResponseMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

        import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e ->
                errors.put(e.getField(), e.getDefaultMessage())
        );
        return ResponseEntity.badRequest().body(
                new ResponseMessage<>(false, "Dữ liệu không hợp lệ!", errors)
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneralException(Exception ex) {
        System.err.println("❌ Unhandled exception caught: " + ex.getMessage());
        ex.printStackTrace();
        return ResponseEntity.internalServerError().body(
                new ResponseMessage<>(false, "Lỗi hệ thống: " + ex.getMessage(), null)
        );
    }
}
