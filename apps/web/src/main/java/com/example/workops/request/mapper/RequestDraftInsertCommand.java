package com.example.workops.request.mapper;

/**
 * DRAFT申請を登録し、MyBatisの生成IDを受け取るMapper入力。
 */
public class RequestDraftInsertCommand {

    private Long id;
    private final Long companyId;
    private final Long requesterUserId;
    private final String processTypeCode;
    private final String title;
    private final String content;
    private final Long createdBy;
    private final Long updatedBy;

    public RequestDraftInsertCommand(
            Long companyId,
            Long requesterUserId,
            String processTypeCode,
            String title,
            String content,
            Long createdBy,
            Long updatedBy) {
        this.companyId = companyId;
        this.requesterUserId = requesterUserId;
        this.processTypeCode = processTypeCode;
        this.title = title;
        this.content = content;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public Long getRequesterUserId() {
        return requesterUserId;
    }

    public String getProcessTypeCode() {
        return processTypeCode;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }
}
