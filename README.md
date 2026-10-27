# SBP Payment Service

Сервис переводов по Системе быстрых платежей (СБП): управление аккаунтами и счетами,
выполнение переводов с антифрод-проверкой, интеграция с внешними системами
(Bitrix24) и бизнес-процессами Camunda.

## Стек

- Java 17, Spring Boot 3.4.4 (Web, Data JPA, Validation, Security)
- PostgreSQL (Hibernate, `ddl-auto=update`)
- Apache Kafka (антифрод-проверка транзакций)
- Camunda 7 (BPMN-процессы, Tasklist/Cockpit)
- JCA-коннекторы (внешний сервис Bitrix24)
- JWT (jjwt) + BCrypt + JAAS
- JCA/SpotBugs, OWASP Dependency-Check (CI)

## Запуск

```bash
# 1. Инфраструктура (Kafka + ZooKeeper + Kafka UI)
cd sbp-lab3-master/sbp-lab3
docker compose up -d

# 2. PostgreSQL должен быть доступен (см. application.properties):
#    jdbc:postgresql://localhost:5432/studs, пользователь postgres

# 3. Сборка и запуск
./mvnw spring-boot:run
```

Сервис поднимается на порту **8087** (`server.port=8087`).
Swagger UI: `http://localhost:8087/swagger-ui.html`.

Пользователи хранятся в `users.xml` (пароли — в виде BCrypt-хешей).

---

## API

Базовый префикс: `/api/v1`. Аутентификация — `Authorization: Bearer <JWT>`.
Публичные эндпоинты: `/api/v1/auth/login`, `/api/v1/auth/register`,
`/api/v1/payments/health`.

### Аутентификация (`/api/v1/auth`)

| Метод | Путь | Права | Описание |
|-------|------|-------|----------|
| POST | `/login` | публичный | Логин, возвращает JWT. Предыдущие токены инвалидируются. |
| POST | `/register` | публичный | Регистрация нового пользователя, возвращает JWT. |
| GET | `/users/{username}` | `USER_MANAGE_ROLES` | Информация о пользователе (только администратор). |
| PUT | `/users/roles` | `USER_MANAGE_ROLES` | Смена роли пользователя. Все его токены инвалидируются. |
| POST | `/logout` | аутентифицированный | Инвалидирует все токены пользователя. |

```bash
# Логин
curl -i -X POST http://localhost:8087/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}'

# Регистрация
curl -i -X POST http://localhost:8087/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"user1","password":"pass12345","email":"u@mail.ru","firstName":"Иван","lastName":"Петров"}'
```

### Счета (`/api/v1/bills`)

| Метод | Путь | Права | Описание |
|-------|------|-------|----------|
| POST | `/` | `BILL_CREATE` | Создать дополнительный счёт (активный). |
| GET | `/{id}` | `BILL_READ` / `BILL_SUPER_READ` | Получить счёт по ID. |
| POST | `/{id}/replenish?accountId=` | `BILL_REPLENISH` | Пополнить счёт на сумму из тела запроса. |

```bash
curl -i -X GET http://localhost:8087/api/v1/bills/1 \
  -H "Authorization: Bearer $TOKEN"
```

### Аккаунты (`/api/v1/accounts`)

| Метод | Путь | Права | Описание |
|-------|------|-------|----------|
| POST | `/` | `ACCOUNT_CREATE` | Создать аккаунт и дефолтный (неактивный) счёт. |
| GET | `/{id}` | `ACCOUNT_READ` / `ACCOUNT_SUPER_READ` | Получить аккаунт по ID. |
| POST | `/{accountId}/activate-default?startBalance=` | `ACCOUNT_ACTIVATE` | Активировать дефолтный счёт с начальным балансом. |

```bash
curl -i -X POST "http://localhost:8087/api/v1/accounts/1/activate-default?startBalance=1000" \
  -H "Authorization: Bearer $TOKEN"
```

### Платежи (`/api/v1/payments`)

| Метод | Путь | Права | Описание |
|-------|------|-------|----------|
| POST | `/sbp` | `PAYMENT_CREATE` | Выполнить перевод по СБП. |
| GET | `/{transactionId}/status` | `PAYMENT_READ_STATUS` / `PAYMENT_SUPER_READ_STATUS` | Статус транзакции. |
| GET | `/health` | публичный | Проверка работоспособности. |

```bash
# Создать перевод
curl -i -X POST http://localhost:8087/api/v1/payments/sbp \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"senderBillId":"1","receiverIdentifier":"79234567890","amount":1000,"message":"Оплата услуг"}'

# Статус
curl -i http://localhost:8087/api/v1/payments/SBP.../status \
  -H "Authorization: Bearer $TOKEN"
```

---

## Реализованные меры защиты

### 1. Защита от SQL-инъекций (SQLi)

- Весь доступ к БД идёт **только через Spring Data JPA** (`JpaRepository`) —
  `src/main/java/com/example/sbp/repository/*`. Никакого конкатенирования SQL-строк,
  `JdbcTemplate` или `java.sql.Statement` с ручной сборкой запросов в проекте нет.
- Производные методы вида `findByEmail`, `existsByEmail`, `findByTransactionId`
  транслируются Hibernate в параметризованные запросы с bind-параметрами.
- Явные JPQL-запросы используют **именованные параметры** (`:name`, `:id`),
  а не конкатенацию. Пример — `SuspicionRepository`:

  ```java
  @Query("SELECT s FROM SuspicionEntity s WHERE s.userName = :userName "
       + "AND s.accountId = :accountId AND s.bankBic = :bankBic "
       + "AND s.analysisDate >= :since")
  Optional<SuspicionEntity> findRecentByUserAndAccountAndBank(
          @Param("userName") String userName,
          @Param("accountId") String accountId,
          @Param("bankBic") String bankBic,
          @Param("since") LocalDateTime since);
  ```

