package com.example.sbp.config;

import com.example.sbp.security.XmlUserDetailsService;
import com.example.sbp.security.jaas.SpringApplicationContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.impl.cfg.AbstractProcessEnginePlugin;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.cmd.CreateMembershipCmd;
import org.camunda.bpm.engine.impl.cmd.SaveUserCmd;
import org.camunda.bpm.engine.impl.interceptor.Command;
import org.camunda.bpm.engine.impl.interceptor.CommandInterceptor;
import org.camunda.bpm.engine.impl.persistence.entity.UserEntity;
import org.camunda.bpm.engine.identity.User;
import org.springframework.stereotype.Component;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class CamundaUserCreatePlugin extends AbstractProcessEnginePlugin {


    //     Флаг "мы уже внутри процесса регистрации пользователя через свой API".
//     Используется, чтобы не дублировать создание пользователя, когда Camunda-пользователь
//     создаётся самим сервисом (путь через /api/v1/auth/register).
    private static final ThreadLocal<Boolean> PLAIN_CREATE_FLOW = ThreadLocal.withInitial(() -> Boolean.FALSE);

    @Override
    public void preInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
        CommandInterceptor interceptor = new CommandInterceptor() {
            @Override
            public <T> T execute(Command<T> command) {
                log.info("TEST command {}", command);
                if (command instanceof SaveUserCmd saveUserCmd) {
                    handleUserSave(saveUserCmd);
                } else if (command instanceof CreateMembershipCmd createMembershipCmd) {
                    handleGroupAssignment(createMembershipCmd);
                }
                return next.execute(command);
            }
        };

        List<CommandInterceptor> preInterceptors =
                processEngineConfiguration.getCustomPreCommandInterceptorsTxRequired();
        if (preInterceptors == null) {
            preInterceptors = new ArrayList<>();
        }
        preInterceptors.add(interceptor);
        processEngineConfiguration.setCustomPreCommandInterceptorsTxRequired(preInterceptors);
    }

    private void handleUserSave(SaveUserCmd cmd) {
        // Пользователя создаёт наш собственный путь (/api/v1/auth/register) — пропускаем.
        if (isPlainCreateFlow()) {
            return;
        }
        try {
            User camundaUser = extractUser(cmd);
            XmlUserDetailsService userDetailsService = XmlUserDetailsServiceHolder.get();
            // Пароль, введённый в Camunda Admin, до шифрования лежит в поле newPassword пользователя.
            String rawPassword = extractNewPassword(camundaUser);
            userDetailsService.createUser(
                    camundaUser.getId(),
                    rawPassword,
                    camundaUser.getEmail(),
                    camundaUser.getFirstName(),
                    camundaUser.getLastName()
            );
        } catch (Exception e) {
            // Не ломаем создание пользователя в Camunda Admin из-за ошибок синхронизации.
            log.warn("Не удалось синхронизировать пользователя из Camunda Admin: {}", e.getMessage());
        }
    }

    private void handleGroupAssignment(CreateMembershipCmd cmd) {
        if (isPlainCreateFlow()) {
            return;
        }
        try {
            String userId = extractUserId(cmd);
            String groupId = extractGroupId(cmd);
            log.info("TEST groupId = {}", groupId);
            log.info("TEST accountId = {}", userId);

            XmlUserDetailsService userDetailsService = XmlUserDetailsServiceHolder.get();
            userDetailsService.updateUserRole(userId, groupIdToRole(groupId));
        } catch (Exception e) {
            log.warn("TEST Не удалось синхронизировать membership: {}", e.getMessage());
        }
    }

    //     Пароль, введённый при создании пользователя, хранится в поле {@code newPassword}
//     сущности {@link UserEntity} и до {@code saveUser} ещё НЕ зашифрован.
//     Метод {@link User#getPassword()} в этот момент возвращает null, поэтому читаем
//     поле через рефлексию.
    private static String extractNewPassword(User camundaUser) {
        if (!(camundaUser instanceof UserEntity userEntity)) {
            log.warn("Пользователь Camunda не является UserEntity, взять пароль не удалось");
            return null;
        }
        try {
            Field field = UserEntity.class.getDeclaredField("newPassword");
            field.setAccessible(true);
            Object raw = field.get(userEntity);
            return raw instanceof String s && !s.isBlank() ? s : null;
        } catch (Exception e) {
            log.warn("Не удалось прочитать newPassword пользователя Camunda: {}", e.getMessage());
            return null;
        }
    }

    private static User extractUser(SaveUserCmd cmd) {
        try {
            Field field = SaveUserCmd.class.getDeclaredField("user");
            field.setAccessible(true);
            return (User) field.get(cmd);
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось получить пользователя из SaveUserCmd", e);
        }
    }

    private static String extractUserId(CreateMembershipCmd cmd) {
        try {
            Field field = CreateMembershipCmd.class.getDeclaredField("userId");
            field.setAccessible(true);
            return (String) field.get(cmd);
        } catch (Exception e) {
            log.warn("Не удалось прочитать userId: {}", e.getMessage());
            return null;
        }
    }

    private static String extractGroupId(CreateMembershipCmd cmd) {
        try {
            Field field = CreateMembershipCmd.class.getDeclaredField("groupId");
            field.setAccessible(true);
            return (String) field.get(cmd);
        } catch (Exception e) {
            log.warn("Не удалось прочитать groupId: {}", e.getMessage());
            return null;
        }
    }

    public static void markPlainCreateFlow() {
        PLAIN_CREATE_FLOW.set(Boolean.TRUE);
    }

    public static void clearPlainCreateFlow() {
        PLAIN_CREATE_FLOW.remove();
    }

    private static boolean isPlainCreateFlow() {
        return PLAIN_CREATE_FLOW.get();
    }


    //    Ленивая выдача сервиса, чтобы разорвать циклическую зависимость между плагином(создаётся на этапе построения Camunda engine) и сервисом, который зависит от engine.
    private static final class XmlUserDetailsServiceHolder {
        private static XmlUserDetailsService instance;

        static synchronized XmlUserDetailsService get() {
            if (instance == null) {
                instance = SpringApplicationContextHolder.getBean(XmlUserDetailsService.class);
            }
            return instance;
        }
    }

    private String groupIdToRole(String groupId) {
        return switch (groupId) {
            case "camunda-admin" -> "ADMIN";
            case "managerGroup" -> "MANAGER";
            case "userGroup" -> "USER";
            default -> null;
        };
    }
}