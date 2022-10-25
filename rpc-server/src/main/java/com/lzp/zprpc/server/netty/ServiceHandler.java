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

 import com.lzp.zprpc.common.api.ApiMeteDate;
 import com.lzp.zprpc.common.constant.Cons;
 import com.lzp.zprpc.common.constant.RpcEnum;
 import com.lzp.zprpc.common.dtos.RequestDTO;
 import com.lzp.zprpc.common.dtos.ResponseDTO;
 import com.lzp.zprpc.registry.api.RegistryClient;
 import com.lzp.zprpc.registry.nacos.NacosClient;
 import com.lzp.zprpc.common.util.PropertyUtil;
 import com.lzp.zprpc.common.util.RequestSearialUtil;
 import com.lzp.zprpc.common.util.ResponseSearialUtil;
 import com.lzp.zprpc.common.util.ThreadFactoryImpl;
 import com.lzp.zprpc.registry.redis.RedisClient;
 import io.netty.channel.ChannelHandlerContext;
 import io.netty.channel.SimpleChannelInboundHandler;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import com.lzp.zprpc.server.util.LogoUtil;

 import java.lang.reflect.Method;
 import java.util.*;
 import java.util.concurrent.*;
 import java.util.stream.Collectors;

 /**
  * Description:根据消息调用相应服务的handler
  *
  * @author: Lu ZePing
  * @date: 2020/9/29 21:31
  */
 public class ServiceHandler extends SimpleChannelInboundHandler<byte[]> {
     private static final Logger LOGGER = LoggerFactory.getLogger(ServiceHandler.class);

     private static Map<String, Object> idServiceMap;

     private static ExecutorService serviceThreadPool;

     private static Map<MethodMete, Object> services = new ConcurrentHashMap<>();

     private static void startUpService(ApiMeteDate mete) {
         MethodMete methodMete = MethodMete.builder().methodName(mete.getMethodName())
                 .paramTypes(mete.getParamTypes()).service(mete.getService()).build();
         if (!services.containsKey(methodMete)) {
             Object service = null;
             try {
                 service = mete.getServiceType().newInstance();
             } catch (InstantiationException | IllegalAccessException e) {
                 LOGGER.error("服务实例话失败 service={}", mete.getService(), e);
             }
             services.put(methodMete, service);
             LOGGER.info("启动服务 service={}", service);
         }
     }

     private Map<String, List<ApiMeteDate>> apis(String service) {
         HashMap<String, List<ApiMeteDate>> res = new HashMap<>();
         idServiceMap.forEach((k,v)-> {
             if (k == null || k.equals(service) && v instanceof List) {
                 List<ApiMeteDate> as = ((List<?>) v).stream().filter(m -> m instanceof ApiMeteDate).map(m -> (ApiMeteDate) ((ApiMeteDate) m).clo()).collect(Collectors.toList());
                 res.put(k, as);
             }
         });
         return res;
     }

     private static MethodMete convApiMete(RequestDTO requestDTO) {
         return MethodMete.builder().methodName(requestDTO.getMethodName()).service(requestDTO.getService()).paramTypes(requestDTO.getParamTypes()).build();
     }

     @Override
     protected void channelRead0(ChannelHandlerContext channelHandlerContext, byte[] bytes) {
         serviceThreadPool.execute(() -> {
             RequestDTO requestDTO = RequestSearialUtil.deserialize(bytes);
             if (Cons.REGISTRY_API.equals(requestDTO.getMethodName())) {
                 LOGGER.info("获取方法表 service={}", requestDTO.getParams()[0]);
                 channelHandlerContext.writeAndFlush(ResponseSearialUtil.serialize(new ResponseDTO(apis((String) requestDTO.getParams()[0]),requestDTO.getThreadId())));
                 return;
             }
             try {

                 MethodMete methodMete = convApiMete(requestDTO);
                 Object service = services.get(methodMete);
                 Method method = service.getClass().getDeclaredMethod(methodMete.getMethodName(), methodMete.getParamTypes());
                 channelHandlerContext.writeAndFlush(ResponseSearialUtil.serialize(new ResponseDTO(method
                         .invoke(service, requestDTO.getParams()), requestDTO.getThreadId())));
             } catch (Exception e) {
                 channelHandlerContext.writeAndFlush(ResponseSearialUtil.serialize(new ResponseDTO(Cons.EXCEPTION + getDetailMsgOfException(e), requestDTO.getThreadId())));
             }
         });
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
     static RegistryClient regiService() {
         try {
             //默认用nacos做注册中心
             RegistryClient registryClient;
             switch (RegistryClient.TYPE) {
                 case Cons.NACOS: {
                     registryClient = new NacosClient();
                     break;
                 }
                 case Cons.REDIS: {
                     registryClient = new RedisClient();
                     break;
                 }
                 default:
                     registryClient = new NacosClient();
             }
             LogoUtil.printLogo();
             idServiceMap = registryClient.searchAndRegiInstance(PropertyUtil.getBasePack(), Server.getIp(), Server.getPort());
             // 非IOC框架需要手动注入
             idServiceMap.values().stream().filter(o->o instanceof List).collect(Collectors.toList()).forEach(s-> {
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

     private String getDetailMsgOfException(Throwable t) {
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
