package lsfusion.gwt.server.form;

import lsfusion.gwt.client.action.GAction;
import lsfusion.gwt.client.action.GThrowExceptionAction;
import lsfusion.gwt.client.controller.remote.action.form.FormAction;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.convert.ClientActionToGwtConverter;
import lsfusion.http.provider.form.FormSessionObject;
import lsfusion.interop.action.ServerResponse;

public abstract class FormServerResponseActionHandler<A extends FormAction<ServerResponseResult>> extends FormActionHandler<A, ServerResponseResult> {
    private static ClientActionToGwtConverter clientActionConverter = ClientActionToGwtConverter.getInstance();

    protected FormServerResponseActionHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    protected ServerResponseResult getServerResponseResult(FormSessionObject form, ServerResponse serverResponse) {
        return getServerResponseResult(form, serverResponse, servlet);
    }

    public static ServerResponseResult getServerResponseResult(FormSessionObject form, ServerResponse serverResponse, MainDispatchServlet servlet) {
        GAction[] resultActions;
        if (serverResponse.actions == null) {
            resultActions = null;
        } else {
            resultActions = new GAction[serverResponse.actions.length];
            for (int i = 0; i < serverResponse.actions.length; i++) {
                try {
                    resultActions[i] = clientActionConverter.convertAction(serverResponse.actions[i], form, servlet);
                } catch (Exception e) {
                    resultActions[i] = new GThrowExceptionAction(new IllegalStateException("Can't convert server action: " + e.getMessage(), e));
                }
            }
        }

        return new ServerResponseResult(resultActions, serverResponse.requestIndex, serverResponse.resumeInvocation);
    }

}
