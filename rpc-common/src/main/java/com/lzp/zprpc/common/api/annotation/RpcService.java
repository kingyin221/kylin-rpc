package com.lzp.zprpc.common.api.annotation;


import com.lzp.zprpc.common.api.constant.Constant;
import com.lzp.zprpc.common.api.constant.ServiceName;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author ：kingyin
 * @date ：创建于 2022/10/23 17:03
 * @description ：
 * @modified By：
 * @version: 1.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RpcService {

    String name() default Constant.EMPTY_STRING;

    String module() default Constant.EMPTY_STRING;

    Class<?> ref();

    ServiceName nameStrategy() default ServiceName.HUMP_SIMPLE;

}
