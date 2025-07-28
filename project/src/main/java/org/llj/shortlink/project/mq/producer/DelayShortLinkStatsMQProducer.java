package org.llj.shortlink.project.mq.producer;


import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.llj.shortlink.project.common.constant.RocketMQConstant;
import org.llj.shortlink.project.dto.biz.ShortLinkStatsRecordDTO;
import org.llj.shortlink.project.mq.domain.MessageWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.UUID;
/**
 * 延时队列 短连接统计数据 消费者   Rocket MQ 实现
 */
@Slf4j
@Component
public class DelayShortLinkStatsMQProducer extends AbstractCommonSendProduceTemplate<ShortLinkStatsRecordDTO>{

    private final ConfigurableEnvironment environment;

    //构造注入
    public DelayShortLinkStatsMQProducer(@Autowired RocketMQTemplate rocketMQTemplate, @Autowired ConfigurableEnvironment environment) {
        super(rocketMQTemplate);
        this.environment = environment;
    }
   //设置消息的基本参数，包括事件名称、键（使用完整短链接）、主题和标签
    @Override
    protected BaseSendExtendDTO buildBaseSendExtendParam(ShortLinkStatsRecordDTO messageSendEvent) {
        return BaseSendExtendDTO.builder()
                .eventName("延迟访问统计")
                .keys(messageSendEvent.getFullShortUrl())//使用完整短链接作为消息键
                .topic(environment.resolvePlaceholders(RocketMQConstant.DELAY_STATS_TOPIC_KEY))
                .tag(environment.resolvePlaceholders(RocketMQConstant.DELAY_STATS_TAG_KEY))
                .sentTimeout(2000L)
                // RocketMQ 延迟消息级别 1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h
                .delayLevel(1)//延时1s
                .build();
    }

    @Override
    protected Message<?> buildMessage(ShortLinkStatsRecordDTO messageSendEvent, BaseSendExtendDTO requestParam) {
        String keys = StrUtil.isEmpty(requestParam.getKeys()) ? UUID.randomUUID().toString() : requestParam.getKeys();
        return MessageBuilder
                .withPayload(new MessageWrapper(requestParam.getKeys(), messageSendEvent))
                .setHeader(MessageConst.PROPERTY_KEYS, keys)
                .setHeader(MessageConst.PROPERTY_TAGS, requestParam.getTag())
                .build();
    }
}
