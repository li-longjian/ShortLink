package org.llj.shortlink.project.common.config;

import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RBloomFilterConfiguration {

    /**
     * 布隆过滤器
     */
    @Bean
    public RBloomFilter<String> ShortLinkCreateCachePenetrationBloomFilter(RedissonClient redissonClient) {
        RBloomFilter<String> cachePenetrationBloomFilter = redissonClient.getBloomFilter("ShortLinkCreateCachePenetrationBloomFilter");
        cachePenetrationBloomFilter.tryInit(10000000, 0.001);
        return cachePenetrationBloomFilter;
    }
}
