package com.jfrengineering.digitalwallet.web.exception;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.List;

@Slf4j
@ControllerAdvice
public class TransactionExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<List<String>> validationErrorHandler(MethodArgumentNotValidException e) {
        List<String> errors = e.getFieldErrors().stream()
                .map(fieldError -> String.format("'%s' %s", fieldError.getField(), fieldError.getDefaultMessage()))
                .toList();
        log.error("Request validation failed with errors: \n" + errors);
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<String> notFoundErrorHandler(EntityNotFoundException e) {
        log.info(e.getMessage());
        return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({ DataIntegrityViolationException.class, EntityExistsException.class })
    public ResponseEntity<String> dataIntegrityViolationErrorHandler(DataIntegrityViolationException e) {
        log.warn("Transaction rejected: having repeated 'correlationId'", e);
        String responseMsg = "Transaction rejected. Another transaction with the same 'correlationId' was previously processed";
        return new ResponseEntity<>(responseMsg, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(UnacceptedTransactionAmountException.class)
    public ResponseEntity<String> unacceptedTransactionAmountErrorHandler(UnacceptedTransactionAmountException e) {
        log.info("Transaction rejected: having unaccepted amount or insufficient credit");
        return new ResponseEntity<>("Transaction rejected. " + e.getMessage(), HttpStatus.NOT_ACCEPTABLE);
    }

    @ExceptionHandler({PersistenceException.class})
    public ResponseEntity<String> persistenceExceptionErrorHandler(PersistenceException e) {
        log.error("An error with the database has occurred", e);
        return new ResponseEntity<>("There has been an error processing the transaction. Please try again later",
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
