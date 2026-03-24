package com.dustopus.infinitechat.offline.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dustopus.infinitechat.offline.model.OfflineMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OfflineMessageMapper extends BaseMapper<OfflineMessage> {
}
