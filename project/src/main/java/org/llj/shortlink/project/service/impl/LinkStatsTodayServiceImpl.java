package org.llj.shortlink.project.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.llj.shortlink.project.dao.entity.LinkTodayStatsDO;
import org.llj.shortlink.project.dao.mapper.LinkTodayStatsMapper;
import org.llj.shortlink.project.service.LinkStatsTodayService;
import org.springframework.stereotype.Service;

@Service
public class LinkStatsTodayServiceImpl extends ServiceImpl<LinkTodayStatsMapper, LinkTodayStatsDO> implements LinkStatsTodayService {
}
