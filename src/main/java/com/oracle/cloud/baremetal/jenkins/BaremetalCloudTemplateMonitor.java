package com.oracle.cloud.baremetal.jenkins;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import hudson.Extension;
import hudson.model.AsyncPeriodicWork;
import hudson.model.TaskListener;
import hudson.slaves.Cloud;


@Extension
public class BaremetalCloudTemplateMonitor extends AsyncPeriodicWork{
    private static final Logger LOGGER = Logger.getLogger(BaremetalCloudInstanceMonitor.class.getName());
    private static final Long recurrencePeriod = TimeUnit.MINUTES.toMillis(3);

    public BaremetalCloudTemplateMonitor() {
        super("Oracle Oracle Cloud Infrastructure Compute Templates Monitor");
        LOGGER.log(Level.FINE, "Oracle Cloud Infrastructure Compute Templates Monitor check period is {0}ms", recurrencePeriod);
    }

    @Override
    protected void execute(TaskListener taskListener) throws IOException {

        for (Cloud c : JenkinsUtil.getJenkinsInstance().clouds) {
            if (c instanceof BaremetalCloud) {
                BaremetalCloud cloud = (BaremetalCloud) c;

                for (BaremetalCloudAgentTemplate template: cloud.getTemplates()) {

                    if(template.isTemplateSleep()) {
                        long retryTimeOutMins = TimeUnit.MINUTES.toMillis(template.getRetryTimeoutMins());
                        LOGGER.log(Level.INFO,"Monitoring sleeping template " + template.getDisplayName()
                        + " provided retryTime "+ template.getRetryTimeoutMins()+" minutes.");
                        long differenceTime = System.currentTimeMillis()-template.getSleepStartTime();
                        if (differenceTime > retryTimeOutMins){
                            template.setTemplateSleep(false);
                            if(template.getDisableCause()==null) {
                                LOGGER.log(Level.INFO, "Template {0} is available for provisioning now.", template.getDisplayName());
                            } else {
                                LOGGER.log(Level.INFO, "Template {0} is disabled after encountering 20 failures.", template.getDisplayName());
                            }

                        } else {
                            if(template.getDisableCause()==null){
                                LOGGER.log(Level.INFO,"Not yet available, wait for atleast {0} minutes.",
                                    (TimeUnit.MILLISECONDS.toMinutes(retryTimeOutMins-differenceTime)+1));
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public long getRecurrencePeriod() {
        return recurrencePeriod;
    }
}
