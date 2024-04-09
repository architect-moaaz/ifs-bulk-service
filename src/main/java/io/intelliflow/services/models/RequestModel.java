package io.intelliflow.services.models;

import java.util.Date;

public class RequestModel {

    private String workspaceName;

    private String miniAppName;

    private byte[] excelContent;

    private long uploadId = -1;

    private String sheetName;

    private Date createdOn;

    private Date processStartTime;

    private Date processEndTime;

    private String dataModelName;

    private Status status;

    private String remark;

    private String uploadedBy;

    private String fileName;

    private long totalRecords;

    private boolean deleted = false;

    private int successRecordsCount;

    private int errorRecordsCount;

    public String getSheetName() {
        return sheetName;
    }

    public void setSheetName(String sheetName) {
        this.sheetName = sheetName;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String reason) {
        this.remark = reason;
    }


    public long getUploadId() {
        return uploadId;
    }

    public void setUploadId(long uploadId) {
        this.uploadId = uploadId;
    }

    public String getWorkspaceName() {
        return workspaceName;
    }

    public void setWorkspaceName(String workspaceName) {
        this.workspaceName = workspaceName;
    }

    public String getMiniAppName() {
        return miniAppName;
    }

    public void setMiniAppName(String miniAppName) {
        if (miniAppName != null) {
            this.miniAppName = miniAppName.toLowerCase().replace(" ", "-");
        }
    }

    public byte[] getExcelContent() {
        return excelContent;
    }

    public void setExcelContent(byte[] excelContent) {
        this.excelContent = excelContent;
    }

    public String getDataModelName() {
        return dataModelName;
    }

    public void setDataModelName(String dataModelName) {
        this.dataModelName = dataModelName;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Date getProcessStartTime() {
        return processStartTime;
    }

    public void setProcessStartTime(Date processStartTime) {
        this.processStartTime = processStartTime;
    }

    public Date getProcessEndTime() {
        return processEndTime;
    }

    public void setProcessEndTime(Date processEndTime) {
        this.processEndTime = processEndTime;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Date getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Date createdOn) {
        this.createdOn = createdOn;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public int getSuccessRecordsCount() {
        return successRecordsCount;
    }

    public void setSuccessRecordsCount(int successRecordsCount) {
        this.successRecordsCount = successRecordsCount;
    }

    public int getErrorRecordsCount() {
        return errorRecordsCount;
    }

    public void setErrorRecordsCount(int errorRecordsCount) {
        this.errorRecordsCount = errorRecordsCount;
    }

    public long getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(long totalRecords) {
        this.totalRecords = totalRecords;
    }
}
