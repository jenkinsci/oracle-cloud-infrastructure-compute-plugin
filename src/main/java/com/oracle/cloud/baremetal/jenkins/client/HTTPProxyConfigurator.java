package com.oracle.cloud.baremetal.jenkins.client;

import java.net.Proxy;

import com.oracle.bmc.http.ClientConfigurator;
import com.oracle.bmc.http.client.HttpClientBuilder;
import com.oracle.bmc.http.client.StandardClientProperties;

import jenkins.model.Jenkins;

public class HTTPProxyConfigurator implements ClientConfigurator {

    @Override
    public void customizeClient(HttpClientBuilder builder) {
        Jenkins jenkins = Jenkins.getInstanceOrNull();
        if (jenkins == null) {
            return;
        }
        hudson.ProxyConfiguration jenkinsProxy = jenkins.getProxy();
        if (jenkinsProxy == null || jenkinsProxy.getName() == null || jenkinsProxy.getName().isEmpty()) {
            return;
        }
        Proxy proxy = jenkinsProxy.createProxy();
        if (proxy != null && proxy.type() != Proxy.Type.DIRECT) {
            builder.property(StandardClientProperties.PROXY,
                    new com.oracle.bmc.http.client.ProxyConfiguration(proxy));
        }
    }
}
