package paas.manager.server;

import org.apache.commons.exec.*;
import org.apache.log4j.Logger;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.serialization.ObjectDecoder;
import paas.PaasBusinessLogics;
import paas.manager.common.ConfigurationEventData;
import paas.scripted.ScriptedBusinessLogics;
import platform.base.NullOutpuStream;
import platform.interop.remote.ApplicationTerminal;
import platform.server.ContextAwareDaemonThreadFactory;
import platform.server.lifecycle.LifecycleAdapter;
import platform.server.lifecycle.LifecycleEvent;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.Executors;

public final class AppManager {
    private final static Logger logger = Logger.getLogger(AppManager.class);

    private static final String javaExe = System.getProperty("java.home") + "/bin/java";

    private final ManagedAppLifecycleListener lifecycleListener = new ManagedAppLifecycleListener();

    private final ChannelGroup openChannels = new DefaultChannelGroup();

    private ChannelFactory channelFactory;

    private boolean started = false;

    private final int acceptPort;
    private PaasBusinessLogics paas;

    public AppManager(int acceptPort) {
        this.acceptPort = acceptPort;
    }

    public void start() {
        if (started) {
            return;
        }
        channelFactory = new NioServerSocketChannelFactory(
                Executors.newCachedThreadPool(),
                Executors.newCachedThreadPool(new ContextAwareDaemonThreadFactory(paas)));

        ServerBootstrap bootstrap = new ServerBootstrap(channelFactory);

        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
            public ChannelPipeline getPipeline() throws Exception {
                return Channels.pipeline(
                        new ObjectDecoder(),
                        new AppManagerChannelHandler(AppManager.this)
                );
            }
        });

        bootstrap.setOption("child.tcpNoDelay", true);
        bootstrap.setOption("child.keepAlive", true);

        Channel serverChannel = bootstrap.bind(new InetSocketAddress(acceptPort));

        openChannels.add(serverChannel);

        started = true;
    }

    public void stop() {
        if (!started) {
            return;
        }

        ChannelGroupFuture future = openChannels.close();
        future.awaitUninterruptibly();
        channelFactory.releaseExternalResources();
        started = false;
    }

    public void addOpenedChannel(Channel channel) {
        openChannels.add(channel);
    }

    public void lifecycleEvent(LifecycleEvent event) {
        lifecycleListener.lifecycleEvent(event);
    }

    public void setLogics(PaasBusinessLogics logics) {
        paas = logics;
    }

    public String getStatus(int port) {
        try {
            ApplicationTerminal remoteManager = (ApplicationTerminal) Naming.lookup("rmi://localhost:" + port + "/AppTerminal");
            return "started";
        } catch (Exception e) {
            return isPortAvailable(port) ? "stopped" : "busyPort";
        }
    }

    public boolean isPortAvailable(int port) {
        Socket socket = null;
        try {
            socket = new Socket("localhost", port);
        } catch (Exception e) {
            // Getting exception means the port is not used by other applications
            return true;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ioe) {
                    // Do nothing
                }
            }
        }

        return false;
    }

    public void stopApplication(Integer port) throws RemoteException, MalformedURLException, NotBoundException {
        ApplicationTerminal remoteManager = (ApplicationTerminal) Naming.lookup("rmi://localhost:" + port + "/AppTerminal");
        remoteManager.stop();
    }

    public void executeScriptedBL(int confId, int port, String dbName, List<String> moduleNames, List<String> scriptFilePaths, long processId) throws IOException, InterruptedException {
        assert moduleNames.size() == scriptFilePaths.size();

        CommandLine commandLine = new CommandLine(javaExe);
        commandLine.addArgument("-Dlsf.settings.path=conf/scripted/settings.xml");
        commandLine.addArgument("-Dpaas.manager.conf.id=" + confId);
        commandLine.addArgument("-Dpaas.manager.host=localhost");
        commandLine.addArgument("-Dpaas.manager.port=" + acceptPort);
        commandLine.addArgument("-Dpaas.manager.process.id=" + processId);
        commandLine.addArgument("-Dpaas.scripted.port=" + port);
        commandLine.addArgument("-Dpaas.scripted.db.name=" + dbName);
        commandLine.addArgument("-Dpaas.scripted.modules.names=" + toParameters(moduleNames), false);
        commandLine.addArgument("-Dpaas.scripted.modules.paths=" + toParameters(scriptFilePaths), false);
        String rmiServerHostname = System.getProperty("java.rmi.server.hostname");
        if (rmiServerHostname != null) {
            commandLine.addArgument("-Djava.rmi.server.hostname=" + rmiServerHostname);
        }

        commandLine.addArgument("-cp");
        commandLine.addArgument(System.getProperty("java.class.path"));
        commandLine.addArgument(ScriptedBusinessLogics.class.getName());
        commandLine.addArguments("", true);

        Executor executor = new DefaultExecutor();
        executor.setStreamHandler(new PumpStreamHandler(new NullOutpuStream(), new NullOutpuStream()));
//        executor.setStreamHandler(new PumpStreamHandler());
        executor.setExitValue(1);

        executor.execute(commandLine, new ManagedLogicsExecutionHandler(confId));
    }

    private class ManagedLogicsExecutionHandler extends DefaultExecuteResultHandler {
        private final int configurationId;

        public ManagedLogicsExecutionHandler(int configurationId) {
            this.configurationId = configurationId;
        }

        @Override
        public void onProcessFailed(ExecuteException e) {
            logger.error("Error executing process: " + e.getMessage(), e.getCause());
            lifecycleListener.onError(
                    new LifecycleEvent(
                            LifecycleEvent.ERROR,
                            new ConfigurationEventData(configurationId, "Error while executing the process: " + e.getMessage())
                    )
            );
        }
    }

    private String toParameters(List<String> strings) {
        StringBuilder result = new StringBuilder(strings.size() * 30);
        for (String string : strings) {
            if (result.length() != 0) {
                result.append(";");
            }
            result.append(string);
        }

        return result.toString();
    }

    public class ManagedAppLifecycleListener extends LifecycleAdapter {
        @Override
        public void lifecycleEvent(LifecycleEvent event) {
            super.lifecycleEvent(event);
            logger.debug("Lifecycle event: " + event);
        }

        private int getConfigurationId(LifecycleEvent event) {
            return ((ConfigurationEventData) event.getData()).configurationId;
        }

        private Object getEventData(LifecycleEvent event) {
            return ((ConfigurationEventData) event.getData()).data;
        }

        @Override
        protected void onStarted(LifecycleEvent event) {
            paas.changeConfigurationStatus(getConfigurationId(event), "started");
        }

        @Override
        protected void onStopped(LifecycleEvent event) {
            paas.changeConfigurationStatus(getConfigurationId(event), "stopped");
        }

        @Override
        protected void onError(LifecycleEvent event) {
            String errorMsg = (String) getEventData(event);

            paas.pushConfigurationLaunchError(getConfigurationId(event), errorMsg);
            logger.error("Error on managed app: " + errorMsg);
        }

        @Override
        protected void onOtherEvent(LifecycleEvent event) {
            logger.warn("Unrecognized lifecycle event was received from managed application.");
        }
    }
}
