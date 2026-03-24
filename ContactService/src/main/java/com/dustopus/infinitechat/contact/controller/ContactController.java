package com.dustopus.infinitechat.contact.controller;

import com.dustopus.infinitechat.common.dto.contact.ContactDTO;
import com.dustopus.infinitechat.common.result.Result;
import com.dustopus.infinitechat.contact.service.ContactService;
import com.dustopus.infinitechat.contact.vo.FriendApplyRequest;
import com.dustopus.infinitechat.contact.vo.FriendHandleRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/contact")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    @PostMapping("/friend/apply")
    public Result<?> applyFriend(@RequestHeader("X-User-Id") Long userId,
                                  @RequestBody @Valid FriendApplyRequest request) {
        contactService.applyFriend(userId, request);
        return Result.ok();
    }

    @PostMapping("/friend/handle")
    public Result<?> handleFriendRequest(@RequestHeader("X-User-Id") Long userId,
                                          @RequestBody @Valid FriendHandleRequest request) {
        contactService.handleFriendRequest(userId, request);
        return Result.ok();
    }

    @GetMapping("/friend/list")
    public Result<List<ContactDTO>> getContactList(@RequestHeader("X-User-Id") Long userId) {
        return Result.ok(contactService.getContactList(userId));
    }

    @GetMapping("/friend/requests")
    public Result<List<Object>> getFriendRequests(@RequestHeader("X-User-Id") Long userId) {
        return Result.ok(contactService.getFriendRequests(userId));
    }

    @DeleteMapping("/friend/{friendId}")
    public Result<?> deleteFriend(@RequestHeader("X-User-Id") Long userId,
                                   @PathVariable Long friendId) {
        contactService.deleteFriend(userId, friendId);
        return Result.ok();
    }

    @PutMapping("/friend/{friendId}/remark")
    public Result<?> updateRemark(@RequestHeader("X-User-Id") Long userId,
                                   @PathVariable Long friendId,
                                   @RequestParam String remark) {
        contactService.updateRemark(userId, friendId, remark);
        return Result.ok();
    }
}
