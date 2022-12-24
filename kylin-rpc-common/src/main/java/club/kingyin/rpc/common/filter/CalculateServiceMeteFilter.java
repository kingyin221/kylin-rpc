package club.kingyin.rpc.common.filter;

import club.kingyin.rpc.common.constant.Cons;

import java.util.Map;

/**
 * @author ：kingyin
 * @date ：创建于 2022/10/29 20:07
 * @description ：
 * @modified By：
 * @version: 1.0.0
 */
@Deprecated
public class CalculateServiceMeteFilter extends MeteFilter {
    @Override
    public boolean filterIn(Map<String, Object> mete) {
        mete.put(Cons.INVOKE_START_TIMER, System.nanoTime());
        return true;
    }

    @Override
    public void filterOut(Map<String, Object> mete) {
        mete.put(Cons.INVOKE_STOP_TIMER, System.nanoTime());
    }
}
