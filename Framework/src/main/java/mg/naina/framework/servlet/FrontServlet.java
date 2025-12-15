package mg.naina.framework.servlet;

import mg.naina.framework.annotation.Controller;
import mg.naina.framework.annotation.PathVariable;
import mg.naina.framework.annotation.RequestParam;
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
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class FrontServlet extends HttpServlet {
    
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
            System.getProperty("user.dir")
        }) {
            if (path != null && new File(path).exists() && 
                new File(path, "Framework").exists()) {
                projectRoot = path;
                return;
            }
        }
        projectRoot = System.getProperty("user.dir");
    } catch (Exception e) {
        projectRoot = System.getProperty("user.dir");
    }
}

    private void scanControllers() {
    Reflections reflections = new Reflections(new ConfigurationBuilder().forPackages(basePackage));
    Set<Class<?>> controllers = reflections.getTypesAnnotatedWith(Controller.class);
    
    for (Class<?> cls : controllers) {
        try {
            Object instance = cls.getDeclaredConstructor().newInstance();
            for (Method method : cls.getDeclaredMethods()) {
                if (method.isAnnotationPresent(UrlMapping.class)) {
                    UrlMapping urlMapping = method.getAnnotation(UrlMapping.class);
                    String pattern = urlMapping.value();
                    String httpMethod = urlMapping.method();
                    
                    // Vérifier que le pattern n'est pas vide
                    if (pattern != null && !pattern.trim().isEmpty()) {
                        urlMappings.add(new Mapping(
                            cls.getName(), 
                            method.getName(), 
                            pattern, 
                            httpMethod, 
                            method, 
                            instance
                        ));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace(); // Pour voir les erreurs lors du scan
        }
    }
    
    // Debug : Afficher les mappings chargés
    System.out.println("=== Mappings chargés ===");
    for (Mapping m : urlMappings) {
        System.out.println("Pattern: " + m.getPattern() + 
                         " | Method: " + m.getHttpMethod() + 
                         " | Class: " + m.getClassName() + 
                         " | MethodName: " + m.getMethodName());
    }
}


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        processRequest(req, resp);
    }
    
    @Override
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
    if (pattern == null || pattern.isEmpty()) {
        return false;
    }
    if (!pattern.contains("{")) {
        return pattern.equals(path);
    }
    // Remplacer {variable} par un groupe capturant
    String regex = pattern.replaceAll("\\{[^}]+\\}", "([^/]+)");
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
        path = path.substring(4);
    }
    if (path.isEmpty()) path = "/";
    
    String httpMethod = request.getMethod(); // GET, POST, etc.
    
    // Ressources statiques
    if (path.toLowerCase().matches(".*\\.(jsp|html|htm|css|js|png|jpg|gif|ico)$")) {
        File resourceFile = findFile(path);
        if (resourceFile != null) {
            serveFile(resourceFile, response);
            return;
        }
    }
    
    // Mappings d'URL
    Mapping mapping = null;
    for (Mapping m : urlMappings) {
        String pattern = m.getPattern();
        String mappingMethod = m.getHttpMethod();
        
        if (pattern != null && matches(path, pattern) && matchesHttpMethod(httpMethod, mappingMethod)) {
            mapping = m;
            break;
        }
    }
    
    if (mapping != null) {
        response.setContentType("text/html;charset=UTF-8");
        try {
            Object result = executeMapping(mapping, request, path);
            
            if (result instanceof ModelView) {
                ModelView modelView = (ModelView) result;
                String viewPath = modelView.getView();
                
                for (Map.Entry<String, Object> entry : modelView.getData().entrySet()) {
                    request.setAttribute(entry.getKey(), entry.getValue());
                }
                
                if (viewPath.endsWith(".jsp")) {
                    try {
                        RequestDispatcher dispatcher = request.getRequestDispatcher("/" + viewPath);
                        if (dispatcher != null) {
                            dispatcher.forward(request, response);
                            return;
                        }
                    } catch (Exception ignored) {}
                }
                
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
            e.printStackTrace();
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
            String methodInfo = m.getHttpMethod() != null && !m.getHttpMethod().isEmpty() ? " [" + m.getHttpMethod() + "]" : "";
            String pattern = m.getPattern() != null ? m.getPattern() : "N/A";
            out.println("<li><a href='" + request.getContextPath() + "/app" + pattern + "'>" + pattern + 
                       "</a>" + methodInfo + " → " + m.getClassName() + "." + m.getMethodName() + "()</li>");
        }
        out.println("</ul>");
    }
    out.println("</body></html>");
}

    private boolean matchesHttpMethod(String requestMethod, String mappingMethod) {
        if (mappingMethod == null || mappingMethod.isEmpty() || mappingMethod.equals("GET")) {
            return requestMethod.equalsIgnoreCase("GET");
        }
        return requestMethod.equalsIgnoreCase(mappingMethod);
    }

    private Object executeMapping(Mapping mapping, HttpServletRequest request, String path) throws Exception {
        Method method = mapping.getMethod();
        Object instance = mapping.getControllerInstance();
        
        java.lang.reflect.Parameter[] parameters = method.getParameters();
        
        if (parameters.length == 0) {
            return method.invoke(instance);
        }
        
        Object[] args = new Object[parameters.length];
        
        // Extraire les variables de chemin si le pattern en contient
        Map<String, String> pathVariables = extractPathVariables(mapping.getPattern(), path);

        // Cas spécial pour Map<String, Object> formData
        if (parameters.length == 1 && parameters[0].getType() == Map.class) {
            Map<String, Object> formData = new HashMap<>();
            Enumeration<String> paramNames = request.getParameterNames();
            List<String> valuesList = new ArrayList<>();

            while (paramNames.hasMoreElements()) {
                String paramName = paramNames.nextElement();
                String paramValue = request.getParameter(paramName);

                // Créer une Map pour chaque champ
                Map<String, String> fieldMap = new HashMap<>();
                fieldMap.put(paramName, paramValue);
                formData.put(paramName, fieldMap);

                // Ajouter à la liste des valeurs pour le tableau
                valuesList.add(paramValue);
            }

            // Réunir toutes les valeurs dans un tableau String
            formData.put("all", valuesList.toArray(new String[0]));

            args[0] = formData;
        } else {
            // Injection automatique de bean (POJO)
            for (int i = 0; i < parameters.length; i++) {
                java.lang.reflect.Parameter param = parameters[i];
                Class<?> paramType = param.getType();
                String paramValue = null;

                // Vérifier @PathVariable
                PathVariable pathVarAnnotation = param.getAnnotation(PathVariable.class);
                if (pathVarAnnotation != null) {
                    paramValue = pathVariables.get(pathVarAnnotation.value());
                    args[i] = convertParameter(paramValue, paramType);
                    continue;
                }

                // Vérifier @RequestParam
                RequestParam requestParamAnnotation = param.getAnnotation(RequestParam.class);
                if (requestParamAnnotation != null) {
                    String paramName = requestParamAnnotation.value();
                    paramValue = request.getParameter(paramName);
                    args[i] = convertParameter(paramValue, paramType);
                    continue;
                }

                // Si c'est un bean (POJO) personnalisé
                if (!paramType.isPrimitive() && !paramType.getName().startsWith("java.")) {
                    try {
                        Object bean = paramType.getDeclaredConstructor().newInstance();
                        java.lang.reflect.Field[] fields = paramType.getDeclaredFields();
                        for (java.lang.reflect.Field field : fields) {
                            String fieldName = field.getName();
                            String fieldValue = request.getParameter(fieldName);
                            if (fieldValue != null) {
                                field.setAccessible(true);
                                Object converted = convertParameter(fieldValue, field.getType());
                                field.set(bean, converted);
                            }
                        }
                        args[i] = bean;
                        continue;
                    } catch (Exception e) {
                        args[i] = null;
                        continue;
                    }
                }

                // Utiliser le nom du paramètre de la méthode (sans annotation)
                String paramName = param.getName();
                paramValue = request.getParameter(paramName);
                args[i] = convertParameter(paramValue, paramType);
            }
        }

        return method.invoke(instance, args);
    }

    private Map<String, String> extractPathVariables(String pattern, String path) {
    Map<String, String> variables = new HashMap<>();
    if (pattern == null || !pattern.contains("{")) return variables;
    
    // Créer une regex avec des groupes nommés
    String regex = pattern;
    List<String> varNames = new ArrayList<>();
    
    Pattern varPattern = Pattern.compile("\\{([^}]+)\\}");
    Matcher varMatcher = varPattern.matcher(pattern);
    
    while (varMatcher.find()) {
        varNames.add(varMatcher.group(1));
    }
    
    // Remplacer {variable} par des groupes capturants
    regex = pattern.replaceAll("\\{[^}]+\\}", "([^/]+)");
    
    Pattern pathPattern = Pattern.compile(regex);
    Matcher pathMatcher = pathPattern.matcher(path);
    
    if (pathMatcher.matches()) {
        for (int i = 0; i < varNames.size(); i++) {
            variables.put(varNames.get(i), pathMatcher.group(i + 1));
        }
    }
    
    return variables;
}

    private Object convertParameter(String value, Class<?> targetType) {
        if (value == null) return null;
        
        try {
            if (targetType == String.class) {
                return value;
            } else if (targetType == Integer.class || targetType == int.class) {
                return Integer.parseInt(value);
            } else if (targetType == Long.class || targetType == long.class) {
                return Long.parseLong(value);
            } else if (targetType == Double.class || targetType == double.class) {
                return Double.parseDouble(value);
            } else if (targetType == Boolean.class || targetType == boolean.class) {
                return Boolean.parseBoolean(value);
            }
        } catch (Exception e) {
            return null;
        }
        
        return null;
    }
}