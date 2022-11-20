package com.lzp.zprpc.common.exception;

import lombok.Data;

import java.io.Serializable;

/**
 * @author ：kingyin
 * @date ：创建于 2022/10/31 21:21
 * @description ：
 * @modified By：
 * @version: 1.0.0
 */
@Data
public class ServiceError implements Serializable {

    private static final long serialVersionUID = 1835844453471669530L;

    private Long code;

    private String msg;

    public ServiceError() {
        code = 0L;
    }

    public ServiceError(Long code, String message) {
        this.code = code == null ? 0 : code;
        this.msg = message;
    }

    // 用指定的详细信息和原因构造一个新的异常
    public ServiceError(Long code, String message, Throwable cause) {
        this.code = code == null ? 0 : code;
        this.msg = message + " : " + cause.getLocalizedMessage();
    }

    //用指定原因构造一个新的异常
    public ServiceError(Long code, Throwable cause) {
        this.code = code == null ? 0 : code;
        this.msg = cause.getLocalizedMessage();
    }

    public ServiceError(BaseException baseException) {
        this.code = baseException.getCode();
        this.msg = baseException.getMsg();
    }

}
