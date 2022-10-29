package com.lzp.zprpc.common.filter;

import com.lzp.zprpc.common.constant.Cons;

import java.util.Map;

/**
 * @author ：kingyin
 * @date ：创建于 2022/10/29 20:07
 * @description ：
 * @modified By：
 * @version: 1.0.0
 */
public class CalculateServiceMeteFilter extends MeteFilter {
    @Override
    public boolean filterIn(Map<String, Object> mete) {
        mete.put(Cons.INVOKE_START_TIMER, System.currentTimeMillis());
        return true;
    }

    @Override
    public void filterOut(Map<String, Object> mete) {
        mete.put(Cons.INVOKE_STOP_TIMER, System.currentTimeMillis());
    }
}
