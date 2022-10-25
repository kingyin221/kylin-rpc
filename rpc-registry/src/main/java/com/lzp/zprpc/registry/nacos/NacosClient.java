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

 package com.lzp.zprpc.registry.nacos;

 import com.alibaba.nacos.api.exception.NacosException;
 import com.alibaba.nacos.api.naming.NamingFactory;
 import com.alibaba.nacos.api.naming.NamingService;
 import com.lzp.zprpc.common.api.ApiMeteDate;
 import com.lzp.zprpc.common.api.annotation.Gateway;
 import com.lzp.zprpc.common.api.annotation.RpcService;
 import com.lzp.zprpc.common.annotation.Service;
 import com.lzp.zprpc.common.api.constant.Constant;
 import com.lzp.zprpc.common.api.constant.ServiceName;
 import com.lzp.zprpc.registry.api.RegistryClient;
 import com.lzp.zprpc.registry.util.ClazzUtils;
 import org.apache.commons.lang3.ObjectUtils;
 import org.apache.commons.lang3.StringUtils;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;

 import java.lang.reflect.Method;
 import java.util.*;
 import java.util.stream.Collectors;

 /**
  * Description:实现了统一接口的nacos客户端
  *
  * @author: Lu ZePing
  * @date: 2020/10/9 14:10
  */
 public class NacosClient implements RegistryClient {
     private static final Logger LOGGER = LoggerFactory.getLogger(NacosClient.class);

     NamingService namingService;

     {
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
         Map<String, Object> idServiceMap = new HashMap(16);
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


     private void regiInstanceIfNecessary(String ip, int port, Map<String, Object> idServiceMap, Class<?> cls) throws InstantiationException, IllegalAccessException, NacosException {
         if (cls.isAnnotationPresent(RpcService.class)) {
             RpcService rpcService = cls.getDeclaredAnnotation(RpcService.class);
             registerService(rpcService, cls, ip, port, idServiceMap);
         } else if (Arrays.stream(cls.getDeclaredMethods()).anyMatch(method -> method.isAnnotationPresent(Gateway.class))) {
             registerService(null, cls, ip, port, idServiceMap);
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
             LOGGER.info("解析服务 module={} name={}", module, name);
             id = ServiceName.FULL.serviceName(module, name);
         }
         LOGGER.info("id ={}", id);

         namingService.registerInstance(id, ip, port);
         // 方法解析
         List<ApiMeteDate> mete = Arrays.stream(type.getDeclaredMethods()).filter(ObjectUtils::isNotEmpty)
                 .filter(method -> method.isAnnotationPresent(Gateway.class)).map(this::analysis)
                 .collect(Collectors.toList());
         mete.forEach(m-> {
             m.setServiceType(type);
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
                 .build();
         LOGGER.info("方法解析 mete={}", mete);
         return mete;
     }

     public static void main(String[] args) {
         System.out.println(NacosClient.class.getSimpleName());
     }

     private String getId(Service service) {
         String name;
         String group;
         if ((name = service.name()).startsWith("$")) {
             name = System.getenv(name.substring(1));
         }
         if ((group = service.group()).startsWith("$")) {
             group = System.getenv(group.substring(1));
         }
         return name + "." + group;
     }

     @Override
     public void close() throws Exception {
         namingService.shutDown();
     }


 }
