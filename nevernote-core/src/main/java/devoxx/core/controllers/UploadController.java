package devoxx.core.controllers;

import java.io.*;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import devoxx.core.fwk.api.Controller;
import devoxx.api.*;

import javax.inject.Inject;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

@Path("/bundle")
public class UploadController implements Controller {
    
    @Inject BundleContext ctx;

    @POST @Path("/upload")
    public Response uploadFile(File file) {
        String uploadedFileLocation = "/tmp/" + file.getName();
        try {
            InputStream uploadedInputStream = new FileInputStream(file);
            //writeToFile(uploadedInputStream, uploadedFileLocation);
            Bundle newBundle = ctx.installBundle("file://" + uploadedFileLocation, uploadedInputStream);
            newBundle.start();
            return Response.status(200).entity("ok").build();
        } catch (Exception ex) {
            ex.printStackTrace();
            return Response.status(500).entity("ko").build();
        }
    }

    private void writeToFile(InputStream uploadedInputStream,
            String uploadedFileLocation) {
        try {
            OutputStream out = new FileOutputStream(new File(
                    uploadedFileLocation));
            int read = 0;
            byte[] bytes = new byte[1024];
            out = new FileOutputStream(new File(uploadedFileLocation));
            while ((read = uploadedInputStream.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
