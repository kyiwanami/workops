package com.example.workops.request.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.example.workops.request.service.RequestQueryService;

/**
 * 申請一覧と申請詳細を表示するController。
 */
@Controller
public class RequestController {

    private final RequestQueryService requestQueryService;

    public RequestController(RequestQueryService requestQueryService) {
        this.requestQueryService = requestQueryService;
    }

    @GetMapping("/requests")
    @PreAuthorize("hasAnyAuthority('TENANT_VIEWER','TENANT_EDITOR','TENANT_MANAGER')")
    public String list(Model model) {
        model.addAttribute("requests", requestQueryService.findList());
        return "request/list";
    }

    @GetMapping("/requests/{id}")
    @PreAuthorize("hasAnyAuthority('TENANT_VIEWER','TENANT_EDITOR','TENANT_MANAGER')")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("request", requestQueryService.findDetail(id));
        return "request/detail";
    }
}
