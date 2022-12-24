package club.kingyin.rpc.common.filter.scanner;

import club.kingyin.rpc.common.filter.RpcFilter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author ：kingyin
 * @date ：创建于 2022/11/20 17:44
 * @description ：
 * @modified By：
 * @version: 1.0.0
 */
@Slf4j
public class FilterServiceAutoScanner extends AbstractAutoScanner<RpcFilter> {
    @Override
    protected RpcFilter instance(Class<?> type) throws InstantiationException, IllegalAccessException {
        return (RpcFilter) type.newInstance();
    }

    @Override
    protected Boolean isScan(Class<?> type) {
        if (type.isAnnotationPresent(club.kingyin.rpc.common.filter.annotation.RpcFilter.class)) {
            boolean server = type.getDeclaredAnnotation(club.kingyin.rpc.common.filter.annotation.RpcFilter.class).server();
            boolean assignableFrom = RpcFilter.class.isAssignableFrom(type);
            if (!assignableFrom) {
                log.warn("过滤器:{}未实现{}", type, RpcFilter.class);
            }
            return assignableFrom && server;
        }
        return false;
    }
}
