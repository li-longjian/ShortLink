package org.llj.shortlink.project.utils;


import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ThreadLocalRandom;

public class SetCacheTimeUtil {
    public static final long ONE_WEEK_SECONDS = 7 * 24 * 60 * 60;
    public static final long ONE_DAY_SECONDS = 24 * 60 * 60;

    /**
     * 获取短链接缓存的过期秒数
     *
     * 规则说明：
     * 1. 如果 date 超过当前时间一周，那么设置过期时间为一周，并随机增加一个 0~24 小时的时间，用于缓存雪崩防护
     * 2. 如果 date 没有超过当前时间一周，那么设置为指定的时间差值
     * 3. 如果 date 为 null，即短连接为长期有效，则设置过期时间为一周，并随机增加一个 0~24 小时的时间
     *
     * @param date 过期日期
     * @return 距离缓存过期的秒数
     */
    public static long getLinkCacheExpirationSeconds(LocalDateTime date) {
        LocalDateTime now = LocalDateTime.now();

        // 处理 date 为 null 的情况
        if (date == null) {
            return getRandomizedWeekExpiration();
        }



        long secondsUntilExpire = now.until(date, ChronoUnit.SECONDS);

        // 如果超过一周，限制为一周并添加随机偏移
        if (secondsUntilExpire > ONE_WEEK_SECONDS) {
            return getRandomizedWeekExpiration();
        }

        return secondsUntilExpire;
    }

    /**
     * 获取一周加上随机 0~24 小时的过期秒数
     */
    private static long getRandomizedWeekExpiration() {
        long randomOffset = ThreadLocalRandom.current().nextLong(0, ONE_DAY_SECONDS + 1);
        return ONE_WEEK_SECONDS + randomOffset;
    }
}
