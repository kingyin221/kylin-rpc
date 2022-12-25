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

 package club.kingyin.rpc.client.nacos;


 import club.kingyin.rpc.client.connectionpool.FixedShareableChannelPool;
 import club.kingyin.rpc.client.connectionpool.ServiceChannelPoolImp;
 import club.kingyin.rpc.client.connectionpool.SingleChannelPool;
 import club.kingyin.rpc.client.netty.ResultHandler;
 import club.kingyin.rpc.common.api.Api;
 import club.kingyin.rpc.common.api.ApiMeteDate;
 import club.kingyin.rpc.common.api.RpcRequest;
 import club.kingyin.rpc.common.api.Service;
 import club.kingyin.rpc.common.api.constant.HttpMethod;
 import club.kingyin.rpc.common.constant.Cons;
 import club.kingyin.rpc.common.dtos.RequestDTO;
 import club.kingyin.rpc.common.exception.BaseException;
 import club.kingyin.rpc.common.exception.CallException;
 import club.kingyin.rpc.common.exception.ServiceError;
 import club.kingyin.rpc.common.filter.BaseApiRpcFilter;
 import club.kingyin.rpc.common.filter.LinksClientFilter;
 import club.kingyin.rpc.common.filter.LoggerApiRpcFilter;
 import club.kingyin.rpc.common.filter.RpcFilter;
 import club.kingyin.rpc.common.filter.scanner.AutoScanner;
 import club.kingyin.rpc.common.filter.scanner.FilterClientAutoScanner;
 import club.kingyin.rpc.common.util.RequestSearialUtil;
 import club.kingyin.rpc.common.util.ThreadFactoryImpl;
 import club.kingyin.rpc.common.util.ThrowUtils;
 import com.alibaba.nacos.api.exception.NacosException;
 import com.alibaba.nacos.api.naming.NamingFactory;
 import com.alibaba.nacos.api.naming.NamingService;
 import com.alibaba.nacos.api.naming.listener.NamingEvent;
 import com.alibaba.nacos.api.naming.pojo.Instance;
 import com.alibaba.nacos.api.naming.pojo.ListView;
 import com.alibaba.nacos.shaded.com.google.common.collect.Lists;
 import org.apache.commons.lang3.ObjectUtils;
 import org.apache.commons.lang3.StringUtils;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.springframework.context.ApplicationContext;

 import java.lang.reflect.InvocationTargetException;
 import java.lang.reflect.Proxy;
 import java.net.ConnectException;
 import java.util.*;
 import java.util.concurrent.*;
 import java.util.concurrent.atomic.AtomicReference;
 import java.util.concurrent.locks.LockSupport;

 import static club.kingyin.rpc.common.util.ThrowUtils.getDetailMsgOfException;

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

     private static final Long TIMEOUT = 20 * 1000L;

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
                 new ThreadFactoryImpl("rpc-register"), (r, executor) -> r.run());
         filter(new LinksClientFilter());
         filter(new BaseApiRpcFilter());
         filter(new LoggerApiRpcFilter());

     }

     public static Map<Service, List<ServiceMete>> getServices() {
         return services;
     }

     public static Map<Api, Service> getApiServiceMap() {
         return apiServiceMap;
     }

     public static Map<Service, Api> getServiceApiMap() {
         return serviceApiMap;
     }

     public static void close() {
         if (state.get() == ServiceState.STOP) {
             LOGGER.info("服务已关闭");
             return;
         }
         if (state.get() != ServiceState.STOP) {
             synchronized (ServiceFactory.class) {
                 state.set(ServiceState.STOP);
             }
         }
     }

     public static void connection(String host, Integer poolSize) {
         if (state.get() == ServiceState.RUNNING) {
             LOGGER.info("服务已启动");
             return;
         }
         if (state.get() != ServiceState.STARING) {
             synchronized (ServiceFactory.class) {
                 try {
                     //如果需要查配置文件,必须打在同一classpath下(如果是OSGI环境,可以通过插件配置)
                     naming = NamingFactory.createNamingService(host);

                     if (poolSize == null) {
                         channelPool = new SingleChannelPool();
                     } else {
                         channelPool = new ServiceChannelPoolImp(poolSize);
                     }
                     state.set(ServiceState.STARING);
                 } catch (NacosException e) {
                     LOGGER.error("Throw an exception when initializing NamingService", e);
                 }
             }
         }
     }

     public static void scanFilter(String basePack, ApplicationContext context) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
         AutoScanner<RpcFilter> filterAutoScanner = new FilterClientAutoScanner();
         List<RpcFilter> rpcFilters;
         if (ObjectUtils.isEmpty(context)) {
             rpcFilters = filterAutoScanner.scanList(basePack);
         } else {
             rpcFilters = filterAutoScanner.scanList(basePack, context);
         }
         for (RpcFilter rpcFilter : rpcFilters) {
             LOGGER.info("注册过滤器{}", rpcFilter.getClass().getName());
             filter(rpcFilter);
         }
     }

     public static Object getService(String module, String name, Class<?> type, Long timeout) {
         // 构建id
         String id = buildId(module, name);
         if (containsServiceId(id)) {
             registerById(id);
         }
         // 默认超时时间
         if (ObjectUtils.isEmpty(timeout) || timeout <= 0) {
             timeout = TIMEOUT;
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

     public static Api match(String url, HttpMethod httpMethod, Map<String, Object> pathValues) {
         return apiServiceMap.keySet().stream().filter(a -> a.getHttpMethod().equals(httpMethod)).filter(a -> a.matchUri(url, pathValues)).findFirst().orElse(null);
     }


     /**
      * Description:监听指定服务。当被监听的服务实例列表发生变化，更新本地缓存
      **/
     public static void addListener(String serviceId) throws NacosException {
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
         Object result = callAndGetResult(target, rpcRequest, System.currentTimeMillis() + 1000, serviceId);
         boolean fault = ObjectUtils.isEmpty(result) || result instanceof Exception || result instanceof ServiceError;
         LOGGER.info("服务列表 result={} success={}", result, fault);
         if (fault) {
             LOGGER.warn("服务注册异常 {}", serviceId);
             return;
         }
         Map<String, List<ApiMeteDate>> res = (Map<String, List<ApiMeteDate>>) result;
         res.get(serviceId).forEach(v -> {
             if (services.containsKey(encoderApiMete(v))) {
                 services.get(encoderApiMete(v)).add(ServiceMete.builder().id(serviceId).ip(instance.getIp()).port(instance.getPort()).build());
                 LOGGER.info("方法副本注册 {}", v);
             } else {
                 services.put(encoderApiMete(v), Lists.newArrayList(ServiceMete.builder().id(serviceId).ip(instance.getIp()).port(instance.getPort()).build()));
                 LOGGER.info("方法注册 {}", v);
             }
         });
         LOGGER.info("服务注册成功 {}", serviceId);
//         LOGGER.info("注册表 reg={}", services);
//         LOGGER.info("api res={}", serviceApiMap);

     }

     private static Service encoderApiMete(ApiMeteDate apiMeteDate) {
         Service service = new Service(apiMeteDate.getService(), apiMeteDate.getMethodName(), apiMeteDate.getParamTypes(), apiMeteDate.getMete());
         if (StringUtils.isNotBlank(apiMeteDate.getUrl())) {
             Api api = new Api(apiMeteDate.getUrl(), apiMeteDate.getType(), apiMeteDate.getParmaNames());
             apiServiceMap.put(api, service);
             serviceApiMap.put(service, api);
         }
         return service;
     }

     public static Object proxy(String service, Class<?> interfaceCls, long timeout) {
         return Proxy.newProxyInstance(ServiceFactory.class.getClassLoader(),
                 new Class[]{interfaceCls}, (proxy, method, args) ->
                         callAndGetResult(new RpcRequest.Builder().service(service, method.getName(), method.getParameterTypes()), timeout, args));
     }

     public static Object proxyApi(String service, Class<?> interfaceCls, long timeout) {
         return Proxy.newProxyInstance(ServiceFactory.class.getClassLoader(),
                 new Class[]{interfaceCls}, (proxy, method, args) -> {
                     return callAndGetResult(new RpcRequest.Builder().service(service, method.getName(), method.getParameterTypes()), timeout, args);
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

     /**
      * 注意：如果此方法在apiAccess之前调用则不启动全接口同步
      */
     public static synchronized void startState() {
         state.set(ServiceState.RUNNING);
     }

     public static void apiAccess() {

         if (state.get() != ServiceState.RUNNING) {
             synchronized (ServiceFactory.class) {
                 if (state.get() != ServiceState.RUNNING) {
                     registerThreadPool.execute(() -> {
                         LOGGER.info("接口注册服务启动");
                         int stopCount = 0;
                         while (true) {
                             try {
                                 if (state.get() == ServiceState.STOP || stopCount == 1)
                                     return;
                                 if (!"UP".equals(naming.getServerStatus())) {
                                     LOGGER.info("注册中心连接已断开");
                                     stopCount = 1;
                                     TimeUnit.SECONDS.sleep(5);
                                     continue;
                                 }
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
                             } catch (Exception e) {
                                 LOGGER.error("服务发现异常 {}", ThrowUtils.getDetailMsgOfException(e), e);
                             }
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
                 return new ServiceError(101000L, "Service not available");
             } else {
                 return callAndGetResult(rpcRequest, deadline, args);
             }
         } catch (IllegalArgumentException e) {
             ResultHandler.reqIdThreadMap.remove(Thread.currentThread().getId());
             throw new CallException("no service available");
         }
     }

     private static Object doCall(List<ServiceMete> metes, RpcRequest rpcRequest, long deadline, Thread thisThread, Object[] args) throws ConnectException {
         Object res;
         try {
             RequestDTO request = RequestDTO.builder().params(args).paramTypes(rpcRequest.getParamsType()).mete(rpcRequest.getMete())
                     .service(rpcRequest.getVar1()).methodName(rpcRequest.getVar2()).threadId(thisThread.getId()).build();
             ResultHandler.ThreadResultAndTime threadResultAndTime = new ResultHandler.ThreadResultAndTime(System.currentTimeMillis() + deadline, thisThread);
             ResultHandler.reqIdThreadMap.put(thisThread.getId(), threadResultAndTime);
             filters.chainBefore(request);
             ServiceMete serviceMete = metes.get(ThreadLocalRandom.current().nextInt(metes.size()));
             channelPool.getChannel(serviceMete.address())
                     .writeAndFlush(RequestSearialUtil.serialize(request));
             //用while，防止虚假唤醒
             while (!threadResultAndTime.isFinished()) {
                 LockSupport.park();
             }
             filters.chainAfter(request, threadResultAndTime.getResponse());
             res = threadResultAndTime.getResult();
             return res;
         } catch (Exception e) {
             if (e instanceof InvocationTargetException) {
                 Throwable targetException = ((InvocationTargetException) e).getTargetException();
                 if (targetException instanceof BaseException) {
                     res = new ServiceError(((BaseException) targetException).getCode(), getDetailMsgOfException(e));
                 } else {
                     res = new ServiceError(102000L, getDetailMsgOfException(e));
                 }
                 LOGGER.warn("service-error", targetException);
             } else {
                 res = new ServiceError(102100L, getDetailMsgOfException(e));
             }
             LOGGER.warn("service-error", e);
         }
         return res;
     }

     public static Object callAndGetResult(RpcRequest rpcRequest, long deadline, Object... args) {
         if (state.get() != ServiceState.RUNNING) {
             int count = 0;
             while (state.get() != ServiceState.RUNNING || count++ < 5) {
                 LOGGER.warn("服务暂不可用 state={} retry={}", state.get(), count);
                 try {
                     TimeUnit.SECONDS.sleep(1);
                 } catch (InterruptedException e) {
                     e.printStackTrace();
                 }
             }
             return null;
         }
         try {
             //根据serviceid找到所有提供这个服务的ip+port
             Thread thisThread = Thread.currentThread();
             Object key = rpcRequest.key();
             List<ServiceMete> metes;
             if (rpcRequest.isApi()) {
                 Service service = apiServiceMap.get((Api) key);
                 rpcRequest.conv(service);
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
                 return new ServiceError(101000L, "Service not available");
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
         Object res;
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
             //用while，防止虚假唤醒
             while (!threadResultAndTime.isFinished()) {
                 LockSupport.park();
             }
             filters.chainAfter(request, threadResultAndTime.getResponse());
             res = threadResultAndTime.getResult();
         } catch (Exception e) {
             if (e instanceof ConnectException) {
                 //当服务缩容时,服务关闭后,nacos没刷新(nacos如果不是高可用,可能会一直进入这里,直到超时)
                 if (System.currentTimeMillis() > deadline) {
                     ResultHandler.reqIdThreadMap.remove(Thread.currentThread().getId());
                     return new ServiceError(101000L, "Service not available");
                 } else {
                     return callAndGetResult(hostAndPorts, rpcRequest, deadline, args);
                 }
             } else if (e instanceof IllegalArgumentException) {
                 ResultHandler.reqIdThreadMap.remove(Thread.currentThread().getId());
                 throw new CallException("no service available");
             } else {
                 res = new ServiceError(102100L, getDetailMsgOfException(e));
             }
             LOGGER.warn("service-error", e);
         }
         return res;
     }

 }
