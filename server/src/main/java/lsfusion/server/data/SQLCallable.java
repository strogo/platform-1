package lsfusion.server.data;

import lsfusion.base.lambda.E2Callable;

import java.sql.SQLException;

public interface SQLCallable<R> extends E2Callable<R, SQLException, SQLHandledException> {
}
