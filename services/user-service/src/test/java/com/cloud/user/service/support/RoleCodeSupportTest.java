package com.cloud.user.service.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RoleCodeSupportTest {

  @Test
  void normalizeRoleCodeAddsPrefixAndUppercasesValue() {
    assertThat(RoleCodeSupport.normalizeRoleCode(" merchant ")).isEqualTo("ROLE_MERCHANT");
    assertThat(RoleCodeSupport.normalizeRoleCode("ROLE_admin")).isEqualTo("ROLE_ADMIN");
  }

  @Test
  void normalizeRoleCodesFiltersBlankValuesAndKeepsInsertionOrder() {
    Set<String> roles =
        RoleCodeSupport.normalizeRoleCodes(List.of("user", " ROLE_user ", "", "merchant"));

    assertThat(roles).containsExactly("ROLE_USER", "ROLE_MERCHANT");
  }

  @Test
  void stripRolePrefixOnlyRemovesCanonicalPrefix() {
    assertThat(RoleCodeSupport.stripRolePrefix("ROLE_ADMIN")).isEqualTo("ADMIN");
    assertThat(RoleCodeSupport.stripRolePrefix("role_admin")).isEqualTo("role_admin");
    assertThat(RoleCodeSupport.stripRolePrefix("")).isEmpty();
    assertThat(RoleCodeSupport.stripRolePrefix(null)).isNull();
  }
}
