package com.lzp.zprpc.common.filter.scanner;

import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.Map;

/**
 * @author ：kingyin
 * @date ：创建于 2022/11/20 17:17
 * @description ：
 * @modified By：
 * @version: 1.0.0
 */
public interface AutoScanner<T> {

    /**
     * 扫描路径下的class，并加载进内存
     *
     * @param basePackage 包路径
     * @return 实例对象 key:class -> value : bean
     * @throws ClassNotFoundException 类找不到
     * @throws InstantiationException 实例化失败
     * @throws IllegalAccessException 实例化失败
     */
    Map<Class<?>, T> scan(String basePackage) throws ClassNotFoundException, InstantiationException, IllegalAccessException;

    /**
     * 扫描路径下的class，并从IOC加载
     *
     * @param basePackage 包路径
     * @param context     Spring IOC
     * @return 实例对象 key:class -> value : bean
     * @throws ClassNotFoundException 类找不到
     */
    Map<Class<?>, T> scan(String basePackage, ApplicationContext context) throws ClassNotFoundException;

    /**
     * 扫描路径下的class，并加载进内存
     *
     * @param basePackage 包路径
     * @return 实例对象 key:class -> value : bean
     * @throws ClassNotFoundException 类找不到
     * @throws InstantiationException 实例化失败
     * @throws IllegalAccessException 实例化失败
     */
    List<T> scanList(String basePackage) throws ClassNotFoundException, InstantiationException, IllegalAccessException;

    /**
     * 扫描路径下的class，并从IOC加载
     *
     * @param basePackage 包路径
     * @param context     Spring IOC
     * @return 实例对象 key:class -> value : bean
     * @throws ClassNotFoundException 类找不到
     */
    List<T> scanList(String basePackage, ApplicationContext context) throws ClassNotFoundException;
}
