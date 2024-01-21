package com.jfrengineering.digitalwallet.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.jfrengineering.digitalwallet.repository.BalanceRepository;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import static com.jfrengineering.digitalwallet.util.TestUtils.CUSTOMER_ID_1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerCacheServiceImplTest {

    @Mock
    private BalanceRepository balanceRepository;

    @InjectMocks
    private CustomerCacheServiceImpl underTest;

    private ListAppender<ILoggingEvent> logWatcher;

    @BeforeEach
    void setUp() {
        logWatcher = new ListAppender<>();
        logWatcher.start();
        ((Logger) LoggerFactory.getLogger(CustomerCacheServiceImpl.class)).addAppender(logWatcher);
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void customerBalanceExists_verifiesAndReturnsIfCustomerExists(boolean exists) {
        // Given
        when(balanceRepository.existsById(CUSTOMER_ID_1)).thenReturn(exists);

        // When
        boolean customerBalanceExists = underTest.customerBalanceExists(CUSTOMER_ID_1);

        // Then
        assertEquals(exists, customerBalanceExists);
        assertThat(logWatcher.list).hasSize(1);
        assertThat(logWatcher.list.get(0))
                .extracting("level", "formattedMessage")
                .containsExactly(Level.INFO, "Hitting the database to verify if customer's Balance exists, as not cached yet");
    }

    @Test
    void customerBalanceExists_rethrowsPersistenceException() {
        // Given
        when(balanceRepository.existsById(CUSTOMER_ID_1))
                .thenThrow(new PersistenceException());

        // When
        assertThrows(PersistenceException.class, () -> underTest.customerBalanceExists(CUSTOMER_ID_1));
    }
}