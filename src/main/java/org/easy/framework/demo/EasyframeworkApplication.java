package org.easy.framework.demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Slf4j
@EnableTransactionManagement(proxyTargetClass = true)
@EnableCaching
@ServletComponentScan
@SpringBootApplication
@ComponentScan(basePackages = {"org.easy.framework"})
public class EasyframeworkApplication {

    public static void main(String[] args) {
        SpringApplication.run(EasyframeworkApplication.class, args);
    }

}
