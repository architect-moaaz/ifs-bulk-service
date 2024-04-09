package io.intelliflow.services.controller;

 /*
    @author rahul
    @created on 20-09-2022
 */

import io.intelliflow.services.centralcustomexceptionhandler.CustomException;
import io.intelliflow.services.centralcustomexceptionhandler.StatusException;
import io.intelliflow.services.client.ExtensionService;
import io.intelliflow.services.fileoperationsexceptions.FileOperationsException;
import io.intelliflow.services.helper.RepositoryHelper;
import io.intelliflow.services.helper.TemplateFileHelper;
import io.intelliflow.services.models.*;
import io.intelliflow.services.repo.TemplateQueueRepository;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Date;
import java.util.List;


@ApplicationScoped
@Path("/upload-file")
@Produces(MediaType.APPLICATION_JSON)
public class BulkUploadResource {

    @Inject
    @RestClient
    ExtensionService extensionService;

    @Inject
    RepositoryHelper repositoryHelper;

    @Inject
    TemplateFileHelper templateFileHelper;

    @Inject
    TemplateQueueRepository templateQueueRepository;

    @HeaderParam("user")
    String user;

    private static final String MINI_APP_NULL_MESSAGE = "mini app name cannot be null or empty";

    private static final String WORK_SPACE_NULL_MESSAGE = "workspace name cannot be null or empty";

    @HeaderParam("workspace")
    String workspace;

    @HeaderParam("columnWiseData")
    boolean columnWiseData;

    @POST
    @Path("/uploadData")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public final RequestModel uploadData(@MultipartForm MultipartBody data) throws IOException, CustomException {
        RequestModel requestModel = new RequestModel();
        requestModel.setWorkspaceName(workspace);
        requestModel.setMiniAppName(data.miniAppName);
        requestModel.setExcelContent(data.file.readAllBytes());
        requestModel.setUploadedBy(user);
        if (requestModel.getWorkspaceName() == null || requestModel.getWorkspaceName().isEmpty()) {
            throw new CustomException(WORK_SPACE_NULL_MESSAGE, StatusException.BAD_REQUEST);
        }
        if (requestModel.getMiniAppName() == null || requestModel.getMiniAppName().isEmpty()) {
            throw new CustomException(MINI_APP_NULL_MESSAGE, StatusException.BAD_REQUEST);
        }
       if (columnWiseData) {
           repositoryHelper.convertRowsToColumns(requestModel, true);
        } else {
           repositoryHelper.addRequestToQueue(requestModel, true);
       }
        requestModel.setStatus(Status.QUEUED);
        requestModel.setExcelContent(null);
        requestModel.setRemark("The request has been added to the process queue, It will be processed soon.");
        return requestModel;
    }

    @POST
    @Path("/saveData")
    public final RequestModel saveData(RequestModel requestModel) throws CustomException {
        if (requestModel.getWorkspaceName() == null || requestModel.getWorkspaceName().isEmpty()) {
            throw new CustomException(WORK_SPACE_NULL_MESSAGE, StatusException.BAD_REQUEST);
        }
        if (requestModel.getMiniAppName() == null || requestModel.getMiniAppName().isEmpty()) {
            throw new CustomException(MINI_APP_NULL_MESSAGE, StatusException.BAD_REQUEST);
        }

        if (requestModel.getUploadId() == -1) {
            long nextUploadId = repositoryHelper.nextUploadId();
            requestModel.setUploadId(nextUploadId);
        }
        try {
            repositoryHelper.fileUpload(false, extensionService.getMetaData(requestModel.getWorkspaceName(), requestModel.getMiniAppName()), requestModel);
            requestModel.setStatus(Status.SUCCESS);
            requestModel.setExcelContent(null);
            requestModel.setRemark("The data was successfully uploaded");
            return requestModel;
        } catch (FileOperationsException e) {
            int status = e.getStatusCode();
            int uploadedRecords = e.getUploadedRecords();
            int failedRecords = e.getFailedRecords();
            if((uploadedRecords + failedRecords) > 0) {
                requestModel.setSuccessRecordsCount(uploadedRecords);
                requestModel.setErrorRecordsCount(failedRecords);
            }else {
                requestModel.setSuccessRecordsCount(0);
                requestModel.setErrorRecordsCount((int) requestModel.getTotalRecords());
            }
            if (status == 500) {
                requestModel.setStatus(Status.FAILED);
            } else if (status == 206) {
                requestModel.setStatus(Status.PARTIAL);
            }
            requestModel.setExcelContent(null);
            if (uploadedRecords == 1) {
                requestModel.setRemark("The file has only " + uploadedRecords + " correct record, " + failedRecords + " records do not match the data model properties.");
            } else if (uploadedRecords == 0) {
                requestModel.setRemark("The file has no records matching the data model properties.");
            } else {
                requestModel.setRemark("The file has only " + uploadedRecords + " correct records, " + failedRecords + " records do not match the data model properties.");
            }
            //Log.error(e);
            return requestModel;
        } catch (Exception e) {
            requestModel.setStatus(Status.ABORTED);
            requestModel.setExcelContent(null);
            requestModel.setErrorRecordsCount((int) requestModel.getTotalRecords());
            requestModel.setSuccessRecordsCount(0);
            requestModel.setRemark("The file could not be validated, Please check the file.");
            //Log.error(e);
            return requestModel;
        }
    }

