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

 package zprpc.demo.nacos.producer;

 import com.lzp.zprpc.common.api.annotation.Gateway;
 import com.lzp.zprpc.common.api.annotation.Query;
 import com.lzp.zprpc.common.api.annotation.RpcService;
 import com.lzp.zprpc.common.api.constant.HttpMethod;

 /**
  * Description:示例接口
  *
  * @author: Zeping Lu
  * @date: 2020/10/18 10:21
  */
 @RpcService(ref = DemoServiceImpl.class)
 public interface DemoService {

     @Gateway(url = "/test", type = HttpMethod.GET)
     String sayHello(@Query String name) throws InterruptedException;

     @Gateway(url = "/list", type = HttpMethod.GET)
     String listDevice(@Query Integer id);

 }