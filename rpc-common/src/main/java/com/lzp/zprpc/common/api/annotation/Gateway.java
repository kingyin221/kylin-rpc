package com.lzp.zprpc.common.api.annotation;


import com.lzp.zprpc.common.api.constant.HttpMethod;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author ：kingyin
 * @date ：创建于 2022/10/23 17:06
 * @description ：
 * @modified By：
 * @version: 1.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Gateway {

    String url();

    HttpMethod type() default HttpMethod.GET;

}
