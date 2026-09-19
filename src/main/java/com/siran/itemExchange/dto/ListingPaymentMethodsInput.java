package com.siran.itemExchange.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ListingPaymentMethodsInput {

    @NotEmpty
    private List<@Pattern(regexp = "cash|venmo|zelle|paypal",
            message = "must be one of: cash, venmo, zelle, paypal") String> methods;
}