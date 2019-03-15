package lsfusion.server.logics.controller.remote;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.lambda.CallableWithParam;
import lsfusion.interop.action.ReportPath;
import lsfusion.interop.connection.AuthenticationToken;
import lsfusion.interop.exception.RemoteMessageException;
import lsfusion.interop.logics.RemoteLogicsInterface;
import lsfusion.interop.navigator.NavigatorInfo;
import lsfusion.interop.navigator.RemoteNavigatorInterface;
import lsfusion.interop.session.ExternalRequest;
import lsfusion.interop.session.ExternalResponse;
import lsfusion.interop.session.RemoteSessionInterface;
import lsfusion.interop.session.SessionInfo;
import lsfusion.server.physics.admin.logging.ServerLoggers;
import lsfusion.server.base.controller.lifecycle.LifecycleEvent;
import lsfusion.server.base.controller.lifecycle.LifecycleListener;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.LogicsInstance;
import lsfusion.server.logics.controller.manager.RestartManager;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.navigator.controller.manager.NavigatorsManager;
import lsfusion.server.logics.navigator.controller.remote.RemoteNavigator;
import lsfusion.server.physics.admin.authentication.security.controller.manager.SecurityManager;
import lsfusion.server.physics.exec.db.DBManager;
import lsfusion.server.base.controller.remote.context.ContextAwarePendingRemoteObject;
import lsfusion.server.base.controller.remote.RMIManager;
import lsfusion.server.physics.admin.logging.RemoteLoggerAspect;
import lsfusion.server.logics.action.session.controller.remote.RemoteSession;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

public class RemoteLogics<T extends BusinessLogics> extends ContextAwarePendingRemoteObject implements RemoteLogicsInterface, InitializingBean, LifecycleListener {
    protected final static Logger logger = ServerLoggers.remoteLogger;

    protected T businessLogics;
    protected BaseLogicsModule baseLM;

    protected NavigatorsManager navigatorsManager;

    private RMIManager rmiManager; // for sessions

    protected RestartManager restartManager;
    
    protected SecurityManager securityManager;

    protected DBManager dbManager;

    public void setBusinessLogics(T businessLogics) {
        this.businessLogics = businessLogics;
    }

    public void setLogicsInstance(LogicsInstance logicsInstance) {
        setContext(logicsInstance.getContext());
    }

    public void setNavigatorsManager(NavigatorsManager navigatorsManager) {
        this.navigatorsManager = navigatorsManager;
    }

    public void setRmiManager(RMIManager rmiManager) {
        this.rmiManager = rmiManager;
    }

    public void setRestartManager(RestartManager restartManager) {
        this.restartManager = restartManager;
    }

    public void setSecurityManager(SecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    public void setDbManager(DBManager dbManager) {
        this.dbManager = dbManager;
    }

    public RemoteLogics() {
        super("logics");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(businessLogics, "businessLogics must be specified");
        Assert.notNull(navigatorsManager, "navigatorsManager must be specified");
        Assert.notNull(restartManager, "restartManager must be specified");
        Assert.notNull(securityManager, "securityManager must be specified");
        Assert.notNull(dbManager, "dbManager must be specified");
        //assert logicsInstance by checking the context
        Assert.notNull(getContext(), "logicsInstance must be specified");
    }

    @Override
    public int getOrder() {
        return BLLOADER_ORDER - 1;
    }

    @Override
    public void lifecycleEvent(LifecycleEvent event) {
        if (LifecycleEvent.INIT.equals(event.getType())) {
            this.baseLM = businessLogics.LM;
        }
    }

    public RemoteNavigatorInterface createNavigator(AuthenticationToken token, NavigatorInfo navigatorInfo) {
        RemoteNavigator.checkEnableUI(token.isAnonymous());
        checkClientVersion(navigatorInfo);
        
        return navigatorsManager.createNavigator(getStack(), token, navigatorInfo);
    }

    private void checkClientVersion(NavigatorInfo navigatorInfo) {
        String error = BaseUtils.checkClientVersion(BaseUtils.getPlatformVersion(), BaseUtils.getApiVersion(),
                                                 navigatorInfo.platformVersion, navigatorInfo.apiVersion);
        if(error != null) {
            throw new RemoteMessageException(error);
        }
    }

    @Override
    public RemoteSessionInterface createSession(AuthenticationToken token, SessionInfo sessionInfo) throws RemoteException {
        RemoteSession.checkEnableApi(token.isAnonymous());

        return createSession(rmiManager.getExportPort(), token, sessionInfo);
    }

    public RemoteSession createSession(int port, AuthenticationToken token, SessionInfo sessionInfo) throws RemoteException {
        try {
            return new RemoteSession(port, getContext().getLogicsInstance(), token, sessionInfo, getStack());
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
    
    private ExternalResponse runInNewSession(AuthenticationToken token, SessionInfo sessionInfo, CallableWithParam<RemoteSession, ExternalResponse> callable) throws RemoteException {
        // in theory it's better to cache sessions for a token in some pool (clearing them after usage) 
        RemoteSession session = createSession(-1, token, sessionInfo);
        try {
            return callable.call(session);
        } finally {
            session.localClose();
        }
    }

    @Override
    public ExternalResponse exec(AuthenticationToken token, SessionInfo sessionInfo, final String action, final ExternalRequest request) throws RemoteException {
        return runInNewSession(token, sessionInfo, new CallableWithParam<RemoteSession, ExternalResponse>() {
            public ExternalResponse call(RemoteSession session) {
                return session.exec(action, request);
            }
        });
    }

    @Override
    public ExternalResponse eval(AuthenticationToken token, SessionInfo sessionInfo, final boolean action, final Object paramScript, final ExternalRequest request) throws RemoteException {
        return runInNewSession(token, sessionInfo, new CallableWithParam<RemoteSession, ExternalResponse>() {
            public ExternalResponse call(RemoteSession session) {
                return session.eval(action, paramScript, request);
            }
        });
    }

    public void ping() throws RemoteException {
        //for filterIncl-alive
    }

    public void sendPingInfo(String computerName, Map<Long, List<Long>> pingInfoMap) {
        Map<Long, List<Long>> pingInfoEntry = RemoteLoggerAspect.pingInfoMap.get(computerName);
        pingInfoEntry = pingInfoEntry != null ? pingInfoEntry : MapFact.<Long, List<Long>>getGlobalConcurrentHashMap();
        pingInfoEntry.putAll(pingInfoMap);
        RemoteLoggerAspect.pingInfoMap.put(computerName, pingInfoEntry);
    }

    @Override
    public AuthenticationToken authenticateUser(String userName, String password) throws RemoteException {
        return securityManager.authenticateUser(userName, password, getStack());
    }

    @Override
    public long generateID() throws RemoteException {
        return dbManager.generateID();
    }

    @Override
    protected boolean isEnabledUnreferenced() { // иначе когда отключаются все клиенты логика закрывается
        return false;
    }

    @Override
    protected boolean isUnreferencedSyncedClient() { // если ушли все ссылки считаем синхронизированным, так как клиент уже ни к чему обращаться не может
        return true;
    }

    @Override
    public List<ReportPath> saveAndGetCustomReportPathList(String formSID, boolean recreate) throws RemoteException {
        try {
            return FormInstance.saveAndGetCustomReportPathList(businessLogics.findForm(formSID), recreate);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public Object getProfiledObject() {
        return "l";
    }
}

