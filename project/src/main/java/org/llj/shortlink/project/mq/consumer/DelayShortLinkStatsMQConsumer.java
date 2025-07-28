package org.llj.shortlink.project.mq.consumer;

import com.alibaba.fastjson.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.llj.shortlink.project.common.constant.RocketMQConstant;
import org.llj.shortlink.project.dto.biz.ShortLinkStatsRecordDTO;
import org.llj.shortlink.project.mq.domain.MessageWrapper;
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


    private final ShortLinkService shortLinkService;
    @Override
    public void onMessage(MessageWrapper<ShortLinkStatsRecordDTO> message) {
        ShortLinkStatsRecordDTO statsRecordDTO = message.getMessage();
        try{
            log.info("[延迟访问统计Consumer] 开始消费：{}", JSON.toJSONString(message));
            shortLinkService.shortLinkStats(null,null,statsRecordDTO);
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }
}
