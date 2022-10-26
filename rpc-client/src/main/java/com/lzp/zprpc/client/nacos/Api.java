package com.lzp.zprpc.client.nacos;

import com.lzp.zprpc.common.api.constant.HttpMethod;
import lombok.*;

/**
 * @author leize
 * @date 2022/10/26
 */
@Data
@Builder
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class Api {

    private String url;
    private HttpMethod httpMethod;

}
