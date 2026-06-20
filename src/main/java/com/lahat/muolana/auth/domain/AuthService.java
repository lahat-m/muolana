package com.lahat.muolana.auth.domain;

import com.lahat.muolana.auth.config.JwtTokenProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public AuthResponse register(RegisterCmd cmd) {
        createAdminUser(cmd);
        UserEntity user = userRepository.getByEmail(cmd.email().toLowerCase());
        return buildTokens(user);
    }

    public UserVM createAdminUser(RegisterCmd cmd) {
        if (userRepository.existsByEmail(cmd.email().toLowerCase())) {
            throw new AuthExceptions.EmailAlreadyInUseException();
        }
        String hash = passwordEncoder.encode(cmd.password());
        UserEntity user = new UserEntity(cmd.email().toLowerCase(), hash, cmd.fullName(), Role.ADMIN);
        return toVM(userRepository.save(user));
    }

    public void createAdminIfAbsent(String email, String password, String fullName) {
        if (userRepository.existsByEmail(email.toLowerCase())) return;
        String hash = passwordEncoder.encode(password);
        userRepository.save(new UserEntity(email.toLowerCase(), hash, fullName, Role.ADMIN));
    }

    public AuthResponse login(String email, String password) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email.toLowerCase(), password));
        } catch (BadCredentialsException exception) {
            throw new AuthExceptions.InvalidCredentialsException();
        }
        UserEntity user = userRepository.getByEmail(email.toLowerCase());
        return buildTokens(user);
    }

    public AuthResponse refresh(String refreshToken) {
        Jwt decoded;
        try {
            decoded = jwtTokenProvider.decode(refreshToken);
        } catch (Exception exception) {
            throw new AuthExceptions.InvalidRefreshTokenException();
        }
        if (!"refresh".equals(decoded.getClaimAsString("type"))) {
            throw new AuthExceptions.InvalidRefreshTokenException();
        }
        String jti = decoded.getClaimAsString("jti");
        RefreshTokenEntity stored = refreshTokenRepository.findByJti(jti)
                .orElseThrow(AuthExceptions.InvalidRefreshTokenException::new);
        if (stored.isRevoked()) {
            throw new AuthExceptions.TokenRevokedException();
        }
        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw new AuthExceptions.InvalidRefreshTokenException();
        }
        refreshTokenRepository.delete(stored);
        UserEntity user = userRepository.getByEmail(decoded.getSubject());
        return buildTokens(user);
    }

    public void logout(String refreshToken) {
        try {
            Jwt decoded = jwtTokenProvider.decode(refreshToken);
            String jti = decoded.getClaimAsString("jti");
            refreshTokenRepository.findByJti(jti).ifPresent(storedToken -> {
                storedToken.revoke();
                refreshTokenRepository.save(storedToken);
            });
        } catch (Exception ignored) {
            // invalid token on logout — treat as already logged out
        }
    }

    @Transactional(readOnly = true)
    public UserVM getUserById(UUID userId) {
        return toVM(userRepository.getById(userId));
    }

    @Transactional(readOnly = true)
    public Page<UserVM> listUsers(String role, Boolean isActive, Pageable pageable) {
        if (role != null && isActive != null) {
            Role resolvedRole = Role.valueOf(role.toUpperCase());
            return userRepository.findByRoleAndIsActive(resolvedRole, isActive, pageable).map(this::toVM);
        }
        if (role != null) {
            Role resolvedRole = Role.valueOf(role.toUpperCase());
            return userRepository.findByRole(resolvedRole, pageable).map(this::toVM);
        }
        if (isActive != null) {
            return userRepository.findByIsActive(isActive, pageable).map(this::toVM);
        }
        return userRepository.findAll(pageable).map(this::toVM);
    }

    public UserVM updateUser(UUID userId, UpdateUserCmd cmd) {
        UserEntity user = userRepository.getById(userId);
        if (cmd.role() != null) user.changeRole(cmd.role());
        if (cmd.isActive() != null) {
            if (cmd.isActive()) user.activate();
            else user.deactivate();
        }
        return toVM(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public UserVM getUserByEmail(String email) {
        return toVM(userRepository.getByEmail(email.toLowerCase()));
    }

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email.toLowerCase());
    }

    private AuthResponse buildTokens(UserEntity user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String jti = UUID.randomUUID().toString();
        long refreshExpiry = jwtTokenProvider.getRefreshExpiration();
        String refreshToken = jwtTokenProvider.generateRefreshToken(user, jti);
        Instant expiresAt = Instant.now().plusMillis(refreshExpiry);
        refreshTokenRepository.save(new RefreshTokenEntity(user.getId(), jti, expiresAt));
        return new AuthResponse(accessToken, refreshToken, jwtTokenProvider.getAccessExpiration());
    }

    private UserVM toVM(UserEntity userEntity) {
        return new UserVM(userEntity.getId(), userEntity.getEmail(), userEntity.getFullName(), userEntity.getRole().name(), userEntity.isActive(), userEntity.getCreatedAt());
    }
}
