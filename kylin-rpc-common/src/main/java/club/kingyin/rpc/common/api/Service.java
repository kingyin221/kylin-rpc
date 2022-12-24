package club.kingyin.rpc.common.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
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
public class Service {

    private String service;
    private String methodName;
    private Class<?>[] paramTypes;
    private Map<String, Object> mete;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Service service1 = (Service) o;
        return service.equals(service1.service) && methodName.equals(service1.methodName) && Arrays.equals(paramTypes, service1.paramTypes);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(service, methodName);
        result = 31 * result + Arrays.hashCode(paramTypes);
        return result;
    }
}
