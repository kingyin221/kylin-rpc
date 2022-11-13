package com.lzp.zprpc.common.api;

import com.lzp.zprpc.common.api.constant.HttpMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
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

    public boolean matchUri(String uri, Map<String, Object> pathValues) {
        if (url.contains("{")) {
            String[] sources = url.split("/");
            String[] target = uri.split("/");
            if (sources.length != target.length) return false;
            HashMap<String, Object> values = new HashMap<>();
            for (int i = 0; i < sources.length; i++) {
                if (sources[i].contains("{")) {
                    values.put(sources[i].replace("{", "").replace("}", ""), target[i]);
                } else if (!sources[i].equals(target[i])) {
                    return false;
                }
                i++;
            }
            pathValues.putAll(values);
            return true;
        } else {
            return url.equals(uri);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, httpMethod);
    }
}
