package mg.naina.framework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Sprint 10: Annotation pour marquer les paramètres qui reçoivent des fichiers uploadés
 * Permet de gérer l'upload de fichiers (simple ou multiple) dans les contrôleurs
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface UploadedFile {
    /**
     * Nom du champ de fichier dans le formulaire
     */
    String value();
}
