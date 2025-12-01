package mg.naina.framework.servlet;

import mg.naina.framework.annotation.Controller;
import mg.naina.framework.annotation.UrlMapping;
import mg.naina.framework.core.Mapping;
import mg.naina.framework.models.ModelView;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.*;

public class FrontController extends HttpServlet {
    
    private List<Mapping> urlMappings = new ArrayList<>();
    private String basePackage;
    private String projectRoot;

    @Override
    public void init() throws ServletException {
        super.init();
        basePackage = getInitParameter("basePackage");
        if (basePackage == null || basePackage.trim().isEmpty()) basePackage = "mg.naina.test";
        detectProjectRoot();
        try {
            scanControllers();
        } catch (Exception e) {
            throw new ServletException("Erreur init", e);
        }
    }

    private void detectProjectRoot() {
        try {
            String webappPath = getServletContext().getRealPath("/");
            File webappDir = webappPath != null ? new File(webappPath) : null;
            
            for (String path : new String[] {
                webappDir != null && webappDir.getParentFile() != null ? webappDir.getParentFile().getParentFile().getAbsolutePath() : null,
                System.getProperty("user.dir"),
                "C:\\Users\\HP\\Documents\\ITU\\Mr_Naina\\Mr_Naina_Framework\\Sprint1_Bis"
            }) {
                if (path != null && new File(path).exists() && 
                    new File(path, "Framework").exists() && 
                    new File(path, "Test").exists()) {
                    projectRoot = path;
                    return;
                }
            }
            projectRoot = "C:\\Users\\HP\\Documents\\ITU\\Mr_Naina\\Mr_Naina_Framework\\Sprint1_Bis";
        } catch (Exception e) {
            projectRoot = System.getProperty("user.dir");
        }
    }

