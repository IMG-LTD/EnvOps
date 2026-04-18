package com.img.envops.framework.web;

import com.img.envops.common.exception.ConflictException;
import com.img.envops.common.exception.NotFoundException;
import com.img.envops.common.exception.UnauthorizedException;
import com.img.envops.common.response.R;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<R<Void>> handleIllegalArgumentException(IllegalArgumentException exception) {
    return ResponseEntity.badRequest().body(R.fail("400", exception.getMessage()));
  }

  @ExceptionHandler(UnauthorizedException.class)
  public ResponseEntity<R<Void>> handleUnauthorizedException(UnauthorizedException exception) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(R.fail("401", exception.getMessage()));
  }

  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<R<Void>> handleConflictException(ConflictException exception) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(R.fail("409", exception.getMessage()));
  }

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<R<Void>> handleNotFoundException(NotFoundException exception) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(R.fail("404", exception.getMessage()));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<R<Void>> handleDataIntegrityViolationException(DataIntegrityViolationException exception) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(R.fail("409", "Resource already exists or is still referenced"));
  }

  @ExceptionHandler({MethodArgumentTypeMismatchException.class, TypeMismatchException.class})
  public ResponseEntity<R<Void>> handleTypeMismatchException(TypeMismatchException exception) {
    String parameterName = exception instanceof MethodArgumentTypeMismatchException methodArgumentTypeMismatchException
        ? methodArgumentTypeMismatchException.getName()
        : null;
    Object value = exception.getValue();
    String renderedValue = value == null ? "null" : value.toString();
    String message = parameterName == null
        ? "Invalid request parameter value: " + renderedValue
        : "Invalid value for parameter '" + parameterName + "': " + renderedValue;
    return ResponseEntity.badRequest().body(R.fail("400", message));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<R<Void>> handleException(Exception exception) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(R.fail("500", "Internal server error"));
  }
}
