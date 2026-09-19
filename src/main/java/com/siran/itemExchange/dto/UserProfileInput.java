package com.siran.itemExchange.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * The editable half of a profile. Separate from UserInput because that one requires
 * username and password, and UserResponse never hands the password back to the client.
 */
@Getter
@Setter
public class UserProfileInput {

    @NotBlank
    @Email
    @Size(max = 100)
    private String email;

    @Size(max = 20)
    private String phone;

    @Size(max = 100)
    private String college;
}
