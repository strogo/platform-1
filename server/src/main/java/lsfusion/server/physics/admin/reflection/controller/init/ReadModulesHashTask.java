package lsfusion.server.physics.admin.reflection.controller.init;

import com.google.common.base.Throwables;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.action.session.DataSession;
import org.apache.log4j.Logger;

import java.sql.SQLException;

import static lsfusion.server.base.controller.thread.ThreadLocalContext.createSession;

public class ReadModulesHashTask extends ReflectionTask {

    @Override
    public String getCaption() {
        return "Reading modules hash";
    }

    @Override
    public void run(Logger logger) {
        try(DataSession session = createSession()) {
            getReflectionManager().readModulesHash(session);
        } catch (SQLException | SQLHandledException | ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}