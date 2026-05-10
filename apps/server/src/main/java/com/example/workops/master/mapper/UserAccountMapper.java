package com.example.workops.master.mapper;

import java.util.Optional;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 現在ユーザーのDB由来情報を取得するMapper。
 */
@Mapper
public interface UserAccountMapper {

    Optional<UserAccountRow> findByCognitoSub(@Param("cognitoSub") String cognitoSub);

    List<PermissionSetRow> findPermissionSetsByUserId(@Param("userId") Long userId);
}
