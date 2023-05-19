package com.oracle.cloud.baremetal.jenkins;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import com.oracle.bmc.core.model.Instance;
import com.oracle.cloud.baremetal.jenkins.client.BaremetalCloudClient;
import com.oracle.cloud.baremetal.jenkins.ssh.SshComputerLauncher;

import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.model.Descriptor.FormException;
import hudson.slaves.AbstractCloudComputer;
import hudson.slaves.AbstractCloudSlave;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.NodeProperty;
import hudson.slaves.OfflineCause;
import hudson.slaves.RetentionStrategy;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;

public class BaremetalCloudAgent extends AbstractCloudSlave{

	/**
     * The default user on created instances. See
     * <a href="https://docs.us-phoenix-1.oraclecloud.com/Content/GSG/Tasks/testingconnection.htm">Connecting to Your Linux Instance from a Unix-style System</a>.
     */
    private static final Logger LOGGER = Logger.getLogger(BaremetalCloud.class.getName());
    private static final long serialVersionUID = 1;

    private static RetentionStrategy<? extends Computer> createRetentionStrategy(String idleTerminationMinutes) {
        int idleMinutes = idleTerminationMinutes == null || idleTerminationMinutes.trim().isEmpty() ? 0 : Integer.parseInt(idleTerminationMinutes);

        if (idleMinutes == 0) {
            return new RetentionStrategy.Always();
        }
        return new BaremetalCloudRetentionStrategy(idleMinutes);
    }

    public final String cloudName;
    private final String instanceId;
    public final String jenkinsAgentUser;
    public final String customJavaPath;
    public final String customJVMOpts;
    public final String initScript;
    public final int templateId;
    public boolean verificationStrategy;
    private String hostip="";

    public BaremetalCloudAgent(final String name,
            final BaremetalCloudAgentTemplate template,
            final String cloudName,
            final String instanceId,
            final String host) throws IOException, FormException{
    	this(
    			name,
    			template.getDescription(),
                template.getRemoteFS(),
                template.getSshCredentialsId(),
                template.getAssignPublicIP(),
                template.getUsePublicIP(),
                template.getNumExecutors(),
                template.getMode(),
                template.getLabelString(),
                template.getIdleTerminationMinutes(),
                Collections.<NodeProperty<?>> emptyList(),
                cloudName,
                template.getSshConnectTimeoutMillis(),
                template.getVerificationStrategy(),
                instanceId,
                template.getJenkinsAgentUser(),
                template.getCustomJavaPath(),
                template.getCustomJVMOpts(),
                template.getInitScriptEnvVarsVersion(),
                template.getInitScriptTimeoutSeconds(),
                host,
                template.getTemplateId());
    }

    @DataBoundConstructor
    public BaremetalCloudAgent(final String name,
                               final String description,
                               final String remoteFS,
                               final String sshCredentialsId,
                               final Boolean assignPublicIP,
                               final Boolean usePrivateIP,
                               final int numExecutors,
                               final Mode mode,
                               final String labelString,
                               final String idleTerminationMinutes,
                               final List<? extends NodeProperty<?>> nodeProperties,
                               final String cloudName,
                               final int sshConnectTimeoutMillis,
                               boolean verificationStrategy,
                               final String instanceId,
                               final String jenkinsAgentUser,
                               final String customJavaPath,
                               final String customJVMOpts,
                               final String initScript,
                               final int initScriptTimeoutSeconds,
                               final String host,
                               final int templateId) throws IOException, FormException{
        super(name,
                description,
                remoteFS,
                numExecutors,
                mode,
                labelString,
                new SshComputerLauncher(
                        host,
                        sshConnectTimeoutMillis,
                        jenkinsAgentUser,
                        customJavaPath,
                        customJVMOpts,
                        initScript,
                        initScriptTimeoutSeconds,
                        sshCredentialsId,
                        verificationStrategy),
                createRetentionStrategy(idleTerminationMinutes),
                nodeProperties);
        this.cloudName = cloudName;
        this.instanceId = instanceId;
        this.jenkinsAgentUser = jenkinsAgentUser;
        this.initScript = initScript;
        this.templateId = templateId;
        this.customJavaPath = customJavaPath;
        this.customJVMOpts = customJVMOpts;
        this.verificationStrategy = verificationStrategy;
        this.hostip = host;
    }

    private BaremetalCloudAgent(final String name,
                                final String description,
                                final String remoteFS,
                                final int numExecutors,
                                final Mode mode,
                                final String labelString,
                                final List<? extends NodeProperty<?>> nodeProperties,
                                final String cloudName,
                                boolean verificationStrategy,
                                final String instanceId,
                                final String jenkinsAgentUser,
                                final String customJavaPath,
                                final String customJVMOpts,
                                final String initScript,
                                final ComputerLauncher computerLauncher,
                                final RetentionStrategy retentionStrategy,
                                final int templateId) throws IOException,
            FormException {
        super(name, description, remoteFS, numExecutors, mode, labelString, computerLauncher, retentionStrategy,
                nodeProperties);
        this.cloudName = cloudName;
        this.instanceId = instanceId;
        this.jenkinsAgentUser = jenkinsAgentUser;
        this.customJavaPath = customJavaPath;
        this.customJVMOpts = customJVMOpts;
        this.verificationStrategy = verificationStrategy;
        this.initScript = initScript;
        this.templateId = templateId;
    }

