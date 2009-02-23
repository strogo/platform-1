package platform.server.logics.auth;

public class ClassSecurityPolicy {

    ViewClassSecurityPolicy view = new ViewClassSecurityPolicy();
    public EditClassSecurityPolicy edit = new EditClassSecurityPolicy();

    public void override(ClassSecurityPolicy cls) {
        view.override(cls.view);
        edit.override(cls.edit);
    }
}
