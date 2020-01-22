package org.easy.framework.cache.ehcache;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author: Zhangtong
 * @description:
 * @Date: 2020/1/22.
 */

@Data
@Component
@ConfigurationProperties(prefix = "cache.ehcache")
public class EhcacheProperties {

}
