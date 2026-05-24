# CampusHub Phase 11 Beta Readiness Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make CampusHub Beta-ready by adding safe demo credential bootstrap, authenticated smoke coverage, browser acceptance matrices, operational/admin/user runbooks, and final handoff docs.

**Architecture:** Keep Phase 11 as an operational readiness wrapper around the deployed Phase 10 platform. Avoid new business domains and schema changes; add a disabled-by-default backend demo account bootstrapper, lightweight policy pages, verification scripts/docs, and README/CLAUDE handoff updates. Verification must avoid printing secrets and must use low-impact checks on the small shared production server.

**Tech Stack:** Java 17, Spring Boot 3.3, Spring Data JPA/JdbcTemplate, BCrypt PasswordEncoder, Vue 3, Vue Router, Element Plus, Bash/curl for smoke scripts, Docker Compose production runbook, Playwriter browser verification.

---

## File structure

- Create `backend/src/main/java/com/campushub/beta/BetaDemoProperties.java`: configuration properties for disabled-by-default demo account reset.
- Create `backend/src/main/java/com/campushub/beta/BetaDemoAccountInitializer.java`: startup component that creates/resets only configured demo student/admin accounts and required role/wallet records.
- Modify `backend/src/main/resources/application.yml`: add non-secret `campushub.beta.demo-*` environment-backed properties.
- Create `backend/src/test/java/com/campushub/beta/BetaDemoAccountInitializerIntegrationTest.java`: proves disabled-by-default behavior, demo account creation, role assignment, wallet creation, and weak-placeholder rejection.
- Create `scripts/beta-auth-smoke.sh`: curl-based authenticated smoke script that prints endpoint/status/pass-fail only, never tokens or passwords.
- Create `docs/operations/campushub-beta-runbook.md`: deployment, rollback, health, logs, smoke, and Playwriter checklist.
- Create `docs/operations/campushub-backup-restore.md`: MySQL backup/restore rehearsal without secret printing.
- Create `docs/operations/campushub-admin-playbook.md`: report, violation, credit, restriction, wallet, payment, and review operations.
- Create `frontend/src/views/PolicyView.vue`: user-facing service/privacy/risk/wallet/Beta notice page.
- Modify `frontend/src/router/index.ts`: register `/policy` route.
- Modify `frontend/src/layouts/MainLayout.vue`: add policy entry to desktop/mobile directory navigation.
- Modify `README.md`: add Phase 11 Beta readiness summary and links.
- Modify `CLAUDE.md`: append Phase 11 handoff and constraints.

---

### Task 1: Add disabled-by-default demo account bootstrap configuration

**Files:**
- Create: `backend/src/main/java/com/campushub/beta/BetaDemoProperties.java`
- Modify: `backend/src/main/resources/application.yml`
- Test: `backend/src/test/java/com/campushub/beta/BetaDemoAccountInitializerIntegrationTest.java`

- [ ] **Step 1: Write the configuration properties class**

Create `backend/src/main/java/com/campushub/beta/BetaDemoProperties.java`:

```java
package com.campushub.beta;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "campushub.beta")
public class BetaDemoProperties {

    private boolean demoResetEnabled;
    private DemoAccount student = new DemoAccount();
    private DemoAccount admin = new DemoAccount();

    public boolean isDemoResetEnabled() {
        return demoResetEnabled;
    }

    public void setDemoResetEnabled(boolean demoResetEnabled) {
        this.demoResetEnabled = demoResetEnabled;
    }

    public DemoAccount getStudent() {
        return student;
    }

    public void setStudent(DemoAccount student) {
        this.student = student;
    }

    public DemoAccount getAdmin() {
        return admin;
    }

    public void setAdmin(DemoAccount admin) {
        this.admin = admin;
    }

    public static class DemoAccount {
        private String email;
        private String username;
        private String password;
        private String studentNo;
        private String realName;
        private String nickname;
        private String phone;
        private String wechatContact;
        private String qqContact;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getStudentNo() {
            return studentNo;
        }

        public void setStudentNo(String studentNo) {
            this.studentNo = studentNo;
        }

        public String getRealName() {
            return realName;
        }

        public void setRealName(String realName) {
            this.realName = realName;
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getWechatContact() {
            return wechatContact;
        }

        public void setWechatContact(String wechatContact) {
            this.wechatContact = wechatContact;
        }

        public String getQqContact() {
            return qqContact;
        }

        public void setQqContact(String qqContact) {
            this.qqContact = qqContact;
        }
    }
}
```

- [ ] **Step 2: Register safe environment-backed defaults**

Modify `backend/src/main/resources/application.yml` under `campushub:` after the `jwt` block:

```yaml
  beta:
    demo-reset-enabled: ${CAMPUSHUB_BETA_DEMO_RESET_ENABLED:false}
    student:
      email: ${CAMPUSHUB_BETA_STUDENT_EMAIL:}
      username: ${CAMPUSHUB_BETA_STUDENT_USERNAME:}
      password: ${CAMPUSHUB_BETA_STUDENT_PASSWORD:}
      student-no: ${CAMPUSHUB_BETA_STUDENT_NO:}
      real-name: ${CAMPUSHUB_BETA_STUDENT_REAL_NAME:Beta Student}
      nickname: ${CAMPUSHUB_BETA_STUDENT_NICKNAME:Beta Student}
      phone: ${CAMPUSHUB_BETA_STUDENT_PHONE:}
      wechat-contact: ${CAMPUSHUB_BETA_STUDENT_WECHAT:}
      qq-contact: ${CAMPUSHUB_BETA_STUDENT_QQ:}
    admin:
      email: ${CAMPUSHUB_BETA_ADMIN_EMAIL:}
      username: ${CAMPUSHUB_BETA_ADMIN_USERNAME:}
      password: ${CAMPUSHUB_BETA_ADMIN_PASSWORD:}
      student-no: ${CAMPUSHUB_BETA_ADMIN_STUDENT_NO:}
      real-name: ${CAMPUSHUB_BETA_ADMIN_REAL_NAME:Beta Admin}
      nickname: ${CAMPUSHUB_BETA_ADMIN_NICKNAME:Beta Admin}
      phone: ${CAMPUSHUB_BETA_ADMIN_PHONE:}
      wechat-contact: ${CAMPUSHUB_BETA_ADMIN_WECHAT:}
      qq-contact: ${CAMPUSHUB_BETA_ADMIN_QQ:}
```

