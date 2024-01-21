package com.jfrengineering.digitalwallet.web.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.EqualsAndHashCode;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

@EqualsAndHashCode
public class TransactionsPageResponse extends PageImpl<TransactionResponse> {

    private final UUID customerId;
    private final List<TransactionResponse> content;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public TransactionsPageResponse(@JsonProperty("customerId") UUID customerId,
                                    @JsonProperty("content") List<TransactionResponse> content,
                                    @JsonProperty("number") int pageNumber,
                                    @JsonProperty("size") int pageSize,
                                    @JsonProperty("totalElements") Long totalElements,
                                    @JsonProperty("pageable") JsonNode pageable,
                                    @JsonProperty("last") boolean last,
                                    @JsonProperty("totalPages") int totalPages,
                                    @JsonProperty("sort") JsonNode sort,
                                    @JsonProperty("first") boolean first,
                                    @JsonProperty("numberOfElements") int numberOfElements) {
        super(content, PageRequest.of(pageNumber, pageSize), totalElements);
        this.customerId = customerId;
        this.content = content;
    }

    public TransactionsPageResponse(UUID customerId, List<TransactionResponse> content, Pageable pageable, long total) {
        super(content, pageable, total);
        this.customerId = customerId;
        this.content = content;
    }
}
