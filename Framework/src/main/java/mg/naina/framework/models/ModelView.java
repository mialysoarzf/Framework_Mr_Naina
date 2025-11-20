package mg.naina.framework.models;

/**
 * Classe représentant une vue modèle pour dispatcher vers une page JSP
 */
public class ModelView {
    private String view;

    public ModelView() {}

    public ModelView(String view) {
        this.view = view;
    }

    public String getView() {
        return view;
    }

    public void setView(String view) {
        this.view = view;
    }
}