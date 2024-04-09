package io.intelliflow.services.models;

 /*
    @author rahul.malawadkar@intelliflow.ai
    @created on 27-07-2023
 */
import org.jboss.resteasy.annotations.providers.multipart.PartType;

import javax.ws.rs.FormParam;
import javax.ws.rs.core.MediaType;
import java.io.InputStream;

public class MultipartBody {

    @FormParam("file")
    @PartType(MediaType.APPLICATION_OCTET_STREAM)
    public InputStream file;

    @FormParam("miniAppName")
    @PartType(MediaType.TEXT_PLAIN)
    public String miniAppName;
}
