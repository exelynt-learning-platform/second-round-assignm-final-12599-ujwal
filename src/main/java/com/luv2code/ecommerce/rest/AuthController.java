package com.luv2code.ecommerce.rest;

import com.luv2code.ecommerce.dto.LoginRequest;
import com.luv2code.ecommerce.dto.RegisterRequest;
import com.luv2code.ecommerce.entity.User;
import com.luv2code.ecommerce.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;

    /**
     * Helper method to create consistent error response
     */
    private Map<String, Object> createErrorResponse(String error, String code) {
        return Map.of("error", error, "code", code);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req, BindingResult binding) {
        if (binding.hasErrors()) {
            Map<String, String> errs = new HashMap<>();
            binding.getFieldErrors().forEach(e -> 
                errs.put(e.getField(), e.getDefaultMessage())
            );
            return ResponseEntity.badRequest().body(errs);
        }

        try {
            User user = authService.registerUser(
                    req.getUsername(),
                    req.getEmail(),
                    req.getPassword(),
                    req.getFirstName(),
                    req.getLastName()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(user);
        } catch (IllegalArgumentException e) {
            logger.warn("Registration validation failed: {}", e.getMessage());
            String msg = e.getMessage();
            HttpStatus status = HttpStatus.BAD_REQUEST;
            if (msg != null && msg.contains("Username")) {
                return ResponseEntity.status(status).body(createErrorResponse("Username already taken", "USERNAME_TAKEN"));
            } else if (msg != null && msg.contains("Email")) {
                return ResponseEntity.status(status).body(createErrorResponse("Email already registered", "EMAIL_TAKEN"));
            }
            return ResponseEntity.status(status).body(createErrorResponse(e.getMessage(), "VALIDATION_ERROR"));
        } catch (RuntimeException e) {
            logger.error("Registration error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(createErrorResponse("Registration failed", "REGISTRATION_ERROR"));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        try {
            String token = authService.loginUser(req.getUsername(), req.getPassword());
            Map<String, String> res = new HashMap<>();
            res.put("token", token);
            return ResponseEntity.ok(res);
        } catch (IllegalArgumentException e) {
            logger.warn("Login validation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                createErrorResponse("Invalid credentials", "INVALID_CREDENTIALS")
            );
        } catch (RuntimeException e) {
            logger.error("Authentication error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                createErrorResponse("Authentication failed", "AUTH_ERROR")
            );
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> me() {
        try {
            User u = authService.getCurrentUser();
            if (u == null) {
                logger.warn("User not authenticated when accessing profile");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("Not authenticated", "NOT_AUTHENTICATED"));
            }
            return ResponseEntity.ok(u);
        } catch (RuntimeException e) {
            logger.error("Error retrieving user profile: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Failed to retrieve user profile", "PROFILE_ERROR"));
        }
    }

    @PutMapping("/update")
    public ResponseEntity<?> update(@RequestBody Map<String, String> data) {
        try {
            User current = authService.getCurrentUser();
            if (current == null) {
                logger.warn("Unauthorized update attempt");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("Not authenticated", "NOT_AUTHENTICATED"));
            }

            User updated = authService.updateUser(
                    current.getId(),
                    data.get("firstName"),
                    data.get("lastName"),
                    data.get("phoneNumber"),
                    data.get("address"),
                    data.get("city"),
                    data.get("state"),
                    data.get("zipCode"),
                    data.get("country")
            );
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            logger.warn("Profile update validation failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(createErrorResponse(e.getMessage(), "VALIDATION_ERROR"));
        } catch (RuntimeException e) {
            logger.error("Profile update error: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(createErrorResponse("Failed to update profile", "UPDATE_ERROR"));
        }
    }
}
