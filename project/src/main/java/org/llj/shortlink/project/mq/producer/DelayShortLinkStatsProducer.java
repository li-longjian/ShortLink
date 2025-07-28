package org.llj.shortlink.project.mq.producer;

import lombok.RequiredArgsConstructor;
import org.llj.shortlink.project.dto.biz.ShortLinkStatsRecordDTO;
import org.redisson.api.RBlockingDeque;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

import static org.llj.shortlink.project.common.constant.RedisKey.DELAY_QUEUE_STATS_KEY;

@Component
@RequiredArgsConstructor
public class DelayShortLinkStatsProducer {

    private  final RedissonClient redisson;

    /**
     * 将短链接统计记录发送到 Redisson 的延迟队列中
     * @param record
     */
    public void send(ShortLinkStatsRecordDTO record) {
        RBlockingDeque<ShortLinkStatsRecordDTO> blockingDeque = redisson.getBlockingDeque(DELAY_QUEUE_STATS_KEY);
        redisson.getDelayedQueue(blockingDeque).offer(record,5, TimeUnit.SECONDS);
    }
}
