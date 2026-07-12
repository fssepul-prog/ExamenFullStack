package com.foodmarket.report.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;

/**
 * Manejo centralizado de errores del report-service (@RestControllerAdvice),
 * consistente con el resto de los microservicios del ecosistema.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> typeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("[REPORT] Parametro con tipo invalido: {}", e.getName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "El parámetro '" + e.getName() + "' tiene un formato inválido"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> general(Exception e) {
        log.error("[REPORT] Error interno no controlado: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error interno del servidor"));
    }
}