The resulting `campushub:` tree must still include the existing `jwt`, `file-storage`, `payment`, `secondhand`, and `mail` blocks.

- [ ] **Step 3: Write disabled-by-default binding test**

Create `backend/src/test/java/com/campushub/beta/BetaDemoAccountInitializerIntegrationTest.java` with this initial content:

```java
package com.campushub.beta;

import static org.assertj.core.api.Assertions.assertThat;

import com.campushub.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "campushub.mail.enabled=false")
@ActiveProfiles("test")
class BetaDemoAccountInitializerIntegrationTest {

    @Autowired
    private BetaDemoProperties properties;

    @Autowired
    private UserRepository userRepository;

    @Test
    void betaDemoResetIsDisabledByDefault() {
        assertThat(properties.isDemoResetEnabled()).isFalse();
        assertThat(userRepository.findByEmail("beta.student@example.edu.cn")).isEmpty();
        assertThat(userRepository.findByEmail("beta.admin@example.edu.cn")).isEmpty();
    }
}
```

- [ ] **Step 4: Run test to verify configuration compiles**

Run:

```bash
mvn -f backend/pom.xml -Dtest=BetaDemoAccountInitializerIntegrationTest test
```

Expected: the test may fail because `BetaDemoProperties` is not yet enabled as configuration properties. If it fails with `No qualifying bean of type 'com.campushub.beta.BetaDemoProperties'`, continue to Step 5.

- [ ] **Step 5: Enable configuration properties**

Modify `backend/src/main/java/com/campushub/CampusHubApplication.java` to include the properties type:

```java
package com.campushub;

import com.campushub.beta.BetaDemoProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(BetaDemoProperties.class)
public class CampusHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusHubApplication.class, args);
    }
}
```

If the file already has imports or annotations, preserve them and add only `@EnableConfigurationProperties(BetaDemoProperties.class)`.

- [ ] **Step 6: Run test to verify it passes**

Run:

```bash
mvn -f backend/pom.xml -Dtest=BetaDemoAccountInitializerIntegrationTest test
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/campushub/CampusHubApplication.java backend/src/main/java/com/campushub/beta/BetaDemoProperties.java backend/src/main/resources/application.yml backend/src/test/java/com/campushub/beta/BetaDemoAccountInitializerIntegrationTest.java
git commit -m "add beta demo account configuration"
```

---

### Task 2: Implement safe demo student/admin creation and reset

**Files:**
- Create: `backend/src/main/java/com/campushub/beta/BetaDemoAccountInitializer.java`
- Modify: `backend/src/main/java/com/campushub/user/User.java`
- Modify: `backend/src/test/java/com/campushub/beta/BetaDemoAccountInitializerIntegrationTest.java`

- [ ] **Step 1: Add explicit safe update methods to User**

Modify `backend/src/main/java/com/campushub/user/User.java` by adding these methods before the closing brace:

```java
    public void resetPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void updateBetaProfile(String realName, String nickname, String phone, String wechatContact, String qqContact) {
        this.realName = realName;
        this.nickname = nickname;
        this.phone = phone;
        this.wechatContact = wechatContact;
        this.qqContact = qqContact;
    }
```

- [ ] **Step 2: Write failing enabled-reset integration test**

Replace `backend/src/test/java/com/campushub/beta/BetaDemoAccountInitializerIntegrationTest.java` with:

```java
package com.campushub.beta;

import static org.assertj.core.api.Assertions.assertThat;

import com.campushub.auth.JwtTokenService;
import com.campushub.auth.UserRoleLookup;
import com.campushub.user.User;
import com.campushub.user.UserRepository;
import com.campushub.wallet.WalletAccountRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "campushub.mail.enabled=false",
        "campushub.beta.demo-reset-enabled=true",
        "campushub.beta.student.email=beta.student@example.edu.cn",
        "campushub.beta.student.username=beta-student",
        "campushub.beta.student.password=BetaStudentPass123!",
        "campushub.beta.student.student-no=BETA-STUDENT-001",
        "campushub.beta.student.real-name=Beta Student",
        "campushub.beta.student.nickname=Beta Student",
        "campushub.beta.student.phone=13900000001",
        "campushub.beta.student.wechat-contact=beta-student-wechat",
        "campushub.beta.admin.email=beta.admin@example.edu.cn",
        "campushub.beta.admin.username=beta-admin",
        "campushub.beta.admin.password=BetaAdminPass123!",
        "campushub.beta.admin.student-no=BETA-ADMIN-001",
        "campushub.beta.admin.real-name=Beta Admin",
        "campushub.beta.admin.nickname=Beta Admin",
        "campushub.beta.admin.phone=13900000002",
        "campushub.beta.admin.wechat-contact=beta-admin-wechat"
})
@ActiveProfiles("test")
class BetaDemoAccountInitializerIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRoleLookup userRoleLookup;

    @Autowired
    private WalletAccountRepository walletAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void createsDemoStudentAndAdminWithRolesAndWallets() {
        User student = userRepository.findByEmail("beta.student@example.edu.cn").orElseThrow();
        User admin = userRepository.findByEmail("beta.admin@example.edu.cn").orElseThrow();

        assertThat(student.getUsername()).isEqualTo("beta-student");
        assertThat(student.getWechatContact()).isEqualTo("beta-student-wechat");
        assertThat(passwordEncoder.matches("BetaStudentPass123!", student.getPasswordHash())).isTrue();
        assertThat(userRoleLookup.findRoleCodes(student.getId())).contains("ROLE_STUDENT");
        assertThat(walletAccountRepository.findByUserId(student.getId())).isPresent();

        assertThat(admin.getUsername()).isEqualTo("beta-admin");
        assertThat(admin.getWechatContact()).isEqualTo("beta-admin-wechat");
        assertThat(passwordEncoder.matches("BetaAdminPass123!", admin.getPasswordHash())).isTrue();
        assertThat(userRoleLookup.findRoleCodes(admin.getId())).contains("ROLE_STUDENT", "ROLE_ADMIN");
        assertThat(walletAccountRepository.findByUserId(admin.getId())).isPresent();
    }
}
```

