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

 package com.lzp.zprpc.common.dtos;

 import lombok.AllArgsConstructor;
 import lombok.Builder;
 import lombok.Data;
 import lombok.NoArgsConstructor;

 import java.util.Map;

 /**
  * Description:RPC请求对象
  *
  * @author: Lu ZePing
  * @date: 2020/9/29 14:16
  */
 @Data
 @Builder
 @AllArgsConstructor
 @NoArgsConstructor
 public class RequestDTO {
     /**
      * 发起rpc请求的线程的线程id。
      * 不用包装类型原因：
      * 1为了性能，自动装箱需要new一次对象
      * 2这个对象只用作自己定义的底层协议，业务场景不会出现阿里规范里说的情况，。
      */
     private long threadId;
     /**
      * 被调用的方法
      */
     private String methodName;

     /**
      * 方法参数类型
      */
     private Class<?>[] paramTypes;

     /**
      * 调用参数
      */
     private Object[] params;
     /**
      * 调用的服务
      */
     private String service;

     /**
      * 元数据
      */
     private Map<String, Object> mete;

 }
