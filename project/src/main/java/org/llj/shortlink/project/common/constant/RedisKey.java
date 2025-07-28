package org.llj.shortlink.project.common.constant;


public class RedisKey {
    public static final String FULL_SHORT_URL_KEY = "shortlink:fullShortUrl:%s";
    public static final String LOCK_FULL_SHORT_URL_KEY = "shortlink:lock:fullShortUrl:%s";
    public static final String IS_NULL_FULL_SHORT_URL_KEY = "shortlink:isNull:fullShortUrl:%s";
    public static final String REDIS_UV_KEY = "shortlink:status:uv:" ;
    public static final String REDIS_UIP_KEY = "shortlink:status:uip:" ;

    /**
     * 短链接修改分组id锁前缀
     */
    public static final String LOCK_GID_UPDATE_KEY = "shortlink:lock:gid:%s";
    /**
     * 延迟队列消费统计 key
     */
    public static final String DELAY_QUEUE_STATS_KEY = "shortlink:delay_queue:stats";
}
