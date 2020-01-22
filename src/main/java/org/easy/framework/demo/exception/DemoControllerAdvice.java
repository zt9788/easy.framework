package org.easy.framework.demo.exception;

import org.easy.framework.exception.EasyException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

/**
 * @author: Zhangtong
 * @description:
 * @Date: 2020/1/22.
 */

@ControllerAdvice
public class DemoControllerAdvice {
    /**
     * 全局异常捕捉处理
     * @param ex
     * @return
     */
    @ResponseBody
    @ExceptionHandler(value = Exception.class)
    public String errorHandler(Exception ex) {
        return "-100:"+ex.getMessage();
    }

    @ResponseBody
    @ExceptionHandler(value = RuntimeException.class)
    public String errorHandler(RuntimeException ex) {
        return "-101:"+ex.getMessage();
    }

    @ResponseBody
    @ExceptionHandler(value = EasyException.class)
    public String errorHandler(EasyException ex) {
        return "-102:"+ex.getMessage();
    }


}
