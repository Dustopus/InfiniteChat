package com.dustopus.infinitechat.messaging.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dustopus.infinitechat.messaging.model.Message;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {
}