    @POST
    @Path("/validateData")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public final RequestModel validateFile(@MultipartForm MultipartBody data) throws CustomException, IOException {
        RequestModel requestModel = new RequestModel();
        requestModel.setWorkspaceName(workspace);
        requestModel.setMiniAppName(data.miniAppName);
        requestModel.setExcelContent(data.file.readAllBytes());
        if (columnWiseData) {
            requestModel = repositoryHelper.convertRowsToColumns(requestModel, false);
        } else {
            requestModel = repositoryHelper.addRequestToQueue(requestModel, false);
        }
        if (requestModel.getWorkspaceName() == null || requestModel.getWorkspaceName().isEmpty()) {
            throw new CustomException(WORK_SPACE_NULL_MESSAGE, StatusException.BAD_REQUEST);
        }
        if (requestModel.getMiniAppName() == null || requestModel.getMiniAppName().isEmpty()) {
            throw new CustomException(MINI_APP_NULL_MESSAGE, StatusException.BAD_REQUEST);
        }

        if (requestModel.getUploadId() == -1) {
            long nextUploadId = repositoryHelper.nextUploadId();
            requestModel.setUploadId(nextUploadId);
        }
        try {
            repositoryHelper.fileUpload(true, extensionService.getMetaData(requestModel.getWorkspaceName(), requestModel.getMiniAppName()), requestModel);
            requestModel.setExcelContent(null);
            requestModel.setRemark("All the records in the file are with no error, The file can be uploaded.");
            requestModel.setStatus(Status.SUCCESS);
            return requestModel;
        } catch (FileOperationsException e) {
            int status = e.getStatusCode();
            int uploadedRecords = e.getUploadedRecords();
            int failedRecords = e.getFailedRecords();
            if((uploadedRecords + failedRecords) > 0) {
                requestModel.setSuccessRecordsCount(uploadedRecords);
                requestModel.setErrorRecordsCount(failedRecords);
            }
            if (status == 500) {
                requestModel.setStatus(Status.FAILED);
                requestModel.setErrorRecordsCount((int) requestModel.getTotalRecords());
            } else if (status == 206) {
                requestModel.setStatus(Status.PARTIAL);
            }
            if (uploadedRecords == 1) {
                requestModel.setRemark("The file has only " + uploadedRecords + " correct record, " + failedRecords + " records do not match the data model properties.");
            } else if (uploadedRecords == 0) {
                requestModel.setRemark("The file has no records matching the data model properties.");
            } else {
                requestModel.setRemark("The file has only " + uploadedRecords + " correct records, " + failedRecords + " records do not match the data model properties.");
            }
            requestModel.setExcelContent(null);
            //Log.error(e);
            return requestModel;
        } catch (Exception e) {
            requestModel.setStatus(Status.ABORTED);
            requestModel.setExcelContent(null);
            requestModel.setErrorRecordsCount((int) requestModel.getTotalRecords());
            requestModel.setSuccessRecordsCount(0);
            requestModel.setRemark("The file could not be validated, Please check the file.");
            //Log.error(e);
            return requestModel;
        }
    }

