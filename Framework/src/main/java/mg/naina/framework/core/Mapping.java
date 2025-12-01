package mg.naina.framework.core;

import java.lang.reflect.Method;

/**
 * Classe représentant le mapping entre une URL et une méthode de contrôleur
 */
public class Mapping {
    private String className;
    private String methodName;
    private Method method;
    private Object controllerInstance;

    public Mapping() {}

    public Mapping(String className, String methodName, Method method, Object controllerInstance) {
        this.className = className;
        this.methodName = methodName;
        this.method = method;
        this.controllerInstance = controllerInstance;
    }

    // Getters et Setters
    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Object getControllerInstance() {
        return controllerInstance;
    }

    public void setControllerInstance(Object controllerInstance) {
        this.controllerInstance = controllerInstance;
    }

    /**
     * Exécute la méthode mappée
     */
    public Object execute() throws Exception {
        return method.invoke(controllerInstance);
    }
}