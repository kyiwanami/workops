package com.example.workops.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import com.example.workops.admin.company.mapper.CompanyAdminMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 会社作成と会社別汎用マスタ初期値投入SQLをTestcontainers MySQLで確認する。
 */
class CompanyAdminMapperIntegrationTests extends MapperIntegrationTestBase {

    @Autowired
    private CompanyAdminMapper companyAdminMapper;

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
        Long assetCategoryMasterId = companyAdminMapper.findActiveGenericMasterIdByCode("ASSET_CATEGORY")
                .orElseThrow();
        Long requestTypeMasterId = companyAdminMapper.findActiveGenericMasterIdByCode("REQUEST_TYPE")
                .orElseThrow();

        insertAssetCategories(companyId, assetCategoryMasterId);
        insertRequestTypes(companyId, requestTypeMasterId);

        assertThat(companyAdminMapper.countGenericMasterValuesByCompanyIdAndMasterCode(companyId, "ASSET_CATEGORY"))
                .isEqualTo(6L);
        assertThat(companyAdminMapper.countGenericMasterValuesByCompanyIdAndMasterCode(companyId, "REQUEST_TYPE"))
                .isEqualTo(3L);
    }

    @Test
    void existingCompanyCodeIsRejectedByDatabaseConstraint() {
        assertThatThrownBy(() -> companyAdminMapper.insertCompany("KTHM_PRECISION", "M9重複会社", 7L, 7L))
                .isInstanceOf(DataIntegrityViolationException.class);
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
