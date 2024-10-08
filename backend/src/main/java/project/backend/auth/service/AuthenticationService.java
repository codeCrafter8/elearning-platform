package project.backend.auth.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import project.backend.auth.dto.AuthenticationRequest;
import project.backend.auth.dto.AuthenticationResponse;
import project.backend.auth.dto.RegisterRequest;
import project.backend.config.service.JwtService;
import project.backend.exception.types.ConflictException;
import project.backend.token.Token;
import project.backend.token.TokenRepository;
import project.backend.token.TokenType;
import project.backend.user.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserRepository repository;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;

    public ResponseEntity<Object> register(RegisterRequest request) {
        if (repository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Email already in use");
        }
        var user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(Role.USER)
                .build();
        repository.save(user);
        var jwtAccessToken = jwtService.generateAccessToken(user);
        var jwtRefreshToken = jwtService.generateRefreshToken(user);
        saveUserToken(user, jwtAccessToken, jwtRefreshToken);
        var userDTO = userMapper.mapToDTO(user);

        return ResponseEntity.ok(AuthenticationResponse.builder()
                .accessToken(jwtAccessToken)
                .refreshToken(jwtRefreshToken)
                .user(userDTO)
                .build());
    }

    public AuthenticationResponse registerRefactor(RegisterRequest request) {
        if (repository.findByEmail(request.getEmail()).isPresent()) {
            throw new ConflictException("Email already in use");
        }
        var user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(Role.USER)
                .build();
        repository.save(user);
        var jwtAccessToken = jwtService.generateAccessToken(user);
        var jwtRefreshToken = jwtService.generateRefreshToken(user);
        saveUserToken(user, jwtAccessToken, jwtRefreshToken);
        var userDTO = userMapper.mapToDTO(user);

        return new AuthenticationResponse(jwtAccessToken, jwtRefreshToken, userDTO);
    }

    public ResponseEntity<Object> authenticate(AuthenticationRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
            var user = repository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new Exception("User not found"));
            var jwtAccessToken = jwtService.generateAccessToken(user);
            var jwtRefreshToken = jwtService.generateRefreshToken(user);
            saveUserToken(user, jwtAccessToken, jwtRefreshToken);
            var userDTO = userMapper.mapToDTO(user);

            return ResponseEntity.ok(AuthenticationResponse.builder()
                    .accessToken(jwtAccessToken)
                    .refreshToken(jwtRefreshToken)
                    .user(userDTO)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password");
        }
    }

    private void saveUserToken(User user, String accessToken, String refreshToken) {
        var token = Token.builder()
                .user(user)
                .token(accessToken)
                .refreshToken(refreshToken)
                .token(accessToken)
                .refreshToken(refreshToken)
                .tokenType(TokenType.BEARER)
                .expired(false)
                .revoked(false)
                .build();
        tokenRepository.save(token);
    }

    private void revokeAllUserTokens(User user) {
        var validUserTokens = tokenRepository.findAllValidTokenByUser(user.getId().intValue());
        if (validUserTokens.isEmpty())
            return;
        validUserTokens.forEach(token -> {
            token.setExpired(true);
            token.setRevoked(true);
        });
        tokenRepository.saveAll(validUserTokens);
    }

    public User findUserByEmail(String email) {
        return repository.findByEmail(email).orElse(null);
    }

    public User createUserFromGooglePayload(GoogleIdToken.Payload payload) {
        User newUser = User.builder()
                .email(payload.getEmail())
                .firstName((String) payload.get("given_name"))
                .lastName((String) payload.get("family_name"))
                .password(passwordEncoder.encode("random"))
                .role(Role.USER)
                .build();
        repository.save(newUser);
        return newUser;
    }
}
