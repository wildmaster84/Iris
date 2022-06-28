package com.volmit.iris.platform;

import java.util.stream.Stream;

public interface IrisPlatform {
    String getPlatformName();

    Stream<PlatformBlock> getBlocks();

    Stream<PlatformBiome> getBiomes();

    boolean isWorldLoaded(String name);

    PlatformWorld getWorld(String name);

    PlatformBlock parseBlock(String raw);

    PlatformNamespaceKey key(String namespace, String key);

    default PlatformNamespaceKey key(String nsk)
    {
        return key("minecraft", nsk);
    }
}