package io.intelliflow.services.centralcustomexceptionhandler;

import java.io.Serializable;

public class CustomException extends Exception implements Serializable {

    private static final long serialVersionUID = 1L;

    private EventResponseModel responseModel;

    private StatusException statusCode;

    public CustomException() {
        super();
    }

    public CustomException(String message, StatusException status) {
        this.statusCode = status;
        this.responseModel = new EventResponseModel(message, null);
    }

    public CustomException(String msg) {
        super(msg);
    }

    public CustomException(String msg, Exception e) {
        super(msg, e);
    }

    public EventResponseModel getResponseModel() {
        return responseModel;
    }

    public void setResponseModel(EventResponseModel responseModel) {
        this.responseModel = responseModel;
    }

    public StatusException getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(StatusException statusCode) {
        this.statusCode = statusCode;
    }
}
