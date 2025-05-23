package com.crossoverjie.cim.common.constant;

/**
 * Function:常量
 *
 * @author crossoverJie
 *         Date: 28/03/2018 17:41
 * @since JDK 1.8
 */
public class Constants {


    /**
     * 服务端手动 push 次数
     */
    public static final String COUNTER_SERVER_PUSH_COUNT = "counter.server.push.count" ;


    /**
     * 客户端手动 push 次数
     */
    public static final String COUNTER_CLIENT_PUSH_COUNT = "counter.client.push.count" ;

    public static class MetaKey {
        public static final String SEND_USER_ID = "sendUserId" ;
        public static final String SEND_USER_NAME = "sendUserName" ;
        public static final String RECEIVE_USER_ID = "receiveUserId" ;
        public static final String RECEIVE_USER_NAME = "receiveUserName" ;
    }

}
