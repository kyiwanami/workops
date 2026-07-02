package com.example.workops.common.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.workops.common.security.CurrentUserProvider;
import com.example.workops.common.security.LoginUserContext;
import com.example.workops.common.security.PermissionSetContext;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

class HomeControllerTests {

  private final CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
  private final HomeController homeController = new HomeController(currentUserProvider);

  @Test
  void platformAdminSeesOnlyPlatformLinks() {
    when(currentUserProvider.requireCurrentUser()).thenReturn(user("PLATFORM_ADMIN"));
    Model model = new ExtendedModelMap();

    String view = homeController.index(model);

    assertThat(view).isEqualTo("index");
    assertThat(model.asMap())
        .containsEntry("canManagePlatformCompanies", true)
        .containsEntry("canManagePlatformDepartments", true)
        .containsEntry("canManagePlatformUsers", true)
        .containsEntry("canViewRequests", false)
        .containsEntry("canViewAssets", false)
        .containsEntry("canManageRequestTypes", false)
        .containsEntry("canManageAssetCategories", false)
        .containsEntry("canManageTenantDepartments", false)
        .containsEntry("canManageTenantUsers", false)
        .containsEntry("homeLeadText", "申請管理と資産管理の日常業務を一つの画面から始められます。");
  }

  @Test
  void tenantManagerSeesTenantBusinessAndTenantManagementLinks() {
    when(currentUserProvider.requireCurrentUser()).thenReturn(user("TENANT_MANAGER"));
    Model model = new ExtendedModelMap();

    homeController.index(model);

    assertThat(model.asMap())
        .containsEntry("canViewRequests", true)
        .containsEntry("canViewAssets", true)
        .containsEntry("canManageRequestTypes", true)
        .containsEntry("canManageAssetCategories", true)
        .containsEntry("canManageTenantDepartments", true)
        .containsEntry("canManageTenantUsers", true)
        .containsEntry("canManagePlatformCompanies", false)
        .containsEntry("canManagePlatformDepartments", false)
        .containsEntry("canManagePlatformUsers", false);
  }

  @Test
  void tenantViewerSeesOnlyTenantBusinessLinks() {
    when(currentUserProvider.requireCurrentUser()).thenReturn(user("TENANT_VIEWER"));
    Model model = new ExtendedModelMap();

    homeController.index(model);

    assertThat(model.asMap())
        .containsEntry("canViewRequests", true)
        .containsEntry("canViewAssets", true)
        .containsEntry("canManageRequestTypes", false)
        .containsEntry("canManageAssetCategories", false)
        .containsEntry("canManageTenantDepartments", false)
        .containsEntry("canManageTenantUsers", false)
        .containsEntry("canManagePlatformCompanies", false)
        .containsEntry("canManagePlatformDepartments", false)
        .containsEntry("canManagePlatformUsers", false);
  }

  @Test
  void tenantEditorSeesOnlyTenantBusinessLinks() {
    when(currentUserProvider.requireCurrentUser()).thenReturn(user("TENANT_EDITOR"));
    Model model = new ExtendedModelMap();

    homeController.index(model);

    assertThat(model.asMap())
        .containsEntry("canViewRequests", true)
        .containsEntry("canViewAssets", true)
        .containsEntry("canManageRequestTypes", false)
        .containsEntry("canManageAssetCategories", false)
        .containsEntry("canManageTenantDepartments", false)
        .containsEntry("canManageTenantUsers", false)
        .containsEntry("canManagePlatformCompanies", false)
        .containsEntry("canManagePlatformDepartments", false)
        .containsEntry("canManagePlatformUsers", false);
  }

  @Test
  void missingLoginUserContextIsRejected() {
    when(currentUserProvider.requireCurrentUser()).thenThrow(new AccessDeniedException("denied"));

    assertThatThrownBy(() -> homeController.index(new ExtendedModelMap()))
        .isInstanceOf(AccessDeniedException.class);
  }

  private LoginUserContext user(String permissionCode) {
    Long companyId = 1L;
    String actorType = "TENANT";
    if (permissionCode.equals("PLATFORM_ADMIN")) {
      companyId = null;
      actorType = "PLATFORM";
    }
    return new LoginUserContext(
        10L,
        "user",
        "user@example.com",
        actorType,
        companyId,
        List.of(new PermissionSetContext(permissionCode, permissionCode)));
  }
}
