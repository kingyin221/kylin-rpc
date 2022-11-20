package com.lzp.zprpc.common.filter.scanner;

import com.lzp.zprpc.common.filter.RpcFilter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author ：kingyin
 * @date ：创建于 2022/11/20 17:44
 * @description ：
 * @modified By：
 * @version: 1.0.0
 */
@Slf4j
public class FilterClientAutoScanner extends AbstractAutoScanner<RpcFilter> {
    @Override
    protected RpcFilter instance(Class<?> type) throws InstantiationException, IllegalAccessException {
        return (RpcFilter) type.newInstance();
    }

    @Override
    protected Boolean isScan(Class<?> type) {
        if (type.isAnnotationPresent(com.lzp.zprpc.common.filter.annotation.RpcFilter.class)) {
            boolean client = type.getDeclaredAnnotation(com.lzp.zprpc.common.filter.annotation.RpcFilter.class).client();
            boolean assignableFrom = RpcFilter.class.isAssignableFrom(type);
            if (!assignableFrom) {
                log.warn("过滤器:{}未实现{}", type, RpcFilter.class);
            }
            return assignableFrom && client;
        }
        return false;
    }
}
