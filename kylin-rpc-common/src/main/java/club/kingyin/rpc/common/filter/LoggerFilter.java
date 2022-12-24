package club.kingyin.rpc.common.filter;

import club.kingyin.rpc.common.constant.Cons;
import club.kingyin.rpc.common.dtos.RequestDTO;
import lombok.extern.slf4j.Slf4j;

/**
 * @author ：kingyin
 * @date ：创建于 2022/10/29 17:18
 * @description ：
 * @modified By：
 * @version: 1.0.0
 */
@Slf4j
@Deprecated
public class LoggerFilter extends AbstractRpcFilter {
    @Override
    public boolean beforeInvoke(RequestDTO request) {
        log.info("远程方法调用 request={}", request);
        return true;
    }

    @Override
    public void afterInvoke(RequestDTO request, Object res) {
        log.info("远程方法调用结束 res={}", res);
        if (request.getMete().containsKey(Cons.RPC_STOP_TIMER)) {
            log.info("远程调用总耗时：{}ms", (((Long) request.getMete().get(Cons.RPC_STOP_TIMER)) - ((Long) request.getMete().get(Cons.RPC_START_TIMER))));
        }
        if (request.getMete().containsKey(Cons.INVOKE_STOP_TIMER)) {
            log.info("远程调用方法耗时：{}ms", (((Long) request.getMete().get(Cons.INVOKE_STOP_TIMER)) - ((Long) request.getMete().get(Cons.INVOKE_START_TIMER))));
        }
    }
}
