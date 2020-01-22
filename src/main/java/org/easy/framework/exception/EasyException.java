package org.easy.framework.exception;

/**
 * @author: Zhangtong
 * @description:
 * @Date: 2020/1/22.
 */
public class EasyException extends RuntimeException {
    public EasyException(){
        super();
    }
    public EasyException(String error){
        super(error);
    }
}
