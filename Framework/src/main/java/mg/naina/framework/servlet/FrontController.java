package mg.naina.framework.servlet;

import mg.naina.framework.annotation.Controller;
import mg.naina.framework.annotation.UrlMapping;
import mg.naina.framework.core.Mapping;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Servlet principal du framework qui gère toutes les requêtes
 */
public class FrontController extends HttpServlet {
    
    private Map<String, Mapping> urlMappings = new HashMap<>();
    private String basePackage;

    @Override
    public void init() throws ServletException {
        super.init();
        
        System.out.println("=== DEMARRAGE FRONTCONTROLLER ===");
        
        // Récupération du paramètre basePackage depuis web.xml
        basePackage = getInitParameter("basePackage");
        if (basePackage == null || basePackage.trim().isEmpty()) {
            basePackage = "mg.naina.test";
        }
        
        System.out.println("Initialisation FrontController avec basePackage: " + basePackage);
        System.out.println("ClassLoader: " + this.getClass().getClassLoader());
        
        // Test de la disponibilité de Reflections
        try {
            Class.forName("org.reflections.Reflections");
            System.out.println("Reflections library disponible");
        } catch (ClassNotFoundException e) {
            System.err.println("ERREUR CRITIQUE: Reflections library non trouvée!");
            throw new ServletException("Reflections library manquante", e);
        }
        
        scanControllers();
        System.out.println("Nombre de mappings trouvés: " + urlMappings.size());
        
        // Affichage de tous les mappings
        System.out.println("=== MAPPINGS ENREGISTRES ===");
        for (Map.Entry<String, Mapping> entry : urlMappings.entrySet()) {
            System.out.println(entry.getKey() + " -> " + entry.getValue().getClassName() + "." + entry.getValue().getMethodName());
        }
        System.out.println("=== FIN DEMARRAGE ===");
    }

    /**
     * Scanne tous les contrôleurs et crée les mappings URL
     */
    private void scanControllers() {
        try {
            System.out.println("Démarrage du scan des contrôleurs dans le package: " + basePackage);
            
            // Configuration explicite de Reflections
            ConfigurationBuilder config = new ConfigurationBuilder()
                .forPackages(basePackage);
                
            Reflections reflections = new Reflections(config);
            Set<Class<?>> controllers = reflections.getTypesAnnotatedWith(Controller.class);
            
            System.out.println("Classes trouvées avec @Controller: " + controllers.size());
            
            if (controllers.isEmpty()) {
                System.err.println("ATTENTION: Aucun contrôleur trouvé dans le package " + basePackage);
                
                // Test direct de chargement de la classe HomeController
                try {
                    Class<?> homeController = Class.forName("mg.naina.test.controller.HomeController");
                    System.out.println("HomeController trouvé directement: " + homeController.getName());
                    
                    if (homeController.isAnnotationPresent(Controller.class)) {
                        System.out.println("HomeController a l'annotation @Controller");
                        controllers.add(homeController);
                    }
                } catch (ClassNotFoundException e) {
                    System.err.println("HomeController non trouvé: " + e.getMessage());
                }
            }

            for (Class<?> controllerClass : controllers) {
                System.out.println("Traitement du contrôleur: " + controllerClass.getName());
                try {
                    Object controllerInstance = controllerClass.getDeclaredConstructor().newInstance();
                    
                    for (Method method : controllerClass.getDeclaredMethods()) {
                        if (method.isAnnotationPresent(UrlMapping.class)) {
                            UrlMapping urlMapping = method.getAnnotation(UrlMapping.class);
                            String url = urlMapping.value();
                            
                            Mapping mapping = new Mapping(
                                controllerClass.getName(),
                                method.getName(),
                                method,
                                controllerInstance
                            );
                            
                            urlMappings.put(url, mapping);
                            System.out.println("Mapping ajouté: " + url + " -> " + controllerClass.getName() + "." + method.getName());
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Erreur lors de l'instantiation du contrôleur: " + controllerClass.getName());
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du scan des contrôleurs:");
            e.printStackTrace();
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Traite la requête en trouvant le mapping correspondant
     */
    private void processRequest(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String requestURI = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = requestURI.substring(contextPath.length());
        
        // Gestion du path vide
        if (path.isEmpty()) {
            path = "/";
        }
        
        System.out.println("Requête reçue - URI: " + requestURI + ", Context: " + contextPath + ", Path: " + path);

        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();

        Mapping mapping = urlMappings.get(path);
        
        if (mapping != null) {
            try {
                System.out.println("Exécution du mapping pour: " + path);
                Object result = mapping.execute();
                
                if (result != null) {
                    out.println(result.toString());
                } else {
                    out.println("<h1>Méthode exécutée avec succès</h1>");
                    out.println("<p>URL: " + path + "</p>");
                    out.println("<p>Contrôleur: " + mapping.getClassName() + "</p>");
                    out.println("<p>Méthode: " + mapping.getMethodName() + "</p>");
                }
            } catch (Exception e) {
                System.err.println("Erreur lors de l'exécution du mapping: " + e.getMessage());
                e.printStackTrace();
                out.println("<h1>Erreur lors de l'exécution</h1>");
                out.println("<p>URL: " + path + "</p>");
                out.println("<p>Erreur: " + e.getMessage() + "</p>");
            }
        } else {
            System.out.println("Aucun mapping trouvé pour: " + path);
            // URL ne correspond à aucun contrôleur
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head><title>Framework - Debug</title></head>");
            out.println("<body>");
            out.println("<h1>Framework - Debug Info</h1>");
            out.println("<p><strong>URL demandée:</strong> " + path + "</p>");
            out.println("<p><strong>Package de base:</strong> " + basePackage + "</p>");
            out.println("<p><strong>Nombre de mappings:</strong> " + urlMappings.size() + "</p>");
            out.println("<hr>");
            
            if (urlMappings.isEmpty()) {
                out.println("<h3 style='color: red;'>PROBLÈME: Aucun mapping trouvé!</h3>");
                out.println("<p>Le framework n'a trouvé aucun contrôleur. Vérifiez:</p>");
                out.println("<ul>");
                out.println("<li>Que HomeController.class est dans WEB-INF/classes/</li>");
                out.println("<li>Que la bibliothèque Reflections est dans WEB-INF/lib/</li>");
                out.println("<li>Les logs de démarrage de Tomcat</li>");
                out.println("</ul>");
            } else {
                out.println("<h3>URLs disponibles:</h3>");
                out.println("<ul>");
                for (String url : urlMappings.keySet()) {
                    Mapping m = urlMappings.get(url);
                    out.println("<li><a href='" + contextPath + url + "'>" + url + "</a> → " + 
                               m.getClassName() + "." + m.getMethodName() + "()</li>");
                }
                out.println("</ul>");
            }
            
            out.println("</body>");
            out.println("</html>");
        }
    }

    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }
}