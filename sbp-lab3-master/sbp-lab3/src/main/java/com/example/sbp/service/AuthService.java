package com.example.sbp.service;

import com.example.sbp.config.CamundaUserCreatePlugin;
import com.example.sbp.dto.UserResponseDTO;
import com.example.sbp.security.CustomUserDetails;
import com.example.sbp.security.JwtTokenProvider;
import com.example.sbp.security.XmlUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.identity.User;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.sbp.exception.RoleNotFoundException;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final XmlUserDetailsService userDetailsService;
    private final IdentityService identityService;

    @Transactional
    public String login(String username, String password) {
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(username, password);
        authenticationManager.authenticate(authToken);

        userDetailsService.incrementTokenVersion(username);

        CustomUserDetails userDetails = userDetailsService.getUser(username);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        return tokenProvider.generateToken(authentication);
    }

    @Transactional
    public String register(
            String username,
            String password,
            String email,
            String firstName,
            String lastName
    ) {
        userDetailsService.createUser(username, password, email, firstName, lastName);

        CamundaUserCreatePlugin.markPlainCreateFlow();
        createCamundaUser(username, password, email, firstName, lastName);
        CamundaUserCreatePlugin.clearPlainCreateFlow();

        CustomUserDetails userDetails = userDetailsService.getUser(username);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        return tokenProvider.generateToken(authentication);
    }

    private void createCamundaUser(String username, String password, String email, String firstName, String lastName) {
        try {
            User existing = identityService.createUserQuery().userId(username).singleResult();
            if (existing == null) {
                User user = identityService.newUser(username);
                user.setPassword(password);
                user.setEmail(email);
                user.setFirstName(firstName);
                user.setLastName(lastName);
                identityService.saveUser(user);
                log.debug("TEST Создан пользователь Camunda: {}", username);
            } else {
                log.debug("TEST Пользователь Camunda уже существует, пропускаем создание: {}", username);
            }
        } catch (Exception e) {
            log.warn("TEST Не удалось создать пользователя Camunda для {}: {}", username, e.getMessage());
        }
    }

    private void addToGroup(String username, String groupId) {
        try {
            long membership = identityService.createGroupQuery()
                    .groupId(groupId)
                    .groupMember(username)
                    .count();
            if (membership == 0) {
                identityService.createMembership(username, groupId);
                log.debug("TEST Пользователь Camunda добавлен в группу {}: {}", groupId, username);
            }
        } catch (Exception e) {
            log.warn("TEST Не удалось добавить {} в группу {}: {}", username, groupId, e.getMessage());
        }
    }

    public UserResponseDTO getUser(String username) {
        CustomUserDetails user = userDetailsService.getUser(username);
        return new UserResponseDTO(user.getUsername(), user.getRole());
    }

    @Transactional
    public UserResponseDTO updateUserRole(String username, String role) {
        String upperRole = role.toUpperCase();

        // Проверяем, что роль соответствует группе Camunda, вместо БД roles.
        String groupId = roleToGroupId(upperRole);
        if (groupId == null || identityService.createGroupQuery().groupId(groupId).count() == 0) {
            throw new RoleNotFoundException("Роль не найдена: " + role);
        } else {
            addToGroup(username, groupId);
        }
        userDetailsService.updateUserRole(username, upperRole);
        CustomUserDetails user = userDetailsService.getUser(username);
        return new UserResponseDTO(user.getUsername(), user.getRole());
    }

    private String roleToGroupId(String role) {
        return switch (role) {
            case "ADMIN" -> "camunda-admin";
            case "MANAGER" -> "managerGroup";
            case "USER" -> "userGroup";
            default -> null;
        };
    }

    @Transactional
    public int logout(String username) {
        return userDetailsService.incrementTokenVersion(username);
    }
}
