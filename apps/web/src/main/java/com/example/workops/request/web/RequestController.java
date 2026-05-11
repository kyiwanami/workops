package com.example.workops.request.web;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.workops.request.form.RequestForm;
import com.example.workops.request.model.RequestDetail;
import com.example.workops.request.service.RequestCommandService;
import com.example.workops.request.service.RequestQueryService;

/**
 * 申請一覧と申請詳細を表示するController。
 */
@Controller
public class RequestController {

    private final RequestQueryService requestQueryService;
    private final RequestCommandService requestCommandService;

    public RequestController(RequestQueryService requestQueryService, RequestCommandService requestCommandService) {
        this.requestQueryService = requestQueryService;
        this.requestCommandService = requestCommandService;
    }

    @GetMapping("/requests")
    @PreAuthorize("hasAnyAuthority('TENANT_VIEWER','TENANT_EDITOR','TENANT_MANAGER')")
    public String list(Model model) {
        model.addAttribute("requests", requestQueryService.findList());
        model.addAttribute("canCreate", requestQueryService.canCreateDraft());
        return "request/list";
    }

    @GetMapping("/requests/new")
    @PreAuthorize("hasAnyAuthority('TENANT_EDITOR','TENANT_MANAGER')")
    public String newForm(Model model) {
        model.addAttribute("requestForm", new RequestForm());
        prepareFormModel(model, false, null);
        return "request/form";
    }

    @PostMapping("/requests")
    @PreAuthorize("hasAnyAuthority('TENANT_EDITOR','TENANT_MANAGER')")
    public String create(
            @Valid @ModelAttribute("requestForm") RequestForm requestForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareFormModel(model, false, null);
            return "request/form";
        }

        Long requestId = requestCommandService.createDraft(requestForm);
        redirectAttributes.addFlashAttribute("message", "申請を下書き保存しました。");
        return "redirect:/requests/" + requestId;
    }

    @GetMapping("/requests/{id}")
    @PreAuthorize("hasAnyAuthority('TENANT_VIEWER','TENANT_EDITOR','TENANT_MANAGER')")
    public String detail(@PathVariable Long id, Model model) {
        RequestDetail request = requestQueryService.findDetail(id);
        model.addAttribute("request", request);
        model.addAttribute("canEdit", requestQueryService.canEditDraft(request));
        model.addAttribute("canSubmit", requestQueryService.canSubmit(request));
        model.addAttribute("canWithdraw", requestQueryService.canWithdraw(request));
        return "request/detail";
    }

    @GetMapping("/requests/{id}/edit")
    @PreAuthorize("hasAnyAuthority('TENANT_EDITOR','TENANT_MANAGER')")
    public String editForm(@PathVariable Long id, Model model) {
        RequestDetail request = requestCommandService.findDraftForEdit(id);
        model.addAttribute("requestForm", RequestForm.from(request));
        prepareFormModel(model, true, id);
        return "request/form";
    }

    @PostMapping("/requests/{id}")
    @PreAuthorize("hasAnyAuthority('TENANT_EDITOR','TENANT_MANAGER')")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("requestForm") RequestForm requestForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        requestCommandService.findDraftForEdit(id);
        if (bindingResult.hasErrors()) {
            prepareFormModel(model, true, id);
            return "request/form";
        }

        requestCommandService.updateDraft(id, requestForm);
        redirectAttributes.addFlashAttribute("message", "申請を下書き保存しました。");
        return "redirect:/requests/" + id;
    }

    @PostMapping("/requests/{id}/submit")
    @PreAuthorize("hasAnyAuthority('TENANT_EDITOR','TENANT_MANAGER')")
    public String submit(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        requestCommandService.submitDraft(id);
        redirectAttributes.addFlashAttribute("message", "申請を提出しました。");
        return "redirect:/requests/" + id;
    }

    @PostMapping("/requests/{id}/withdraw")
    @PreAuthorize("hasAnyAuthority('TENANT_EDITOR','TENANT_MANAGER')")
    public String withdraw(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        requestCommandService.withdrawSubmitted(id);
        redirectAttributes.addFlashAttribute("message", "申請を取下げました。");
        return "redirect:/requests/" + id;
    }

    private void prepareFormModel(Model model, boolean edit, Long requestId) {
        model.addAttribute("processTypeOptions", requestQueryService.findProcessTypeOptions());
        model.addAttribute("edit", edit);
        model.addAttribute("requestId", requestId);
    }
}
