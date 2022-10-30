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

 package com.lzp.zprpc.client.nacos;


 import com.alibaba.nacos.api.exception.NacosException;
 import com.alibaba.nacos.api.naming.NamingFactory;
 import com.alibaba.nacos.api.naming.NamingService;
 import com.alibaba.nacos.api.naming.listener.NamingEvent;
 import com.alibaba.nacos.api.naming.pojo.Instance;
 import com.alibaba.nacos.api.naming.pojo.ListView;
 import com.google.common.collect.Lists;
 import com.lzp.zprpc.client.connectionpool.FixedShareableChannelPool;
 import com.lzp.zprpc.client.connectionpool.ServiceChannelPoolImp;
 import com.lzp.zprpc.client.connectionpool.SingleChannelPool;
 import com.lzp.zprpc.client.netty.ResultHandler;
 import com.lzp.zprpc.common.api.ApiMeteDate;
 import com.lzp.zprpc.common.constant.Cons;
 import com.lzp.zprpc.common.dtos.RequestDTO;
 import com.lzp.zprpc.common.exception.CallException;
 import com.lzp.zprpc.common.exception.RemoteException;
 import com.lzp.zprpc.common.exception.RpcTimeoutException;
 import com.lzp.zprpc.common.filter.CalculateClientMeteFilter;
 import com.lzp.zprpc.common.filter.LoggerFilter;
 import com.lzp.zprpc.common.filter.RpcFilter;
 import com.lzp.zprpc.common.util.PropertyUtil;
 import com.lzp.zprpc.common.util.RequestSearialUtil;
 import com.lzp.zprpc.common.util.ThreadFactoryImpl;
 import com.lzp.zprpc.registry.api.RegistryClient;
 import org.apache.commons.lang3.ObjectUtils;
 import org.apache.commons.lang3.StringUtils;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;

 import java.lang.reflect.Proxy;
 import java.net.ConnectException;
 import java.util.*;
 import java.util.concurrent.*;
 import java.util.concurrent.atomic.AtomicReference;
 import java.util.concurrent.locks.LockSupport;

 /**
  * Description:提供代理bean，用以远程调服务。代理bean是单例的
  *
  * @author: Lu ZePing
  * @date: 2020/9/27 18:32
  */
 public class ServiceFactory {
     private static final Logger LOGGER = LoggerFactory.getLogger(ServiceFactory.class);

     private static final Map<Service, List<ServiceMete>> services = new ConcurrentHashMap<>();
     private static final Map<Api, Service> apiServiceMap = new ConcurrentHashMap<>();
     private static final Map<Service, Api> serviceApiMap = new ConcurrentHashMap<>();
     private static NamingService naming;
     private static FixedShareableChannelPool channelPool;
     private static final AtomicReference<ServiceState> state = new AtomicReference<>(ServiceState.STOP);
     private static final ExecutorService registerThreadPool;

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

         registerThreadPool = new ThreadPoolExecutor(3, 3, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1000),
                 new ThreadFactoryImpl("rpc register"), (r, executor) -> r.run());
         filter(new CalculateClientMeteFilter());
         filter(new LoggerFilter());
         try {
             //如果需要查配置文件,必须打在同一classpath下(如果是OSGI环境,可以通过插件配置)
             naming = NamingFactory.createNamingService(RegistryClient.HOST);

             String connectionPoolSize;
             if ((connectionPoolSize = PropertyUtil.getConnetionPoolSize()) == null) {
                 channelPool = new SingleChannelPool();
             } else {
                 channelPool = new ServiceChannelPoolImp(Integer.parseInt(connectionPoolSize));
             }
         } catch (NacosException e) {
             LOGGER.error("Throw an exception when initializing NamingService", e);
         }
     }

     public static Object getService(String module, String name, Class<?> type, long timeout) {
         // 构建id
         String id = buildId(module, name);
         if (containsServiceId(id)) {
             registerById(id);
         }
         // 默认超时时间
         if (timeout == -1) {
             timeout = 10 * 60 * 1000;
         }
         return proxy(id, type, timeout);
     }

     private static String buildId(String module, String name) {
         return "[" + module + "]" + name;
     }

     private static void registerById(String id) {
         try {
             List<Instance> services = naming.selectInstances(id, true);
             // 注册服务商
             services.forEach(instance -> doRegister(id, instance));
         } catch (NacosException e) {
             e.printStackTrace();
         }
     }


     /**
      * Description:监听指定服务。当被监听的服务实例列表发生变化，更新本地缓存
      **/
     private static void addListener(String serviceId) throws NacosException {
         naming.subscribe(serviceId, event -> {
             if (event instanceof NamingEvent) {
                 for (Instance instance : ((NamingEvent) event).getInstances()) {
                     if (!instance.isHealthy()) {
                         // 服务生病了
                         ServiceMete mete = new ServiceMete(instance.getIp(), instance.getPort(), serviceId);
                         if (containsMete(mete)) {
                             LOGGER.info("服务下线 service={}", instance);
                             removeServiceMete(mete);
                             LOGGER.info("注册表 res={}", services);
                             LOGGER.info("api res={}", serviceApiMap);
                         }
                     } else if (instance.isHealthy()) {
                         ServiceMete mete = new ServiceMete(instance.getIp(), instance.getPort(), serviceId);
                         // 服务更新
                         if (!containsMete(mete))
                             doRegister(serviceId, instance);
                     }
                 }
             }
         });
     }

     private static boolean containsMete(ServiceMete mete) {
         return services.values().stream().flatMap(Collection::stream).anyMatch(mete::equals);
     }

     private static boolean containsServiceId(String id) {
         return services.values().stream().flatMap(Collection::stream).map(ServiceMete::getId).noneMatch(id::equals);
     }

     private static void removeServiceMete(ServiceMete mete) {
         Iterator<Service> iterator = services.keySet().iterator();
         Service service;
         List<ServiceMete> metes;
         while (iterator.hasNext()) {
             service = iterator.next();
             metes = services.get(service);
             if (ObjectUtils.isNotEmpty(mete)) {
                 if (metes.stream().map(ServiceMete::getId).anyMatch(mete.getId()::equals)) {
                     metes.remove(mete);
                     if (metes.isEmpty()) {
                         services.remove(service);
                         Api api = serviceApiMap.remove(service);
                         apiServiceMap.remove(api);
                     }
                 }
             }

         }
     }

     private static void doRegister(String serviceId, Instance instance) {
         String target = instance.getIp() + Cons.COLON + instance.getPort();
         LOGGER.info("注册服务 service={}", instance);
         RpcRequest rpcRequest = new RpcRequest.Builder().service(null, Cons.REGISTRY_API);
         Map<String, List<ApiMeteDate>> res = (Map<String, List<ApiMeteDate>>) callAndGetResult(target, rpcRequest, System.currentTimeMillis() + 1000, serviceId);
         if (ObjectUtils.isEmpty(res)) {
             return;
         }
         res.get(serviceId).forEach(v -> {
             if (services.containsKey(encoderApiMete(v))) {
                 services.get(encoderApiMete(v)).add(ServiceMete.builder().id(serviceId).ip(instance.getIp()).port(instance.getPort()).build());
             } else {
                 services.put(encoderApiMete(v), Lists.newArrayList(ServiceMete.builder().id(serviceId).ip(instance.getIp()).port(instance.getPort()).build()));
             }
         });
         LOGGER.info("注册表 reg={}", services);
         LOGGER.info("api res={}", serviceApiMap);

     }

     private static Service encoderApiMete(ApiMeteDate apiMeteDate) {
         Service service = new Service(apiMeteDate.getService(), apiMeteDate.getMethodName(), apiMeteDate.getParamTypes());
         if (StringUtils.isNotBlank(apiMeteDate.getUrl())) {
             Api api = new Api(apiMeteDate.getUrl(), apiMeteDate.getType(), apiMeteDate.getParmaNames());
             apiServiceMap.put(api, service);
             serviceApiMap.put(service, api);
         }
         return service;
     }

     private static Object proxy(String service, Class<?> interfaceCls, long timeout) {
         return Proxy.newProxyInstance(ServiceFactory.class.getClassLoader(),
                 new Class[]{interfaceCls}, (proxy, method, args) -> {
                     Object result = callAndGetResult(new RpcRequest.Builder().service(service, method.getName(), method.getParameterTypes()), timeout, args);
                     if (result instanceof String && ((String) result).startsWith(Cons.EXCEPTION)) {
                         String message;
                         if (Cons.TIMEOUT.equals(message = ((String) result).substring(Cons.THREE))) {
                             throw new RpcTimeoutException("rpc timeout");
                         } else {
                             throw new RemoteException(message);
                         }
                     }
                     return result;
                 });
     }

     private static void apiRegister(String id) throws NacosException {
         for (Instance instance : naming.selectInstances(id, true)) {
             doRegister(id, instance);
         }
     }

     public static void async() {
         while (state.get() != ServiceState.RUNNING) ;
     }

     public static void apiAccess() {

         if (state.get() != ServiceState.RUNNING) {
             synchronized (ServiceFactory.class) {
                 if (state.get() != ServiceState.RUNNING) {
                     LOGGER.info("启动");
                     registerThreadPool.execute(() -> {
                         try {
                             while (true) {
                                 int start = 1;
                                 int size = 50;
                                 int count = Integer.MAX_VALUE;
                                 while ((start * size) < count) {
                                     ListView<String> res = naming.getServicesOfServer(start, size);
                                     count = res.getCount();
                                     ++start;
                                     if (ObjectUtils.isNotEmpty(res.getData())) {
                                         for (String id : res.getData()) {
                                             if (containsServiceId(id)) {
                                                 // 新服务注册
                                                 apiRegister(id);
                                                 addListener(id);
                                             }
                                         }
                                     }
                                 }
                                 state.set(ServiceState.RUNNING);
                                 TimeUnit.SECONDS.sleep(5);
                             }
                         } catch (Exception e) {
                             LOGGER.error("服务发现异常", e);
                             state.set(ServiceState.ERROR);
                         }
                     });
                 }
             }

         }
     }

     private static Object callAndGetResult(List<ServiceMete> metes, RpcRequest rpcRequest, long deadline, Object... args) {
         try {
             //根据serviceid找到所有提供这个服务的ip+port
             Thread thisThread = Thread.currentThread();
             return doCall(metes, rpcRequest, deadline, thisThread, args);
         } catch (ConnectException e) {
             //当服务缩容时,服务关闭后,nacos没刷新(nacos如果不是高可用,可能会一直进入这里,直到超时)
             if (System.currentTimeMillis() > deadline) {
                 ResultHandler.reqIdThreadMap.remove(Thread.currentThread().getId());
                 return Cons.EXCEPTION + Cons.TIMEOUT;
             } else {
                 return callAndGetResult(rpcRequest, deadline, args);
             }
         } catch (IllegalArgumentException e) {
             ResultHandler.reqIdThreadMap.remove(Thread.currentThread().getId());
             throw new CallException("no service available");
         }
     }

     private static Object doCall(List<ServiceMete> metes, RpcRequest rpcRequest, long deadline, Thread thisThread, Object[] args) throws ConnectException {
         RequestDTO request = RequestDTO.builder().params(args).paramTypes(rpcRequest.getParamsType()).mete(rpcRequest.getMete())
                 .service(rpcRequest.getVar1()).methodName(rpcRequest.getVar2()).threadId(thisThread.getId()).build();
         ResultHandler.ThreadResultAndTime threadResultAndTime = new ResultHandler.ThreadResultAndTime(System.currentTimeMillis() + deadline, thisThread);
         ResultHandler.reqIdThreadMap.put(thisThread.getId(), threadResultAndTime);
         filters.chainBefore(request);
         ServiceMete serviceMete = metes.get(ThreadLocalRandom.current().nextInt(metes.size()));
         channelPool.getChannel(serviceMete.address())
                 .writeAndFlush(RequestSearialUtil.serialize(request));
         Object result;
         //用while，防止虚假唤醒
         while ((result = threadResultAndTime.getResult()) == null) {
             LockSupport.park();
         }
         filters.chainAfter(request, result);
         return result;
     }

     public static Object callAndGetResult(RpcRequest rpcRequest, long deadline, Object... args) {
         if (state.get() != ServiceState.RUNNING) {
             while (state.get() != ServiceState.RUNNING)
                 LOGGER.warn("服务暂不可用 state={}", state.get());
             return null;
         }
         try {
             //根据serviceid找到所有提供这个服务的ip+port
             Thread thisThread = Thread.currentThread();
             Object key = rpcRequest.key();
             List<ServiceMete> metes;
             if (rpcRequest.isApi()) {
                 Service service = apiServiceMap.get((Api) key);
                 metes = services.get(service);
             } else {
                 metes = services.get(((Service) key));
             }
             Optional.ofNullable(metes).filter(ObjectUtils::isNotEmpty).orElseThrow(() -> new CallException("无服务"));
             return doCall(metes, rpcRequest, deadline, thisThread, args);
         } catch (ConnectException e) {
             //当服务缩容时,服务关闭后,nacos没刷新(nacos如果不是高可用,可能会一直进入这里,直到超时)
             if (System.currentTimeMillis() > deadline) {
                 ResultHandler.reqIdThreadMap.remove(Thread.currentThread().getId());
                 return Cons.EXCEPTION + Cons.TIMEOUT;
             } else {
                 return callAndGetResult(rpcRequest, deadline, args);
             }
         } catch (IllegalArgumentException e) {
             ResultHandler.reqIdThreadMap.remove(Thread.currentThread().getId());
             throw new CallException("no service available");
         }
     }

     public static Object callAndGetResult(String hostAndPorts, RpcRequest rpcRequest, long deadline, Object... args) {
         if (rpcRequest.isApi() && state.get() != ServiceState.RUNNING) {
             LOGGER.warn("服务暂不可用 state={}", state.get());
             return null;
         }
         try {
             //根据serviceid找到所有提供这个服务的ip+port
             Thread thisThread = Thread.currentThread();
             Object key = rpcRequest.key();
             if (rpcRequest.isApi()) {
                 Service service = apiServiceMap.get((Api) key);
                 rpcRequest.conv(service);
             }
             RequestDTO request = RequestDTO.builder().params(args).paramTypes(rpcRequest.getParamsType()).mete(rpcRequest.getMete())
                     .service(rpcRequest.getVar1()).methodName(rpcRequest.getVar2()).threadId(thisThread.getId()).build();

             ResultHandler.ThreadResultAndTime threadResultAndTime = new ResultHandler.ThreadResultAndTime(System.currentTimeMillis() + deadline, thisThread);
             ResultHandler.reqIdThreadMap.put(thisThread.getId(), threadResultAndTime);
             filters.chainBefore(request);
             channelPool.getChannel(hostAndPorts)
                     .writeAndFlush(RequestSearialUtil.serialize(request));
             Object result;
             //用while，防止虚假唤醒
             while ((result = threadResultAndTime.getResult()) == null) {
                 LockSupport.park();
             }
             filters.chainAfter(request, result);
             return result;
         } catch (ConnectException e) {
             //当服务缩容时,服务关闭后,nacos没刷新(nacos如果不是高可用,可能会一直进入这里,直到超时)
             if (System.currentTimeMillis() > deadline) {
                 ResultHandler.reqIdThreadMap.remove(Thread.currentThread().getId());
                 return Cons.EXCEPTION + Cons.TIMEOUT;
             } else {
                 return callAndGetResult(hostAndPorts, rpcRequest, deadline, args);
             }
         } catch (IllegalArgumentException e) {
             ResultHandler.reqIdThreadMap.remove(Thread.currentThread().getId());
             throw new CallException("no service available");
         }
     }

 }
