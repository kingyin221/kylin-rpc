package club.kingyin.rpc.common.util;

/**
 * @author ：kingyin
 * @date ：创建于 2022/11/26 15:36
 * @description ：
 * @modified By：
 * @version: 1.0.0
 */
public class ThrowUtils {

    public static String getDetailMsgOfException(Throwable t) {
        Throwable th;
        do {
            th = t;
        } while ((t = t.getCause()) != null);
        return th.getMessage();
    }
}
