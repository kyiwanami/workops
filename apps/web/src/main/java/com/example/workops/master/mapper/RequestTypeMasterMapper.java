package com.example.workops.master.mapper;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.workops.master.form.RequestTypeMasterSearchForm;
import com.example.workops.master.model.RequestTypeMasterDetail;
import com.example.workops.master.model.RequestTypeMasterListItem;

/**
 * 申請種別マスタ管理のSQLを実行するMapper。
 */
@Mapper
public interface RequestTypeMasterMapper {

    Optional<Long> findRequestTypeMasterId();

    List<RequestTypeMasterListItem> findListByCompanyIdAndSearchForm(
            @Param("companyId") Long companyId,
            @Param("requestTypeMasterSearchForm") RequestTypeMasterSearchForm requestTypeMasterSearchForm);

    Optional<RequestTypeMasterDetail> findActiveByIdAndCompanyId(
            @Param("id") Long id,
            @Param("companyId") Long companyId);

    Optional<RequestTypeMasterDetail> findDeletedByIdAndCompanyId(
            @Param("id") Long id,
            @Param("companyId") Long companyId);

    boolean existsCodeByCompanyId(
            @Param("companyId") Long companyId,
            @Param("code") String code);

    int insertRequestType(
            @Param("genericMasterId") Long genericMasterId,
            @Param("companyId") Long companyId,
            @Param("code") String code,
            @Param("name") String name,
            @Param("sortOrder") Integer sortOrder,
            @Param("createdBy") Long createdBy,
            @Param("updatedBy") Long updatedBy);

    int updateActiveByIdAndCompanyId(
            @Param("id") Long id,
            @Param("companyId") Long companyId,
            @Param("name") String name,
            @Param("sortOrder") Integer sortOrder,
            @Param("updatedBy") Long updatedBy);

    int logicalDeleteActiveByIdAndCompanyId(
            @Param("id") Long id,
            @Param("companyId") Long companyId,
            @Param("updatedBy") Long updatedBy);

    int restoreDeletedByIdAndCompanyId(
            @Param("id") Long id,
            @Param("companyId") Long companyId,
            @Param("updatedBy") Long updatedBy);
}
