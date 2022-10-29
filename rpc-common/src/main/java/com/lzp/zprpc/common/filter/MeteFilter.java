package com.lzp.zprpc.common.filter;

import com.lzp.zprpc.common.dtos.RequestDTO;

import java.util.Map;

/**
 * @author ：kingyin
 * @date ：创建于 2022/10/29 17:29
 * @description ：
 * @modified By：
 * @version: 1.0.0
 */
public abstract class MeteFilter extends AbstractRpcFilter {
    @Override
    public boolean beforeInvoke(RequestDTO request) {
        return filterIn(request.getMete());
    }

    @Override
    public void afterInvoke(RequestDTO request, Object res) {
        filterOut(request.getMete());
    }

    public abstract boolean filterIn(Map<String, Object> mete);

    public abstract void filterOut(Map<String, Object> mete);
}
