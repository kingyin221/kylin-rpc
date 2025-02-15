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

 package club.kingyin.rpc.common.util;

 import club.kingyin.rpc.common.dtos.ResponseDTO;
 import io.protostuff.LinkedBuffer;
 import io.protostuff.ProtostuffIOUtil;
 import io.protostuff.runtime.RuntimeSchema;

 /**
  * Description:序列化、反序列化响应对象的工具
  *
  * @author: Lu ZePing
  * @date: 2020/9/29 14:03
  */
 public class ResponseSearialUtil {

     private static RuntimeSchema<ResponseDTO> schema = RuntimeSchema.createFrom(ResponseDTO.class);

     /**
      * 序列化方法，将Object对象序列化为字节数组
      *
      * @param response
      * @return
      */
     public static byte[] serialize(ResponseDTO response) {
         // Serializes the {@code message} into a byte array using the given schema
//         return JSON.toJSONBytes(response);
         return ProtostuffIOUtil.toByteArray(response, schema, LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE));
     }

     /**
      * 反序列化方法，将字节数组反序列化为Object对象
      *
      * @param array
      * @return
      */
     public static ResponseDTO deserialize(byte[] array) {
//         return JSON.parseObject(array, ResponseDTO.class);
         ResponseDTO response = schema.newMessage();
         // Merges the {@code message} with the byte array using the given {@code schema}
         ProtostuffIOUtil.mergeFrom(array, response, schema);
         return response;
     }
 }
