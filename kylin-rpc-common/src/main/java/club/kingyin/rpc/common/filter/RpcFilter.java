package club.kingyin.rpc.common.filter;

import club.kingyin.rpc.common.dtos.RequestDTO;

/**
 * @author ：kingyin
 * @date ：创建于 2022/10/29 16:54
 * @description ：
 * @modified By：
 * @version: 1.0.0
 */
public interface RpcFilter {

    boolean beforeInvoke(RequestDTO request);

    default void afterInvoke(RequestDTO request, Object res) {

    }

    void chainBefore(RequestDTO request);

    void chainAfter(RequestDTO request, Object res);


    void next(RpcFilter next);

    RpcFilter next();

}
