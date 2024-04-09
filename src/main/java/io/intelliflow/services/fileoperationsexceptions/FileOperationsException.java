package io.intelliflow.services.fileoperationsexceptions;

import org.bson.Document;

import java.util.List;

public class FileOperationsException extends Exception {
    private final int statusCode;

    private final int uploadedRecords;

    private final  int failedRecords;

    private final List<Document> documentList;

    public FileOperationsException(List<Document> documentList, int statusCode, int uploadedRecords, int failedRecords) {
        super(documentList.toString());
        this.statusCode = statusCode;
        this.uploadedRecords = uploadedRecords;
        this.failedRecords = failedRecords;
        this.documentList = documentList;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public int getUploadedRecords() {
        return uploadedRecords;
    }

    public int getFailedRecords() {
        return failedRecords;
    }

    public List<Document> getDocumentList() {
        return documentList;
    }
}
