package com.jhanp.psraerobicsbackend.auth.controller;

import com.jhanp.psraerobicsbackend.auth.dto.request.ChangePasswordRequest;
import com.jhanp.psraerobicsbackend.auth.dto.request.ForgotPasswordRequest;
import com.jhanp.psraerobicsbackend.auth.dto.request.LoginRequest;
import com.jhanp.psraerobicsbackend.auth.dto.request.ResetPasswordRequest;
import com.jhanp.psraerobicsbackend.auth.dto.response.AuthResponse;
import com.jhanp.psraerobicsbackend.auth.service.IAuthService;
import com.jhanp.psraerobicsbackend.auth.service.IPasswordRecoveryService;
import com.jhanp.psraerobicsbackend.auth.service.IPasswordResetService;
import com.jhanp.psraerobicsbackend.shared.response.MessageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final IAuthService authService;
    private final IPasswordRecoveryService passwordRecoveryService;
    private final IPasswordResetService passwordResetService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /** Responde 200 con el mismo mensaje exista o no el correo. */
    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(passwordRecoveryService.requestReset(request));
    }

    /** Canjea el token del correo por una contraseña nueva. Sirve para invitación y para olvido. */
    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(passwordResetService.reset(request));
    }

    /**
     * Cambio de contraseña estando dentro. No lleva @PreAuthorize porque la ruta ya exige
     * autenticación en SecurityConfig, y el identificador sale del token: nadie puede cambiarle la
     * contraseña a otro por mucho que manipule el body.
     */
    @PatchMapping("/change-password")
    public ResponseEntity<MessageResponse> changePassword(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody ChangePasswordRequest request) {
        return ResponseEntity.ok(authService.changePassword(user.getUsername(), request));
    }
}
