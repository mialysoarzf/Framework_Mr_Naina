package mg.naina.framework.core;

import java.lang.reflect.Method;

public class Mapping {
    private String className;
    private String methodName;
    private String pattern;
    private String httpMethod;
    private Method method;
    private Object controllerInstance;

    public Mapping(String className, String methodName, String pattern, String httpMethod, Method method, Object controllerInstance) {
        this.className = className;
        this.methodName = methodName;
        this.pattern = pattern;
        this.httpMethod = httpMethod;
        this.method = method;
        this.controllerInstance = controllerInstance;
    }

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

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
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
}