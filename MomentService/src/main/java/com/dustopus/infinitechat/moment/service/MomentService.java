package com.dustopus.infinitechat.moment.service;

import com.dustopus.infinitechat.common.dto.moment.MomentDTO;
import com.dustopus.infinitechat.moment.vo.CommentVO;
import com.dustopus.infinitechat.moment.vo.PublishMomentVO;

import java.util.List;

public interface MomentService {

    /** 发布朋友圈 */
    Long publishMoment(Long userId, PublishMomentVO vo);

    /** 获取朋友圈列表（时间线） */
    List<MomentDTO> getTimeline(Long userId, Long lastMomentId, int pageSize);

    /** 点赞 */
    void likeMoment(Long userId, Long momentId);

    /** 取消点赞 */
    void unlikeMoment(Long userId, Long momentId);

    /** 评论 */
    void commentMoment(Long userId, CommentVO vo);

    /** 获取评论列表 */
    List<?> getComments(Long momentId);

    /** 删除朋友圈 */
    void deleteMoment(Long userId, Long momentId);
}
