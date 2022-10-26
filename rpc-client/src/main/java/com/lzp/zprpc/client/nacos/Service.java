package com.lzp.zprpc.client.nacos;

import lombok.*;

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
}
