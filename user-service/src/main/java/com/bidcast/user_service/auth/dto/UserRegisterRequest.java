package com.bidcast.user_service.auth.dto;

import java.util.Set;

import com.bidcast.user_service.user.UserRole;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record UserRegisterRequest(
    
    @NotBlank(message = "El nombre es obligatorio")
    String fullName,

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El formato del email es inválido")
    String email,

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    String password,

    @NotEmpty(message = "Debes seleccionar al menos un rol")
    Set<UserRole> roles 
) {}

