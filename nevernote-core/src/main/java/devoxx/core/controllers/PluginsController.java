package devoxx.core.controllers;

import com.google.common.base.Joiner;
import devoxx.core.fwk.api.Controller;
import devoxx.api.*;
import devoxx.core.fwk.*;
import devoxx.core.fwk.F.Tuple;
import devoxx.core.fwk.F.Tuple3;
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
import org.jboss.weld.environment.osgi.api.annotation.Required;
import org.jboss.weld.environment.osgi.api.annotation.Specification;
import org.jboss.weld.environment.osgi.api.events.ServiceEvents;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

@Path("plugins")
public class PluginsController implements Controller {

    @Inject
    @Required
    Service<Plugin> plugins;
    //@Inject @OSGiService @Lang(Language.EN) Plugin plugin;
    @Inject
    BundleContext context;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getPluginIds() {
        List<String> ids = new ArrayList<String>();
        for (Plugin plugin : plugins) {
            if (plugin.modifyContent()) {
                ids.add("{\"id\":\"" + plugin.pluginId()
                    + "\", \"icon\":\"" + plugin.icon()
                    + "\", \"name\":\"" + plugin.name()
                    + "\", \"desc\":\"" + plugin.desc() + "\"}");
            }
        }
        return "[" + Joiner.on(", ").join(ids) + "]";
    }

    @POST
    @Path("apply/{pluginId}")
    @Produces(MediaType.TEXT_PLAIN)
    public String apply(@PathParam("pluginId") String pluginid, @FormParam("content") String content) {
        for (Plugin plugin : plugins) {
            if (plugin.pluginId().equals(pluginid)) {
                return plugin.apply(content);
            }
        }
        return "Error while processing content ...";
    }

    @GET
    @Path("messages")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> getPopupMessages(@QueryParam("since") Long since) {
        List<Tuple<Long, String>> messagesCopy = new ArrayList<Tuple<Long, String>>();
        messagesCopy.addAll(messages);
        List<String> values = new ArrayList<String>();
        for (Tuple<Long, String> t : messages) {
            if (since == null) {
                values.add("{\"last\":" + t._1 + ", \"message\":\"" + t._2 + "\"}");
            } else if (t._1 > since) {
                values.add("{\"last\":" + t._1 + ", \"message\":\"" + t._2 + "\"}");
            }
        }
        return values;
    }

    @GET
    @Path("active")
    @Produces(MediaType.APPLICATION_JSON)
    public String getActivePlugins() {
        List<String> result = new ArrayList<String>();
        for (Plugin plugin : plugins) {
            result.add(pluginToJSon(plugin));
        }
        return "[" + Joiner.on(',').join(result) + "]";
    }

    @GET
    @Path("installed")
    @Produces(MediaType.APPLICATION_JSON)
    public String getInstalledPlugins() {
        List<String> result = new ArrayList<String>();
        for (Bundle bundle : context.getBundles()) {
            if (bundle.getSymbolicName().contains("plugin") && bundle.getState() != Bundle.ACTIVE) {
                result.add(bundleToJSon(bundle));
            }
        }
        return "[" + Joiner.on(',').join(result) + "]";
    }

    private String bundleToJSon(Bundle bundle) {
        StringBuilder stringBuilder = new StringBuilder("{\"bundleId\": ").append(bundle.getBundleId())
                .append(", \"bundleName\": \"").append(bundle.getSymbolicName())
                .append("\", \"pluginId\": \"")
                .append("\", \"pluginName\": \"")
                .append("\", \"state\": \"").append(stateToString(bundle.getState()))
                .append("\"}");
        return stringBuilder.toString();
    }

