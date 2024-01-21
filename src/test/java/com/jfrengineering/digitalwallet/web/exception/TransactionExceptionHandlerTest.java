package com.jfrengineering.digitalwallet.web.exception;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TransactionExceptionHandlerTest {

    private TransactionExceptionHandler underTest = new TransactionExceptionHandler();

    private ListAppender<ILoggingEvent> logWatcher;

    @BeforeEach
    void setUp() {
        logWatcher = new ListAppender<>();
        logWatcher.start();
        ((Logger) LoggerFactory.getLogger(TransactionExceptionHandler.class)).addAppender(logWatcher);
    }

    @Test
    void validationErrorHandler() {
        // Given
        String objectName = "objectName";
        Map<String, String> fieldErrorsMap = Map.of(
                "fieldA", "Wrong value for fieldA",
                "fieldB", "Something incorrect with this field"
        );

        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        when(exception.getFieldErrors()).thenReturn(fieldErrorsMap.entrySet().stream()
                .map(entry -> new FieldError(objectName, entry.getKey(), entry.getValue()))
                .toList()
        );

        // When
        ResponseEntity<List<String>> responseEntity = underTest.validationErrorHandler(exception);

        // Then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        List<String> expectedErrorList = fieldErrorsMap.entrySet().stream()
                .map(entry -> String.format("'%s' %s", entry.getKey(), entry.getValue()))
                .toList();
        assertThat(responseEntity.getBody()).isEqualTo(expectedErrorList);

        // And
        assertThat(logWatcher.list.size()).isEqualTo(1);
        verifyLogs(Level.ERROR, "Request validation failed with errors: \n" + expectedErrorList);
    }

    @Test
    void notFoundErrorHandler() {
        // Given
        String errorMessage = "Entity not found";
        EntityNotFoundException exception = new EntityNotFoundException(errorMessage);

        // When
        ResponseEntity<String> responseEntity = underTest.notFoundErrorHandler(exception);

        // Then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(responseEntity.getBody()).isEqualTo(errorMessage);

        // And
        verifyLogs(Level.INFO, errorMessage);
    }

    @Test
    void dataIntegrityViolationErrorHandler() {
        // Given
        DataIntegrityViolationException exception = new DataIntegrityViolationException("");

        // When
        ResponseEntity<String> responseEntity = underTest.dataIntegrityViolationErrorHandler(exception);

        // Then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(responseEntity.getBody())
                .isEqualTo("Transaction rejected. Another transaction with the same 'correlationId' was previously processed");

        // And
        verifyLogs(Level.WARN, "Transaction rejected: having repeated 'correlationId'");
    }

    @Test
    void unacceptedTransactionAmountErrorHandler() {
        // Given
        String exceptionMessage = "An exception";
        UnacceptedTransactionAmountException exception = new UnacceptedTransactionAmountException(exceptionMessage);

        // When
        ResponseEntity<String> responseEntity = underTest.unacceptedTransactionAmountErrorHandler(exception);

        // Then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_ACCEPTABLE);
        assertThat(responseEntity.getBody()).isEqualTo("Transaction rejected. " + exceptionMessage);

        // And
        verifyLogs(Level.INFO, "Transaction rejected: having unaccepted amount or insufficient credit");
    }

    @Test
    void persistenceExceptionErrorHandler() {
        // Given
        PersistenceException exception = new PersistenceException("");

        // When
        ResponseEntity<String> responseEntity = underTest.persistenceExceptionErrorHandler(exception);

        // Then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(responseEntity.getBody())
                .isEqualTo("There has been an error processing the transaction. Please try again later");

        // And
        verifyLogs(Level.ERROR, "An error with the database has occurred");
    }

    private void verifyLogs(Level level, String message) {
        assertThat(logWatcher.list.size()).isEqualTo(1);
        assertThat(logWatcher.list.get(0))
                .extracting("level", "formattedMessage")
                .containsExactly(level, message);
    }
}