package club.kingyin.rpc.common.api.annotation;


import club.kingyin.rpc.common.api.constant.Constant;
import club.kingyin.rpc.common.api.constant.ServiceName;

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
@Target({ElementType.TYPE, ElementType.FIELD})
public @interface RpcService {

    String name() default Constant.EMPTY_STRING;

    String module() default Constant.EMPTY_STRING;

    Class<?> ref() default Void.class;

    String service() default Constant.EMPTY_STRING;

    ServiceName nameStrategy() default ServiceName.HUMP_SIMPLE;

    long timout() default 0L;

}
