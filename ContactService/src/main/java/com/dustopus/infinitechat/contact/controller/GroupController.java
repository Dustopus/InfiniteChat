package com.dustopus.infinitechat.contact.controller;

import com.dustopus.infinitechat.common.result.Result;
import com.dustopus.infinitechat.contact.service.GroupService;
import com.dustopus.infinitechat.contact.vo.AddGroupMembersRequest;
import com.dustopus.infinitechat.contact.vo.CreateGroupRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/group")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @PostMapping("/create")
    public Result<Long> createGroup(@RequestHeader("X-User-Id") Long userId,
                                     @RequestBody @Valid CreateGroupRequest request) {
        return Result.ok(groupService.createGroup(userId, request));
    }

    @PostMapping("/members/add")
    public Result<?> addMembers(@RequestHeader("X-User-Id") Long userId,
                                 @RequestBody @Valid AddGroupMembersRequest request) {
        groupService.addMembers(userId, request);
        return Result.ok();
    }

    @GetMapping("/{groupId}/info")
    public Result<Map<String, Object>> getGroupInfo(@PathVariable Long groupId) {
        return Result.ok(groupService.getGroupInfo(groupId));
    }

    @GetMapping("/my/list")
    public Result<List<Map<String, Object>>> getUserGroups(@RequestHeader("X-User-Id") Long userId) {
        return Result.ok(groupService.getUserGroups(userId));
    }

    @GetMapping("/{groupId}/members")
    public Result<List<Map<String, Object>>> getGroupMembers(@PathVariable Long groupId) {
        return Result.ok(groupService.getGroupMembers(groupId));
    }

    @PostMapping("/{groupId}/quit")
    public Result<?> quitGroup(@RequestHeader("X-User-Id") Long userId,
                                @PathVariable Long groupId) {
        groupService.quitGroup(userId, groupId);
        return Result.ok();
    }

    @DeleteMapping("/{groupId}/dissolve")
    public Result<?> dissolveGroup(@RequestHeader("X-User-Id") Long userId,
                                    @PathVariable Long groupId) {
        groupService.dissolveGroup(userId, groupId);
        return Result.ok();
    }
}
