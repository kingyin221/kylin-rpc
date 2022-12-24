package club.kingyin.rpc.common.filter;

import club.kingyin.rpc.common.constant.Cons;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * @author ：kingyin
 * @date ：创建于 2022/10/29 20:07
 * @description ：
 * @modified By：
 * @version: 1.0.0
 */
@Slf4j
@Deprecated
public class CalculateClientMeteFilter extends MeteFilter {
    @Override
    public boolean filterIn(Map<String, Object> mete) {
        mete.put(Cons.RPC_START_TIMER, System.currentTimeMillis());
        return true;
    }

    @Override
    public void filterOut(Map<String, Object> mete) {
        mete.put(Cons.RPC_STOP_TIMER, System.currentTimeMillis());
    }
}
