package com.crossoverjie.cim.route.service.impl;

import com.crossoverjie.cim.common.core.proxy.RpcProxyManager;
import com.crossoverjie.cim.common.enums.StatusEnum;
import com.crossoverjie.cim.common.exception.CIMException;
import com.crossoverjie.cim.common.pojo.CIMUserInfo;
import com.crossoverjie.cim.common.pojo.RouteInfo;
import com.crossoverjie.cim.common.util.RouteInfoParseUtil;
import com.crossoverjie.cim.route.api.vo.req.ChatReqVO;
import com.crossoverjie.cim.route.api.vo.req.LoginReqVO;
import com.crossoverjie.cim.route.api.vo.res.CIMServerResVO;
import com.crossoverjie.cim.route.api.vo.res.RegisterInfoResVO;
import com.crossoverjie.cim.route.service.AccountService;
import com.crossoverjie.cim.route.service.UserInfoCacheService;
import com.crossoverjie.cim.server.api.ServerApi;
import com.crossoverjie.cim.server.api.vo.req.SendMsgReqVO;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static com.crossoverjie.cim.common.enums.StatusEnum.OFF_LINE;
import static com.crossoverjie.cim.route.constant.Constant.ACCOUNT_PREFIX;
import static com.crossoverjie.cim.route.constant.Constant.ROUTE_PREFIX;

/**
 * Function:
 *
 * @author crossoverJie
 * Date: 2018/12/23 21:58
 * @since JDK 1.8
 */
@Slf4j
@Service
public class AccountServiceRedisImpl implements AccountService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private UserInfoCacheService userInfoCacheService;

    @Autowired
    private OkHttpClient okHttpClient;

    @Override
    public RegisterInfoResVO register(RegisterInfoResVO info) {
        String key = ACCOUNT_PREFIX + info.getUserId();

        String name = redisTemplate.opsForValue().get(info.getUserName());
        if (null == name) {
            //为了方便查询，冗余一份
            redisTemplate.opsForValue().set(key, info.getUserName());
            redisTemplate.opsForValue().set(info.getUserName(), key);
        } else {
            long userId = Long.parseLong(name.split(":")[1]);
            info.setUserId(userId);
            info.setUserName(info.getUserName());
        }

        return info;
    }

    @Override
    public StatusEnum login(LoginReqVO loginReqVO) throws Exception {
        //再去Redis里查询
        String key = ACCOUNT_PREFIX + loginReqVO.getUserId();
        String userName = redisTemplate.opsForValue().get(key);
        if (null == userName) {
            return StatusEnum.ACCOUNT_NOT_MATCH;
        }

        if (!userName.equals(loginReqVO.getUserName())) {
            return StatusEnum.ACCOUNT_NOT_MATCH;
        }

        //登录成功，保存登录状态
        boolean status = userInfoCacheService.saveAndCheckUserLoginStatus(loginReqVO.getUserId());
        if (status == false) {
            //重复登录
            return StatusEnum.REPEAT_LOGIN;
        }

        return StatusEnum.SUCCESS;
    }

    @Override
    public void saveRouteInfo(LoginReqVO loginReqVO, String msg) throws Exception {
        String key = ROUTE_PREFIX + loginReqVO.getUserId();
        redisTemplate.opsForValue().set(key, msg);
    }

    @Override
    public Map<Long, CIMServerResVO> loadRouteRelated() {

        Map<Long, CIMServerResVO> routes = new HashMap<>(64);


        RedisConnection connection = redisTemplate.getConnectionFactory().getConnection();
        ScanOptions options = ScanOptions.scanOptions()
                .match(ROUTE_PREFIX + "*")
                .build();
        Cursor<byte[]> scan = connection.scan(options);

        while (scan.hasNext()) {
            byte[] next = scan.next();
            String key = new String(next, StandardCharsets.UTF_8);
            log.info("key={}", key);
            parseServerInfo(routes, key);

        }
        scan.close();

        return routes;
    }

    @Override
    public CIMServerResVO loadRouteRelatedByUserId(Long userId) {
        String value = redisTemplate.opsForValue().get(ROUTE_PREFIX + userId);

        if (value == null) {
            throw new CIMException(OFF_LINE);
        }

        RouteInfo parse = RouteInfoParseUtil.parse(value);
        CIMServerResVO cimServerResVO = new CIMServerResVO(parse.getIp(), parse.getCimServerPort(), parse.getHttpPort());
        return cimServerResVO;
    }

    private void parseServerInfo(Map<Long, CIMServerResVO> routes, String key) {
        long userId = Long.valueOf(key.split(":")[1]);
        String value = redisTemplate.opsForValue().get(key);
        RouteInfo parse = RouteInfoParseUtil.parse(value);
        CIMServerResVO cimServerResVO = new CIMServerResVO(parse.getIp(), parse.getCimServerPort(), parse.getHttpPort());
        routes.put(userId, cimServerResVO);
    }


    @Override
    public void pushMsg(CIMServerResVO cimServerResVO, long sendUserId, ChatReqVO groupReqVO) throws Exception {
        CIMUserInfo cimUserInfo = userInfoCacheService.loadUserInfoByUserId(sendUserId);

        String url = "http://" + cimServerResVO.getIp() + ":" + cimServerResVO.getHttpPort();
        ServerApi serverApi = RpcProxyManager.create(ServerApi.class, url, okHttpClient);
        SendMsgReqVO vo = new SendMsgReqVO(cimUserInfo.getUserName() + ":" + groupReqVO.getMsg(), groupReqVO.getUserId());
        serverApi.sendMsg(vo);
    }

    @Override
    public void offLine(Long userId) throws Exception {

        // TODO: 2019-01-21 改为一个原子命令，以防数据一致性

        //删除路由
        redisTemplate.delete(ROUTE_PREFIX + userId);

        //删除登录状态
        userInfoCacheService.removeLoginStatus(userId);
    }
}
