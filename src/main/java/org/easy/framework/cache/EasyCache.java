package org.easy.framework.cache;

import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @author: Zhangtong
 * @description:
 * @Date: 2020/1/22.
 */
@Aspect
@Order(0x1000001)
@Component
public class EasyCache {
}
