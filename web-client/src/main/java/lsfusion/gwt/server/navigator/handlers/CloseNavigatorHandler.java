package lsfusion.gwt.server.navigator.handlers;

import lsfusion.gwt.server.navigator.NavigatorActionHandler;
import lsfusion.gwt.shared.result.VoidResult;
import lsfusion.gwt.server.LSFusionDispatchServlet;
import lsfusion.gwt.shared.actions.navigator.CloseNavigator;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;

import java.io.IOException;

public class  CloseNavigatorHandler extends NavigatorActionHandler<CloseNavigator, VoidResult> {
    public CloseNavigatorHandler(LSFusionDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public VoidResult executeEx(CloseNavigator action, ExecutionContext context) throws DispatchException, IOException {
        removeLogicsAndNavigatorSessionObject(action.sessionID);
        return new VoidResult();
    }
}