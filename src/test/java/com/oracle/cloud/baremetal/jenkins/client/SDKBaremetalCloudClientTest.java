package com.oracle.cloud.baremetal.jenkins.client;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider;
import com.oracle.bmc.core.ComputeAsyncClient;
import com.oracle.bmc.core.ComputeClient;
import com.oracle.bmc.core.VirtualNetworkClient;
import com.oracle.bmc.core.model.Image;
import com.oracle.bmc.core.requests.ListImagesRequest;
import com.oracle.bmc.core.responses.ListImagesResponse;
import com.oracle.bmc.identity.IdentityClient;
import com.oracle.bmc.identity.requests.GetTenancyRequest;
import com.oracle.bmc.identity.requests.GetUserRequest;
import com.oracle.bmc.identity.responses.GetTenancyResponse;
import com.oracle.bmc.identity.responses.GetUserResponse;
import com.oracle.bmc.model.BmcException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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
        SDKBaremetalCloudClient.clearImageResolutionCache();
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

    @Test
    public void testResolveImageIdReturnsLegacyOcidAsIs() throws Exception {
        String legacyOcid = "ocid1.image.oc1.phx.aaaaaaaaexamplelegacy";

        String resolved = client.resolveImageId("ocid1.compartment.oc1..aaa", legacyOcid);

        assertEquals(legacyOcid, resolved);
    }

    @Test
    public void testResolveImageIdByNameReturnsNewest() throws Exception {
        Image newest = Image.builder()
                .id("ocid1.image.oc1.phx.aaaaaaaanewest")
                .displayName("my-agent-image")
                .build();
        Image older = Image.builder()
                .id("ocid1.image.oc1.phx.aaaaaaaaolder")
                .displayName("my-agent-image")
                .build();

        String resolved = resolveViaMockedListImages(Arrays.asList(newest, older), "my-agent-image");

        assertEquals("ocid1.image.oc1.phx.aaaaaaaanewest", resolved);
    }

    @Test
    public void testResolveImageIdByNameNoMatchReturnsNull() throws Exception {
        String resolved = resolveViaMockedListImages(Collections.<Image>emptyList(), "missing-image");

        assertNull(resolved);
    }

    @SuppressWarnings("unchecked")
    private String resolveViaMockedListImages(List<Image> items, String displayName) throws Exception {
        ComputeAsyncClient asyncClient = mock(ComputeAsyncClient.class);
        ListImagesResponse listImagesResponse = mock(ListImagesResponse.class);
        Future<ListImagesResponse> future = mock(Future.class);

        when(listImagesResponse.getItems()).thenReturn(items);
        when(future.get()).thenReturn(listImagesResponse);
        when(asyncClient.listImages(any(ListImagesRequest.class), any())).thenReturn(future);

        SDKBaremetalCloudClient spyClient = spy(client);
        doReturn(asyncClient).when(spyClient).getComputeAsyncClient();

        return spyClient.resolveImageId("ocid1.compartment.oc1..aaa", displayName);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testResolveImageIdByNameIsCached() throws Exception {
        Image img = Image.builder()
                .id("ocid1.image.oc1.phx.aaaaaaaacached")
                .displayName("cached-image")
                .build();
        ComputeAsyncClient asyncClient = mock(ComputeAsyncClient.class);
        ListImagesResponse listImagesResponse = mock(ListImagesResponse.class);
        Future<ListImagesResponse> future = mock(Future.class);
        when(listImagesResponse.getItems()).thenReturn(Arrays.asList(img));
        when(future.get()).thenReturn(listImagesResponse);
        when(asyncClient.listImages(any(ListImagesRequest.class), any())).thenReturn(future);

        SDKBaremetalCloudClient spyClient = spy(client);
        doReturn(asyncClient).when(spyClient).getComputeAsyncClient();

        String first = spyClient.resolveImageId("ocid1.compartment.oc1..aaa", "cached-image");
        String second = spyClient.resolveImageId("ocid1.compartment.oc1..aaa", "cached-image");

        assertEquals("ocid1.image.oc1.phx.aaaaaaaacached", first);
        assertEquals("ocid1.image.oc1.phx.aaaaaaaacached", second);
        verify(asyncClient, times(1)).listImages(any(ListImagesRequest.class), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testResolveImageIdByNameRetriesOnTooManyRequests() throws Exception {
        Image img = Image.builder()
                .id("ocid1.image.oc1.phx.aaaaaaaaretry")
                .displayName("retry-image")
                .build();
        ComputeAsyncClient asyncClient = mock(ComputeAsyncClient.class);

        Future<ListImagesResponse> throttled = mock(Future.class);
        BmcException tooMany = new BmcException(429, "TooManyRequests", "throttled", "req-id");
        when(throttled.get()).thenThrow(new java.util.concurrent.ExecutionException(tooMany));

        ListImagesResponse okResponse = mock(ListImagesResponse.class);
        when(okResponse.getItems()).thenReturn(Arrays.asList(img));
        Future<ListImagesResponse> okFuture = mock(Future.class);
        when(okFuture.get()).thenReturn(okResponse);

        when(asyncClient.listImages(any(ListImagesRequest.class), any()))
                .thenReturn(throttled)
                .thenReturn(okFuture);

        SDKBaremetalCloudClient spyClient = spy(client);
        doReturn(asyncClient).when(spyClient).getComputeAsyncClient();

        String resolved = spyClient.resolveImageId("ocid1.compartment.oc1..aaa", "retry-image");

        assertEquals("ocid1.image.oc1.phx.aaaaaaaaretry", resolved);
        verify(asyncClient, times(2)).listImages(any(ListImagesRequest.class), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testConcurrentResolvesCoalesceToSingleListImages() throws Exception {
        Image img = Image.builder()
                .id("ocid1.image.oc1.phx.aaaaaaaasingleflight")
                .displayName("burst-image")
                .build();
        ListImagesResponse listImagesResponse = mock(ListImagesResponse.class);
        when(listImagesResponse.getItems()).thenReturn(Arrays.asList(img));

        int threadCount = 10;
        CountDownLatch allStarted = new CountDownLatch(threadCount);
        ComputeAsyncClient asyncClient = mock(ComputeAsyncClient.class);
        Future<ListImagesResponse> future = mock(Future.class);
        // Block the single lookup until every thread has entered resolve, forcing them to coalesce.
        when(future.get()).thenAnswer(inv -> {
            allStarted.await(5, TimeUnit.SECONDS);
            return listImagesResponse;
        });
        when(asyncClient.listImages(any(ListImagesRequest.class), any())).thenReturn(future);

        SDKBaremetalCloudClient spyClient = spy(client);
        doReturn(asyncClient).when(spyClient).getComputeAsyncClient();

        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        List<Future<String>> results = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            results.add(pool.submit(() -> {
                allStarted.countDown();
                return spyClient.resolveImageId("ocid1.compartment.oc1..aaa", "burst-image");
            }));
        }
        for (Future<String> r : results) {
            assertEquals("ocid1.image.oc1.phx.aaaaaaaasingleflight", r.get());
        }
        pool.shutdown();

        verify(asyncClient, times(1)).listImages(any(ListImagesRequest.class), any());
    }
}
