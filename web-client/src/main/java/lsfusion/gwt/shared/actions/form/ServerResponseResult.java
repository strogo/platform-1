package lsfusion.gwt.shared.actions.form;

import net.customware.gwt.dispatch.shared.Result;
import lsfusion.gwt.shared.view.actions.GAction;

public class ServerResponseResult implements Result {
    public GAction[] actions;
    public boolean resumeInvocation;

    public ServerResponseResult() {}

    public ServerResponseResult(GAction[] actions, boolean resumeInvocation) {
        this.actions = actions;
        this.resumeInvocation = resumeInvocation;
    }
}