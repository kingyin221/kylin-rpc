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

 package club.kingyin.rpc.common.exception;

 import java.io.Serializable;

 /**
  * Description:远程调用出现异常,当远程方法抛出异常后,会返回这个异常
  *
  * @author: Zeping Lu
  * @date: 2020/10/16 20:40
  */
 public class RemoteException extends RuntimeException implements Serializable {
     private static final long serialVersionUID = -1070127031948629652L;

     public RemoteException(String message) {
         super(message);
     }
 }
