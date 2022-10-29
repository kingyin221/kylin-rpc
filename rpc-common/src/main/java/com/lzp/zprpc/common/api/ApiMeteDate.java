package com.lzp.zprpc.common.api;

import com.lzp.zprpc.common.api.constant.HttpMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    private Map<String, Object> meter;

    private Class<?> serviceType;

    private String service;

    public ApiMeteDate clo() {
        return new ApiMeteDate(url, type, methodName, paramTypes, parmaNames, meter, null, service);
    }

}
