package com.jfrengineering.digitalwallet.web.exception;

public class UnacceptedTransactionAmountException extends RuntimeException {
    public UnacceptedTransactionAmountException(String reason) {
        super(reason);
    }
}
