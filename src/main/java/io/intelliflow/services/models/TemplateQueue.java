package io.intelliflow.services.models;

 /*
    @author rahul.malawadkar@intelliflow.ai
    @created on 27-07-2023
 */

import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.types.ObjectId;

import java.util.Date;

@MongoEntity(collection = "BulkUpload-TemplateQueue")
public class TemplateQueue {

    public ObjectId id;
    private long uploadId;

    private String appName;

    private String workSpace;

    private Status uploadStatus;

    private Date createdOn;

    private String uploadedBy;

    private String fileName;

    private Status status;

    public TemplateQueue() {
    }

    public TemplateQueue(long uploadId, String appName, String workSpace, Status uploadStatus, Date createdOn, String uploadedBy, String fileName) {
        this.uploadId = uploadId;
        this.appName = appName;
        this.workSpace = workSpace;
        this.uploadStatus = uploadStatus;
        this.createdOn = createdOn;
        this.uploadedBy = uploadedBy;
        this.fileName = fileName;
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

    public Date getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Date createdOn) {
        this.createdOn = createdOn;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
