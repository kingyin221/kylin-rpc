package com.lzp.zprpc.common.api.constant;

/**
 * @author ：kingyin
 * @date ：创建于 2022/10/23 17:26
 * @description ：
 * @modified By：
 * @version: 1.0.0
 */
public enum ServiceName {

    HUMP_SIMPLE {
        @Override
        public String getName(Class<?> type) {
            char[] cs=type.getSimpleName().toCharArray();
            cs[0]+=32;
            return String.valueOf(cs);
        }
    },

    IDENTIFY_SIMPLE {
        @Override
        public String getName(Class<?> type) {
            return type.getSimpleName();
        }
    },

    FULL {
        @Override
        public String getName(Class<?> type) {
            return type.getName();
        }
    };

    public String getName(Class<?> type) {
        return HUMP_SIMPLE.getName(type);
    }

    public String serviceName(String module, String name) {
        return "["+module+"]"+name;
    }

}
