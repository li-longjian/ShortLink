package org.llj.shortlink.project.mq.consumer;

import lombok.RequiredArgsConstructor;
import org.llj.shortlink.project.dto.biz.ShortLinkStatsRecordDTO;
import org.llj.shortlink.project.service.ShortLinkService;
import org.redisson.api.RBlockingDeque;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.locks.LockSupport;

import static org.llj.shortlink.project.common.constant.RedisKey.DELAY_QUEUE_STATS_KEY;

/**
 * 延迟记录短链接统计数据组件：
 * 为什么要延迟记录？ ：当短连接进行更新时，会加写锁，此时读任务会阻塞，
 * 但是为了使用户体验更好，可以把读任务（获取统计数据）放到延时队列中，待写锁解除后进行读操作
 * 这里暂时使用了redission实现的延迟队列，后面可以优化为 mq中延迟队列
 */
@Component
@RequiredArgsConstructor
public class DelayShortLinkStatsConsumer  implements InitializingBean {
    private final RedissonClient redisson;
    private final ShortLinkService shortLinkService;
    /**
     * 对延迟队列中的消息进行消费
     */
    public void onMessage(){
        Executors.newSingleThreadExecutor(
                runnable ->{
                    Thread thread = new Thread(runnable);
                    thread.setDaemon(true);//线程被设置为守护线程
                    thread.setName("DelayShortLinkStatsConsumer");
                    return thread;
                }
        )
                .execute(() -> {
                    RBlockingDeque<ShortLinkStatsRecordDTO> blockingDeque = redisson.getBlockingDeque(DELAY_QUEUE_STATS_KEY);
                    RDelayedQueue<ShortLinkStatsRecordDTO> delayedQueue = redisson.getDelayedQueue(blockingDeque);
                    for(;;){
                        try{
                            ShortLinkStatsRecordDTO statsRecord = delayedQueue.poll();
                            if(statsRecord != null){
                                shortLinkService.shortLinkStats(null,null,statsRecord);
                                continue;
                            }
                            LockSupport.park(500);
                        }catch (Throwable ignored){

                        }
                    }
                });
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        onMessage();
    }


}
