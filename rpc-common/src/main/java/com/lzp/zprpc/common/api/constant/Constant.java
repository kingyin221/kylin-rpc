package com.lzp.zprpc.common.api.constant;

/**
 * @author ：kingyin
 * @date ：创建于 2022/10/23 17:07
 * @description ：
 * @modified By：
 * @version: 1.0.0
 */
public class Constant {

    public static final String EMPTY_STRING = "";

    public static final String DEFAULT_MODULE = "kylin";

    public static final String INVOKE_TYPE = "kylin-rpc-type";

    public static final String INVOKE_API = "kylin-rpc-api";

    public static final String INVOKE_SERVICE = "kylin-rpc-service";

    public static final String BODY_DEFAULT = "context-data";

    public static final String TYPE_REF = "param-type";

    public static final String REQUEST_ID = "request-id";

    public static Class<?> getType(Class<?> target) {
        if (String.class.equals(target)) {
            return String.class;
        } else if (Integer.class.equals(target)) {
            return Integer.class;
        } else if (Long.class.equals(target)) {
            return Long.class;
        } else if (Boolean.class.equals(target)) {
            return Boolean.class;
        } else if (Short.class.equals(target)) {
            return Short.class;
        } else if (Byte.class.equals(target)) {
            return Byte.class;
        } else if (Double.class.equals(target)) {
            return Double.class;
        } else if (Float.class.equals(target)) {
            return Float.class;
        }
        return Reference.class;
    }
}
