# Mr Naina Framework

## Description
Framework web MVC pour Java développé par Mr Naina.

## Fonctionnalités
- Mapping automatique des URLs vers les contrôleurs
- Annotation `@Controller` pour marquer les classes contrôleurs
- Annotation `@UrlMapping` pour mapper les méthodes aux URLs
- Gestion automatique des URLs non mappées avec affichage informatif
- Génération de JAR pour distribution

## Structure du projet
```
Framework/
├── src/main/java/mg/naina/framework/
│   ├── annotation/
│   │   ├── Controller.java
│   │   └── UrlMapping.java
│   ├── core/
│   │   └── Mapping.java
│   └── servlet/
│       └── FrontController.java
├── pom.xml
└── README.md
```

## Utilisation

### 1. Créer un contrôleur
```java
@Controller
public class HomeController {
    
    @UrlMapping("/")
    public String index() {
        return "<h1>Hello World!</h1>";
    }
    
    @UrlMapping("/about")
    public String about() {
        return "<h1>About Page</h1>";
    }
}
```

### 2. Configuration web.xml
```xml
<servlet>
    <servlet-name>FrontController</servlet-name>
    <servlet-class>mg.naina.framework.servlet.FrontController</servlet-class>
    <init-param>
        <param-name>basePackage</param-name>
        <param-value>votre.package.controllers</param-value>
    </init-param>
</servlet>

<servlet-mapping>
    <servlet-name>FrontController</servlet-name>
    <url-pattern>/*</url-pattern>
</servlet-mapping>
```

## Compilation et génération du JAR
```bash
mvn clean compile package
```

Le JAR sera généré dans le répertoire `target/`.

## Gestion des URLs non mappées
Lorsqu'une URL ne correspond à aucun contrôleur mappé, le framework affiche automatiquement :
- L'URL qui a été cliquée
- La liste des URLs disponibles dans l'application
- Les contrôleurs et méthodes correspondants

## Auteur
Mr Naina - ITU

## Version
1.0.0