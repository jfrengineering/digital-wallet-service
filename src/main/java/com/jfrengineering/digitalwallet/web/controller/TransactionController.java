package com.jfrengineering.digitalwallet.web.controller;

import com.jfrengineering.digitalwallet.service.TransactionService;
import com.jfrengineering.digitalwallet.web.model.TransactionBalanceResponse;
import com.jfrengineering.digitalwallet.web.model.TransactionRequest;
import com.jfrengineering.digitalwallet.web.model.TransactionsPageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @Operation(summary = "Get Customer Transactions given its id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Page of Transactions",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TransactionsPageResponse.class)) }),
            @ApiResponse(responseCode = "404", description = "Customer not found",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string")))
    })
    @GetMapping("/{customerId}")
    public ResponseEntity<TransactionsPageResponse> getCustomerTransactions(
            @PathVariable UUID customerId,
            @RequestParam(required = false, defaultValue = "0") int pageNumber,
            @RequestParam(required = false, defaultValue = "10") int pageSize
    ) {
        log.info("Received request to get customer transactions for customer with ID " + customerId);
        return ResponseEntity.ok(transactionService.getTransactionsByCustomerId(customerId, pageNumber, pageSize));
    }

    @Operation(summary = "Create a Debit/Credit Transaction")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Transaction created",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TransactionBalanceResponse.class)) }),
            @ApiResponse(responseCode = "400", description = "Bad Transaction request, one or more fields with invalid values",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string"))),
            @ApiResponse(responseCode = "404", description = "Customer not found",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string"))),
            @ApiResponse(responseCode = "409", description = "Rejected Transaction with repeated 'correlationId'",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string"))),
            @ApiResponse(responseCode = "406", description = "Rejected Transaction with wrong 'amount'",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string")))
    })
    @PostMapping
    public ResponseEntity<TransactionBalanceResponse> createTransaction(@Valid @RequestBody TransactionRequest transactionRequest) {
        log.info("Received request to create transaction: " + transactionRequest);
        TransactionBalanceResponse transactionBalanceResponse = transactionService.createTransaction(transactionRequest);
        return new ResponseEntity<>(transactionBalanceResponse, HttpStatus.CREATED);
    }
}
