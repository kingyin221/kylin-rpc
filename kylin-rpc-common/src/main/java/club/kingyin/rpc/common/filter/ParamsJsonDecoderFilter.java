package club.kingyin.rpc.common.filter;

import club.kingyin.rpc.common.dtos.RequestDTO;
import com.alibaba.fastjson2.JSONObject;
import club.kingyin.rpc.common.exception.ServiceError;
import lombok.extern.slf4j.Slf4j;

/**
 * @author ：kingyin
 * @date ：创建于 2022/11/1 14:25
 * @description ：
 * @modified By：
 * @version: 1.0.0
 */
@Slf4j
public class ParamsJsonDecoderFilter extends AbstractRpcFilter {
    @Override
    public boolean beforeInvoke(RequestDTO request) {
        for (int i = 0; i < request.getParamTypes().length; i++) {
            request.getParams()[i] = decoderPram(request.getParams()[i], request.getParamTypes()[i]);
        }
        return true;
    }

    private Object decoderPram(Object source, Class<?> target) {
        if (source == null) {
            return null;
        }
        if (source.getClass().equals(target)) {
            return source;
        } else if (source instanceof String) {
            return JSONObject.parseObject((String) source, target);
        }
        log.info("参数类型解析异常 param={}, target={}", source, target);
        return new ServiceError(101100L, "param type error");
    }
}
