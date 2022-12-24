package club.kingyin.rpc.server.netty;

import lombok.*;

/**
 * @author ：kingyin
 * @date ：创建于 2022/10/25 21:35
 * @description ：
 * @modified By：
 * @version: 1.0.0
 */
@Data
@Builder
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class MethodMete {
    private String methodName;
    private String service;
    private Class<?>[] paramTypes;
}
