package club.kingyin.rpc.common.filter;

import club.kingyin.rpc.common.dtos.RequestDTO;
import club.kingyin.rpc.common.exception.CallException;

/**
 * @author ：kingyin
 * @date ：创建于 2022/10/29 17:14
 * @description ：
 * @modified By：
 * @version: 1.0.0
 */
public abstract class AbstractRpcFilter implements RpcFilter {

    private RpcFilter next;

    @Override
    public void chainBefore(RequestDTO request) {
        boolean res = beforeInvoke(request);
        if (res && next != null) {
            next.chainBefore(request);
        } else if (!res) {
            throw new CallException("filter fault");
        }
    }

    @Override
    public void chainAfter(RequestDTO request, Object res) {
        afterInvoke(request, res);
        if (next != null) {
            next.chainAfter(request, res);
        }
    }

    public void next(RpcFilter next) {
        this.next = next;
    }

    @Override
    public RpcFilter next() {
        return next;
    }
}
