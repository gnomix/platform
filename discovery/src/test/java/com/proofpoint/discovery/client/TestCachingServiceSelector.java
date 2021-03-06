package com.proofpoint.discovery.client;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.proofpoint.discovery.client.testing.InMemoryDiscoveryClient;
import com.proofpoint.node.NodeInfo;
import com.proofpoint.testing.Assertions;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class TestCachingServiceSelector
{
    private static final ServiceDescriptor APPLE_1_SERVICE = new ServiceDescriptor(UUID.randomUUID(), "node-A", "apple", "pool", "location", ServiceState.RUNNING, ImmutableMap.of("a", "apple"));
    private static final ServiceDescriptor APPLE_2_SERVICE = new ServiceDescriptor(UUID.randomUUID(), "node-B", "apple", "pool", "location", ServiceState.RUNNING, ImmutableMap.of("a", "apple"));
    private static final ServiceDescriptor DIFFERENT_TYPE = new ServiceDescriptor(UUID.randomUUID(), "node-A", "banana", "pool", "location", ServiceState.RUNNING, ImmutableMap.of("b", "banana"));
    private static final ServiceDescriptor DIFFERENT_POOL = new ServiceDescriptor(UUID.randomUUID(), "node-B", "apple", "fool", "location", ServiceState.RUNNING, ImmutableMap.of("a", "apple"));

    private ScheduledExecutorService executor;
    private NodeInfo nodeInfo;

    @BeforeMethod
    protected void setUp()
            throws Exception
    {
        executor = new ScheduledThreadPoolExecutor(10,
                new ThreadFactoryBuilder().setNameFormat("Discovery-%s").setDaemon(true).build());
        nodeInfo = new NodeInfo("environment");
    }

    @AfterMethod
    public void tearDown()
            throws Exception
    {
        executor.shutdownNow();
    }

    @Test
    public void testBasics()
    {
        CachingServiceSelector serviceSelector = new CachingServiceSelector("type",
                new ServiceSelectorConfig().setPool("pool"),
                new InMemoryDiscoveryClient(nodeInfo),
                executor);

        Assert.assertEquals(serviceSelector.getType(), "type");
        Assert.assertEquals(serviceSelector.getPool(), "pool");
    }

    @Test
    public void testNotStartedEmpty()
    {
        CachingServiceSelector serviceSelector = new CachingServiceSelector("type",
                new ServiceSelectorConfig().setPool("pool"),
                new InMemoryDiscoveryClient(nodeInfo),
                executor);

        Assert.assertEquals(serviceSelector.selectAllServices(), ImmutableList.of());
    }

    @Test
    public void testStartedEmpty()
            throws Exception
    {
        CachingServiceSelector serviceSelector = new CachingServiceSelector("type",
                new ServiceSelectorConfig().setPool("pool"),
                new InMemoryDiscoveryClient(nodeInfo),
                executor);

        serviceSelector.start();

        Assert.assertEquals(serviceSelector.selectAllServices(), ImmutableList.of());
    }

    @Test
    public void testNotStartedWithServices()
    {
        InMemoryDiscoveryClient discoveryClient = new InMemoryDiscoveryClient(nodeInfo);
        discoveryClient.addDiscoveredService(APPLE_1_SERVICE);
        discoveryClient.addDiscoveredService(APPLE_2_SERVICE);
        discoveryClient.addDiscoveredService(DIFFERENT_TYPE);
        discoveryClient.addDiscoveredService(DIFFERENT_POOL);

        CachingServiceSelector serviceSelector = new CachingServiceSelector("apple",
                new ServiceSelectorConfig().setPool("pool"),
                discoveryClient,
                executor);

        Assert.assertEquals(serviceSelector.selectAllServices(), ImmutableList.of());
    }

    @Test
    public void testStartedWithServices()
            throws Exception
    {
        InMemoryDiscoveryClient discoveryClient = new InMemoryDiscoveryClient(nodeInfo);
        discoveryClient.addDiscoveredService(APPLE_1_SERVICE);
        discoveryClient.addDiscoveredService(APPLE_2_SERVICE);
        discoveryClient.addDiscoveredService(DIFFERENT_TYPE);
        discoveryClient.addDiscoveredService(DIFFERENT_POOL);

        CachingServiceSelector serviceSelector = new CachingServiceSelector("apple",
                new ServiceSelectorConfig().setPool("pool"),
                discoveryClient,
                executor);

        serviceSelector.start();

        Thread.sleep(100);

        Assertions.assertEqualsIgnoreOrder(serviceSelector.selectAllServices(), ImmutableList.of(APPLE_1_SERVICE, APPLE_2_SERVICE));
    }
}
