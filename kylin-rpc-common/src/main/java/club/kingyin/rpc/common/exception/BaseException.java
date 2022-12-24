package club.kingyin.rpc.common.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;

/**
 * @author ：kingyin
 * @date ：创建于 2022/11/19 21:00
 * @description ：
 * @modified By：
 * @version: 1.0.0
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class BaseException extends InvocationTargetException implements Serializable {

    private static final long serialVersionUID = -191774511571499262L;

    private Long code;
    private String msg;

    public BaseException() {
        super();
        code = 103000L;
    }

    public BaseException(Long code, String message) {
        super(null, message);
        this.code = code == null ? 103000L : code;
        this.msg = super.getLocalizedMessage();
    }

    // 用指定的详细信息和原因构造一个新的异常
    public BaseException(Long code, String message, Throwable cause) {
        super(cause, message);
        this.code = code == null ? 103000L : code;
        this.msg = super.getLocalizedMessage();
    }

    //用指定原因构造一个新的异常
    public BaseException(Long code, Throwable cause) {
        super(cause);
        this.code = code == null ? 103000L : code;
        this.msg = super.getLocalizedMessage();
    }
}
