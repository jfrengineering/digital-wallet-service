package com.jfrengineering.digitalwallet.service;

import java.util.UUID;

public interface CustomerCacheService {
    boolean customerBalanceExists(UUID customerId);
}
