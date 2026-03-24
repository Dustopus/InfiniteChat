package com.dustopus.infinitechat.contact.controller;

import com.dustopus.infinitechat.common.result.Result;
import com.dustopus.infinitechat.contact.service.GroupService;
import com.dustopus.infinitechat.contact.vo.CreateGroupVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/group")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    /** 创建群聊 */
    @PostMapping("/create")
    public Result<Long> createGroup(@RequestHeader("X-User-Id") Long userId,
                                     @RequestBody @Valid CreateGroupVO vo) {
        return Result.ok(groupService.createGroup(userId, vo));
    }

    /** 获取群信息 */
    @GetMapping("/{groupId}")
    public Result<?> getGroupInfo(@PathVariable Long groupId) {
        return Result.ok(groupService.getGroupInfo(groupId));
    }

    /** 获取群成员列表 */
    @GetMapping("/{groupId}/members")
    public Result<?> getGroupMembers(@PathVariable Long groupId) {
        return Result.ok(groupService.getGroupMembers(groupId));
    }

    /** 获取用户加入的群列表 */
    @GetMapping("/my/list")
    public Result<?> getUserGroups(@RequestHeader("X-User-Id") Long userId) {
        return Result.ok(userId);
    }

    /** 邀请用户入群 */
    @PostMapping("/{groupId}/invite")
    public Result<?> inviteToGroup(@RequestHeader("X-User-Id") Long userId,
                                    @PathVariable Long groupId,
                                    @RequestBody List<Long> userIds) {
        groupService.inviteToGroup(userId, groupId, userIds);
        return Result.ok();
    }

    /** 退出群聊 */
    @PostMapping("/{groupId}/leave")
    public Result<?> leaveGroup(@RequestHeader("X-User-Id") Long userId,
                                 @PathVariable Long groupId) {
        groupService.leaveGroup(userId, groupId);
        return Result.ok();
    }

    /** 解散群聊 */
    @DeleteMapping("/{groupId}")
    public Result<?> dissolveGroup(@RequestHeader("X-User-Id") Long userId,
                                    @PathVariable Long groupId) {
        groupService.dissolveGroup(userId, groupId);
        return Result.ok();
    }
}
