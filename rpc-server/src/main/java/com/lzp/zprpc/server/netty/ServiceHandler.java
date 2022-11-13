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

 package com.lzp.zprpc.server.netty;

 import com.alibaba.fastjson2.JSON;
 import com.lzp.zprpc.common.api.Api;
 import com.lzp.zprpc.common.api.ApiMeteDate;
 import com.lzp.zprpc.common.api.RpcRequest;
 import com.lzp.zprpc.common.api.constant.Constant;
 import com.lzp.zprpc.common.constant.Cons;
 import com.lzp.zprpc.common.dtos.RequestDTO;
 import com.lzp.zprpc.common.dtos.ResponseDTO;
 import com.lzp.zprpc.common.exception.CallException;
 import com.lzp.zprpc.common.exception.ServiceException;
 import com.lzp.zprpc.common.filter.CalculateServiceMeteFilter;
 import com.lzp.zprpc.common.filter.LinksFilter;
 import com.lzp.zprpc.common.filter.LoggerFilter;
 import com.lzp.zprpc.common.filter.RpcFilter;
 import com.lzp.zprpc.common.util.RequestSearialUtil;
 import com.lzp.zprpc.common.util.ResponseSearialUtil;
 import com.lzp.zprpc.common.util.SpringUtils;
 import com.lzp.zprpc.common.util.ThreadFactoryImpl;
 import com.lzp.zprpc.registry.api.RegistryClient;
 import com.lzp.zprpc.registry.nacos.NacosClient;
 import com.lzp.zprpc.server.util.LogoUtil;
 import io.netty.channel.ChannelHandlerContext;
 import io.netty.channel.SimpleChannelInboundHandler;
 import org.apache.commons.lang3.ObjectUtils;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.springframework.core.annotation.AnnotationUtils;
 import org.springframework.stereotype.Component;
 import org.springframework.stereotype.Service;

 import java.lang.reflect.InvocationTargetException;
 import java.lang.reflect.Method;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 import java.util.concurrent.*;
 import java.util.stream.Collectors;


 /**
  * Description:根据消息调用相应服务的handler
  *
  * @author: Lu ZePing
  * @date: 2020/9/29 21:31
  */
 @Service
 public class ServiceHandler extends SimpleChannelInboundHandler<byte[]> {
     private static final Logger LOGGER = LoggerFactory.getLogger(ServiceHandler.class);

     private static Map<String, Object> idServiceMap;

     private static ExecutorService serviceThreadPool;

     private static final Map<Class<?>, Object> services = new ConcurrentHashMap<>();

     private static final Map<MethodMete, Class<?>> serviceClassMap = new ConcurrentHashMap<>();

     private static final Map<Api, MethodMete> apiService = new ConcurrentHashMap<>();

     private static RpcFilter filters;

     private static RpcFilter preFilter;

     public static void filter(RpcFilter filter) {
         if (filters == null) {
             filters = filter;
             preFilter = filter;
         } else {
             preFilter.next(filter);
             RpcFilter cur = filter;
             while (cur != null && cur.next() != null) cur = filter.next();
             preFilter = cur;
         }
     }

     static {
         filter(new LinksFilter());
         filter(new CalculateServiceMeteFilter());
         filter(new LoggerFilter());
     }

     private static void startUpService(ApiMeteDate mete) {
         MethodMete methodMete = MethodMete.builder().methodName(mete.getMethodName())
                 .paramTypes(mete.getParamTypes()).service(mete.getService()).build();
         if (!serviceClassMap.containsKey(methodMete)) {
             serviceClassMap.put(methodMete, mete.getServiceType());
             if (!services.containsKey(mete.getServiceType())) {
                 Object service = null;
                 try {
                     if (ObjectUtils.isNotEmpty(AnnotationUtils.findAnnotation(mete.getServiceType(), Component.class))) {
                         service = SpringUtils.getBean(mete.getServiceType());
                         LOGGER.info("从SpringIOC构建服务 service={}", service);
                     } else {
                         service = mete.getServiceType().newInstance();
                         LOGGER.info("主动构建服务 service={}", service);
                     }
                 } catch (InstantiationException | IllegalAccessException e) {
                     LOGGER.error("服务获取失败 service={}", mete.getService(), e);
                 }
                 services.put(mete.getServiceType(), service);
                 apiService.put(new Api(mete.getUrl(), mete.getType(), mete.getParmaNames()), methodMete);
             }
         }
     }

     public static void main(String[] args) {
         System.out.println();
     }

     private Map<String, List<ApiMeteDate>> apis(String service) {
         HashMap<String, List<ApiMeteDate>> res = new HashMap<>();
         idServiceMap.forEach((k, v) -> {
             if (k == null || k.equals(service) && v instanceof List) {
                 List<ApiMeteDate> as = ((List<?>) v).stream().filter(m -> m instanceof ApiMeteDate).map(m -> (ApiMeteDate) ((ApiMeteDate) m).clo()).collect(Collectors.toList());
                 res.put(k, as);
             }
         });
         System.out.println(res);
         return res;
     }

     private static MethodMete convApiMete(RequestDTO requestDTO) throws ClassNotFoundException {
         if (requestDTO.getMete().containsKey(Constant.TYPE_REF)) {
             Map<Integer, String> reTypes = (Map<Integer, String>) requestDTO.getMete().get(Constant.TYPE_REF);
             for (Integer site : reTypes.keySet()) {
                 requestDTO.getParamTypes()[site] = Class.forName(reTypes.get(site));
             }
         }
         return MethodMete.builder().methodName(requestDTO.getMethodName()).service(requestDTO.getService()).paramTypes(requestDTO.getParamTypes()).build();
     }

     @Override
     protected void channelRead0(ChannelHandlerContext channelHandlerContext, byte[] bytes) {
         serviceThreadPool.execute(() -> {
             RequestDTO requestDTO = RequestSearialUtil.deserialize(bytes);
             if (Cons.REGISTRY_API.equals(requestDTO.getMethodName())) {
                 LOGGER.info("获取方法表 service={}", requestDTO.getParams()[0]);
                 channelHandlerContext.writeAndFlush(ResponseSearialUtil.serialize(new ResponseDTO(apis((String) requestDTO.getParams()[0]), requestDTO.getThreadId())));
                 return;
             }
             try {
                 Object res = callService(requestDTO);
                 channelHandlerContext.writeAndFlush(ResponseSearialUtil.serialize(new ResponseDTO(res, requestDTO.getThreadId())));
             } catch (Exception e) {
                 ServiceException exception;
                 if (e instanceof ServiceException) {
                     exception = (ServiceException) e;
                 } else {
                     exception = new ServiceException(null, getDetailMsgOfException(e));
                 }
                 channelHandlerContext.writeAndFlush(ResponseSearialUtil.serialize(new ResponseDTO(exception, requestDTO.getThreadId())));
             }
         });
     }

     public static Api get(Api api) {
         return apiService.keySet().stream().filter(api::equals).findFirst().orElse(null);
     }

     public static Object callService(RpcRequest request, Object[] args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
         if (request.isApi()) {
             Api api = (Api) request.key();
             MethodMete methodMete = apiService.get(api);
             Class<?> key = serviceClassMap.get(methodMete);
             if (ObjectUtils.isEmpty(key)) throw new CallException("not found url" + api.getUrl());
             Object service = services.get(key);
             Method method = service.getClass().getDeclaredMethod(methodMete.getMethodName(), methodMete.getParamTypes());
             return JSON.toJSON(method.invoke(service, args));

         }
         throw new CallException("call type error");
     }

     private static Object callService(RequestDTO requestDTO) {
         Object res;
         try {
             MethodMete methodMete = convApiMete(requestDTO);
             Class<?> key = serviceClassMap.get(methodMete);
             if (ObjectUtils.isEmpty(key)) throw new CallException("not found method=" + methodMete);
             Object service = services.get(key);
             Method method = service.getClass().getDeclaredMethod(methodMete.getMethodName(), methodMete.getParamTypes());
             filters.chainBefore(requestDTO);
             res = method.invoke(service, requestDTO.getParams());
             filters.chainAfter(requestDTO, res);
             if (Constant.INVOKE_API.equals(requestDTO.getMete().get(Constant.INVOKE_TYPE))) {
                 res = JSON.toJSON(res);
                 LOGGER.info("Gateway res={}", res);
             }
         } catch (Exception e) {
             if (e instanceof ServiceException) {
                 res = e;
             } else {
                 res = new ServiceException(null, getDetailMsgOfException(e));
             }
         }
         return res;
     }

     static void initServiceThreadPool() {
         int logicalCpuCore = Runtime.getRuntime().availableProcessors();
         //被调用的服务可能会涉及到io操作，所以核心线程数设置比逻辑处理器个数多点
         serviceThreadPool = new ThreadPoolExecutor(logicalCpuCore + 1, 2 * logicalCpuCore,
                 100, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100000),
                 new ThreadFactoryImpl("rpc service"), (r, executor) -> r.run());
     }

     static boolean shutDownServiceThreadPool(long timeToWait, TimeUnit unit) throws InterruptedException {
         serviceThreadPool.shutdown();
         return serviceThreadPool.awaitTermination(timeToWait, unit);
     }


     /**
      * @return 注册中心客户端
      */
     static RegistryClient regiService(String host, String basePack) {
         try {
             //默认用nacos做注册中心
             RegistryClient registryClient;
             switch (RegistryClient.TYPE) {
                 case Cons.NACOS: {
                     registryClient = new NacosClient(host);
                     break;
                 }
                 case Cons.REDIS: {
                     throw new RuntimeException("redis 注册中心暂不支持");
                 }
                 default:
                     registryClient = new NacosClient(host);
             }
             LogoUtil.printLogo();
             idServiceMap = registryClient.searchAndRegiInstance(basePack, Server.getIp(), Server.getPort());
             // 非IOC框架需要手动注入
             idServiceMap.values().stream().filter(o -> o instanceof List).flatMap(l -> ((List<?>) l).stream()).forEach(s -> {
                 if (s instanceof ApiMeteDate) {
                     ServiceHandler.startUpService((ApiMeteDate) s);
                 }
             });
             LOGGER.info("publish service successfully");
             return registryClient;
         } catch (Exception e) {
             LOGGER.error(e.getMessage(), e);
             return null;
         }
     }

     private static String getDetailMsgOfException(Throwable t) {
         Throwable th;
         do {
             th = t;
         } while ((t = t.getCause()) != null);
         return th.getMessage();
     }

     static Set<String> getRegisteredServices() {
         return idServiceMap.keySet();
     }
 }
