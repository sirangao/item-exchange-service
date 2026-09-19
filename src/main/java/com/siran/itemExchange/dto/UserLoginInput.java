package com.siran.itemExchange.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserLoginInput {

    @NotBlank
    private String username;

    @NotBlank
    private String password;
}
