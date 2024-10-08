package com.crossoverjie.cim.route.config;

import com.crossoverjie.cim.common.metastore.MetaStore;
import com.crossoverjie.cim.common.metastore.ZkConfiguration;
import com.crossoverjie.cim.common.metastore.ZkMetaStoreImpl;
import com.crossoverjie.cim.common.route.algorithm.RouteHandle;
import com.crossoverjie.cim.common.route.algorithm.consistenthash.AbstractConsistentHash;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.I0Itec.zkclient.ZkClient;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * Function:
 *
 * @author crossoverJie
 * Date: 2018/12/23 00:25
 * @since JDK 1.8
 */
@Configuration
@Slf4j
public class BeanConfig {


    @Autowired
    private AppConfiguration appConfiguration;


    @Bean
    public MetaStore metaStore() throws Exception {
        MetaStore metaStore = new ZkMetaStoreImpl();
        ExponentialBackoffRetry retryPolicy = new ExponentialBackoffRetry(1000, 3);
        metaStore.initialize(ZkConfiguration.builder()
                .metaServiceUri(appConfiguration.getZkAddr())
                .timeoutMs(appConfiguration.getZkConnectTimeout())
                .retryPolicy(retryPolicy)
                .build());
        metaStore.listenServerList((root, currentChildren) -> {
            log.info("Server list change, root=[{}], currentChildren=[{}]", root, currentChildren);
        });
        return metaStore;
    }


    /**
     * Redis bean
     *
     * @param factory
     * @return
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory factory) {
        StringRedisTemplate redisTemplate = new StringRedisTemplate(factory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }


    /**
     * http client
     *
     * @return okHttp
     */
    @Bean
    public OkHttpClient okHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true);
        return builder.build();
    }

    @Bean
    public RouteHandle buildRouteHandle() throws Exception {
        String routeWay = appConfiguration.getRouteWay();
        RouteHandle routeHandle = (RouteHandle) Class.forName(routeWay).newInstance();
        log.info("Current route algorithm is [{}]", routeHandle.getClass().getSimpleName());
        if (routeWay.contains("ConsistentHash")) {
            //一致性 hash 算法
            Method method = Class.forName(routeWay).getMethod("setHash", AbstractConsistentHash.class);
            AbstractConsistentHash consistentHash = (AbstractConsistentHash)
                    Class.forName(appConfiguration.getConsistentHashWay()).newInstance();
            method.invoke(routeHandle, consistentHash);
            return routeHandle;
        } else {

            return routeHandle;

        }

    }
}
