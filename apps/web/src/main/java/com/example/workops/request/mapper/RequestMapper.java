package com.example.workops.request.mapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.workops.request.model.RequestDetail;
import com.example.workops.request.model.RequestListItem;
import com.example.workops.request.model.RequestAssetOption;
import com.example.workops.request.model.RequestTypeOption;

/**
 * 申請管理の参照系SQLを実行するMapper。
 */
@Mapper
public interface RequestMapper {

    List<RequestListItem> findListByCompanyId(@Param("companyId") Long companyId);

    Optional<RequestDetail> findDetailByIdAndCompanyId(
            @Param("id") Long id,
            @Param("companyId") Long companyId);

    List<RequestTypeOption> findRequestTypeOptionsByCompanyId(@Param("companyId") Long companyId);

    List<RequestAssetOption> findAssetOptionsByCompanyId(@Param("companyId") Long companyId);

    boolean existsRequestTypeByIdAndCompanyId(
            @Param("requestTypeValueId") Long requestTypeValueId,
            @Param("companyId") Long companyId);

    boolean existsSelectableAssetByIdAndCompanyId(
            @Param("assetId") Long assetId,
            @Param("companyId") Long companyId);

    int insertDraft(
            @Param("companyId") Long companyId,
            @Param("requesterUserId") Long requesterUserId,
            @Param("assetId") Long assetId,
            @Param("requestTypeValueId") Long requestTypeValueId,
            @Param("title") String title,
            @Param("content") String content,
            @Param("createdBy") Long createdBy,
            @Param("updatedBy") Long updatedBy);

    Long findLastInsertId();

    int updateDraftByIdAndCompanyId(
            @Param("id") Long id,
            @Param("companyId") Long companyId,
            @Param("requesterUserId") Long requesterUserId,
            @Param("assetId") Long assetId,
            @Param("requestTypeValueId") Long requestTypeValueId,
            @Param("title") String title,
            @Param("content") String content,
            @Param("updatedBy") Long updatedBy);

    int submitDraftByIdAndCompanyId(
            @Param("id") Long id,
            @Param("companyId") Long companyId,
            @Param("requesterUserId") Long requesterUserId,
            @Param("submittedAt") LocalDateTime submittedAt,
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
