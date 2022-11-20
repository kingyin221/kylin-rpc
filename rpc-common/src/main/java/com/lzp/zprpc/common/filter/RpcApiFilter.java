package com.lzp.zprpc.common.filter;

import com.lzp.zprpc.common.dtos.RequestDTO;
import com.lzp.zprpc.common.dtos.ResponseDTO;

/**
 * @author ：kingyin
 * @date ：创建于 2022/11/20 14:00
 * @description ：
 * @modified By：
 * @version: 1.0.0
 */
public abstract class RpcApiFilter extends AbstractRpcFilter {

    public abstract boolean filterApiIn(RequestDTO request);

    public abstract void filterApiOut(RequestDTO request, ResponseDTO response);

    @Override
    public boolean beforeInvoke(RequestDTO request) {
        return filterApiIn(request);
    }

    @Override
    public void afterInvoke(RequestDTO request, Object res) {
        if (res instanceof ResponseDTO) {
            filterApiOut(request, ((ResponseDTO) res));
        }
    }
}