    public String getJenkinsAgentUser() {
        return jenkinsAgentUser;
    }

    public String getInstanceId() {
        return instanceId;
    }

    @Override
    public AbstractCloudComputer<BaremetalCloudAgent> createComputer() {
        return new BaremetalCloudComputer(this);
    }

    public BaremetalCloud getCloud() {
        return (BaremetalCloud) JenkinsUtil.getJenkinsInstance().getCloud(cloudName);
    }

    void sleep(long millis) throws InterruptedException {
        Thread.sleep(millis);
    }

    @Override
    protected void _terminate(TaskListener listener) throws IOException, InterruptedException {
        Computer computer = getComputer();
        if (computer != null) {
            computer.disconnect(null);
            computer.setTemporarilyOffline(true,
                    OfflineCause.create(Messages._BaremetalCloud_termination_offlineCause()));
        }

        BaremetalCloud cloud = getCloud();
        if (cloud == null) {
            LOGGER.log(Level.SEVERE, "Unable to stop or terminate {0} because the Oracle Compute Cloud {1} does not exist",
                    new Object[] { instanceId, BaremetalCloud.nameToCloudName(cloudName) });
            return;
        }

        boolean stopOnIdle = false;
        Optional<? extends BaremetalCloudAgentTemplate> temp = cloud.getTemplates().stream().parallel()
                .filter(t->t.getTemplateId() == templateId)
                .findFirst();
        if (temp.isPresent()){
            stopOnIdle = temp.get().getStopOnIdle();
        }
        if (!stopOnIdle) {
            LOGGER.info("Terminating instance " + instanceId);
            cloud.recycleCloudResources(instanceId);
            File f1=new File("./known_hosts");
            if(f1.exists()) {
                String[] words = null;
                Reader fr = new InputStreamReader(new FileInputStream(f1), StandardCharsets.UTF_8);
                BufferedReader br = new BufferedReader(fr);
                File tempFile = new File("./known_host_temp");
                Writer writer = new OutputStreamWriter(new FileOutputStream(tempFile), StandardCharsets.UTF_8);
                String line;
                String input = hostip;
                while ((line = br.readLine()) != null) {
                    words = line.split(" ");
                    if (words[0].equals(input)) {
                        LOGGER.log(Level.FINE, "IP Address entry to be removed from host file: " + words[0]);
                        continue;
                    }
                    writer.write(line + System.getProperty("line.separator"));
                }

                boolean success = tempFile.renameTo(f1);
                if (!success) {
                    LOGGER.log(Level.INFO, "Cannot rename the file, hence entry not updated.");
                }
                br.close();
                writer.close();
            } else {
                LOGGER.log(Level.INFO, "No knownHost file.");
            }
        } else {
            LOGGER.info("Stopping instance " + instanceId);
            cloud.stopCloudResources(instanceId);
        }
    }

    protected boolean isAlive() throws IOException, InterruptedException{
        BaremetalCloud cloud = getCloud();
        if (cloud == null) {
            throw new IllegalStateException("the Oracle Cloud Infrastructure Compute " + cloudName + " does not exist");
        }

        BaremetalCloudClient client = getCloud().getClient();
        try{
            Instance.LifecycleState currentState = client.getInstanceState(instanceId);
            if(currentState.equals(Instance.LifecycleState.Running) ||
                    currentState.equals(Instance.LifecycleState.Provisioning) ||
                    currentState.equals(Instance.LifecycleState.Starting)){
                return true;
            }
        } catch(Exception e) {
            throw new IOException(e);
        }
        return false;
    }

    @Override
    public Node reconfigure(final StaplerRequest req, JSONObject form) {
        if (form == null) {
            return null;
        }

        String newName = name;
        try {
            newName = form.getString("name");
        } catch (JSONException e) {
            // Pass
        }

        int newNumExecutors = getNumExecutors();
        try {
            newNumExecutors = form.getInt("numExecutors");
        } catch (JSONException e) {
            // Pass
        }

        try {
            return new BaremetalCloudAgent(
                    newName,
                    getNodeDescription(),
                    getRemoteFS(),
                    newNumExecutors,
                    getMode(),
                    getLabelString(),
                    getNodeProperties(),
                    cloudName,
                    verificationStrategy,
                    instanceId,
                    jenkinsAgentUser,
                    customJavaPath,
                    customJVMOpts,
                    initScript,
                    getLauncher(),
                    getRetentionStrategy(),
                    templateId);
        } catch (FormException | IOException e) {
            LOGGER.warning("Failed to reconfigure BareMetalAgent: " + name);
        }

        return this;
    }

    @Extension
    public static class BaremetalAgentDescriptor extends SlaveDescriptor {
        @Override
        public String getDisplayName() {
            return ""; // TODO
        }

        @Override
        public boolean isInstantiable() {
            return false;
        }
    }

}
