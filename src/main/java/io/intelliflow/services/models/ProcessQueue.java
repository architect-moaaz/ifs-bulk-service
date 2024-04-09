package io.intelliflow.services.models;

 /*
    @author rahul.malawadkar@intelliflow.ai
    @created on 27-07-2023
 */

import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.types.ObjectId;

import java.util.Date;

@MongoEntity(collection = "BulkUpload-ProcessQueue")
public class ProcessQueue {

    public ObjectId id;
    private String appName;

    private String workSpace;

    private Status uploadStatus;

    private long uploadId;

    private Date processStartTime;

    private Date processEndTime;

    private String uploadedBy;

    private String sheetName;

    private long successRecordsCount;

    private long errorRecordsCount;

    private String remark;

    public ProcessQueue() {
    }

    public ProcessQueue(String appName, String workSpace, Status uploadStatus, long uploadId, Date processStartTime, Date processEndTime, String uploadedBy, String sheetName) {
        this.appName = appName;
        this.workSpace = workSpace;
        this.uploadStatus = uploadStatus;
        this.uploadId = uploadId;
        this.processStartTime = processStartTime;
        this.processEndTime = processEndTime;
        this.uploadedBy = uploadedBy;
        this.sheetName = sheetName;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getWorkSpace() {
        return workSpace;
    }

    public void setWorkSpace(String workSpace) {
        this.workSpace = workSpace;
    }

    public Status getUploadStatus() {
        return uploadStatus;
    }

    public void setUploadStatus(Status uploadStatus) {
        this.uploadStatus = uploadStatus;
    }

    public long getUploadId() {
        return uploadId;
    }

    public void setUploadId(long uploadId) {
        this.uploadId = uploadId;
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

    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public String getSheetName() {
        return sheetName;
    }

    public void setSheetName(String sheetName) {
        this.sheetName = sheetName;
    }

    public long getSuccessRecordsCount() {
        return successRecordsCount;
    }

    public void setSuccessRecordsCount(long successRecordsCount) {
        this.successRecordsCount = successRecordsCount;
    }

    public long getErrorRecordsCount() {
        return errorRecordsCount;
    }

    public void setErrorRecordsCount(long errorRecordsCount) {
        this.errorRecordsCount = errorRecordsCount;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
