package com.lzp.zprpc.common.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author ：kingyin
 * @date ：创建于 2022/11/20 14:42
 * @description ：
 * @modified By：
 * @version: 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiRpcMete implements Serializable {
    private static final long serialVersionUID = -1189287077496718450L;

    private String methodName;

    private String service;

    private Long netTime;

    private Long invokeTime;

    private Boolean state;
}
