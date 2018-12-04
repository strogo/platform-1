package lsfusion.server.logics.reflection;

import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.*;
import lsfusion.server.logics.DBManager;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ReflectionLogicsModule;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.service.RunService;
import lsfusion.server.logics.service.ServiceDBActionProperty;
import lsfusion.server.logics.table.ImplementTable;

import java.sql.SQLException;
import java.util.Iterator;

import static lsfusion.server.context.ThreadLocalContext.localize;

public class PackTableActionProperty extends ScriptingActionProperty {

    private final ClassPropertyInterface tableInterface;

    public PackTableActionProperty(ReflectionLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        tableInterface = i.next();
    }

    @Override
    public void executeCustom(final ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        DataObject tableObject = context.getDataKeyValue(tableInterface);
        final String tableName = (String) context.getBL().reflectionLM.sidTable.read(context, tableObject);

        ServiceDBActionProperty.run(context, new RunService() {
            public void run(final SQLSession session, boolean isolatedTransaction) throws SQLException, SQLHandledException {
                DBManager.run(session, isolatedTransaction, new DBManager.RunService() {
                    @Override
                    public void run(SQLSession sql) throws SQLException {
                        ImplementTable table = context.getBL().LM.tableFactory.getImplementTablesMap().get(tableName);
                        session.packTable(table, OperationOwner.unknown, TableOwner.global);
                    }});

            }
        });
        context.delayUserInterfaction(new MessageClientAction(localize(LocalizedString.createFormatted("{logics.table.packing.completed}", localize("{logics.table.packing}"))), localize("{logics.table.packing}")));
    }
}