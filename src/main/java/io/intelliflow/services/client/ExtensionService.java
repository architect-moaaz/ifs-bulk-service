package io.intelliflow.services.client;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Singleton
@RegisterRestClient(configKey = "repository-api")
public interface ExtensionService {

    @GET
    @Path("{workspaceName}/{appName}/$metadata")
    @Produces(MediaType.APPLICATION_JSON)
    String getMetaData(@PathParam("workspaceName") String workspaceName, @PathParam("appName") String appName);

}
