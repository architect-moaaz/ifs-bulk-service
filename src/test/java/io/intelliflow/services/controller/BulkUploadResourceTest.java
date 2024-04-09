package io.intelliflow.services.controller;

import io.intelliflow.services.centralcustomexceptionhandler.CustomException;
import io.intelliflow.services.fileoperationsexceptions.FileOperationsException;
import io.intelliflow.services.helper.RepositoryHelper;
import io.intelliflow.services.helper.TemplateFileHelper;
import io.intelliflow.services.models.MultipartBody;
import io.intelliflow.services.models.ProcessQueue;
import io.intelliflow.services.models.RequestModel;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class BulkUploadResourceTest {

    @InjectMock
    RepositoryHelper repositoryHelper;

    @InjectMock
    TemplateFileHelper templateFileHelper;

    @Inject
    BulkUploadResource bulkUploadResource;

    private RequestModel requestModel;

    private String oDataClintResponse;

    @BeforeEach
    void setUp() {
        oDataClintResponse = "<?xml version=\"1.0\"?>\n" +
                "<edmx:Edmx xmlns:edmx=\"http://docs.oasis-open.org/odata/ns/edmx\" Version=\"4.0\">\n" +
                "</edmx:Edmx>";
        requestModel = new RequestModel();
        requestModel.setWorkspaceName("VOS");
        requestModel.setMiniAppName("infinity");
    }

    @Test
    void uploadData() throws IOException, CustomException {
        Mockito.doNothing().when(repositoryHelper).addRequestToQueue(requestModel,true);
        MultipartBody multipartBody = new MultipartBody();
        RequestModel response = bulkUploadResource.uploadData(multipartBody);
        assertNotNull(response);
    }

    @Test
    void uploadExcel() throws FileOperationsException, IOException, ParserConfigurationException, SAXException, CustomException {
        Mockito.doNothing().when(repositoryHelper).fileUpload(false,oDataClintResponse,requestModel);
        RequestModel response = bulkUploadResource.saveData(requestModel);
        assertNotNull(response);
    }

    @Test
    void validateFile() throws FileOperationsException, IOException, ParserConfigurationException, SAXException, CustomException {
        Mockito.doNothing().when(repositoryHelper).fileUpload(true,oDataClintResponse,requestModel);
        RequestModel response = bulkUploadResource.saveData(requestModel);
        assertNotNull(response);
    }

    @Test
    void getErrorLogFiles() throws CustomException {
        Mockito.when(repositoryHelper.createLogFileFromDB(requestModel)).thenReturn(new byte[100]);
        Response response = bulkUploadResource.getErrorLogFiles(requestModel);
        assertNotNull(response);
        assertNotNull(response.getEntity());
    }

    @Test
    void getLogFile() {
        List<ProcessQueue>list = new ArrayList<>();
        Mockito.when(repositoryHelper.getStatus(null,1,1)).thenReturn(list);
        List<ProcessQueue> requestModelList = bulkUploadResource.getLogFile(1,1);
        assertNotNull(requestModelList);
        assertFalse(requestModelList.isEmpty());
    }

    @Test
    void getParticularEventStatus() throws CustomException {
        List<ProcessQueue>list = new ArrayList<>();
        //list.add(requestModel);
        Mockito.when(repositoryHelper.getStatus(requestModel,1,1)).thenReturn(list);
        List<ProcessQueue> requestModelList =  bulkUploadResource.getParticularEventStatus(requestModel,1,1);
        assertNotNull(requestModelList);
        assertFalse(requestModelList.isEmpty());
    }

    @Test
    void getExcelTemplate() throws IOException, ParserConfigurationException, SAXException, CustomException {
        Mockito.when(templateFileHelper.createTemplateFile(requestModel)).thenReturn(new byte[100]);
        Response response = bulkUploadResource.getExcelTemplate(requestModel);
        assertNotNull(response);
        assertNotNull(response.getEntity());
    }
}