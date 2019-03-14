package lsfusion.server.physics.dev.integration.external.to.file;

import com.google.common.base.Throwables;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import lsfusion.server.physics.dev.integration.external.to.file.client.DeleteFileClientAction;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.UtilsLogicsModule;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.language.ScriptingActionProperty;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;

public class DeleteActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface sourceInterface;
    private final ClassPropertyInterface isClientInterface;

    public DeleteActionProperty(UtilsLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        sourceInterface = i.next();
        isClientInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            String sourcePath = (String) context.getKeyValue(sourceInterface).getValue();
            boolean isClient = context.getKeyValue(isClientInterface).getValue() != null;
            if (sourcePath != null) {
                if (isClient) {
                    String result = (String) context.requestUserInteraction(new DeleteFileClientAction(sourcePath));
                    if (result != null)
                        throw new RuntimeException(result);
                } else {
                    FileUtils.delete(sourcePath);
                }
            }
        } catch (IOException | JSchException | SftpException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}