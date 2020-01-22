package org.easy.framework.cache;

import com.alibaba.fastjson.support.spring.FastJsonRedisSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleCacheResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.temporal.TemporalUnit;
import java.util.Arrays;

/**
 * @author: Zhangtong
 * @description:
 * @Date: 2020/1/22.
 */
@Slf4j
@Primary
@Configuration
@EnableCaching
public class CacheConfiguration {

    @Resource
    RedisTemplate redisTemplate;

    @Bean
    @Primary
    public CacheResolver cacheResolver() {
        return new SimpleCacheResolver(cacheManager());
    }

    @Bean("keyGenerator")
    public KeyGenerator keyGenerator(){
        return (o, method, objects) -> {
            StringBuilder sb = new StringBuilder();
            sb.append(o.getClass().getName()).append(".");
            sb.append(method.getName()).append(".");
            Arrays.asList(method.getParameterTypes()).forEach(item->{
                sb.append(item.getName());
                sb.append("-");
            });
            for (Object obj : objects) {
                sb.append(obj.toString());
                sb.append("-");
            }
            log.debug("keyGenerator=" + sb.toString());
            return sb.toString();
        };
    }

    @Bean
    @Primary
    public CacheManager cacheManager() {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig();
        config = config.serializeKeysWith(RedisSerializationContext
                .SerializationPair
                .fromSerializer(new StringRedisSerializer()));
        config = config.serializeValuesWith(RedisSerializationContext
                .SerializationPair
                .fromSerializer(new FastJsonRedisSerializer<>(Object.class)));
        config.entryTtl(Duration.ofSeconds(1000*10L));
        RedisCacheManager cacheManager = RedisCacheManager.builder(redisTemplate.getConnectionFactory()).cacheDefaults(config).build();
        return cacheManager;
    }
}
