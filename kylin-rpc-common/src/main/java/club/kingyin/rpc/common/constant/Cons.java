
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

package club.kingyin.rpc.common.constant;

/**
 * Description:常量类
 * 牺牲微小的内存占用，提高可读、可维护性。
 * 别的类用到这个类里的常量，在编译的时候就会把这个类里的常量编译到那个类的class常量池中。
 *
 * @author: Zeping Lu
 * @date: 2021/1/18 14:49
 */
public class Cons {
    public static final String TIMEOUT = "timeout";
    public static final String EXCEPTION = "exç";
    public static final int THREE = 3;
    public static final int MIN_PORT = 1024;
    public static final int MAX_PORT = 49152;
    public static final String DOCKER_NAME = "docker";
    public static final String K8S_NAME = "flannel";
    public static final String COMMA = ",";
    public static final char COLON = ':';
    public static final String BODY = "body";
    public static final String QUERY = "query";
    public static final String PATH = "path";
    public static final String HEADER = "header";
    public static final String PARAM = "param";
    public static final String NACOS = "nacos";
    public static final String REDIS = "redis";
    public static final String REGISTRY = "registry";
    public static final String REGISTRY_IP_LIST = "registryIpList";
    public static final String REGISTRY_API = "register-api";
    public static final String RPC_START_TIMER = "PRC_START_TIMER";
    public static final String INVOKE_START_TIMER = "INVOKE_START_TIMER";
    public static final String RPC_STOP_TIMER = "PRC_START_TIMER";
    public static final String INVOKE_STOP_TIMER = "INVOKE_STOP_TIMER";
}
