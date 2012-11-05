/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package devoxx.core.controllers;

import devoxx.core.fwk.api.Controller;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 *
 * @author mathieuancelin
 */

@Path("hello")
public class Application implements Controller {
    
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String get() {
        return "Hello";
    }
    
}
