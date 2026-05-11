package com.example.workops.asset.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.example.workops.asset.service.AssetQueryService;

/**
 * 資産一覧と資産詳細を表示するController。
 */
@Controller
public class AssetController {

    private final AssetQueryService assetQueryService;

    public AssetController(AssetQueryService assetQueryService) {
        this.assetQueryService = assetQueryService;
    }

    @GetMapping("/assets")
    @PreAuthorize("hasAnyAuthority('TENANT_VIEWER','TENANT_EDITOR','TENANT_MANAGER')")
    public String list(Model model) {
        model.addAttribute("assets", assetQueryService.findList());
        return "asset/list";
    }

    @GetMapping("/assets/{id}")
    @PreAuthorize("hasAnyAuthority('TENANT_VIEWER','TENANT_EDITOR','TENANT_MANAGER')")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("asset", assetQueryService.findDetail(id));
        return "asset/detail";
    }
}
