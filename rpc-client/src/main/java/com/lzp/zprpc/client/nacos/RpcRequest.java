package com.lzp.zprpc.client.nacos;

import com.google.common.collect.Lists;
import com.lzp.zprpc.common.api.constant.HttpMethod;
import com.lzp.zprpc.common.constant.Cons;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Arrays;

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

    public static class Builder {
        private short type;
        private String var1;
        public RpcRequest api(String url, HttpMethod httpMethod) {
            this.type = 1;
            this.var1 = url;
            return new RpcRequest(type, var1, null, httpMethod, null);
        }

        public RpcRequest service(String serviceName, String methodName, Class<?>... paramsTypes) {
            this.type = 2;
            this.var1 = serviceName;
            return new RpcRequest(type, var1, methodName, null, paramsTypes);
        }

    }

    public boolean isApi() {
        return type == 1;
    }

    public String key() {
        if (isApi()) {
            return this.httpMethod.name() + Cons.COLON + this.var1;
        } else  {
            return this.var1 + Cons.COLON + var2 + Cons.COLON + (this.paramsType == null ? Lists.newArrayList().toString() : Arrays.toString(this.paramsType));
        }
    }

    public void decoderService(String service) {
        String[] split = service.split(String.valueOf(Cons.COLON));
        this.var1 = split[0];
        this.var2 = split[1];
        // todo 参数类型转换
    }

}
