package com.dustopus.infinitechat.contact.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dustopus.infinitechat.common.dto.user.UserDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserSearchMapper extends BaseMapper<UserDTO> {

    @Select("SELECT user_id, user_name, avatar, phone FROM user WHERE user_name LIKE CONCAT('%', #{keyword}, '%') OR phone LIKE CONCAT('%', #{keyword}, '%') LIMIT 20")
    List<UserDTO> searchUsers(@Param("keyword") String keyword);
}
