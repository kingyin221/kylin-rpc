package com.lzp.zprpc.common.api;

import com.lzp.zprpc.common.api.constant.HttpMethod;
import com.lzp.zprpc.common.dtos.RequestDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

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

    private Map<String, Object> meter;

    private Class<?> serviceType;

    private String service;

    public ApiMeteDate clo() {
        return new ApiMeteDate(url, type, methodName, paramTypes, meter, null, service);
    }

}
