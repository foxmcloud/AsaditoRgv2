package com.appasadito.AsaditoRg.dtos;

import javax.validation.constraints.*;

public class CrearOperadorRequest {
    @NotBlank @Email @Size(max = 120)
    public String email;
    @NotBlank @Size(min = 10, max = 72)
    public String password;
    public String getEmail() { return email; }
    public String getPassword() { return password; }
}