- [ ] **Step 3: Run test to verify it fails because initializer is missing**

Run:

```bash
mvn -f backend/pom.xml -Dtest=BetaDemoAccountInitializerIntegrationTest test
```

Expected: FAIL because the configured demo users do not exist.

- [ ] **Step 4: Implement initializer**

Create `backend/src/main/java/com/campushub/beta/BetaDemoAccountInitializer.java`:

```java
package com.campushub.beta;

import com.campushub.user.User;
import com.campushub.user.UserRepository;
import com.campushub.wallet.WalletAccount;
import com.campushub.wallet.WalletAccountRepository;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
public class BetaDemoAccountInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BetaDemoAccountInitializer.class);

    private final BetaDemoProperties properties;
    private final UserRepository userRepository;
    private final WalletAccountRepository walletAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    public BetaDemoAccountInitializer(
            BetaDemoProperties properties,
            UserRepository userRepository,
            WalletAccountRepository walletAccountRepository,
            PasswordEncoder passwordEncoder,
            JdbcTemplate jdbcTemplate) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.walletAccountRepository = walletAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.isDemoResetEnabled()) {
            return;
        }
        upsertDemoAccount(properties.getStudent(), false);
        upsertDemoAccount(properties.getAdmin(), true);
        log.info("Beta demo accounts are ready; disable CAMPUSHUB_BETA_DEMO_RESET_ENABLED after verification");
    }

    private void upsertDemoAccount(BetaDemoProperties.DemoAccount account, boolean admin) {
        validateAccount(account, admin ? "admin" : "student");
        User user = userRepository.findByEmail(account.getEmail())
                .orElseGet(() -> new User(
                        account.getStudentNo(),
                        account.getUsername(),
                        passwordEncoder.encode(account.getPassword()),
                        account.getRealName(),
                        account.getNickname(),
                        account.getPhone(),
                        account.getEmail(),
                        "ACTIVE"));
        user.resetPasswordHash(passwordEncoder.encode(account.getPassword()));
        user.updateBetaProfile(
                account.getRealName(),
                account.getNickname(),
                account.getPhone(),
                account.getWechatContact(),
                account.getQqContact());
        User saved = userRepository.save(user);
        ensureRole(saved.getId(), "ROLE_STUDENT");
        if (admin) {
            ensureRole(saved.getId(), "ROLE_ADMIN");
        }
        ensureWallet(saved.getId());
    }

    private void validateAccount(BetaDemoProperties.DemoAccount account, String label) {
        requireText(account.getEmail(), label + " email");
        requireText(account.getUsername(), label + " username");
        requireText(account.getPassword(), label + " password");
        requireText(account.getStudentNo(), label + " student number");
        requireText(account.getPhone(), label + " phone");
        if (account.getPassword().length() < 12 || account.getPassword().equalsIgnoreCase("change-me")) {
            throw new IllegalStateException("Beta demo " + label + " password must be non-placeholder and at least 12 characters");
        }
    }

    private void requireText(String value, String field) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("Missing beta demo " + field);
        }
    }

    private void ensureRole(Long userId, String roleCode) {
        Long roleId = jdbcTemplate.query(
                "SELECT id FROM roles WHERE code = ?",
                resultSet -> resultSet.next() ? resultSet.getLong("id") : null,
                roleCode);
        if (roleId == null) {
            return;
        }
        Integer existing = jdbcTemplate.query(
                "SELECT COUNT(*) FROM user_roles WHERE user_id = ? AND role_id = ?",
                resultSet -> resultSet.next() ? resultSet.getInt(1) : 0,
                userId,
                roleId);
        if (existing == null || existing == 0) {
            jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, roleId);
        }
    }

    private void ensureWallet(Long userId) {
        walletAccountRepository.findByUserId(userId).orElseGet(() -> walletAccountRepository.save(new WalletAccount(
                userRepository.getReferenceById(userId),
                BigDecimal.ZERO,
                "ACTIVE")));
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run:

```bash
mvn -f backend/pom.xml -Dtest=BetaDemoAccountInitializerIntegrationTest test
```

Expected: PASS.

- [ ] **Step 6: Add weak-password failure test**

Append this nested test class to `BetaDemoAccountInitializerIntegrationTest.java` or create `backend/src/test/java/com/campushub/beta/BetaDemoAccountInitializerValidationTest.java` if the existing Spring context cannot mix failing startup properties:

```java
package com.campushub.beta;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;

class BetaDemoAccountInitializerValidationTest {

