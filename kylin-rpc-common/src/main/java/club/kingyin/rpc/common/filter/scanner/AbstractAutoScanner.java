package club.kingyin.rpc.common.filter.scanner;

import club.kingyin.rpc.common.util.ClazzUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author ：kingyin
 * @date ：创建于 2022/11/20 17:21
 * @description ：自动扫描类
 * @modified By：
 * @version: 1.0.0
 */
public abstract class AbstractAutoScanner<T> implements AutoScanner<T> {
    @Override
    public Map<Class<?>, T> scan(String basePackage) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        HashMap<Class<?>, T> res = new HashMap<>();
        for (String path : ClazzUtils.getClazzName(basePackage)) {
            Class<?> cls = Class.forName(path);
            if (isScan(cls)) {
                res.put(cls, instance(cls));
            }
        }
        return res;
    }

    @Override
    public Map<Class<?>, T> scan(String basePackage, ApplicationContext context) throws ClassNotFoundException {
        HashMap<Class<?>, T> res = new HashMap<>();
        for (String path : ClazzUtils.getClazzName(basePackage)) {
            Class<?> cls = Class.forName(path);
            if (isScan(cls)) {
                res.put(cls, (T) context.getBean(cls));
            }
        }
        return res;
    }

    @Override
    public List<T> scanList(String basePackage) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        return scan(basePackage).values().stream().sorted(AnnotationAwareOrderComparator.INSTANCE).collect(Collectors.toList());
    }

    @Override
    public List<T> scanList(String basePackage, ApplicationContext context) throws ClassNotFoundException {
        return scan(basePackage, context).values().stream().sorted(AnnotationAwareOrderComparator.INSTANCE).collect(Collectors.toList());
    }

    /**
     * 普通实例化方法
     *
     * @param type class
     * @return bean
     * @throws InstantiationException 实例化失败
     * @throws IllegalAccessException 实例化失败
     */
    abstract protected T instance(Class<?> type) throws InstantiationException, IllegalAccessException;

    /**
     * 校验类是否需要实例化
     *
     * @param type class
     * @return bean
     */
    abstract protected Boolean isScan(Class<?> type);
}
