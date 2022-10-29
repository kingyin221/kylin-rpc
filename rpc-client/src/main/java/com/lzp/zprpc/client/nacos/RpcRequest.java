package com.lzp.zprpc.client.nacos;

import com.lzp.zprpc.common.api.constant.Constant;
import com.lzp.zprpc.common.api.constant.HttpMethod;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * @author ：kingyin
 * @date ：创建于 2022/10/25 23:43
 * @description ：
 * @modified By：
 * @version: 1.0.0
 */
@Data
@AllArgsConstructor
public class RpcRequest {

    private short type;

    private String var1;
    private String var2;
    private HttpMethod httpMethod;
    private Class<?>[] paramsType;
    private Map<String, Object> mete;

    private RpcRequest() {

    }

    public void addMete(String key, Object value) {
        this.mete.put(key, value);
    }

    public static class Builder {
        private short type;
        private String var1;
        private Map<String, Object> mete = new HashMap<>();

        public RpcRequest api(String url, HttpMethod httpMethod) {
            this.type = 1;
            this.var1 = url;
            mete.put(Constant.INVOKE_TYPE, Constant.INVOKE_API);
            return new RpcRequest(type, var1, null, httpMethod, null, mete);
        }

        public RpcRequest service(String serviceName, String methodName, Class<?>... paramsTypes) {
            this.type = 2;
            this.var1 = serviceName;
            mete.put(Constant.INVOKE_TYPE, Constant.INVOKE_SERVICE);
            return new RpcRequest(type, var1, methodName, null, paramsTypes, mete);
        }

    }

    public boolean isApi() {
        return type == 1;
    }

    public Object key() {
        if (isApi()) {
            return new Api(this.var1, this.httpMethod);
        } else {
            return new Service(var1, var2, paramsType);
        }
    }

    public void conv(Service service) {
        this.var1 = service.getService();
        this.var2 = service.getMethodName();
        this.paramsType = service.getParamTypes();
    }


}
