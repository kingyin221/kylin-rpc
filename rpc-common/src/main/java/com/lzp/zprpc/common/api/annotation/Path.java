package com.lzp.zprpc.common.api.annotation;

import com.lzp.zprpc.common.api.constant.Constant;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author ：kingyin
 * @date ：创建于 2022/10/29 16:44
 * @description ：
 * @modified By：
 * @version: 1.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Path {

    String name() default Constant.EMPTY_STRING;

}
