package zprpc.demo.nacos.producer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @author ：kingyin
 * @date ：创建于 2022/11/1 13:55
 * @description ：
 * @modified By：
 * @version: 1.0.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Message implements Serializable {

    private static final long serialVersionUID = 5529718596923442776L;
    private String msg;
    private Object res;
    private BigDecimal price;
}
