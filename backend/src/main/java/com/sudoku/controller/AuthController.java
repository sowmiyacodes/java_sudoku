package com.sudoku.controller;

import com.sudoku.dto.LoginRequest;
import com.sudoku.dto.RegisterRequest;
import com.sudoku.dto.UpdateProfileRequest;
import com.sudoku.dto.UserProfileResponse;
import com.sudoku.model.User;
import com.sudoku.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Locale;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    public AuthController(
            UserService userService,
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository
    ) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        try {
            User user = userService.register(request);
            authenticate(user.getUsername(), request.password(), servletRequest, servletResponse);
            return ResponseEntity.status(HttpStatus.CREATED).body(UserProfileResponse.from(user));
        } catch (org.springframework.web.server.ResponseStatusException exception) {
            return ResponseEntity.status(exception.getStatusCode()).body(Map.of("message", exception.getReason()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        try {
            Authentication authentication = authenticate(
                    request.identifier().trim().toLowerCase(Locale.ROOT),
                    request.password(),
                    servletRequest,
                    servletResponse
            );
            User user = userService.getByUsername(authentication.getName());
            return ResponseEntity.ok(UserProfileResponse.from(user));
        } catch (BadCredentialsException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid username/email or password."));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        new SecurityContextLogoutHandler().logout(request, response, SecurityContextHolder.getContext().getAuthentication());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(@RequestAttribute("_csrf") CsrfToken csrfToken) {
        return Map.of("token", csrfToken.getToken(), "headerName", csrfToken.getHeaderName());
    }

    @GetMapping("/me")
    public UserProfileResponse currentUser(Authentication authentication) {
        return UserProfileResponse.from(userService.getByUsername(authentication.getName()));
    }

    @PutMapping("/me")
    public UserProfileResponse updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return UserProfileResponse.from(userService.updateProfile(authentication.getName(), request));
    }

    private Authentication authenticate(
            String identifier,
            String password,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(identifier, password));
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
        return authentication;
    }
}