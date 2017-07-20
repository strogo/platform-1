package lsfusion.server.logics.property.actions.flow;

import lsfusion.base.FunctionSet;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.SessionDataProperty;
import lsfusion.server.logics.property.actions.SystemExplicitActionProperty;
import lsfusion.server.logics.scripted.ScriptingActionProperty;

import java.sql.SQLException;

public class CancelActionProperty extends SystemExplicitActionProperty {

    private final FunctionSet<SessionDataProperty> keepSessionProps;
    public CancelActionProperty(LocalizedString caption, FunctionSet<SessionDataProperty> keepSessionProps) {
        super(caption);
        this.keepSessionProps = keepSessionProps;
    }

    @Override
    public boolean hasFlow(ChangeFlowType type) {
        if(type == ChangeFlowType.CANCEL)
            return true;
        return super.hasFlow(type);
    }

    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        context.cancel(keepSessionProps);
    }

}
