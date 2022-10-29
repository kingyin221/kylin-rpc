package com.lzp.zprpc.client.nacos;

import com.lzp.zprpc.common.api.constant.HttpMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

/**
 * @author leize
 * @date 2022/10/26
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Api {

    private String url;

    private HttpMethod httpMethod;

    private String[] paramNames;

    public Api(String url, HttpMethod httpMethod) {
        this.url = url;
        this.httpMethod = httpMethod;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Api api = (Api) o;
        return Objects.equals(url, api.url) && httpMethod == api.httpMethod;
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, httpMethod);
    }
}