    private String pluginToJSon(Plugin plugin) {
        Bundle bundle = plugin.bundle();
        StringBuilder stringBuilder = new StringBuilder("{\"bundleId\": ").append(bundle.getBundleId())
                .append(", \"bundleName\": \"").append(bundle.getSymbolicName())
                .append("\", \"pluginId\": \"").append(plugin.pluginId())
                .append("\", \"pluginName\": \"").append(plugin.name())
                .append("\", \"state\": \"").append(stateToString(bundle.getState()))
                .append("\"}");
        return stringBuilder.toString();
    }

    private String stateToString(int state) {
        String result = "UNKNOWN";
        switch (state) {
            case Bundle.INSTALLED: result = "INSTALLED"; break;
            case Bundle.RESOLVED: result = "INSTALLED"; break;
            case Bundle.ACTIVE: result = "ACTIVE"; break;
            case Bundle.STOPPING: result = "TREATING"; break;
            case Bundle.STARTING: result = "TREATING"; break;
        }
        return result;
    }

    @GET
    @Path("{pluginId}/start")
    public Response startPlugin(@PathParam("pluginId") String pluginid) {
        try {
            Bundle bundle = context.getBundle(Long.parseLong(pluginid));
            if (bundle == null) {
                throw new WebApplicationException(404);
            }
            if (bundle.getState() == Bundle.ACTIVE) {
                throw new WebApplicationException(403);
            }
            bundle.start();
            return Response.ok().build();
        } catch (NumberFormatException nfe) {
            throw new WebApplicationException(400);
        } catch (BundleException be) {
            throw new WebApplicationException(500);
        }
    }

    @GET
    @Path("{pluginId}/stop")
    public Response stopPlugin(@PathParam("pluginId") String pluginid) {
        try {
            Bundle bundle = context.getBundle(Long.parseLong(pluginid));
            if (bundle == null) {
                throw new WebApplicationException(404);
            }
            if (bundle.getState() != Bundle.ACTIVE) {
                throw new WebApplicationException(403);
            }
            bundle.stop();
            return Response.ok().build();
        } catch (NumberFormatException nfe) {
            throw new WebApplicationException(400);
        } catch (BundleException be) {
            throw new WebApplicationException(500);
        }
    }

    @GET
    @Path("{pluginId}/remove")
    public Response removePlugin(@PathParam("pluginId") String pluginid) {
        try {
            Bundle bundle = context.getBundle(Long.parseLong(pluginid));
            if (bundle == null) {
                throw new WebApplicationException(404);
            }
            if (bundle.getState() == Bundle.UNINSTALLED) {
                throw new WebApplicationException(403);
            }
            bundle.uninstall();
            return Response.ok().build();
        } catch (NumberFormatException nfe) {
            throw new WebApplicationException(400);
        } catch (BundleException be) {
            throw new WebApplicationException(500);
        }
    }

    @GET
    @Path("res/{pluginId}/{route}")
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
    private ConcurrentHashMap<String, Tuple3<Long, String, String>> pluginNames =
            new ConcurrentHashMap<String, Tuple3<Long, String, String>>();
    private List<Tuple<Long, String>> messages =
            Collections.synchronizedList(new ArrayList<Tuple<Long, String>>());

    public void listenArrival(@Observes @Specification(Plugin.class) ServiceEvents.ServiceArrival evt) {
        Plugin p = evt.getService(Plugin.class);
        SimpleLogger.info("A new plugin '{}' is available", p.name());
        if (!pluginNames.containsKey(p.pluginId())) {
            pluginNames.putIfAbsent(p.pluginId(), new Tuple3<Long, String, String>(p.bundleId(), p.name(), p.desc()));
            messages.add(new Tuple<Long, String>(System.currentTimeMillis(), "Plugin " + p.name() + " is now available for use. Enjoy ;-)"));
        }
    }

    public void listenDeparture(@Observes @Specification(Plugin.class) ServiceEvents.ServiceDeparture evt) {
        System.out.println("bye plugin");
        Plugin p = evt.getService(Plugin.class);
        SimpleLogger.info("Plugin '{}' is going away ...", p.name());
        pluginNames.remove(p.pluginId());
    }
}
