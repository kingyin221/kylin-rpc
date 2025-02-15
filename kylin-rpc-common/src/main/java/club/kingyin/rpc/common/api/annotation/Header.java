package club.kingyin.rpc.common.api.annotation;

import club.kingyin.rpc.common.api.constant.Constant;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author ：kingyin
 * @date ：创建于 2022/10/31 21:07
 * @description ：
 * @modified By：
 * @version: 1.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Header {

    String value() default Constant.EMPTY_STRING;

}