    @Test
    void rejectsPlaceholderPasswordsWhenResetIsEnabled() {
        SpringApplication application = new SpringApplication(com.campushub.CampusHubApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setDefaultProperties(java.util.Map.ofEntries(
                java.util.Map.entry("spring.profiles.active", "test"),
                java.util.Map.entry("campushub.mail.enabled", "false"),
                java.util.Map.entry("campushub.beta.demo-reset-enabled", "true"),
                java.util.Map.entry("campushub.beta.student.email", "beta.student@example.edu.cn"),
                java.util.Map.entry("campushub.beta.student.username", "beta-student"),
                java.util.Map.entry("campushub.beta.student.password", "change-me"),
                java.util.Map.entry("campushub.beta.student.student-no", "BETA-STUDENT-001"),
                java.util.Map.entry("campushub.beta.student.phone", "13900000001"),
                java.util.Map.entry("campushub.beta.admin.email", "beta.admin@example.edu.cn"),
                java.util.Map.entry("campushub.beta.admin.username", "beta-admin"),
                java.util.Map.entry("campushub.beta.admin.password", "BetaAdminPass123!"),
                java.util.Map.entry("campushub.beta.admin.student-no", "BETA-ADMIN-001"),
                java.util.Map.entry("campushub.beta.admin.phone", "13900000002")));

        assertThatThrownBy(() -> {
            try (ConfigurableApplicationContext ignored = application.run()) {
                throw new AssertionError("context should not start");
            }
        }).hasRootCauseMessage("Beta demo student password must be non-placeholder and at least 12 characters");
    }
}
```

- [ ] **Step 7: Run beta tests**

Run:

```bash
mvn -f backend/pom.xml -Dtest=BetaDemoAccountInitializerIntegrationTest,BetaDemoAccountInitializerValidationTest test
```

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add backend/src/main/java/com/campushub/user/User.java backend/src/main/java/com/campushub/beta/BetaDemoAccountInitializer.java backend/src/test/java/com/campushub/beta/BetaDemoAccountInitializerIntegrationTest.java backend/src/test/java/com/campushub/beta/BetaDemoAccountInitializerValidationTest.java
git commit -m "add safe beta demo account bootstrap"
```

---

### Task 3: Add authenticated API smoke script

**Files:**
- Create: `scripts/beta-auth-smoke.sh`
- Create: `docs/operations/campushub-beta-runbook.md`

- [ ] **Step 1: Create smoke script**

Create `scripts/beta-auth-smoke.sh`:

```bash
#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${CAMPUSHUB_BASE_URL:-https://ustc.suntomb.qzz.io}"
STUDENT_EMAIL="${CAMPUSHUB_SMOKE_STUDENT_EMAIL:-}"
STUDENT_PASSWORD="${CAMPUSHUB_SMOKE_STUDENT_PASSWORD:-}"
ADMIN_EMAIL="${CAMPUSHUB_SMOKE_ADMIN_EMAIL:-}"
ADMIN_PASSWORD="${CAMPUSHUB_SMOKE_ADMIN_PASSWORD:-}"

if [[ -z "$STUDENT_EMAIL" || -z "$STUDENT_PASSWORD" || -z "$ADMIN_EMAIL" || -z "$ADMIN_PASSWORD" ]]; then
  echo "Missing CAMPUSHUB_SMOKE_STUDENT_EMAIL/PASSWORD or CAMPUSHUB_SMOKE_ADMIN_EMAIL/PASSWORD" >&2
  exit 2
fi

pass_count=0
fail_count=0

check_status() {
  local label="$1"
  local method="$2"
  local path="$3"
  local expected="$4"
  local token="${5:-}"
  local body="${6:-}"
  local auth_args=()
  local body_args=()
  if [[ -n "$token" ]]; then
    auth_args=(-H "Authorization: Bearer $token")
  fi
  if [[ -n "$body" ]]; then
    body_args=(-H "Content-Type: application/json" --data "$body")
  fi
  local status
  status=$(curl -sS -o /tmp/campushub-smoke-response.$$ -w "%{http_code}" -X "$method" "${BASE_URL}${path}" "${auth_args[@]}" "${body_args[@]}" || true)
  rm -f /tmp/campushub-smoke-response.$$
  if [[ "$status" == "$expected" ]]; then
    echo "PASS $label $method $path -> $status"
    pass_count=$((pass_count + 1))
  else
    echo "FAIL $label $method $path expected $expected got $status"
    fail_count=$((fail_count + 1))
  fi
}

login() {
  local email="$1"
  local password="$2"
  local response
  response=$(curl -sS -X POST "${BASE_URL}/api/auth/login" -H "Content-Type: application/json" --data "{\"emailOrUsername\":\"${email}\",\"password\":\"${password}\"}")
  python3 -c 'import json,sys; data=json.load(sys.stdin); print(data.get("data", {}).get("token", ""))' <<<"$response"
}

check_status "anonymous public goods" GET "/api/goods" 200
check_status "anonymous public tasks" GET "/api/tasks" 200
check_status "anonymous write blocked" POST "/api/goods" 401 "" '{}'
check_status "anonymous admin blocked" GET "/api/admin/wallet/recharges" 401

student_token=$(login "$STUDENT_EMAIL" "$STUDENT_PASSWORD")
if [[ -z "$student_token" ]]; then
  echo "FAIL student login did not return token"
  exit 1
fi
echo "PASS student login returned token"
pass_count=$((pass_count + 1))

admin_token=$(login "$ADMIN_EMAIL" "$ADMIN_PASSWORD")
if [[ -z "$admin_token" ]]; then
  echo "FAIL admin login did not return token"
  exit 1
fi
echo "PASS admin login returned token"
pass_count=$((pass_count + 1))

check_status "student me" GET "/api/auth/me" 200 "$student_token"
check_status "student admin forbidden" GET "/api/admin/wallet/recharges" 403 "$student_token"
check_status "student private mismatch forbidden" GET "/api/wallet/users/1" 403 "$student_token"
check_status "admin me" GET "/api/auth/me" 200 "$admin_token"
check_status "admin wallet" GET "/api/admin/wallet/recharges" 200 "$admin_token"
check_status "admin payment" GET "/api/admin/payment/orders" 200 "$admin_token"
check_status "admin ops" GET "/api/admin/ops/analytics/overview" 200 "$admin_token"
check_status "admin governance" GET "/api/admin/governance/dashboard" 200 "$admin_token"

if [[ "$fail_count" -gt 0 ]]; then
  echo "CampusHub beta auth smoke failed: ${fail_count} failed, ${pass_count} passed"
  exit 1
fi

echo "CampusHub beta auth smoke passed: ${pass_count} checks"
```

- [ ] **Step 2: Make script executable**

Run:

```bash
chmod +x scripts/beta-auth-smoke.sh
```

Expected: command succeeds.

- [ ] **Step 3: Create initial runbook section that documents script usage**

Create `docs/operations/campushub-beta-runbook.md`:

```markdown
# CampusHub Beta Runbook

## 1. Safety rules

- Do not print, read, copy, or commit production `.env`, SMTP passwords, JWT secrets, payment-center tokens, database passwords, or Alipay key bodies.
- Do not edit already-applied Flyway migrations V1-V12. Add V13+ only when a schema change is unavoidable.
- Use low-impact checks on the shared small server. Do not run stress tests.
- Keep CampusHub separate from `/opt/ai-relay`; API-Transfer-Station remains responsible for real Alipay integration.

## 2. Demo credential reset

The backend supports a disabled-by-default demo reset path controlled by environment variables:

- `CAMPUSHUB_BETA_DEMO_RESET_ENABLED=true`
- `CAMPUSHUB_BETA_STUDENT_EMAIL`
- `CAMPUSHUB_BETA_STUDENT_USERNAME`
- `CAMPUSHUB_BETA_STUDENT_PASSWORD`
- `CAMPUSHUB_BETA_STUDENT_NO`
- `CAMPUSHUB_BETA_STUDENT_PHONE`
- `CAMPUSHUB_BETA_STUDENT_WECHAT` or `CAMPUSHUB_BETA_STUDENT_QQ`
- `CAMPUSHUB_BETA_ADMIN_EMAIL`
- `CAMPUSHUB_BETA_ADMIN_USERNAME`
- `CAMPUSHUB_BETA_ADMIN_PASSWORD`
- `CAMPUSHUB_BETA_ADMIN_STUDENT_NO`
- `CAMPUSHUB_BETA_ADMIN_PHONE`
- `CAMPUSHUB_BETA_ADMIN_WECHAT` or `CAMPUSHUB_BETA_ADMIN_QQ`

The reset creates or resets only the configured demo student/admin accounts, assigns required roles, ensures wallet accounts, and logs no passwords. Disable `CAMPUSHUB_BETA_DEMO_RESET_ENABLED` after the reset run.

## 3. Authenticated API smoke

Run from a trusted machine or the server with credentials supplied as environment variables. Do not paste passwords into chat or commit them to files.

```bash
CAMPUSHUB_BASE_URL=https://ustc.suntomb.qzz.io \
CAMPUSHUB_SMOKE_STUDENT_EMAIL='demo-student@example.edu.cn' \
CAMPUSHUB_SMOKE_STUDENT_PASSWORD='set-outside-git' \
CAMPUSHUB_SMOKE_ADMIN_EMAIL='demo-admin@example.edu.cn' \
CAMPUSHUB_SMOKE_ADMIN_PASSWORD='set-outside-git' \
./scripts/beta-auth-smoke.sh
```

Expected result: anonymous public reads return 200, anonymous protected routes return 401, student login works, student admin access returns 403, private mismatch returns 403, and admin representative endpoints return 200.
```

- [ ] **Step 4: Syntax-check script without real credentials**

Run:

```bash
bash -n scripts/beta-auth-smoke.sh
```

Expected: no output and exit 0.

- [ ] **Step 5: Run script without credentials to verify safe failure**

Run:

```bash
./scripts/beta-auth-smoke.sh
```

Expected: exits with code 2 and prints only `Missing CAMPUSHUB_SMOKE_STUDENT_EMAIL/PASSWORD or CAMPUSHUB_SMOKE_ADMIN_EMAIL/PASSWORD`.

- [ ] **Step 6: Commit**

```bash
git add scripts/beta-auth-smoke.sh docs/operations/campushub-beta-runbook.md
git commit -m "add beta authenticated smoke script"
```

---

### Task 4: Complete production runbook and backup/restore guide

**Files:**
- Modify: `docs/operations/campushub-beta-runbook.md`
- Create: `docs/operations/campushub-backup-restore.md`

- [ ] **Step 1: Expand production runbook**

Append to `docs/operations/campushub-beta-runbook.md`:

```markdown

## 4. Pre-deploy checklist

1. Confirm local branch and commit:
   ```bash
   git rev-parse --abbrev-ref HEAD
   git log --oneline -1
   git status --short
   ```
2. Confirm no real secret files are staged:
   ```bash
   git status --short
   git diff --cached --name-only
   ```
3. Review migrations. If a new migration exists, verify it is V13+ and that V1-V12 are unchanged.
4. Push code to GitHub only after tests/builds pass.

## 5. Low-impact production deploy

Run on the server in `/opt/campushub`:

```bash
git fetch origin
git rev-parse --abbrev-ref HEAD
git log --oneline -1
git pull --ff-only
```

For backend changes:

```bash
docker compose -f docker-compose.prod.yml build backend
docker compose -f docker-compose.prod.yml up -d backend
```

For frontend-only changes:

```bash
docker compose -f docker-compose.prod.yml build web
docker compose -f docker-compose.prod.yml up -d web
```

For shared backend/frontend changes, build backend first, then web. Avoid rebuilding MySQL.

## 6. Health checks

```bash
docker compose -f docker-compose.prod.yml ps
curl -sS -o /dev/null -w '%{http_code}\n' http://127.0.0.1:18080/
curl -sS -o /dev/null -w '%{http_code}\n' http://127.0.0.1:18080/api/goods
curl -sS -o /dev/null -w '%{http_code}\n' http://127.0.0.1:18080/api/tasks
curl -sS -o /dev/null -w '%{http_code}\n' http://127.0.0.1:18080/api/shops
curl -sS -o /dev/null -w '%{http_code}\n' http://127.0.0.1:18080/api/project-ads
```

Expected public route/API status: 200.

## 7. Logs

Inspect recent logs only. Do not print environment variables or secret files.

```bash
docker compose -f docker-compose.prod.yml logs --tail=100 backend
docker compose -f docker-compose.prod.yml logs --tail=100 web
```

Look for startup failures, Flyway failures, 401/403 regressions, and repeated 5xx responses.

## 8. Browser acceptance matrix

Desktop viewport:

| Area | Route | Expected |
| --- | --- | --- |
| Login | `/auth` | Demo student/admin can login; session display updates |
| Public browse | `/`, `/tasks`, `/goods`, `/shops`, `/project-ads` | Pages render without white screen |
| User center | `/wallet`, `/roles`, `/notifications`, `/credit` | Student can load owned data |
| Admin wallet | `/admin/wallet` | Admin page renders and student is blocked |
| Admin payment | `/admin/payment` | Orders/callback monitor renders |
| Admin ops | `/admin/ops` | Analytics cards and export controls render |
| Admin governance | `/admin/governance` | Dashboard and report queue render |
| Admin review | `/admin/review` | Review workspace renders |

Mobile viewport 390x844:

| Route | Expected |
| --- | --- |
| `/` | bottom tab visible, no document horizontal overflow |
| `/tasks` | list/filter usable, no document horizontal overflow |
| `/goods` | cards usable, no document horizontal overflow |
| `/shops` | cards usable, no document horizontal overflow |
| `/project-ads` | list usable, no document horizontal overflow |
| `/wallet` | wallet cards/dialog entry usable |
| `/notifications` | list or empty state usable |
| `/credit` | credit history/empty state usable |
| `/admin/wallet` | page renders without white screen |
| `/admin/payment` | page renders without white screen |
| `/admin/ops` | page renders without white screen |
| `/admin/governance` | page renders without white screen |
| `/admin/review` | page renders without white screen |

Record route, viewport, expected result, observed result, and any follow-up fix.

## 9. Rollback

Rollback is a human-approved operation. Use the last known-good commit from deployment notes.

```bash
git log --oneline -5
git checkout <known-good-commit>
docker compose -f docker-compose.prod.yml build backend web
docker compose -f docker-compose.prod.yml up -d backend web
docker compose -f docker-compose.prod.yml ps
```

If the failed deploy applied a database migration, do not manually delete or edit data. Stop and decide whether to forward-fix or restore from backup during a maintenance window.

## 10. Incident notes

For production incidents, record:

- current commit;
- time window;
- affected routes/APIs;
- recent backend/web logs;
- whether migrations ran;
- whether payment-center callbacks were involved.

Do not paste secrets, tokens, `.env`, payment callback tokens, SMTP passwords, database passwords, or Alipay key bodies into incident notes.
```

- [ ] **Step 2: Create backup/restore guide**

Create `docs/operations/campushub-backup-restore.md`:

```markdown
# CampusHub Backup and Restore Guide

## 1. Rules

- Do not print `.env`, database passwords, JWT secrets, payment-center tokens, SMTP passwords, or Alipay key bodies.
- Do not paste SQL dumps into chat.
- Do not restore over production without explicit approval and a maintenance window.
- Prefer restore rehearsal into a temporary database or disposable environment.

## 2. Backup

Run on the production server in `/opt/campushub`.

Create a restricted backup directory:

```bash
mkdir -p /opt/campushub/backups
chmod 700 /opt/campushub/backups
```

Create a timestamped dump from inside the MySQL container. This uses container environment variables and does not print the password:

```bash
backup_file="/opt/campushub/backups/campushub-$(date +%Y%m%d-%H%M%S).sql"
docker compose -f docker-compose.prod.yml exec -T mysql sh -c 'mysqldump -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"' > "$backup_file"
chmod 600 "$backup_file"
ls -lh "$backup_file"
```

Expected: backup file exists and has non-zero size.

## 3. Restore rehearsal into temporary database

Create a temporary database in the MySQL container:

```bash
docker compose -f docker-compose.prod.yml exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "CREATE DATABASE IF NOT EXISTS campushub_restore_check"'
```

Restore the dump into the temporary database:

```bash
docker compose -f docker-compose.prod.yml exec -T mysql sh -c 'mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" campushub_restore_check' < "$backup_file"
```

Verify table count without printing table contents:

```bash
docker compose -f docker-compose.prod.yml exec -T mysql sh -c 'mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '\''campushub_restore_check'\''"'
```

Expected: a positive table count.

Drop the temporary database after the rehearsal:

```bash
docker compose -f docker-compose.prod.yml exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "DROP DATABASE campushub_restore_check"'
```

## 4. Production restore

Production restore is destructive. Before restoring:

1. Confirm the exact backup file.
2. Stop backend/web to prevent writes.
3. Take one more fresh backup.
4. Get explicit approval for the maintenance window.

Command shape:

```bash
docker compose -f docker-compose.prod.yml stop backend web
docker compose -f docker-compose.prod.yml exec -T mysql sh -c 'mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"' < "$backup_file"
docker compose -f docker-compose.prod.yml up -d backend web
```

After restore, run container health checks, anonymous API smoke, authenticated API smoke, and browser smoke.
```

- [ ] **Step 3: Commit**

```bash
git add docs/operations/campushub-beta-runbook.md docs/operations/campushub-backup-restore.md
git commit -m "document beta deployment and backup runbooks"
```

---

### Task 5: Add admin playbook and user policy page

**Files:**
- Create: `docs/operations/campushub-admin-playbook.md`
- Create: `frontend/src/views/PolicyView.vue`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/layouts/MainLayout.vue`

- [ ] **Step 1: Create admin playbook**

Create `docs/operations/campushub-admin-playbook.md`:

```markdown
# CampusHub Admin Playbook

## 1. Principles

- Use admin actions to protect Beta users, not to silently rewrite history.
- Write short, factual notes. Do not enter passwords, tokens, private keys, or payment secrets into admin notes.
- Prefer restriction and review workflows over direct data deletion.
- Never ask users for their password.

## 2. Reports and violations

1. Open `/admin/governance`.
2. Review report target, reason, reporter, and current status.
3. Use `IN_REVIEW` for reports requiring investigation.
4. Use `REJECTED` when the report is clearly invalid.
5. Use `RESOLVED` when action is taken.
6. Create a violation when the target behavior breaks platform rules.
7. Choose severity based on impact:
   - low: misleading content, duplicate spam, minor rude wording;
   - medium: repeated spam, unsafe transaction behavior, false information;
   - high: fraud, harassment, malicious content, serious safety risk.
8. Apply credit delta and restrictions consistently with the severity.

## 3. Credit adjustments and restrictions

Use `/admin/governance` for manual credit adjustments and restrictions.

- Add credit for verified helpful behavior or successful issue resolution.
- Subtract credit for confirmed violations.
- Use posting freeze for unsafe content publishing.
- Use service freeze for unsafe task/shop fulfillment behavior.
- Use disabled state only for serious or repeated abuse.

## 4. Wallet operations

Use `/admin/wallet`.

- Alipay recharge should normally settle through payment-center callback.
- WeChat recharge requires manual review because Phase 9 treats it as offline/manual.
- Withdrawal requests freeze balance at submit time.
- Approve only after checking account, amount, and risk notes.
- Complete after offline payout is done.
- Reject if information is invalid or risk is unresolved; the frozen amount should be released by the application flow.

## 5. Payment monitor

Use `/admin/payment`.

- Pending orders may be unpaid or waiting for callback.
- Paid orders should have a matching callback event.
- Failed callback events should be investigated with payment-center logs without printing tokens or Alipay payload secrets.
- Repeated callbacks should be idempotent; do not manually duplicate wallet flows.

## 6. Review queues

Use `/admin/review`, `/admin/ops`, and related admin tabs.

- Role applications: runner and goods publisher are normally auto-approved after deposit; shop merchant requires review.
- Project ads: approve only campus-relevant, safe, non-fraudulent content.
- Shop content: verify service scope and contact safety.

## 7. User support scripts

Safe support response:

> 请不要发送密码、验证码或支付密钥。请提供页面路径、操作时间、订单/记录编号和问题截图，我们会在后台核对状态。

Unsafe requests to reject:

- asking admin to change a real user's password without identity verification;
- asking for raw database dumps;
- asking for Alipay key contents or payment-center tokens;
- asking to delete production data broadly.
```

- [ ] **Step 2: Create policy page**

Create `frontend/src/views/PolicyView.vue`:

```vue
<template>
  <section class="page policy-page">
    <div class="page-header">
      <div>
        <p class="eyebrow">Beta readiness</p>
        <h2>校集 CampusHub 使用说明与风险提示</h2>
        <p class="page-subtitle">本页面说明平台服务边界、隐私规则、交易风险和钱包资金规则，适用于内测和 Beta 使用。</p>
      </div>
    </div>

