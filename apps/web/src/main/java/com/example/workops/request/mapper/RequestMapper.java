package com.example.workops.request.mapper;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.workops.request.model.RequestDetail;
import com.example.workops.request.model.RequestListItem;
import com.example.workops.request.model.RequestProcessTypeOption;

/**
 * 申請管理の参照系SQLを実行するMapper。
 */
@Mapper
public interface RequestMapper {

    List<RequestListItem> findListByCompanyId(@Param("companyId") Long companyId);

    Optional<RequestDetail> findDetailByIdAndCompanyId(
            @Param("id") Long id,
            @Param("companyId") Long companyId);

    List<RequestProcessTypeOption> findProcessTypeOptions();

    boolean existsProcessTypeCode(@Param("processTypeCode") String processTypeCode);

    int insertDraft(
            @Param("companyId") Long companyId,
            @Param("requesterUserId") Long requesterUserId,
            @Param("processTypeCode") String processTypeCode,
            @Param("title") String title,
            @Param("content") String content,
            @Param("createdBy") Long createdBy,
            @Param("updatedBy") Long updatedBy);

    Long findLastInsertId();

    int updateDraftByIdAndCompanyId(
            @Param("id") Long id,
            @Param("companyId") Long companyId,
            @Param("requesterUserId") Long requesterUserId,
            @Param("processTypeCode") String processTypeCode,
            @Param("title") String title,
            @Param("content") String content,
            @Param("updatedBy") Long updatedBy);

    int submitDraftByIdAndCompanyId(
            @Param("id") Long id,
            @Param("companyId") Long companyId,
            @Param("requesterUserId") Long requesterUserId,
            @Param("updatedBy") Long updatedBy);

    int withdrawSubmittedByIdAndCompanyId(
            @Param("id") Long id,
            @Param("companyId") Long companyId,
            @Param("requesterUserId") Long requesterUserId,
            @Param("updatedBy") Long updatedBy);

    int approveSubmittedByIdAndCompanyId(
            @Param("id") Long id,
            @Param("companyId") Long companyId,
            @Param("updatedBy") Long updatedBy);

    int rejectSubmittedByIdAndCompanyId(
            @Param("id") Long id,
            @Param("companyId") Long companyId,
            @Param("reviewComment") String reviewComment,
            @Param("updatedBy") Long updatedBy);

    int remandSubmittedByIdAndCompanyId(
            @Param("id") Long id,
            @Param("companyId") Long companyId,
            @Param("reviewComment") String reviewComment,
            @Param("updatedBy") Long updatedBy);
}
