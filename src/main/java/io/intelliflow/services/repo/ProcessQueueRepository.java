package io.intelliflow.services.repo;

 /*
    @author rahul.malawadkar@intelliflow.ai
    @created on 27-07-2023
 */

import io.intelliflow.services.models.ProcessQueue;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import io.quarkus.panache.common.Sort;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class ProcessQueueRepository implements PanacheMongoRepository<ProcessQueue> {

    public final void saveProcessQueue(ProcessQueue processQueue){
        persistOrUpdate(processQueue);
    }

    public final void updateProcessQueue(ProcessQueue processQueue){
        update(processQueue);
    }

    public final List<ProcessQueue> list(int page, int size) {
        return findAll().page(page,size).list();
    }

    public final List<ProcessQueue> listWithWorkSpace(String workspace, int page, int size) {
        return find("workSpace = ?1", Sort.descending("uploadId"), workspace).page(page, size).list();
    }

    public final List<ProcessQueue> listWithAppNameAndWorkSpace(String workspace,String appName,int page, int size) {
        return find("workSpace =?1 AND appName = ?2",Sort.descending("uploadId"),workspace,appName).page(page,size).list();
    }

    public final List<ProcessQueue> listWithUploadIdAndWorkSpace(String workspace,long uploadId,int page, int size) {
        return find("workSpace =?1 AND uploadId = ?2",Sort.descending("uploadId"),workspace,uploadId).page(page,size).list();
    }

    public final long getCount(){
        return count();
    }

    public final ProcessQueue getAllSortedByUploadIdAscending() {
        return find("uploadStatus = 'QUEUED'",Sort.ascending("uploadId")).firstResult();
    }

    public final List<ProcessQueue> getAllSortedByUploadIdDescending() {
        return listAll(Sort.descending("uploadId"));
    }

    public final List<ProcessQueue> getAllByUpdateStatusAndWorkspace(String workSpace) {
        return find("uploadStatus = 'RUNNING' AND workSpace = ?1",workSpace).list();
    }

}
