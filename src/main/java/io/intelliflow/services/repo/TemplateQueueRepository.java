package io.intelliflow.services.repo;

 /*
    @author rahul.malawadkar@intelliflow.ai
    @created on 27-07-2023
 */

import io.intelliflow.services.models.TemplateQueue;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import io.quarkus.panache.common.Sort;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class TemplateQueueRepository implements PanacheMongoRepository<TemplateQueue> {

    public final void saveTemplateQueue(TemplateQueue templateQueue){
        persistOrUpdate(templateQueue);
    }

    public final List<TemplateQueue> list() {
        return listAll();
    }


    public final long getCount(){
        return count();
    }

    public final List<TemplateQueue> listWithPagination(String workspace,int page, int pageSize) {
        return find("workSpace", Sort.descending("uploadId"), workspace).page(page, pageSize).list();
    }

    public final long deleteTemplateQueue(String workspace, long uploadId){
        return delete("workSpace = ?1 AND uploadId = ?2", workspace, uploadId);
    }

}
