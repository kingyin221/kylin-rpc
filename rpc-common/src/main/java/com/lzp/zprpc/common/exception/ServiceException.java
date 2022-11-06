package com.lzp.zprpc.common.exception;

import lombok.Data;

/**
 * @author ：kingyin
 * @date ：创建于 2022/10/31 21:21
 * @description ：
 * @modified By：
 * @version: 1.0.0
 */
@Data
public class ServiceException extends RuntimeException {

    private Long code;

    private String msg;

    public ServiceException() {
        super();
        code = 0L;
    }

    public ServiceException(Long code, String message) {
        super(message);
        this.code = code == null ? 0 : code;
    }

    // 用指定的详细信息和原因构造一个新的异常
    public ServiceException(Long code, String message, Throwable cause) {
        super(message, cause);
        this.code = code == null ? 0 : code;
    }

    //用指定原因构造一个新的异常
    public ServiceException(Long code, Throwable cause) {
        super(cause);
        this.code = code == null ? 0 : code;
    }

}
