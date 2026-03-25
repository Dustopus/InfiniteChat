package com.dustopus.infinitechat.messaging.service;

import com.dustopus.infinitechat.messaging.vo.SendRedPacketVO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface RedPacketService {

    /** 发送红包 */
    Long sendRedPacket(Long senderId, SendRedPacketVO vo);

    /** 抢红包 */
    BigDecimal grabRedPacket(Long userId, Long packetId);

    /** 获取红包详情 */
    Map<String, Object> getRedPacketDetail(Long userId, Long packetId);

    /** 获取我的红包列表 */
    List<Map<String, Object>> getMyRedPackets(Long userId);

    /** 获取红包领取记录 */
    List<Map<String, Object>> getRedPacketRecords(Long packetId);
}
