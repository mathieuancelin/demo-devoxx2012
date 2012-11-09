package devoxx.core;

import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.servlet.*;
import org.apache.commons.fileupload.disk.*;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class FileUpload extends HttpServlet {
    
    private final ServletFileUpload sfu = new ServletFileUpload(new DiskFileItemFactory(1073741824, new File("/tmp")));
    
    private final File root = new File("/tmp");
    
    private final BundleContext context;

    public FileUpload(BundleContext context) {
        this.context = context;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            List<FileItem> list = sfu.parseRequest(req);
            for (FileItem item : list) {
                String name = item.getName();
                File bundle = new File(root, name);
                if (bundle.exists()) {
                    bundle.delete();
                }
                item.write(bundle);
                Bundle b = context.installBundle("file://" + bundle.getAbsolutePath());
                b.start();
                resp.sendRedirect("/static/settings.html");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    
    
}
