package com.jfrengineering.digitalwallet.service;

import com.jfrengineering.digitalwallet.repository.BalanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerCacheServiceImpl implements CustomerCacheService {

    private final BalanceRepository balanceRepository;

    @Override
    @Cacheable(value = "customerCache", key = "#customerId.toString()")
    public boolean customerBalanceExists(UUID customerId) {
        log.info("Hitting the database to verify if customer's Balance exists, as not cached yet");
        return balanceRepository.existsById(customerId);
    }
}
