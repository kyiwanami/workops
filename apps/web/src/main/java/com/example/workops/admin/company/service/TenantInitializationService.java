package com.example.workops.admin.company.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.workops.admin.company.mapper.CompanyAdminMapper;
import com.example.workops.admin.company.model.TenantInitialMasterValue;

/**
 * 会社作成に伴うテナント初期データ投入を扱うService。
 *
 * <p>このServiceは会社作成トランザクション内で呼び出され、申請種別と資産分類の初期値を
 * 会社別の {@code generic_master_values} として投入する。{@code generic_master} の種別自体は
 * 複製しない。</p>
 */
@Service
public class TenantInitializationService {

    private static final String GENERIC_MASTER_ASSET_CATEGORY = "ASSET_CATEGORY";
    private static final String GENERIC_MASTER_REQUEST_TYPE = "REQUEST_TYPE";

    private static final List<TenantInitialMasterValue> INITIAL_VALUES = List.of(
            new TenantInitialMasterValue(GENERIC_MASTER_ASSET_CATEGORY, "NOTE_PC", "ノートPC", 10),
            new TenantInitialMasterValue(GENERIC_MASTER_ASSET_CATEGORY, "DESKTOP_PC", "デスクトップPC", 20),
            new TenantInitialMasterValue(GENERIC_MASTER_ASSET_CATEGORY, "MONITOR", "モニター", 30),
            new TenantInitialMasterValue(GENERIC_MASTER_ASSET_CATEGORY, "TABLET", "タブレット", 40),
            new TenantInitialMasterValue(GENERIC_MASTER_ASSET_CATEGORY, "NETWORK_DEVICE", "ネットワーク機器", 50),
            new TenantInitialMasterValue(GENERIC_MASTER_ASSET_CATEGORY, "OTHER", "その他", 60),
            new TenantInitialMasterValue(GENERIC_MASTER_REQUEST_TYPE, "EQUIPMENT_PURCHASE", "備品購入申請", 10),
            new TenantInitialMasterValue(GENERIC_MASTER_REQUEST_TYPE, "REPAIR_REQUEST", "修理依頼申請", 20),
            new TenantInitialMasterValue(GENERIC_MASTER_REQUEST_TYPE, "DISPOSAL_REQUEST", "廃棄申請", 30));

    private final CompanyAdminMapper companyAdminMapper;

    public TenantInitializationService(CompanyAdminMapper companyAdminMapper) {
        this.companyAdminMapper = companyAdminMapper;
    }

    /**
     * 新規会社にMVP既定の会社別マスタ値を投入する。
     *
     * @param companyId 初期化対象の会社ID
     * @param currentUserId 作成者・更新者として記録するユーザーID
     * @throws ResponseStatusException 初期化に必要な汎用マスタ種別が存在しない場合
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void initializeTenant(Long companyId, Long currentUserId) {
        Long assetCategoryMasterId = findRequiredGenericMasterId(GENERIC_MASTER_ASSET_CATEGORY);
        Long requestTypeMasterId = findRequiredGenericMasterId(GENERIC_MASTER_REQUEST_TYPE);

        for (TenantInitialMasterValue initialValue : INITIAL_VALUES) {
            Long genericMasterId = resolveGenericMasterId(
                    initialValue.genericMasterCode(),
                    assetCategoryMasterId,
                    requestTypeMasterId);
            companyAdminMapper.insertGenericMasterValue(
                    genericMasterId,
                    companyId,
                    initialValue.code(),
                    initialValue.name(),
                    initialValue.sortOrder(),
                    currentUserId,
                    currentUserId);
        }
    }

    private Long findRequiredGenericMasterId(String code) {
        return companyAdminMapper.findActiveGenericMasterIdByCode(code)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "汎用マスタ種別が見つかりません。"));
    }

    private Long resolveGenericMasterId(
            String genericMasterCode,
            Long assetCategoryMasterId,
            Long requestTypeMasterId) {
        if (GENERIC_MASTER_ASSET_CATEGORY.equals(genericMasterCode)) {
            return assetCategoryMasterId;
        }
        return requestTypeMasterId;
    }
}