    <el-card class="content-card">
      <h3>服务协议摘要</h3>
      <p>CampusHub 是校园二手交易与学生微服务信息平台，提供跑腿任务、二手商品、学生店铺、项目广告、站内通知、信用治理和钱包服务费能力。</p>
      <p>平台用于校园内测与课程设计演示，不承诺覆盖所有真实商业场景。管理员可对违规内容、异常交易和风险账户进行审核、限制或下架。</p>
    </el-card>

    <el-card class="content-card">
      <h3>隐私与联系方式</h3>
      <p>注册和业务流程可能收集邮箱、昵称、学号占位、微信或 QQ 联系方式。联系方式用于交易达成后的沟通，并按业务规则向相关交易方展示。</p>
      <p>请不要在留言、举报、审核备注或公开内容中填写密码、验证码、支付 token、身份证号、银行卡号或支付宝密钥等敏感信息。</p>
    </el-card>

    <el-card class="content-card">
      <h3>交易风险提示</h3>
      <p>线下微信/QQ 沟通和交易需要用户自行核验对方身份、商品状态、服务范围和交付结果。平台治理能力可以处理举报、信用和限制，但不能完全消除线下交易风险。</p>
      <p>遇到欺诈、骚扰、虚假宣传或安全问题，请保留记录并通过举报或管理员渠道反馈。</p>
    </el-card>

