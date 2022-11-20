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
 package com.lzp.zprpc.common.util;

 import org.springframework.beans.BeansException;
 import org.springframework.context.ApplicationContext;
 import org.springframework.context.ApplicationContextAware;
 import org.springframework.stereotype.Component;

 import java.util.HashMap;
 import java.util.Map;

 /**
  * Description:如果项目用到了Spring，就共用Spring容器中的bean，如果找不到bean再自己初始化。
  *
  * @author: Lu ZePing
  * @date: 2020/9/28 19:23
  */
 @Component
 public class SpringUtils implements ApplicationContextAware {

     // todo Spring 自动装配
     private static ApplicationContext applicationContext;

     public static ApplicationContext getApplicationContext() {
         return applicationContext;
     }

     @Override
     public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
         SpringUtils.applicationContext = applicationContext;
     }


     public static <T> Map<String, T> getBeansOfType(Class<T> baseType) {
         if (applicationContext != null) {
             return getApplicationContext().getBeansOfType(baseType);
         } else {
             return new HashMap<>();
         }
     }

     /**
      * 获取类型为requiredType的对象
      *
      * @param clz 类型
      * @return bean
      * @throws org.springframework.beans.BeansException 找不到
      */
     public static <T> T getBean(Class<T> clz) throws BeansException {
         return (T) getApplicationContext().getBean(clz);
     }

 }

