package com.lzp.zprpc.client.nacos;

import com.lzp.zprpc.common.constant.Cons;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

/**
 * @author ：kingyin
 * @date ：创建于 2022/10/23 23:14
 * @description ：
 * @modified By：
 * @version: 1.0.0
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ServiceMete {

    private String ip;
    private Integer port;
    private String id;

    public String address() {
        return ip + Cons.COLON + port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceMete that = (ServiceMete) o;
        return Objects.equals(ip, that.ip) && Objects.equals(port, that.port) && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, port, id);
    }
}
