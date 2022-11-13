package com.lzp.zprpc.common.api;

import lombok.*;

import java.util.Map;

/**
 * @author leize
 * @date 2022/10/26
 */
@Data
@Builder
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class Service {

    private String service;
    private String methodName;
    private Class<?>[] paramTypes;
    private Map<String, Object> mete;


}
