package club.kingyin.rpc.common.filter;

import club.kingyin.rpc.common.dtos.RequestDTO;
import club.kingyin.rpc.common.api.ApiRpcMete;
import club.kingyin.rpc.common.constant.Cons;
import club.kingyin.rpc.common.dtos.ResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Map;

/**
 * @author ：kingyin
 * @date ：创建于 2022/11/20 14:34
 * @description ：
 * @modified By：
 * @version: 1.0.0
 */
@Slf4j
public class LoggerApiRpcFilter extends RpcApiFilter {
    @Override
    public boolean filterApiIn(RequestDTO request) {
        log.info("远程调用 {}", request);
        return true;
    }

    @Override
    public void filterApiOut(RequestDTO request, ResponseDTO response) {
        Map<String, Object> req = response.getMete();
        ApiRpcMete apiRpcMete = new ApiRpcMete();
        Long is = (Long) req.get(Cons.INVOKE_START_TIMER);
        Long rs = (Long) req.get(Cons.RPC_START_TIMER);
        Long ie = (Long) req.get(Cons.INVOKE_STOP_TIMER);
        Object re = req.get(Cons.RPC_STOP_TIMER);
        apiRpcMete.setService(request.getService());
        apiRpcMete.setMethodName(request.getMethodName());
        if (ObjectUtils.isNotEmpty(re)) {
            apiRpcMete.setNetTime(((Long) re) - rs);
        }
        apiRpcMete.setInvokeTime(ie - is);
        apiRpcMete.setState(response.getFinished());
        log.info("远程调用结束 {}", apiRpcMete);
    }
}
