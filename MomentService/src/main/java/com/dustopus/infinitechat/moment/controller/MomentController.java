package com.dustopus.infinitechat.moment.controller;

import com.dustopus.infinitechat.common.dto.moment.MomentDTO;
import com.dustopus.infinitechat.common.result.Result;
import com.dustopus.infinitechat.moment.service.MomentService;
import com.dustopus.infinitechat.moment.vo.CommentVO;
import com.dustopus.infinitechat.moment.vo.PublishMomentVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/moment")
@RequiredArgsConstructor
public class MomentController {

    private final MomentService momentService;

    /** 发布朋友圈 */
    @PostMapping("/publish")
    public Result<Long> publish(@RequestHeader("X-User-Id") Long userId,
                                 @RequestBody @Valid PublishMomentVO vo) {
        return Result.ok(momentService.publishMoment(userId, vo));
    }

    /** 获取朋友圈列表 */
    @GetMapping("/timeline")
    public Result<List<MomentDTO>> getTimeline(@RequestHeader("X-User-Id") Long userId,
                                                @RequestParam(required = false) Long lastMomentId,
                                                @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(momentService.getTimeline(userId, lastMomentId, pageSize));
    }

    /** 点赞 */
    @PostMapping("/{momentId}/like")
    public Result<?> like(@RequestHeader("X-User-Id") Long userId,
                           @PathVariable Long momentId) {
        momentService.likeMoment(userId, momentId);
        return Result.ok();
    }

    /** 取消点赞 */
    @DeleteMapping("/{momentId}/like")
    public Result<?> unlike(@RequestHeader("X-User-Id") Long userId,
                             @PathVariable Long momentId) {
        momentService.unlikeMoment(userId, momentId);
        return Result.ok();
    }

    /** 评论 */
    @PostMapping("/comment")
    public Result<?> comment(@RequestHeader("X-User-Id") Long userId,
                              @RequestBody @Valid CommentVO vo) {
        momentService.commentMoment(userId, vo);
        return Result.ok();
    }

    /** 获取评论列表 */
    @GetMapping("/{momentId}/comments")
    public Result<?> getComments(@PathVariable Long momentId) {
        return Result.ok(momentService.getComments(momentId));
    }

    /** 删除朋友圈 */
    @DeleteMapping("/{momentId}")
    public Result<?> delete(@RequestHeader("X-User-Id") Long userId,
                             @PathVariable Long momentId) {
        momentService.deleteMoment(userId, momentId);
        return Result.ok();
    }
}
