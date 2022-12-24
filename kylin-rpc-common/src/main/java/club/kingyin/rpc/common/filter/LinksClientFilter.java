package club.kingyin.rpc.common.filter;

import club.kingyin.rpc.common.api.constant.Constant;
import club.kingyin.rpc.common.dtos.RequestDTO;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;

/**
 * @author ：kingyin
 * @date ：创建于 2022/11/20 13:38
 * @description ：
 * @modified By：
 * @version: 1.0.0
 */
public class LinksClientFilter extends AbstractRpcFilter {
    @Override
    public boolean beforeInvoke(RequestDTO request) {
        String requestId = MDC.get(Constant.REQUEST_ID);
        if (StringUtils.isNotBlank(requestId)) {
            request.getMete().put(Constant.REQUEST_ID, requestId);
        }
        return true;
    }
}
