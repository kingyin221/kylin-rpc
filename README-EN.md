# zprpc
中文|[English](https://github.com/65487123/LxuCache-Client/blob/master/README-EN.md)
# Features
    A high-performance rpc framework, temporarily only supports nacos as the registry.
#### 特点：
    1、Simple configuration and easy to use.
    2、The application scenarios are rich, and the startup process does not rely on any components, such as tomcat,
	   spring (with or without these components, the usage is the same).
    3、High performance: network communication is based on netty, protostuff for serialization. The connection pool 
	   mechanism is realized, and multiple RPC calls can share a connection at the same time (non-blocking), 
	   singleton proxy object, code details pay attention to performance.                  
# 	使用方法：
### 一、Environment setup
    1、Pull the code to the local, execute the maven compilation command in the project root directory: mvn clean install
    2、Set up the nacos environment: find the nacos source code on github, pull it locally, compile and start.
     For those who are not familiar with the specific operation, please refer to the nacos official website 
   [nacos official website](https://nacos.io/zh-cn/docs/quick-start.html)
### 二、Create project, import dependencies, write configuration and code
    1、Create a new project and define a public interface for service providers and service consumers to rely on
    2、Create a service provider project, rely on the project that provides the interface, and import the maven dependency
    <dependency>
         <groupId>com.lzp.zprpc</groupId>
         <artifactId>artifacts</artifactId>
         <version>${project.version}</version>
    </dependency>
    3、Create an interface implementation class, implement specific methods of the interface, and add com.lzp.annotation.Service 
	annotation to the interface implementation class;
    The annotation has two parameters, namely the id of the service and the fully qualified name of the interface to be used. 
	The id of the service must be unique
    Example：@Service(id = "serviceImpl", interfaceValue = "xxx.xxx.xxx.Service")
    4、Add the configuration file: zprpc.properties under the resources package, and add configuration items. Two of them are mandatory
    (1)The path of the package to be scanned: basePack. Example: basePack=zprpc.demo.producer
    (2)nacos ip: nacosIpList. Example: nacosIpList=192.168.0.101:8848
    5、Start the service provider through code:
    Server.startRpcServer(ip,port);
    or
    Server.startRpcServer(port);
    Do not write ip, the default is the local ip.
    After the service provider is started, it scans the services modified by the @Service annotation, saves them locally (all singletons) 
	after initialization, and publishes the services to nacos.
    
    If the project uses spring and the service is also registered in the spring container, it is recommended to add @Import(SpringUtil.class) 
	to the spring boot class, the fully qualified name is com.lzp.util.SpringUtil.
    In this way, when publishing a service, you will first find it in the spring container. If there is a service instance in the spring 
	container, it will use the spring. If not, it will initialize one itself.
    7、Create service consumer projects, rely on projects that provide interfaces, and import dependencies
    <dependency>
         <groupId>com.lzp.zprpc</groupId>
         <artifactId>artifacts</artifactId>
         <version>${zprpc.version}</version>
    </dependency>
    8、To write a configuration file, one item must be written:
    nacos ip: nacosIpList. Example: nacosIpList=192.168.0.101:8848
    You can also configure the number of connections to the connection pool for each instance.
    Example: connetionPoolSize: 2
    Without configuration, the number in the default connection pool is one. That is, the consumer communicates with all services in a 
	certain service instance through this connection, but there will be no blockage.
    9.Get the proxy object, through the proxy object you can initiate a remote call, just like calling a local method
    ServiceFactory.getServiceBean(String serviceId,Class interfaceCls);
    serviceId is the unique id of the service, and interfaceCls is the Class object of the interface. Return an instance and force it to be an 
	interface type.
    Can also
    ServiceFactory.getServiceBean(String serviceId,Class interfaceCls,int timeOut);
    To obtain the proxy object, remote calls through this object will have a timeout limit, and a timeout exception will be thrown if no result 
	is returned after the specified number of seconds.

#### Main realization principle:
    Similar to other mainstream RPCs, the service provider registers the service in the registry, and the service consumer finds the corresponding 
	service in the registry, and then establishes a connection to initiate an rpc call. But for performance, this rpc has made many optimizations.
    1、Not every time rpc will re-establish the connection, the connection pool mechanism is implemented, and the connections in the connection pool 
	can be shared, not just take out one less, and then put in one more.It is a fixed number of connection pools, the number can be configured through 
	the configuration file. The connection in the connection pool is not fully established at one time, but after the client finds the corresponding 
	instance in the registry,Take it from the connection pool and find that the number of connection pools is not full, it will initialize a connection, 
	put it into the connection pool and return. The connection in the connection pool will have a heartbeat keep-alive mechanism. When the connection is 
	unavailable, it will be disconnected.Drop this connection to initiate a new connection and add it to the connection pool. The technical details 
	involve the issue of multi-thread concurrency, which is similar to the singleton mode, such as visibility, semi-initialization, etc.
    2、Not every rpc call will block a connection. Like http1.0, every http request creates a new connection, and then closes the connection after the 
	request is initiated. During this time, this connection only serves this one A http request. Although http1.1 has keepalive and pipeling mechanisms, 
	at the same time, the connection can only serve one http request, and the next http request must wait for the previous http request Will be sent back. 
	And my connection mechanism is at the same time, multiple rpc requests can share a connection without any blocking.
    3、When initiating rpc, it is not every time to find an instance in nacos based on serviceid. Only the first time I will go to nacos to find, find out 
	the instance will be cached locally, and then add nacos to monitor events, When there is an event, the cache of this instance will be refreshed.
    4、The client proxy object is a singleton, and only the first time it gets the service will be initialized, and then it will be stored in the container.
    5、Solve sticky package and unpack through custom protocol, protostuff for serial number
    