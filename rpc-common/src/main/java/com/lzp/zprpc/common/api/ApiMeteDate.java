package com.lzp.zprpc.common.api;

import com.lzp.zprpc.common.api.constant.Constant;
import com.lzp.zprpc.common.api.constant.HttpMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * @author ：kingyin
 * @date ：创建于 2022/10/23 18:01
 * @description ：
 * @modified By：
 * @version: 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiMeteDate {

    private String url;

    private HttpMethod type;

    private String methodName;

    /**
     * 方法参数类型
     */
    private Class<?>[] paramTypes;

    private String[] parmaNames;

    private Map<String, Object> mete = new HashMap<>();

    private Class<?> serviceType;

    private String service;

    public ApiMeteDate clo() {

        return new ApiMeteDate(url, type, methodName, convRef(paramTypes), parmaNames, mete, null, service);
    }

    private Class<?>[] convRef(Class<?>[] types) {
        Class<?>[] newTypes = new Class<?>[types.length];
        for (int i = 0; i < types.length; i++) {
            newTypes[i] = Constant.getType(types[i]);
        }
        return newTypes;
    }


}
