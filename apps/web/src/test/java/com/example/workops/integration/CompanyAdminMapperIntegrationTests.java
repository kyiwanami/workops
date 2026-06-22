package com.example.workops.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.workops.admin.company.form.CompanySearchForm;
import com.example.workops.admin.company.mapper.CompanyAdminMapper;
import com.example.workops.admin.company.model.CompanyDetail;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

/** 会社作成と会社別汎用マスタ初期値投入SQLをTestcontainers MySQLで確認する。 */
class CompanyAdminMapperIntegrationTests extends MapperIntegrationTestBase {

  @Autowired private CompanyAdminMapper companyAdminMapper;

  @Test
  void findCompaniesExcludesDeletedCompaniesByDefault() {
    companyAdminMapper.insertCompany("M9_DELETED_COMPANY", "M9削除済み会社", 7L, 7L);
    Long companyId = companyAdminMapper.findLastInsertId();
    jdbcTemplate.update("UPDATE companies SET is_deleted = TRUE WHERE id = ?", companyId);

    assertThat(companyAdminMapper.findCompaniesBySearchForm(new CompanySearchForm(false)))
        .extracting("code")
        .doesNotContain("M9_DELETED_COMPANY");
  }

  @Test
  void findCompaniesIncludesDeletedCompaniesWhenRequested() {
    companyAdminMapper.insertCompany("M9_SHOW_DELETED_COMPANY", "M9削除済み表示会社", 7L, 7L);
    Long companyId = companyAdminMapper.findLastInsertId();
    jdbcTemplate.update("UPDATE companies SET is_deleted = TRUE WHERE id = ?", companyId);

    assertThat(companyAdminMapper.findCompaniesBySearchForm(new CompanySearchForm(true)))
        .extracting("code")
        .contains("M9_SHOW_DELETED_COMPANY");
  }

  @Test
  void findCompanyDetailReturnsDeletedCompany() {
    companyAdminMapper.insertCompany("M9_DELETED_DETAIL_COMPANY", "M9削除済み詳細会社", 7L, 7L);
    Long companyId = companyAdminMapper.findLastInsertId();
    jdbcTemplate.update("UPDATE companies SET is_deleted = TRUE WHERE id = ?", companyId);

    CompanyDetail companyDetail = companyAdminMapper.findCompanyDetailById(companyId).orElseThrow();

    assertThat(companyDetail.code()).isEqualTo("M9_DELETED_DETAIL_COMPANY");
    assertThat(companyDetail.isDeleted()).isTrue();
  }

  @Test
  void companyDetailIncludesRelatedRowCounts() {
    CompanyDetail companyDetail = companyAdminMapper.findCompanyDetailById(1L).orElseThrow();

    assertThat(companyDetail.departmentCount()).isGreaterThan(0L);
    assertThat(companyDetail.userCount()).isGreaterThan(0L);
    assertThat(companyDetail.requestCount()).isGreaterThan(0L);
    assertThat(companyDetail.assetCount()).isGreaterThan(0L);
    assertThat(companyDetail.genericMasterValueCount()).isGreaterThan(0L);
  }

  @Test
  void updateCompanyNameKeepsCompanyCode() {
    companyAdminMapper.updateActiveCompanyNameById(1L, "北浜精密機器 更新", 7L);

    CompanyDetail companyDetail = companyAdminMapper.findCompanyDetailById(1L).orElseThrow();

    assertThat(companyDetail.code()).isEqualTo("KTHM_PRECISION");
    assertThat(companyDetail.name()).isEqualTo("北浜精密機器 更新");
  }

  @Test
  void logicalDeleteCompanySetsDeletedWithoutDeletingRelatedRows() {
    Long departmentCount = countRowsByCompanyId("departments", 1L);
    Long userCount = countRowsByCompanyId("users", 1L);
    Long requestCount = countRowsByCompanyId("requests", 1L);
    Long assetCount = countRowsByCompanyId("assets", 1L);
    Long genericMasterValueCount = countRowsByCompanyId("generic_master_values", 1L);

    companyAdminMapper.logicalDeleteActiveCompanyById(1L, 7L);

    CompanyDetail companyDetail = companyAdminMapper.findCompanyDetailById(1L).orElseThrow();
    assertThat(companyDetail.isDeleted()).isTrue();
    assertThat(companyAdminMapper.findActiveCompanyDetailById(1L)).isEmpty();
    assertThat(countRowsByCompanyId("departments", 1L)).isEqualTo(departmentCount);
    assertThat(countRowsByCompanyId("users", 1L)).isEqualTo(userCount);
    assertThat(countRowsByCompanyId("requests", 1L)).isEqualTo(requestCount);
    assertThat(countRowsByCompanyId("assets", 1L)).isEqualTo(assetCount);
    assertThat(countRowsByCompanyId("generic_master_values", 1L))
        .isEqualTo(genericMasterValueCount);
  }

