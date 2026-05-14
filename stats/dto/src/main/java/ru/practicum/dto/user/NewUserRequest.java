package ru.practicum.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewUserRequest {

    @NotBlank(message = "Field: email. Error: must not be blank")
    @Email(message = "Field: email. Error: must be a valid email")
    @Size(min = 6, max = 254, message = "Field: email. Error: must be between 6 and 254 characters")
    private String email;

    @NotBlank(message = "Field: name. Error: must not be blank")
    @Size(min = 2, max = 250, message = "Field: name. Error: size must be between 2 and 250 characters")
    private String name;
}