- Входные DTO валидируются (`@NotBlank`, `@Pattern`, `@Min`/`@Max`), что
  дополнительно ограничивает допустимые значения (например, БИК — строго 9 цифр).

### 2. Защита от XSS

- `HtmlEscapingConfig` (`sbp-lab3-master/sbp-lab3/src/main/java/com/example/sbp/config/HtmlEscapingConfig.java`)
  регистрирует кастомные Jackson-сериализаторы для `String` и `CharSequence`,
  которые прогоняют все строковые значения через `HtmlUtils.htmlEscape(...)`
  перед записью в JSON. Таким образом, любой пользовательский текст
  (сообщение к переводу, имя владельца и т.п.) экранируется централизованно
  на выходе из API, что исключает отражённый XSS при рендере JSON в веб-интерфейсе.
- Принудительная кодировка UTF-8 для запросов/ответов
  (`server.servlet.encoding.*`, `spring.http.encoding.*` в `application.properties`).

### 3. Аутентификация и авторизация

**Аутентификация (JWT + JAAS):**

- `JwtAuthenticationFilter` (`security/JwtAuthenticationFilter.java`) перехватывает
  запросы к `/api/**`, извлекает токен из заголовка `Authorization: Bearer ...`,
  валидирует его и заполняет `SecurityContext`.
- `JwtTokenProvider` подписывает токен HMAC-ключом (`Keys.hmacShaKeyFor`) и
  проверяет подпись при разборе (`verifyWith`). В токен кладутся `role`,
  `privileges`, `accountId`, `email` и `tokenVersion`.
- `XmlLoginModule` (JAAS) проверяет пользователя и пароль через
  `XmlUserDetailsService` и `PasswordEncoder`.
- Пароли хранятся **только в виде BCrypt-хешей** (`PasswordEncoderConfig` → `BCryptPasswordEncoder`,
  `XmlUserDetailsService` пишет `{bcrypt}...`).

**Инвалидация токенов (`tokenVersion`):**

- У каждого пользователя в `users.xml` есть атрибут `tokenVersion`.
- При логине версия увеличивается, при смене роли и логине — тоже.
- `JwtTokenProvider.validateToken` отклоняет токены, у которых
  `tokenVersion` меньше текущего (`tokenVersion < currentVersion → false`).
  Это делает старые токены недействительными после смены роли/повторного входа/логаута.

**Авторизация по привилегиям:**

- Роли и привилегии разделены: `Privilege` (enum) — гранулярные права,
  выдаваемые через `RoleEntity` из БД.
- На контроллерах используется `@PreAuthorize("hasAuthority('...')")`
  (например, `USER_MANAGE_ROLES`, `ACCOUNT_CREATE`).
- Дополнительно `SecurityService` проверяет **владение объектом** (ownership):
  USER видит/меняет только свои счета, аккаунты и транзакции,
  MANAGER/ADMIN (`*_SUPER_READ`) — любые. Например,
  `isBillOwnedByCurrentUser(...)`, `isTransactionRelatedToCurrentUser(...)`.
- Публичные эндпоинты явно перечислены в `SecurityConfig`; всё остальное
  под `/api/**` требует аутентификации.
- CORS/CSRF: сессии отключены (`SessionCreationPolicy.STATELESS`),
  CSRF выключен осознанно — API работает по Bearer-токену без cookie.

### 4. Прочие меры (в тех же коммитах)

- **Защита от XXE** — парсинг `users.xml` выполняется с отключёнными DTD и
  внешними сущностями (`disallow-doctype-decl`, `external-general-entities=false`,
  `external-parameter-entities=false`, `FEATURE_SECURE_PROCESSING`, `XIncludeAware=false`),
  а запись — с запретом внешних DTD/stylesheet.
- **Защита от path traversal** — `resolveUserFile()` нормализует путь и
  запрещает выход за пределы рабочей директории.
- **Защита от log injection** — пользовательские строки перед логированием
  санитизируются (`sanitizeForLog`: замена `\r`/`\n`), например в `BankAccountService`.
- **Предсказуемость идентификаторов** — ID транзакции генерируется через
  `UUID` вместо `System.currentTimeMillis()` + `Math.random()`.
- **Корректная сериализация** — `transient` для несериализуемых полей
  (`PrintWriter logWriter`, JCA-фабрики), `serialVersionUID` у
  `ManagedConnectionFactory`.

---

## CI: SAST / SCA

GitHub Actions workflow — `.github/workflows/ci.yml`:

- **SAST** — SpotBugs + FindSecBugs (`spotbugs:spotbugs`), отчёт `spotbugs-report`.
- **SCA** — OWASP Dependency-Check против NVD (`dependency-check-report.*`),
  шаг помечен `continue-on-error`, чтобы публиковать отчёт, не блокируя пайплайн.
- Отчёты публикуются как артефакты в разделе **Actions** и в **Security → Code scanning**.

---

## Правки по безопасности (по коммитам)

- `7c55be4` — централизованное XSS-экранирование (`HtmlEscapingConfig`), защита от XXE и
  path traversal в `XmlUserDetailsService`, санитизация логов, UUID для ID транзакции,
  `transient`/`serialVersionUID` в JCA, исправление UTF-8 в JWT-ключе.
- `01be2a0` — удаление дублирующей JCA-фабрики `ExchangeRateManagedConnectionFactory`
  из пакета `jca/bitrix24` и приведение оставшейся к контракту (serialVersionUID, transient).
- `dac7800` — возврат фабрики в корректный пакет `jca/exchangerate`.
- `60134ef` — очистка лишних импортов в `ExchangeRateManagedConnectionFactory`.
