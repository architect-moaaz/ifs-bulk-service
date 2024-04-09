package io.intelliflow.services.repo;

 /*
    @author rahul.malawadkar@intelliflow.ai
    @created on 02-08-2023
 */

import io.intelliflow.services.models.IdCountPerRequest;
import io.quarkus.mongodb.panache.PanacheMongoRepository;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class IdCountPerRequestRepository implements PanacheMongoRepository<IdCountPerRequest> {

    public void save(IdCountPerRequest idCountPerRequest){
        persist(idCountPerRequest);
    }

    public IdCountPerRequest getRecord(){
        return findAll().firstResult();
    }

    public void updateRecord(IdCountPerRequest idCountPerRequest){
        update(idCountPerRequest);
    }

    public long getCount(){
        return count();
    }

}
