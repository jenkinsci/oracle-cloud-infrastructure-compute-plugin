package com.oracle.cloud.baremetal.jenkins.client;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider;
import com.oracle.bmc.core.ComputeClient;
import com.oracle.bmc.core.VirtualNetworkClient;
import com.oracle.bmc.identity.IdentityClient;
import com.oracle.bmc.identity.requests.GetTenancyRequest;
import com.oracle.bmc.identity.requests.GetUserRequest;
import com.oracle.bmc.identity.responses.GetTenancyResponse;
import com.oracle.bmc.identity.responses.GetUserResponse;
import com.oracle.bmc.model.BmcException;

public class SDKBaremetalCloudClientTest {

    @Mock
    private BasicAuthenticationDetailsProvider provider;

    @Mock
    private SimpleAuthenticationDetailsProvider simpleProvider;

    @Mock
    private IdentityClient identityClient;

    private SDKBaremetalCloudClient client;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        client = new SDKBaremetalCloudClient(provider, "us-phoenix-1", 10, "tenancyId", "userId");
    }

    @Test
    public void testAuthenticateWithUser() throws Exception {
        // Arrange
        GetUserResponse userResponse = mock(GetUserResponse.class);
        when(identityClient.getUser(any(GetUserRequest.class))).thenReturn(userResponse);

        // Mock the client creation
        SDKBaremetalCloudClient spyClient = spy(client);
        doReturn(identityClient).when(spyClient).getIdentityClient();

        // Act
        spyClient.authenticate();

        // Assert
        verify(identityClient).getUser(any(GetUserRequest.class));
    }

    @Test
    public void testAuthenticateWithTenancy() throws Exception {
        // Arrange
        client = new SDKBaremetalCloudClient(provider, "us-phoenix-1", 10, "tenancyId");
        GetTenancyResponse tenancyResponse = mock(GetTenancyResponse.class);
        when(identityClient.getTenancy(any(GetTenancyRequest.class))).thenReturn(tenancyResponse);

        // Mock the client creation
        SDKBaremetalCloudClient spyClient = spy(client);
        doReturn(identityClient).when(spyClient).getIdentityClient();

        // Act
        spyClient.authenticate();

        // Assert
        verify(identityClient).getTenancy(any(GetTenancyRequest.class));
    }

    @Test
    public void testGetTenant() throws Exception {
        // Arrange
        GetTenancyResponse tenancyResponse = mock(GetTenancyResponse.class);
        when(identityClient.getTenancy(any(GetTenancyRequest.class))).thenReturn(tenancyResponse);

        // Mock the client creation
        SDKBaremetalCloudClient spyClient = spy(client);
        doReturn(identityClient).when(spyClient).getIdentityClient();

        // Act
        spyClient.getTenant();

        // Assert
        verify(identityClient).getTenancy(any(GetTenancyRequest.class));
    }

    @Test
    public void testConstructorWithSimpleAuthenticationDetailsProvider() throws Exception {
        // Arrange
        when(simpleProvider.getTenantId()).thenReturn("simpleTenantId");
        when(simpleProvider.getUserId()).thenReturn("simpleUserId");

        // Act
        SDKBaremetalCloudClient simpleClient = new SDKBaremetalCloudClient(simpleProvider, "us-phoenix-1", 10);

        // Assert
        GetUserResponse userResponse = mock(GetUserResponse.class);
        when(identityClient.getUser(any(GetUserRequest.class))).thenReturn(userResponse);

        SDKBaremetalCloudClient spyClient = spy(simpleClient);
        doReturn(identityClient).when(spyClient).getIdentityClient();

        spyClient.authenticate();

        verify(identityClient).getUser(any(GetUserRequest.class));
    }
}