    private void scanControllers() {
        Reflections reflections = new Reflections(new ConfigurationBuilder().forPackages(basePackage));
        Set<Class<?>> controllers = reflections.getTypesAnnotatedWith(Controller.class);
        
        if (controllers.isEmpty()) {
            try {
                Class<?> homeController = Class.forName("mg.naina.test.controller.HomeController");
                if (homeController.isAnnotationPresent(Controller.class)) controllers.add(homeController);
            } catch (ClassNotFoundException ignored) {}
        }

        for (Class<?> cls : controllers) {
            try {
                Object instance = cls.getDeclaredConstructor().newInstance();
                for (Method method : cls.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(UrlMapping.class)) {
                        String pattern = method.getAnnotation(UrlMapping.class).value();
                        urlMappings.add(new Mapping(cls.getName(), method.getName(), pattern, method, instance));
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        processRequest(req, resp);
    }
    
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        processRequest(req, resp);
    }

    private File findFile(String path) {
        String fileName = path.startsWith("/") ? path.substring(1) : path;
        
        for (String location : new String[] {
            getServletContext().getRealPath("/" + fileName),
            projectRoot + File.separator + "Test" + File.separator + fileName,
            projectRoot + File.separator + "Test" + File.separator + "src" + File.separator + "main" + File.separator + "webapp" + File.separator + fileName
        }) {
            if (location != null) {
                File file = new File(location);
                if (file.exists() && file.isFile()) return file;
            }
        }
        
        try {
            File testDir = new File(projectRoot, "Test");
            return testDir.exists() ? searchFileRecursively(testDir, fileName) : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    private File searchFileRecursively(File dir, String fileName) {
        if (dir == null || !dir.isDirectory()) return null;
        File[] files = dir.listFiles();
        if (files == null) return null;
        
        for (File file : files) {
            if (file.isFile() && file.getName().equals(fileName)) return file;
            if (file.isDirectory()) {
                File found = searchFileRecursively(file, fileName);
                if (found != null) return found;
            }
        }
        return null;
    }
    
    private boolean matches(String path, String pattern) {
        if (!pattern.contains("{")) {
            return pattern.equals(path);
        }
        // Simple replacement for {id}
        String regex = pattern.replace("{id}", "([^/]+)");
        return path.matches(regex);
    }
    
    private void serveFile(File file, HttpServletResponse response) throws IOException {
        String name = file.getName().toLowerCase();
        String mimeType = getServletContext().getMimeType(name);
        
        if (mimeType == null) {
            mimeType = name.endsWith(".html") || name.endsWith(".htm") ? "text/html" :
                      name.endsWith(".css") ? "text/css" :
                      name.endsWith(".js") ? "application/javascript" :
                      name.endsWith(".jsp") ? "text/html" :
                      name.endsWith(".png") ? "image/png" :
                      name.endsWith(".jpg") || name.endsWith(".jpeg") ? "image/jpeg" : "text/plain";
        }
        
        response.setContentType(mimeType);
        
        if (name.endsWith(".jsp")) {
            response.getWriter().println(new String(Files.readAllBytes(file.toPath())));
        } else {
            response.setContentLength((int) file.length());
            try (FileInputStream in = new FileInputStream(file);
                 OutputStream out = response.getOutputStream()) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) out.write(buffer, 0, bytesRead);
            }
        }
    }

    private void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String fullPath = request.getRequestURI().substring(request.getContextPath().length());
        String path = fullPath;
        if (path.startsWith("/app")) {
            path = path.substring(4); // remove /app
        }
        if (path.isEmpty()) path = "/";
        
        // Ressources statiques - essayer de les servir directement
        if (path.toLowerCase().matches(".*\\.(jsp|html|htm|css|js|png|jpg|gif|ico)$")) {
            // Chercher le fichier
            File resourceFile = findFile(path);
            if (resourceFile != null) {
                serveFile(resourceFile, response);
                return;
            }
            // Si le fichier n'existe pas, ne pas utiliser le dispatcher
            // On passe à la suite pour afficher la page de debug
        }
        
        // Mappings d'URL
        Mapping mapping = null;
        for (Mapping m : urlMappings) {
            if (matches(path, m.getPattern())) {
                mapping = m;
                break;
            }
        }
        if (mapping != null) {
            response.setContentType("text/html;charset=UTF-8");
            try {
                Object result = mapping.execute();
                
                // Si le résultat est un ModelView, dispatcher vers la vue
                if (result instanceof ModelView) {
                    ModelView modelView = (ModelView) result;
                    String viewPath = modelView.getView();
                    
                    // Ajouter les données au request
                    for (Map.Entry<String, Object> entry : modelView.getData().entrySet()) {
                        request.setAttribute(entry.getKey(), entry.getValue());
                    }
                    
                    // Pour les JSP, utiliser toujours le dispatcher
                    if (viewPath.endsWith(".jsp")) {
                        try {
                            RequestDispatcher dispatcher = request.getRequestDispatcher("/" + viewPath);
                            if (dispatcher != null) {
                                dispatcher.forward(request, response);
                                return;
                            }
                        } catch (Exception ignored) {}
                    }
                    
                    // Chercher le fichier de vue
                    File viewFile = findFile(viewPath);
                    if (viewFile != null) {
                        serveFile(viewFile, response);
                        return;
                    }
                    
                    response.getWriter().println("<h1>Erreur: Vue '" + viewPath + "' introuvable</h1>");
                    return;
                }
                
                response.getWriter().println(result != null ? result.toString() : 
                    "<h1>Exécution réussie</h1><p>URL: " + path + "</p>");
                return;
            } catch (Exception e) {
                response.getWriter().println("<h1>Erreur: " + e.getMessage() + "</h1>");
                return;
            }
        }
        
        // Gestion spéciale pour l'URL racine "/app/"
        if (path.equals("/")) {
            response.setContentType("text/html;charset=UTF-8");
            try {
                Class<?> cls = Class.forName("mg.naina.test.controller.HomeController");
                Object instance = cls.getDeclaredConstructor().newInstance();
                Method method = cls.getMethod("index");
                Object result = method.invoke(instance);
                response.getWriter().println(result != null ? result.toString() : 
                    "<h1>Exécution réussie</h1><p>URL: /</p>");
                return;
            } catch (Exception e) {
                response.getWriter().println("<h1>Erreur: " + e.getMessage() + "</h1>");
                return;
            }
        }
        File resourceFile = findFile(path);
        if (resourceFile != null) {
            serveFile(resourceFile, response);
            return;
        }
        
        // Page de debug pour URLs non trouvées
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println("<!DOCTYPE html><html><body>");
        out.println("<h1>Framework - Debug Info</h1>");
        out.println("<p><strong>URL demandée:</strong> " + request.getRequestURI() + "</p>");
        out.println("<p><strong>Path traité:</strong> " + path + "</p>");
        out.println("<p><strong>Package:</strong> " + basePackage + "</p>");
        
        if (!urlMappings.isEmpty()) {
            out.println("<h3>URLs disponibles:</h3><ul>");
            for (Mapping m : urlMappings) {
                out.println("<li><a href='" + request.getContextPath() + "/app" + m.getPattern() + "'>" + m.getPattern() + 
                           "</a> → " + m.getClassName() + "." + m.getMethodName() + "()</li>");
            }
            out.println("</ul>");
        }
        out.println("</body></html>");
    }
}