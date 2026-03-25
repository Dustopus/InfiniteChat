package com.dustopus.infinitechat.contact.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dustopus.infinitechat.contact.model.Contact;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ContactMapper extends BaseMapper<Contact> {
}