    @POST
    @Path("/errorLogFile")
    public final Response getErrorLogFiles(RequestModel requestModel) throws CustomException {
        requestModel.setWorkspaceName(workspace);
        if (requestModel.getWorkspaceName() == null || requestModel.getWorkspaceName().isEmpty()) {
            throw new CustomException(WORK_SPACE_NULL_MESSAGE, StatusException.BAD_REQUEST);
        }
        if (requestModel.getMiniAppName() == null || requestModel.getMiniAppName().isEmpty()) {
            throw new CustomException(MINI_APP_NULL_MESSAGE, StatusException.BAD_REQUEST);
        }
        if (requestModel.getUploadId() == -1) {
            throw new CustomException("upload id cannot be null or empty", StatusException.BAD_REQUEST);
        }
        if(!repositoryHelper.collectionExists(requestModel)){
            throw new CustomException("The error file is not present for the uploadId: "+ requestModel.getUploadId(), StatusException.NOT_FOUND);
        }
        String fileName = requestModel.getDataModelName() + " Log File.xlsx";
        return Response.
                ok().
                header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"").
                entity(repositoryHelper.createLogFileFromDB(requestModel)).build();
    }

    @GET
    @Path("/getAllUploadsStatus")
    public final List<ProcessQueue> getLogFile(@QueryParam("pageNumber") @DefaultValue("1") int pageNumber, @QueryParam("pageSize") @DefaultValue("10") int pageSize) {
        if (pageNumber < 1) {
            pageNumber = 1;
        }
        RequestModel requestModel = new RequestModel();
        requestModel.setWorkspaceName(workspace);
        return repositoryHelper.getStatus(requestModel, pageNumber - 1, pageSize);
    }

    @GET
    @Path("/getTemplateDownloadStatus")
    public final List<TemplateQueue> getTemplateLogFile(@QueryParam("pageNumber") @DefaultValue("1") int pageNumber, @QueryParam("pageSize") @DefaultValue("10") int pageSize) {
        if (pageNumber < 1) {
            pageNumber = 1;
        }
        RequestModel requestModel = new RequestModel();
        requestModel.setWorkspaceName(workspace);
        return repositoryHelper.getTemplateStatus(requestModel, pageNumber - 1, pageSize);
    }

    @DELETE
    @Path("/deleteTemplateDownloadStatus/{uploadid}")
    public final RequestModel deleteTemplateLogFile(@PathParam("uploadid") int uploadId) {
        RequestModel requestModel = new RequestModel();
        requestModel.setWorkspaceName(workspace);
        requestModel.setUploadId(uploadId);
        return repositoryHelper.deleteTemplate(requestModel);
    }

    @POST
    @Path("/getParticularUploadsStatus")
    public final List<ProcessQueue> getParticularEventStatus(RequestModel requestModel, @QueryParam("pageNumber") @DefaultValue("1") int pageNumber, @QueryParam("pageSize") @DefaultValue("10") int pageSize) throws CustomException {
        if (pageNumber < 1) {
            pageNumber = 1;
        }
        requestModel.setWorkspaceName(workspace);
        if (requestModel.getWorkspaceName() == null || requestModel.getWorkspaceName().isEmpty()) {
            throw new CustomException(WORK_SPACE_NULL_MESSAGE, StatusException.BAD_REQUEST);
        }
        return repositoryHelper.getStatus(requestModel, pageNumber - 1, pageSize);
    }

    @POST
    @Path("/getExcelTemplate")
    public final Response getExcelTemplate(RequestModel requestModel) throws CustomException {
        requestModel.setWorkspaceName(workspace);
        if (requestModel.getWorkspaceName() == null || requestModel.getWorkspaceName().isEmpty()) {
            throw new CustomException(WORK_SPACE_NULL_MESSAGE, StatusException.BAD_REQUEST);
        }
        if (requestModel.getMiniAppName() == null || requestModel.getMiniAppName().isEmpty()) {
            throw new CustomException(MINI_APP_NULL_MESSAGE, StatusException.BAD_REQUEST);
        }
        long nextId = repositoryHelper.nextTemplateId();
        String fileName = requestModel.getDataModelName();
        if (fileName == null) {
            fileName = requestModel.getMiniAppName();
        }
        try {
            byte[] template = templateFileHelper.createTemplateFile(requestModel);
            TemplateQueue templateQueue = new TemplateQueue(nextId, requestModel.getMiniAppName(), requestModel.getWorkspaceName(), Status.SUCCESS, new Date(), user, fileName);
            templateQueueRepository.saveTemplateQueue(templateQueue);
            return Response.
                    ok().
                    header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"").
                    entity(template).build();
        } catch (Exception e) {
            e.printStackTrace();
            TemplateQueue templateQueue = new TemplateQueue(nextId, requestModel.getMiniAppName(), requestModel.getWorkspaceName(), Status.FAILED, new Date(), user, fileName);
            templateQueueRepository.saveTemplateQueue(templateQueue);
        }
        return Response.status(500).entity("Internal Server Error").build();
    }

}
