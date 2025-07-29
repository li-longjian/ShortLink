package org.llj.shortlink.project.mq.idempotent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageQueueIdempotentHandle {
    private final StringRedisTemplate stringRedisTemplate;
    private static final  String IDEMPOTENT_KEY_PREFIX = "shortlink:idempotent:";

    /**
     * 判断当前消息是否未被消费过
     * @param messageId
     * @return
     */
    public boolean isMessageNotProcessed(String messageId){
        String key = IDEMPOTENT_KEY_PREFIX + messageId;
        // 0: 未消费，
        //通过set nx ： 如果不存在key，返回true,否则返回false
        return Boolean.TRUE.equals(stringRedisTemplate.opsForValue().setIfAbsent(key,"0",2, TimeUnit.MINUTES));
    }

    /**
     * 判断当前消息状态， 是否已经完成
     * @param messageId
     * @return
     */
    public boolean isMessageAccomplished(String messageId){
        String key = IDEMPOTENT_KEY_PREFIX + messageId;
        return Objects.equals(stringRedisTemplate.opsForValue().get(key), "1");
    }
    /**
     * 将当前消息设置为已完成状态
     * @param messageId
     */
    public void setAccomplished(String messageId){
        String key = IDEMPOTENT_KEY_PREFIX + messageId;
        stringRedisTemplate.opsForValue().setIfAbsent(key,"1",2, TimeUnit.MINUTES);
    }

    /**
     * 消息处理过程中遇到异常，删除幂等标识
     * @param messageId
     */
    public void delMessageProcessed(String messageId){
        String key = IDEMPOTENT_KEY_PREFIX + messageId;
        stringRedisTemplate.delete(key);
    }
}
