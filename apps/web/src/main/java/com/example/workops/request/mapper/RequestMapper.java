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

    int insertDraft(RequestDraftInsertCommand command);

    int updateDraftByIdAndCompanyId(
            @Param("id") Long id,
            @Param("companyId") Long companyId,
            @Param("requesterUserId") Long requesterUserId,
            @Param("processTypeCode") String processTypeCode,
            @Param("title") String title,
            @Param("content") String content,
            @Param("updatedBy") Long updatedBy);
}
