package org.llj.shortlink.project.mq.consumer;

import com.alibaba.fastjson.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.llj.shortlink.project.common.Exception.ServiceException;
import org.llj.shortlink.project.common.constant.RocketMQConstant;
import org.llj.shortlink.project.dto.biz.ShortLinkStatsRecordDTO;
import org.llj.shortlink.project.mq.domain.MessageWrapper;
import org.llj.shortlink.project.mq.idempotent.MessageQueueIdempotentHandle;
import org.llj.shortlink.project.service.ShortLinkService;
import org.springframework.stereotype.Component;
/**
 * 使用Rocket MQ 定义 延迟队列， 对统计数据进行延迟处理
 *
 */

@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = RocketMQConstant.DELAY_STATS_TOPIC_KEY,
        selectorExpression = RocketMQConstant.DELAY_STATS_TAG_KEY,
        consumerGroup = RocketMQConstant.DELAY_STATS_CG_KEY
)
public class DelayShortLinkStatsMQConsumer implements RocketMQListener<MessageWrapper<ShortLinkStatsRecordDTO>> {

    private final MessageQueueIdempotentHandle messageQueueIdempotentHandle;
    private final ShortLinkService shortLinkService;
    @Override
    public void onMessage(MessageWrapper<ShortLinkStatsRecordDTO> message) {
        //1. 进行判断，当前消息是否没有被消费过， 防止消息重复消费问题
        if(!messageQueueIdempotentHandle.isMessageNotProcessed(message.getUuid())){
            //若消息已经被记录消费过, 获取消息状态
            if(messageQueueIdempotentHandle.isMessageAccomplished(message.getUuid())){
                //已经被消费且状态已完成，直接返回
                return;
            }
            //否则，尽管被消费过，但是在消费过程中存在异常，消费未完成，需要消息队列重试
            throw  new ServiceException("延迟队列数据统计消息未完成，需要消息队列重试");

        }
        //2. 未被消费过
        ShortLinkStatsRecordDTO statsRecordDTO = message.getMessage();
        try{
            log.info("[延迟队列访问统计Consumer] 开始消费：{}", JSON.toJSONString(message));
            shortLinkService.shortLinkStats(null,null,statsRecordDTO);
        }catch (Throwable e){
            log.error("[延迟队列访问统计Consumer] 短链接：{} 访问统计服务失败", statsRecordDTO.getFullShortUrl(), e);
            //消息处理过程中发生异常
            messageQueueIdempotentHandle.delMessageProcessed(message.getUuid());//删除消息处理过记录
            throw  new ServiceException("延迟队列数据统计消息未完成，需要消息队列重试");
        }
        //3. 一切正常，标记已完成
        messageQueueIdempotentHandle.setAccomplished(message.getUuid());
    }
}
