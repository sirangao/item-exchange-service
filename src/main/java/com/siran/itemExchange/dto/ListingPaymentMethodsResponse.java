package com.siran.itemExchange.dto;

import com.siran.itemExchange.dataObjects.ListingPaymentMethods;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Comparator;
import java.util.List;

@Getter
public class ListingPaymentMethodsResponse {

    private Integer listingId;
    private List<String> methods;

    public ListingPaymentMethodsResponse(Integer listingId, List<String> methods) {
        this.listingId = listingId;
        this.methods = methods;
    }

    public static ListingPaymentMethodsResponse from(Integer listingId, List<ListingPaymentMethods> rows) {
        return new ListingPaymentMethodsResponse(listingId,
                rows.stream()
                        .map(ListingPaymentMethods::getMethod)
                        .sorted(Comparator.naturalOrder())
                        .toList());
    }
}