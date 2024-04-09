package io.intelliflow.services.models;

import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.types.ObjectId;

/*
    @author rahul.malawadkar@intelliflow.ai
    @created on 02-08-2023
 */
@MongoEntity(collection = "BulkUpload-IdCountPerRequest")
public class IdCountPerRequest {
    public ObjectId id;

    private long requestCount;

    public IdCountPerRequest(long requestCount) {
        this.requestCount = requestCount;
    }

    public IdCountPerRequest() {
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public long getRequestCount() {
        return requestCount;
    }

    public void setRequestCount(long requestCount) {
        this.requestCount = requestCount;
    }
}
