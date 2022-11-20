package com.lzp.zprpc.common.filter;

import com.lzp.zprpc.common.constant.Cons;
import com.lzp.zprpc.common.dtos.RequestDTO;
import com.lzp.zprpc.common.dtos.ResponseDTO;

import java.util.Map;

/**
 * @author ：kingyin
 * @date ：创建于 2022/11/20 14:06
 * @description ：
 * @modified By：
 * @version: 1.0.0
 */
public class BaseApiRpcFilter extends RpcApiFilter {
    @Override
    public boolean filterApiIn(RequestDTO request) {
        Map<String, Object> mete = request.getMete();
        if (!mete.containsKey(Cons.RPC_START_TIMER)) {
            // client 调用
            mete.put(Cons.RPC_START_TIMER, System.currentTimeMillis());
        } else {
            // server 调用
            mete.put(Cons.INVOKE_START_TIMER, System.currentTimeMillis());
        }
        return true;
    }

    @Override
    public void filterApiOut(RequestDTO request, ResponseDTO response) {
        Map<String, Object> resMete = request.getMete();
        Map<String, Object> mete = response.getMete();
        if (resMete.containsKey(Cons.INVOKE_START_TIMER)) {
            // server save invoke-start
            mete.put(Cons.INVOKE_START_TIMER, resMete.get(Cons.INVOKE_START_TIMER));
        }
        if (resMete.containsKey(Cons.RPC_START_TIMER)) {
            // server save rpc-start
            mete.put(Cons.RPC_START_TIMER, resMete.get(Cons.RPC_START_TIMER));
        }
        if (!mete.containsKey(Cons.INVOKE_STOP_TIMER)) {
            // server save invoke-stop
            mete.put(Cons.INVOKE_STOP_TIMER, System.currentTimeMillis());
        }
        if (!mete.containsKey(Cons.RPC_STOP_TIMER)) {
            // client save rpc-stop
            mete.put(Cons.RPC_STOP_TIMER, System.currentTimeMillis());
        }
    }
}