  @Test
  void deletedCompanyCodeCannotBeReused() {
    companyAdminMapper.insertCompany("M9_REUSE_DELETED", "M9削除コード再利用会社", 7L, 7L);
    Long companyId = companyAdminMapper.findLastInsertId();
    companyAdminMapper.logicalDeleteActiveCompanyById(companyId, 7L);

    assertThatThrownBy(
            () -> companyAdminMapper.insertCompany("M9_REUSE_DELETED", "M9再利用会社", 7L, 7L))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void insertCompanyReturnsCreatedCompanyId() {
    companyAdminMapper.insertCompany("M9_COMPANY", "M9会社", 7L, 7L);

    Long companyId = companyAdminMapper.findLastInsertId();

    assertThat(companyId).isPositive();
    assertThat(companyAdminMapper.existsCompanyCode("M9_COMPANY")).isTrue();
  }

  @Test
  void insertInitialRequestTypeAndAssetCategoryValuesForCreatedCompany() {
    companyAdminMapper.insertCompany("M9_INIT_COMPANY", "M9初期値会社", 7L, 7L);
    Long companyId = companyAdminMapper.findLastInsertId();
    Long assetCategoryMasterId =
        companyAdminMapper.findActiveGenericMasterIdByCode("ASSET_CATEGORY").orElseThrow();
    Long requestTypeMasterId =
        companyAdminMapper.findActiveGenericMasterIdByCode("REQUEST_TYPE").orElseThrow();

    insertAssetCategories(companyId, assetCategoryMasterId);
    insertRequestTypes(companyId, requestTypeMasterId);

    assertThat(
            companyAdminMapper.countGenericMasterValuesByCompanyIdAndMasterCode(
                companyId, "ASSET_CATEGORY"))
        .isEqualTo(6L);
    assertThat(
            companyAdminMapper.countGenericMasterValuesByCompanyIdAndMasterCode(
                companyId, "REQUEST_TYPE"))
        .isEqualTo(3L);
  }

  @Test
  void existingCompanyCodeIsRejectedByDatabaseConstraint() {
    assertThatThrownBy(() -> companyAdminMapper.insertCompany("KTHM_PRECISION", "M9重複会社", 7L, 7L))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  private Long countRowsByCompanyId(String tableName, Long companyId) {
    return jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM " + tableName + " WHERE company_id = ?", Long.class, companyId);
  }

  private void insertAssetCategories(Long companyId, Long assetCategoryMasterId) {
    companyAdminMapper.insertGenericMasterValue(
        assetCategoryMasterId, companyId, "NOTE_PC", "ノートPC", 10, 7L, 7L);
    companyAdminMapper.insertGenericMasterValue(
        assetCategoryMasterId, companyId, "DESKTOP_PC", "デスクトップPC", 20, 7L, 7L);
    companyAdminMapper.insertGenericMasterValue(
        assetCategoryMasterId, companyId, "MONITOR", "モニター", 30, 7L, 7L);
    companyAdminMapper.insertGenericMasterValue(
        assetCategoryMasterId, companyId, "TABLET", "タブレット", 40, 7L, 7L);
    companyAdminMapper.insertGenericMasterValue(
        assetCategoryMasterId, companyId, "NETWORK_DEVICE", "ネットワーク機器", 50, 7L, 7L);
    companyAdminMapper.insertGenericMasterValue(
        assetCategoryMasterId, companyId, "OTHER", "その他", 60, 7L, 7L);
  }

  private void insertRequestTypes(Long companyId, Long requestTypeMasterId) {
    companyAdminMapper.insertGenericMasterValue(
        requestTypeMasterId, companyId, "EQUIPMENT_PURCHASE", "備品購入申請", 10, 7L, 7L);
    companyAdminMapper.insertGenericMasterValue(
        requestTypeMasterId, companyId, "REPAIR_REQUEST", "修理依頼申請", 20, 7L, 7L);
    companyAdminMapper.insertGenericMasterValue(
        requestTypeMasterId, companyId, "DISPOSAL_REQUEST", "廃棄申請", 30, 7L, 7L);
  }
}