    <el-card class="content-card">
      <h3>钱包与支付边界</h3>
      <p>CampusHub 钱包用于余额、冻结余额、角色保证金、服务费、充值、提现和二手线上托管演示。真实支付宝收款由外部 API-Transfer-Station 支付中心处理。</p>
      <p>CampusHub 不读取、不复制、不保存支付宝私钥/公钥正文。提现和部分微信充值仍需要管理员人工审核。</p>
    </el-card>

    <el-card class="content-card">
      <h3>Beta 数据边界</h3>
      <p>内测期间可能存在标记为 Beta 或 Demo 的账号、订单、钱包记录和审核记录。这些数据用于验收矩阵和演示，不代表真实用户交易。</p>
      <p>真实用户数据修复、删除或生产恢复必须经过单独确认，不能用宽泛删除或重置命令处理。</p>
    </el-card>
  </section>
</template>
```

- [ ] **Step 3: Register policy route**

Modify `frontend/src/router/index.ts`:

Add import near other view imports:

```ts
import PolicyView from '@/views/PolicyView.vue'
```

Add child route after auth route:

```ts
{ path: 'policy', name: 'policy', component: PolicyView },
```

- [ ] **Step 4: Add policy navigation entry**

Modify `frontend/src/layouts/MainLayout.vue` by adding this item in `navItems` after 登录注册:

```ts
  { path: '/policy', label: '协议与风险' },
```

Do not add it to `mobileTabItems`; it will be available in the mobile “更多” drawer.

- [ ] **Step 5: Run frontend build**

Run:

```bash
npm --prefix frontend run build
```

Expected: PASS with only known Vite large chunk and dependency pure-comment warnings.

- [ ] **Step 6: Commit**

```bash
git add docs/operations/campushub-admin-playbook.md frontend/src/views/PolicyView.vue frontend/src/router/index.ts frontend/src/layouts/MainLayout.vue
git commit -m "add beta policy page and admin playbook"
```

---

### Task 6: Update README and CLAUDE handoff

**Files:**
- Modify: `README.md`
- Modify: `CLAUDE.md`

- [ ] **Step 1: Add Phase 11 section to README**

Modify `README.md` after the Phase 10 section:

```markdown
## Phase 11 Beta readiness 与验收矩阵

Phase 11 不新增业务线，而是把已部署平台推进到可控 Beta 发布状态。

已落地/规划能力：

- 后端提供默认关闭的 Beta demo 账号创建/重置路径，只能作用于配置的 demo student 和 demo admin，不读取或打印现有密码哈希；
- `scripts/beta-auth-smoke.sh` 覆盖匿名公开 200、匿名私有/写入/admin 401、普通用户 `/api/auth/me`、普通用户越权 403、普通用户 admin 403、管理员 `/api/admin/**` 代表端点 200；
- `docs/operations/campushub-beta-runbook.md` 记录部署、回滚、健康检查、日志、API smoke 和 Playwriter 桌面/移动端验收矩阵；
- `docs/operations/campushub-backup-restore.md` 记录 MySQL 备份和临时库恢复演练，避免打印 secrets；
- `docs/operations/campushub-admin-playbook.md` 记录举报、违规、信用、限制、钱包充值/提现、支付回调监控和审核队列操作；
- 前端 `/policy` 提供服务协议、隐私、交易风险、钱包边界和 Beta 数据边界说明。

Phase 11 仍不做地图、小程序、推荐、实时聊天、OAuth/SSO、完整监控栈、Kubernetes、CampusHub 直连支付宝密钥或新的支付业务线。生产真实支付仍由 API-Transfer-Station 处理。
```

- [ ] **Step 2: Add operations links to README**

Modify the production deployment section by adding after the GitHub-to-server deployment flow:

```markdown
更多生产 Beta 操作文档：

- `docs/operations/campushub-beta-runbook.md`
- `docs/operations/campushub-backup-restore.md`
- `docs/operations/campushub-admin-playbook.md`
```

- [ ] **Step 3: Append Phase 11 handoff to CLAUDE.md**

Append to `CLAUDE.md`:

```markdown
## Latest Phase 11 beta readiness handoff, 2026-05-24

Phase 11 focuses on Beta readiness rather than new business functionality.

Implemented/expected Phase 11 artifacts:

- Design spec: `docs/superpowers/specs/2026-05-24-campushub-phase11-beta-readiness-design.md`.
- Implementation plan: `docs/superpowers/plans/2026-05-24-campushub-phase11-beta-readiness-upgrade.md`.
- Disabled-by-default Beta demo account reset controlled by `CAMPUSHUB_BETA_DEMO_RESET_ENABLED` and explicit demo student/admin environment variables.
- Authenticated smoke script: `scripts/beta-auth-smoke.sh`.
- Operations docs under `docs/operations/`: beta runbook, backup/restore guide, and admin playbook.
- User-facing policy/risk page: `/policy`.

Important constraints remain:

- Never read, print, copy, or commit real `.env`, SMTP password, JWT secret, payment-center token, database password, or Alipay key contents.
- Production real payment remains in API-Transfer-Station; CampusHub must not handle Alipay key bodies directly.
- Do not edit already-applied migrations V1-V12; add V13+ only if a future schema change is unavoidable.
- Demo reset must only target configured Beta demo accounts and should be disabled after the reset run.
- Use low-impact production verification on the small shared server: targeted backend/web builds, API smoke, and Playwriter checks.

Recommended Phase 12+ directions:

- Beta feedback fixes from the Phase 11 acceptance matrix;
- focused monitoring/alerting improvements;
- map/location enhancement;
- WeChat mini-program exploration;
- recommendation/search improvements;
- real-time chat only after governance and privacy requirements are revisited.
```

- [ ] **Step 4: Commit**

```bash
git add README.md CLAUDE.md
git commit -m "document phase 11 beta readiness handoff"
```

---

### Task 7: Verification and deployment checklist

**Files:**
- No required code files unless verification finds issues.
- Update: `docs/superpowers/plans/2026-05-24-campushub-phase11-beta-readiness-upgrade.md` checkboxes as work completes.

- [ ] **Step 1: Run local/frontend verification where available**

Run:

```bash
npm --prefix frontend run build
bash -n scripts/beta-auth-smoke.sh
git diff --check
git diff --cached --check
```

Expected: frontend build passes with only known warnings; shell syntax check passes; diff checks pass.

- [ ] **Step 2: Run backend verification where available**

Run if Maven is available locally:

```bash
mvn -f backend/pom.xml -Dtest=BetaDemoAccountInitializerIntegrationTest,BetaDemoAccountInitializerValidationTest,SecurityConfigIntegrationTest test
```

Expected: PASS.

If Maven is unavailable locally, document the blocker and run the equivalent server-side Docker backend build after pushing.

- [ ] **Step 3: Push only after local checks pass or local Maven blocker is documented**

```bash
git status --short
git log --oneline -5
git push origin phase9-wallet-escrow
```

Expected: push succeeds.

- [ ] **Step 4: Server deploy with low impact**

On the server in `/opt/campushub`, without printing `.env`:

```bash
git pull --ff-only
docker compose -f docker-compose.prod.yml build backend
docker compose -f docker-compose.prod.yml build web
docker compose -f docker-compose.prod.yml up -d backend web
docker compose -f docker-compose.prod.yml ps
```

Expected: backend/web build succeeds; containers are running; MySQL remains healthy.

- [ ] **Step 5: Demo credential reset run**

Set the required `CAMPUSHUB_BETA_*` variables in the server environment or compose environment without printing their values. Temporarily enable:

```bash
CAMPUSHUB_BETA_DEMO_RESET_ENABLED=true
```

Restart backend once, confirm logs contain only the high-level readiness message and no passwords, then disable the flag and restart backend again.

Expected: demo student/admin can login; reset flag is disabled after setup.

- [ ] **Step 6: Run production API smoke**

Run with credentials supplied through environment variables, not committed files:

```bash
CAMPUSHUB_BASE_URL=https://ustc.suntomb.qzz.io \
CAMPUSHUB_SMOKE_STUDENT_EMAIL='set-outside-git' \
CAMPUSHUB_SMOKE_STUDENT_PASSWORD='set-outside-git' \
CAMPUSHUB_SMOKE_ADMIN_EMAIL='set-outside-git' \
CAMPUSHUB_SMOKE_ADMIN_PASSWORD='set-outside-git' \
./scripts/beta-auth-smoke.sh
```

Expected: all checks pass.

- [ ] **Step 7: Run Playwriter desktop acceptance matrix**

Use desktop viewport and verify:

- `/auth` login works for student and admin;
- `/`, `/tasks`, `/goods`, `/shops`, `/project-ads` render;
- `/wallet`, `/roles`, `/notifications`, `/credit` render for student;
- `/admin/wallet`, `/admin/payment`, `/admin/ops`, `/admin/governance`, `/admin/review` render for admin;
- student is blocked from admin routes.

Expected: no white screens, no visible Element Plus fatal errors.

- [ ] **Step 8: Run Playwriter mobile acceptance matrix**

Use 390x844 viewport and verify:

- `/`, `/tasks`, `/goods`, `/shops`, `/project-ads`, `/wallet`, `/notifications`, `/credit`, `/policy`;
- `/admin/wallet`, `/admin/payment`, `/admin/ops`, `/admin/governance`, `/admin/review`;
- mobile bottom tab and “更多” drawer work;
- `document.documentElement.scrollWidth === document.documentElement.clientWidth` on tested public/user pages.

Expected: tested pages render and no document-level horizontal overflow on public/user pages.

- [ ] **Step 9: Final handoff commit if verification notes changed**

If verification changes README/CLAUDE or plan checkboxes, commit them:

```bash
git add docs/superpowers/plans/2026-05-24-campushub-phase11-beta-readiness-upgrade.md README.md CLAUDE.md
git commit -m "document phase 11 verification"
```

- [ ] **Step 10: Final report**

Report:

- final commit hash;
- server commit hash;
- build results;
- API smoke result;
- Playwriter desktop/mobile result;
- any unresolved caveats.
```
