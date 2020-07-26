package com.wirehall.commandbuilder.exception;

import com.wirehall.commandbuilder.dto.ErrorResponse;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler({BadCredentialsException.class})
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  @ResponseBody
  private ErrorResponse handleUnauthorizedExceptions(Exception ex) {
    return new ErrorResponse(ex.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ResponseBody
  private ErrorResponse handleValidationExceptions(MethodArgumentNotValidException ex) {
    // Get all the field validation error messages
    List<String> errorMessages = ex.getBindingResult().getFieldErrors().stream()
        .map(f -> f.getField() + " : " + f.getDefaultMessage()).collect(Collectors.toList());

    ErrorResponse errorResponse = new ErrorResponse("Validation Failed");
    errorResponse.setDetails(errorMessages);
    return errorResponse;
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  @ResponseBody
  private ErrorResponse handleAll(Exception ex) {
    return new ErrorResponse(ex.getMessage());
  }
}
