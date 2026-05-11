package com.example.workops.request.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.workops.common.security.CurrentUserProvider;
import com.example.workops.common.security.LoginUserContext;
import com.example.workops.request.mapper.RequestMapper;
import com.example.workops.request.model.RequestDetail;
import com.example.workops.request.model.RequestListItem;

/**
 * 申請管理の参照系ユースケースを扱うService。
 */
@Service
public class RequestQueryService {

    private final CurrentUserProvider currentUserProvider;
    private final RequestMapper requestMapper;

    public RequestQueryService(CurrentUserProvider currentUserProvider, RequestMapper requestMapper) {
        this.currentUserProvider = currentUserProvider;
        this.requestMapper = requestMapper;
    }

    public List<RequestListItem> findList() {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        return requestMapper.findListByCompanyId(currentUser.companyId());
    }

    public RequestDetail findDetail(Long id) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        return requestMapper.findDetailByIdAndCompanyId(id, currentUser.companyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "申請が見つかりません。"));
    }
}
