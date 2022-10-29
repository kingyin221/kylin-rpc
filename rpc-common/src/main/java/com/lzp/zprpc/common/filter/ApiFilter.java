package com.lzp.zprpc.common.filter;

import com.lzp.zprpc.common.api.constant.Constant;

import java.util.Map;

/**
 * @author ：kingyin
 * @date ：创建于 2022/10/29 17:33
 * @description ：
 * @modified By：
 * @version: 1.0.0
 */
public abstract class ApiFilter extends MeteFilter {

    public abstract boolean filterApiIn(Map<String, Object> mete);

    public abstract void filterApiOut(Map<String, Object> mete);

    @Override
    public boolean filterIn(Map<String, Object> mete) {
        if (Constant.INVOKE_API.equals(mete.get(Constant.INVOKE_TYPE))) {
            return filterApiIn(mete);
        }
        return true;
    }

    @Override
    public void filterOut(Map<String, Object> mete) {
        if (Constant.INVOKE_API.equals(mete.get(Constant.INVOKE_TYPE))) {
            filterApiOut(mete);
        }
    }
}
