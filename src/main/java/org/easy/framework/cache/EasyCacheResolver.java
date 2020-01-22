package org.easy.framework.cache;

import org.springframework.cache.interceptor.AbstractCacheResolver;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;

import java.util.Collection;

/**
 * @author: Zhangtong
 * @description:
 * @Date: 2020/1/22.
 */
@Deprecated
public class EasyCacheResolver extends AbstractCacheResolver {
    @Override
    protected Collection<String> getCacheNames(CacheOperationInvocationContext<?> cacheOperationInvocationContext) {
        return null;
    }
}
