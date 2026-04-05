package com.finance.dashboard.dto.request;

import com.finance.dashboard.model.enums.Role;
import com.finance.dashboard.model.enums.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * All fields are optional — only non-null fields are applied during update.
 */
@Data
public class UpdateUserRequest {

    @Email(message = "Must be a valid email address")
    private String email;

    @Size(max = 80, message = "Full name must not exceed 80 characters")
    private String fullName;

    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    private Role role;

    private UserStatus status;
}
