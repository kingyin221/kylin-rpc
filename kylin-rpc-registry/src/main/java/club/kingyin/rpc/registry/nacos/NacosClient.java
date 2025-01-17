 /* Copyright zeping lu
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *   http://www.apache.org/licenses/LICENSE-2.0
  *
  *  Unless required by applicable law or agreed to in writing, software
  *  distributed under the License is distributed on an "AS IS" BASIS,
  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  *  See the License for the specific language governing permissions and
  *  limitations under the License.
  */

 package club.kingyin.rpc.registry.nacos;

 import club.kingyin.rpc.common.api.ApiMeteDate;
 import club.kingyin.rpc.common.api.annotation.*;
 import club.kingyin.rpc.common.api.constant.Constant;
 import club.kingyin.rpc.common.api.constant.Reference;
 import club.kingyin.rpc.common.api.constant.ServiceName;
 import club.kingyin.rpc.common.constant.Cons;
 import club.kingyin.rpc.common.exception.CallException;
 import club.kingyin.rpc.common.util.ClazzUtils;
 import club.kingyin.rpc.registry.api.RegistryClient;
 import com.alibaba.nacos.api.exception.NacosException;
 import com.alibaba.nacos.api.naming.NamingFactory;
 import com.alibaba.nacos.api.naming.NamingService;
 import org.apache.commons.lang3.ObjectUtils;
 import org.apache.commons.lang3.StringUtils;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;

 import java.lang.reflect.Method;
 import java.lang.reflect.Parameter;
 import java.util.*;
 import java.util.concurrent.ConcurrentHashMap;
 import java.util.stream.Collectors;

 /**
  * Description:实现了统一接口的nacos客户端
  *
  * @author: Lu ZePing
  * @date: 2020/10/9 14:10
  */
 public class NacosClient implements RegistryClient {
     private static final Logger LOGGER = LoggerFactory.getLogger(NacosClient.class);

     private NamingService namingService;

     public NacosClient(String host) {
         try {
             namingService = NamingFactory.createNamingService(host);
         } catch (NacosException e) {
             LOGGER.error("init nameservice failed", e);
         }
     }

     public NacosClient() {
         try {
             namingService = NamingFactory.createNamingService(HOST);
         } catch (NacosException e) {
             LOGGER.error("init nameservice failed", e);
         }
     }

     /**
      * 扫描指定包下所有类，获得所有被com.lzp.zprpc.common.annotation.@Service修饰的类，返回实例（如果项目用到了Spring，就到
      * Spring容器中找，找不到才自己初始化一个),并注册到注册中心中
      * <p>
      * ______________________________________________
      * |               namespace                     |
      * |   ————————————————————————————————————————  |
      * |  | ____________ group____________________ | |
      * |  || |------------service--------------| | | |
      * |  || | |cluser |          | cluster|   | | | |
      * |  || | |_______|          |________|   | | | |
      * |  || |_________________________________| | | |
      * |  ||_____________________________________| | |
      * |  |_______________________________________ | |
      * ———————————————————————————————————————————————
      * group和serviceid决定一个服务，一个service包含多个cluster，每个cluster
      * 里包含多个instance
      *
      * @param basePack 要扫描的包
      * @param ip       要注册进注册中心的实例（instance)ip
      * @param port     要注册进注册中心的实例（instance)port
      */

     @Override
     public Map<String, Object> searchAndRegiInstance(String basePack, String ip, int port) throws NacosException, InstantiationException, IllegalAccessException, ClassNotFoundException {
         Map<String, Object> idServiceMap = new ConcurrentHashMap<>();
         for (String path : ClazzUtils.getClazzName(basePack)) {
             regiInstanceIfNecessary(ip, port, idServiceMap, Class.forName(path));
         }
         return idServiceMap;
     }

     @Override
     public void deregiServices(Set<String> serivces, String ip, int port) throws Exception {
         for (String service : serivces) {
             namingService.deregisterInstance(service, ip, port);
         }
     }

     private boolean condition(Class<?> cls) {
         String tmp = Objects.requireNonNull(cls.getResource("")).toString();
         if (tmp.startsWith("file")) {
             // 非jar中运行
             return true;
         } else {
             if (tmp.contains("/BOOT-INF/lib/") || tmp.contains("/BOOT-INF/classes!")) {
                 // jar运行
                 return tmp.split(".jar!").length > 2;
             } else {
                 // 非jar运行
                 return false;
             }
         }
     }


     private void regiInstanceIfNecessary(String ip, int port, Map<String, Object> idServiceMap, Class<?> cls) throws InstantiationException, IllegalAccessException, NacosException {
         if (cls.isAnnotationPresent(RpcService.class)) {
             if (!condition(cls)) {
                 return;
             }
             RpcService rpcService = cls.getDeclaredAnnotation(RpcService.class);
             if (cls.isInterface()) {
                 if (Void.class.equals(rpcService.ref())) {
                     LOGGER.info("接口自动注册 {}", cls);
                     registerService(rpcService, cls, ip, port, idServiceMap);
                 } else if (!rpcService.ref().isInterface()) {
                     LOGGER.info("接口主动注册 {} -> {}", cls, rpcService.ref());
                     registerService(rpcService, cls, ip, port, idServiceMap);
                 } else
                     LOGGER.warn("@RpcService不能注册接口 rpcService={}, class={}", rpcService, cls);
             } else {
                 registerService(rpcService, cls, ip, port, idServiceMap);
             }
         } else if (Arrays.stream(cls.getDeclaredMethods()).anyMatch(method -> method.isAnnotationPresent(Gateway.class))) {
             LOGGER.warn("服务未注册，缺失@RpcService {}", cls);
         }
     }

     private void registerService(RpcService rpcService, Class<?> type, String ip, int port, Map<String, Object> idServiceMap) throws NacosException {
         // 服务解析
         String id;
         if (ObjectUtils.isEmpty(rpcService)) {
             id = ServiceName.FULL.serviceName(ServiceName.HUMP_SIMPLE.getName(type), Constant.DEFAULT_MODULE);
         } else {
             String name = Optional.of(rpcService.name()).filter(StringUtils::isNotBlank)
                     .orElse(rpcService.nameStrategy().getName(type));
             String module = Optional.of(rpcService.module()).filter(StringUtils::isNotBlank)
                     .orElse(Constant.DEFAULT_MODULE);
             LOGGER.info("服务解析 module={} name={}", module, name);
             id = ServiceName.FULL.serviceName(module, name);
         }
         namingService.registerInstance(id, ip, port);
         // 方法解析
         List<ApiMeteDate> mete = Arrays.stream(type.getDeclaredMethods()).filter(ObjectUtils::isNotEmpty)
                 .filter(method -> method.isAnnotationPresent(Gateway.class)).map(this::analysis)
                 .collect(Collectors.toList());
         mete.forEach(m -> {
             m.setServiceType(type.isInterface() ? rpcService.ref() : type);
             m.setInterfaceType(type.isInterface() ? type : null);
             m.setName(rpcService.service());
             m.setService(id);
         });
         idServiceMap.put(id, mete);
     }

     private ApiMeteDate analysis(Method method) {
         Gateway gateway = method.getDeclaredAnnotation(Gateway.class);
         ApiMeteDate mete = ApiMeteDate.builder()
                 .url(gateway.url())
                 .type(gateway.type())
                 .methodName(method.getName())
                 .paramTypes(method.getParameterTypes())
                 .parmaNames(analysisParams(method))
                 .mete(analysisMate(method))
                 .build();
         LOGGER.info("方法解析 mete={}", mete);
         return mete;
     }

     private Map<String, Object> analysisMate(Method method) {
         HashMap<String, Object> map = new HashMap<>();
         convParamType(method.getParameterTypes(), map);
         return map;
     }

     private Class<?>[] convParamType(Class<?>[] paramTypes, Map<String, Object> mete) {
         Class<?>[] types = new Class<?>[paramTypes.length];
         Map<Integer, String> reTypes = new HashMap<>();
         for (int i = 0; i < paramTypes.length; i++) {
             types[i] = Constant.getType(paramTypes[i]);
             if (types[i].equals(Reference.class)) {
                 reTypes.put(i, paramTypes[i].getName());
             }
         }
         mete.put(Constant.TYPE_REF, reTypes);
         return types;
     }


     private String[] analysisParams(Method method) {
         Parameter[] parameters = method.getParameters();
         String[] pns = new String[parameters.length];
         int i = 0;
         for (Parameter parameter : parameters) {
             if (parameter.isAnnotationPresent(Query.class)) {
                 pns[i++] = Cons.QUERY + ":" + Optional.of(parameter.getDeclaredAnnotation(Query.class).value())
                         .filter(StringUtils::isNotBlank).orElse(parameter.getName());
             } else if (parameter.isAnnotationPresent(Body.class)) {
                 pns[i++] = Cons.BODY + ":" + Optional.of(parameter.getDeclaredAnnotation(Body.class).value())
                         .filter(StringUtils::isNotBlank).orElse(parameter.getName());
             } else if (parameter.isAnnotationPresent(Path.class)) {
                 pns[i++] = Cons.PATH + ":" + Optional.of(parameter.getDeclaredAnnotation(Path.class).value())
                         .filter(StringUtils::isNotBlank).orElse(parameter.getName());
             } else if (parameter.isAnnotationPresent(Header.class)) {
                 pns[i++] = Cons.HEADER + ":" + Optional.of(parameter.getDeclaredAnnotation(Header.class).value())
                         .filter(StringUtils::isNotBlank).orElse(parameter.getName());
             } else if (parameter.isAnnotationPresent(Param.class)) {
                 pns[i++] = Cons.PARAM + ":" + Optional.of(parameter.getDeclaredAnnotation(Param.class).value())
                         .filter(StringUtils::isNotBlank).orElse(parameter.getName());
             } else {
                 throw new CallException("no parameter type" + method);
             }
         }
         return pns;
     }

     @Override
     public void close() throws Exception {
         namingService.shutDown();
     }


 }
