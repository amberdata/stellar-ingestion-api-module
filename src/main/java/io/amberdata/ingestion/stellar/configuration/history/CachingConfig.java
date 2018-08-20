package io.amberdata.ingestion.stellar.configuration.history;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CachingConfig {
    @Bean("stateHistoryCacheManager")
    public CacheManager cacheManager () {
        return new ConcurrentMapCacheManager("stateHistory");
    }
}
