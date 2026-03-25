package com.dustopus.infinitechat.messaging.controller;

import com.dustopus.infinitechat.common.result.Result;
import com.dustopus.infinitechat.messaging.service.RedPacketService;
import com.dustopus.infinitechat.messaging.vo.SendRedPacketVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/redpacket")
@RequiredArgsConstructor
public class RedPacketController {

    private final RedPacketService redPacketService;

    /** 发送红包 */
    @PostMapping("/send")
    public Result<Long> sendRedPacket(@RequestHeader("X-User-Id") Long userId,
                                      @RequestBody @Valid SendRedPacketVO vo) {
        return Result.ok(redPacketService.sendRedPacket(userId, vo));
    }

    /** 抢红包 */
    @PostMapping("/grab/{packetId}")
    public Result<BigDecimal> grabRedPacket(@RequestHeader("X-User-Id") Long userId,
                                            @PathVariable Long packetId) {
        return Result.ok(redPacketService.grabRedPacket(userId, packetId));
    }

    /** 获取红包详情 */
    @GetMapping("/{packetId}")
    public Result<Map<String, Object>> getRedPacketDetail(@RequestHeader("X-User-Id") Long userId,
                                                          @PathVariable Long packetId) {
        return Result.ok(redPacketService.getRedPacketDetail(userId, packetId));
    }

    /** 获取我的红包列表 */
    @GetMapping("/my")
    public Result<List<Map<String, Object>>> getMyRedPackets(@RequestHeader("X-User-Id") Long userId) {
        return Result.ok(redPacketService.getMyRedPackets(userId));
    }

    /** 获取红包领取记录 */
    @GetMapping("/{packetId}/records")
    public Result<List<Map<String, Object>>> getRedPacketRecords(@PathVariable Long packetId) {
        return Result.ok(redPacketService.getRedPacketRecords(packetId));
    }
}
