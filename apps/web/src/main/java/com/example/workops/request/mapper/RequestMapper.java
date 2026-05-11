package com.example.workops.request.mapper;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.workops.request.model.RequestDetail;
import com.example.workops.request.model.RequestListItem;

/**
 * 申請管理の参照系SQLを実行するMapper。
 */
@Mapper
public interface RequestMapper {

    List<RequestListItem> findListByCompanyId(@Param("companyId") Long companyId);

    Optional<RequestDetail> findDetailByIdAndCompanyId(
            @Param("id") Long id,
            @Param("companyId") Long companyId);
}
