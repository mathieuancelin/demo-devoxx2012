package devoxx.core.controllers;

import devoxx.core.fwk.api.Controller;
import devoxx.api.*;
import devoxx.core.util.F.Tuple;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import javax.activation.MimetypesFileTypeMap;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.jboss.weld.environment.osgi.api.Service;
import org.jboss.weld.environment.osgi.api.annotation.Specification;
import org.jboss.weld.environment.osgi.api.events.ServiceEvents;

@Path("plugins")
public class PluginsController implements Controller {
    
    @Inject Service<Plugin> plugins;
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> getPluginIds() {
        List<String> ids = new ArrayList<String>();
        for (Plugin plugin : plugins) {
            ids.add(plugin.pluginId());
        }
        return ids;
    }
    
    @GET @Path("{pluginId}/apply")
    @Produces(MediaType.APPLICATION_JSON)
    public String apply(@PathParam("pluginId") String pluginid, @FormParam("content") String content) {
        for (Plugin plugin : plugins) {
            if (plugin.pluginId().equals(pluginid)) {
                return plugin.apply(content);
            }
        }
        return "Error while processing content ...";
    }
    
    @GET @Path("messages")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> getPopupMessages() {
        return Collections.emptyList();
    }
    
    @GET @Path("installed")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> getInstalledBundles() {
        List<String> names = new ArrayList<String>();
        for(Tuple<String, String> t : pluginNames.values()) {
            names.add(t._1);
        }
        return names;
    }
    
    @GET
    @Path("{pluginId}/{route}")
    public Response getRes(@PathParam("pluginId") String pluginid, @PathParam("route") String route) {
        for (Plugin plugin : plugins) {
            if (plugin.pluginId().equals(pluginid)) {
                if (plugin.resources().containsKey(route)) {
                    String mt = new MimetypesFileTypeMap().getContentType(plugin.resources().get(route));
                    return Response.ok(plugin.resources().get(route), mt).build();
                }
                throw new WebApplicationException(404);
            }
        }
        throw new WebApplicationException(404);
    }
    
    private ConcurrentHashMap<String, Tuple<String, String>> pluginNames = 
            new ConcurrentHashMap<String, Tuple<String, String>>();
    
    public void listenArrival(@Observes @Specification(Plugin.class) ServiceEvents.ServiceArrival evt) {
        Plugin plugin = evt.getService(Plugin.class);
        if (!pluginNames.containsKey(plugin.pluginId())) {
            pluginNames.putIfAbsent(plugin.pluginId(), new Tuple<String, String>(plugin.name(), plugin.desc()));
        }
    }
    
    public void listenDeparture(@Observes @Specification(Plugin.class) ServiceEvents.ServiceDeparture evt) {
        Plugin plugin = evt.getService(Plugin.class);
        pluginNames.remove(plugin.pluginId());
    }
}
