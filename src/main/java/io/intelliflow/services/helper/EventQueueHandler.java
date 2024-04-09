package io.intelliflow.services.helper;

import io.intelliflow.services.centralcustomexceptionhandler.CustomException;
import io.intelliflow.services.controller.BulkUploadResource;
import io.intelliflow.services.models.ProcessQueue;
import io.intelliflow.services.models.RequestModel;
import io.intelliflow.services.models.Status;
import io.intelliflow.services.repo.ProcessQueueRepository;
import io.quarkus.logging.Log;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Date;
import java.util.List;

@ApplicationScoped
public class EventQueueHandler {
    @Inject
    BulkUploadResource bulkUploadResource;
    @ConfigProperty(name = "file.location")
    String fileLocation;

    @Inject
    ProcessQueueRepository processQueueRepository;

    private void updateEvent(ProcessQueue processQueue) {
        processQueueRepository.updateProcessQueue(processQueue);
    }

    private byte[] getByteFileArray(String appName, String workSpace, long uploadId) throws IOException {
        File file = new File(fileLocation + workSpace + "-" + appName + "-" + uploadId + ".xlsx");
        byte[] arr = new byte[(int) file.length()];
        try (FileInputStream fl = new FileInputStream(file)) {
            fl.read(arr);
        }
        Files.delete(file.toPath());
        return arr;
    }

    public final void processAllTheQueuedEvents() throws IOException, CustomException {
        ProcessQueue result = processQueueRepository.getAllSortedByUploadIdAscending();
        if(result != null) {
            String appName = result.getAppName();
            String workSpace = result.getWorkSpace();
            long uploadId = result.getUploadId();
            List<ProcessQueue> queues = processQueueRepository.getAllByUpdateStatusAndWorkspace(workSpace);
            if (queues.isEmpty()) {
                RequestModel requestModel = new RequestModel();
                requestModel.setMiniAppName(appName);
                requestModel.setWorkspaceName(workSpace);
                requestModel.setUploadId(uploadId);
                requestModel.setExcelContent(getByteFileArray(appName, workSpace, uploadId));
                Log.info("Processing started for: " + appName + "-" + workSpace + "-" + uploadId);

                result.setUploadStatus(Status.RUNNING);
                result.setProcessStartTime(new Date());
                updateEvent(result);

                RequestModel requestModelResponse = bulkUploadResource.saveData(requestModel);

                Log.info("Status of the upload: " + requestModelResponse.getStatus().name());
                result.setUploadStatus(requestModelResponse.getStatus());
                result.setProcessEndTime(new Date());
                result.setRemark(requestModel.getRemark());
                if (!requestModel.getStatus().equals(Status.SUCCESS)) {
                    result.setSuccessRecordsCount(requestModelResponse.getSuccessRecordsCount());
                    result.setErrorRecordsCount(requestModelResponse.getErrorRecordsCount());
                }
                updateEvent(result);
                Log.info("Processing completed for: " + requestModelResponse.getStatus().name() + " " + appName + "-" + workSpace + "-" + uploadId);
            }
        }
    }
}
