package com.lzp.zprpc.common.filter;

import com.lzp.zprpc.common.api.constant.Constant;
import com.lzp.zprpc.common.dtos.RequestDTO;
import org.slf4j.MDC;

/**
 * @author ：kingyin
 * @date ：创建于 2022/11/6 14:41
 * @description ：
 * @modified By：
 * @version: 1.0.0
 */
public class LinksFilter extends AbstractRpcFilter {
    @Override
    public boolean beforeInvoke(RequestDTO request) {
        if (request.getMete().containsKey(Constant.REQUEST_ID))
            MDC.put(Constant.REQUEST_ID, (String) request.getMete().get(Constant.REQUEST_ID));
        return true;
    }

    @Override
    public void afterInvoke(RequestDTO request, Object res) {
        MDC.remove(Constant.REQUEST_ID);
    }
}
