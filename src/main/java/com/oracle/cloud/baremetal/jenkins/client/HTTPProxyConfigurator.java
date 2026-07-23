package com.oracle.cloud.baremetal.jenkins.client;

import java.net.Proxy;

import com.oracle.bmc.http.DefaultConfigurator;
import com.oracle.bmc.http.client.HttpClientBuilder;
import com.oracle.bmc.http.client.StandardClientProperties;

import hudson.ProxyConfiguration;
import jenkins.model.Jenkins;

public class HTTPProxyConfigurator extends DefaultConfigurator {
    @Override
    public void customizeClient(HttpClientBuilder builder) {
        Jenkins jenkins = Jenkins.getInstanceOrNull();
        if (jenkins == null) {
            return;
        }

        ProxyConfiguration config = jenkins.getProxy();
        if (config == null || config.getName() == null || config.getName().isEmpty()) {
            return;
        }

        Proxy proxy = config.createProxy();
        if (proxy != null && proxy.type() != Proxy.Type.DIRECT) {
            builder.property(StandardClientProperties.PROXY,
                    new com.oracle.bmc.http.client.ProxyConfiguration(proxy));
        }
    }
}
