package com.spacemate.modules.client.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.spacemate.common.api.PageResponse;
import com.spacemate.common.cache.CacheKeys;
import com.spacemate.common.cache.HotKeyDetector;
import com.spacemate.common.exception.BusinessException;
import com.spacemate.domain.entity.AppUser;
import com.spacemate.domain.entity.SpaceComment;
import com.spacemate.domain.entity.SpaceCommentLike;
import com.spacemate.infrastructure.persistence.mapper.AppUserMapper;
import com.spacemate.infrastructure.persistence.mapper.SpaceCommentLikeMapper;
import com.spacemate.infrastructure.persistence.mapper.SpaceCommentMapper;
import com.spacemate.modules.client.dto.request.ClientCreateCommentRequest;
import com.spacemate.modules.client.dto.response.ClientCommentLikeResponse;
import com.spacemate.modules.client.dto.response.ClientCommentResponse;
import com.spacemate.modules.client.service.ClientCommentService;
import com.spacemate.modules.client.service.ClientSpaceService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class ClientCommentServiceImpl implements ClientCommentService {

    private final SpaceCommentMapper commentMapper;
    private final SpaceCommentLikeMapper likeMapper;
    private final AppUserMapper appUserMapper;
    private final ClientSpaceService clientSpaceService;
    private final StringRedisTemplate redisTemplate;
    private final Cache<String, PageResponse<ClientCommentResponse>> commentListCache;
    private final HotKeyDetector hotKeyDetector;
    private final ObjectMapper objectMapper;

    public ClientCommentServiceImpl(
        SpaceCommentMapper commentMapper,
        SpaceCommentLikeMapper likeMapper,
        AppUserMapper appUserMapper,
        ClientSpaceService clientSpaceService,
        StringRedisTemplate redisTemplate,
        @Qualifier("clientCommentListCache") Cache<String, PageResponse<ClientCommentResponse>> commentListCache,
        HotKeyDetector hotKeyDetector,
        ObjectMapper objectMapper
    ) {
        this.commentMapper = commentMapper;
        this.likeMapper = likeMapper;
        this.appUserMapper = appUserMapper;
        this.clientSpaceService = clientSpaceService;
        this.redisTemplate = redisTemplate;
        this.commentListCache = commentListCache;
        this.hotKeyDetector = hotKeyDetector;
        this.objectMapper = objectMapper;
    }

    /**
     * 查询某个空间下的评论树。
     *
     * <p>分页只作用在一级评论上，回复会跟随一级评论一起返回。这样前端展示会比较自然：
     * 先看到每条主评论，再看到它下面的回复。</p>
     */
    @Override
    public PageResponse<ClientCommentResponse> list(Long spaceId, String phone, long page, long size) {
        clientSpaceService.detail(spaceId);

        String cacheKey = commentListCacheKey(spaceId, page, size);
        hotKeyDetector.record(cacheKey);

        PageResponse<ClientCommentResponse> localCached = commentListCache.getIfPresent(cacheKey);
        if (localCached != null) {
            return enrichLikedState(localCached, phone);
        }

        PageResponse<ClientCommentResponse> cached = readCommentListFromRedis(cacheKey);
        if (cached != null) {
            commentListCache.put(cacheKey, cached);
            return enrichLikedState(cached, phone);
        }

        Page<SpaceComment> pageResult = commentMapper.selectPage(
            new Page<>(page, size),
            new LambdaQueryWrapper<SpaceComment>()
                .eq(SpaceComment::getSpaceId, spaceId)
                .eq(SpaceComment::getStatus, 1)
                .eq(SpaceComment::getParentId, 0L)
                .orderByDesc(SpaceComment::getCreatedAt)
                .orderByDesc(SpaceComment::getId)
        );

        List<SpaceComment> roots = pageResult.getRecords();
        if (roots.isEmpty()) {
            return new PageResponse<>(Collections.emptyList(), page, size, pageResult.getTotal());
        }

        List<Long> rootIds = roots.stream().map(SpaceComment::getId).collect(Collectors.toList());
        List<SpaceComment> replies = commentMapper.selectList(new LambdaQueryWrapper<SpaceComment>()
            .eq(SpaceComment::getSpaceId, spaceId)
            .eq(SpaceComment::getStatus, 1)
            .in(SpaceComment::getRootId, rootIds)
            .orderByAsc(SpaceComment::getCreatedAt)
            .orderByAsc(SpaceComment::getId));

        List<SpaceComment> all = new ArrayList<>();
        all.addAll(roots);
        all.addAll(replies);

        Map<Long, AppUser> userMap = buildUserMap(all);
        Set<Long> likedCommentIds = buildLikedCommentIds(phone, all);
        Map<Long, List<ClientCommentResponse>> replyMap = replies.stream()
            .map(comment -> toResponse(comment, userMap.get(comment.getUserId()), likedCommentIds.contains(comment.getId())))
            .collect(Collectors.groupingBy(ClientCommentResponse::getRootId));

        List<ClientCommentResponse> items = roots.stream()
            .map(comment -> {
                ClientCommentResponse response = toResponse(
                    comment,
                    userMap.get(comment.getUserId()),
                    likedCommentIds.contains(comment.getId())
                );
                response.setReplies(replyMap.getOrDefault(comment.getId(), Collections.emptyList()));
                return response;
            })
            .collect(Collectors.toList());

        PageResponse<ClientCommentResponse> result = new PageResponse<>(items, page, size, pageResult.getTotal());
        PageResponse<ClientCommentResponse> publicResult = toPublicCommentPage(result);
        writeCommentListToRedis(cacheKey, publicResult);
        commentListCache.put(cacheKey, publicResult);
        return result;
    }

    /**
     * 发布空间评论或回复。
     *
     * <p>parentId 为空或 0 时表示发布一级评论；parentId 有值时表示回复某条已有评论。
     * 回复会同时维护 rootId 和父评论的 replyCount，方便前端和后续统计。</p>
     */
    @Override
    @Transactional
    public ClientCommentResponse create(Long spaceId, String phone, ClientCreateCommentRequest request) {
        clientSpaceService.requireActiveSpace(spaceId);
        AppUser user = findOrCreateUser(phone);
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(403, "用户账号已被禁用");
        }

        String content = normalizeContent(request.getContent());
        Long parentId = request.getParentId() == null ? 0L : request.getParentId();
        Long rootId = 0L;

        if (parentId > 0) {
            SpaceComment parent = requireComment(parentId);
            if (!spaceId.equals(parent.getSpaceId())) {
                throw new BusinessException(400, "不能回复其他空间的评论");
            }
            rootId = parent.getRootId() == null || parent.getRootId() == 0 ? parent.getId() : parent.getRootId();
        }

        SpaceComment comment = new SpaceComment();
        comment.setSpaceId(spaceId);
        comment.setUserId(user.getId());
        comment.setParentId(parentId);
        comment.setRootId(rootId);
        comment.setContent(content);
        comment.setLikeCount(0L);
        comment.setReplyCount(0L);
        comment.setStatus(1);
        commentMapper.insert(comment);

        if (parentId > 0) {
            commentMapper.update(
                new SpaceComment(),
                new LambdaUpdateWrapper<SpaceComment>()
                    .eq(SpaceComment::getId, parentId)
                    .setSql("reply_count = reply_count + 1")
            );
        }

        bumpCommentListVersion(spaceId);
        return toResponse(comment, user, false);
    }

    @Override
    @Transactional
    public ClientCommentLikeResponse like(Long commentId, String phone) {
        SpaceComment comment = requireComment(commentId);
        AppUser user = findOrCreateUser(phone);
        SpaceCommentLike exists = likeMapper.selectOne(new LambdaQueryWrapper<SpaceCommentLike>()
            .eq(SpaceCommentLike::getCommentId, commentId)
            .eq(SpaceCommentLike::getUserId, user.getId()));

        boolean changed = false;
        if (exists == null) {
            SpaceCommentLike like = new SpaceCommentLike();
            like.setCommentId(commentId);
            like.setUserId(user.getId());
            like.setLiked(1);
            likeMapper.insert(like);
            changed = true;
        } else if (!Integer.valueOf(1).equals(exists.getLiked())) {
            exists.setLiked(1);
            likeMapper.updateById(exists);
            changed = true;
        }

        if (changed) {
            incrementLikeCount(commentId, 1);
            comment.setLikeCount(safeCount(comment.getLikeCount()) + 1);
            bumpCommentListVersion(comment.getSpaceId());
        }

        return new ClientCommentLikeResponse(commentId, true, changed, safeCount(comment.getLikeCount()));
    }

    @Override
    @Transactional
    public ClientCommentLikeResponse unlike(Long commentId, String phone) {
        SpaceComment comment = requireComment(commentId);
        AppUser user = findOrCreateUser(phone);
        SpaceCommentLike exists = likeMapper.selectOne(new LambdaQueryWrapper<SpaceCommentLike>()
            .eq(SpaceCommentLike::getCommentId, commentId)
            .eq(SpaceCommentLike::getUserId, user.getId()));

        boolean changed = false;
        if (exists != null && Integer.valueOf(1).equals(exists.getLiked())) {
            exists.setLiked(0);
            likeMapper.updateById(exists);
            changed = true;
        }

        if (changed) {
            incrementLikeCount(commentId, -1);
            comment.setLikeCount(Math.max(0L, safeCount(comment.getLikeCount()) - 1));
            bumpCommentListVersion(comment.getSpaceId());
        }

        return new ClientCommentLikeResponse(commentId, false, changed, safeCount(comment.getLikeCount()));
    }

    private void incrementLikeCount(Long commentId, int delta) {
        String sql = delta > 0
            ? "like_count = like_count + 1"
            : "like_count = GREATEST(like_count - 1, 0)";
        commentMapper.update(new SpaceComment(), new LambdaUpdateWrapper<SpaceComment>()
            .eq(SpaceComment::getId, commentId)
            .setSql(sql));
    }

    private SpaceComment requireComment(Long commentId) {
        SpaceComment comment = commentMapper.selectOne(new LambdaQueryWrapper<SpaceComment>()
            .eq(SpaceComment::getId, commentId)
            .eq(SpaceComment::getStatus, 1));
        if (comment == null) {
            throw new BusinessException(404, "评论不存在");
        }
        return comment;
    }

    private AppUser findOrCreateUser(String phone) {
        validatePhone(phone);
        AppUser user = appUserMapper.selectOne(new LambdaQueryWrapper<AppUser>()
            .eq(AppUser::getPhone, phone));
        if (user != null) {
            return user;
        }

        AppUser entity = new AppUser();
        entity.setPhone(phone);
        entity.setNickname("用户-" + phone.substring(phone.length() - 4));
        entity.setRole(1);
        entity.setStatus(1);
        appUserMapper.insert(entity);
        return entity;
    }

    private Map<Long, AppUser> buildUserMap(List<SpaceComment> comments) {
        Set<Long> userIds = comments.stream()
            .map(SpaceComment::getUserId)
            .filter(id -> id != null)
            .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return new HashMap<>();
        }

        return appUserMapper.selectList(new LambdaQueryWrapper<AppUser>().in(AppUser::getId, userIds))
            .stream()
            .collect(Collectors.toMap(AppUser::getId, Function.identity(), (left, right) -> left));
    }

    private Set<Long> buildLikedCommentIds(String phone, List<SpaceComment> comments) {
        if (!StringUtils.hasText(phone) || !phone.matches("^1\\d{10}$")) {
            return new HashSet<>();
        }

        AppUser user = appUserMapper.selectOne(new LambdaQueryWrapper<AppUser>().eq(AppUser::getPhone, phone));
        if (user == null) {
            return new HashSet<>();
        }

        List<Long> commentIds = comments.stream().map(SpaceComment::getId).collect(Collectors.toList());
        if (commentIds.isEmpty()) {
            return new HashSet<>();
        }

        return likeMapper.selectList(new LambdaQueryWrapper<SpaceCommentLike>()
                .eq(SpaceCommentLike::getUserId, user.getId())
                .eq(SpaceCommentLike::getLiked, 1)
                .in(SpaceCommentLike::getCommentId, commentIds))
            .stream()
            .map(SpaceCommentLike::getCommentId)
            .collect(Collectors.toSet());
    }

    private ClientCommentResponse toResponse(SpaceComment comment, AppUser user, boolean liked) {
        ClientCommentResponse response = new ClientCommentResponse();
        response.setId(comment.getId());
        response.setSpaceId(comment.getSpaceId());
        response.setUserId(comment.getUserId());
        response.setNickname(resolveNickname(user));
        response.setMaskedPhone(maskPhone(user == null ? null : user.getPhone()));
        response.setParentId(comment.getParentId());
        response.setRootId(comment.getRootId());
        response.setContent(comment.getContent());
        response.setLikeCount(safeCount(comment.getLikeCount()));
        response.setReplyCount(safeCount(comment.getReplyCount()));
        response.setLiked(liked);
        response.setCreatedAt(comment.getCreatedAt());
        response.setReplies(Collections.emptyList());
        return response;
    }

    private String commentListCacheKey(Long spaceId, long page, long size) {
        String version = currentCommentListVersion(spaceId);
        return "spacemate:comment:list:" + spaceId + ":v" + version + ":page:" + page + ":size:" + size;
    }

    private String currentCommentListVersion(Long spaceId) {
        String version = redisTemplate.opsForValue().get(CacheKeys.commentListVersion(spaceId));
        return version == null ? "0" : version;
    }

    private void bumpCommentListVersion(Long spaceId) {
        runAfterCommitOrNow(() -> {
            try {
                redisTemplate.opsForValue().increment(CacheKeys.commentListVersion(spaceId));
            } catch (Exception ignored) {
                // 评论列表缓存只是加速层，版本号递增失败时不影响主流程。
            }
        });
    }

    private void runAfterCommitOrNow(Runnable task) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            task.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                task.run();
            }
        });
    }

    private PageResponse<ClientCommentResponse> toPublicCommentPage(PageResponse<ClientCommentResponse> source) {
        List<ClientCommentResponse> publicItems = source.getItems().stream()
            .map(this::toPublicComment)
            .collect(Collectors.toList());
        return new PageResponse<>(publicItems, source.getPage(), source.getSize(), source.getTotal());
    }

    private ClientCommentResponse toPublicComment(ClientCommentResponse source) {
        ClientCommentResponse response = new ClientCommentResponse();
        response.setId(source.getId());
        response.setSpaceId(source.getSpaceId());
        response.setUserId(source.getUserId());
        response.setNickname(source.getNickname());
        response.setMaskedPhone(source.getMaskedPhone());
        response.setParentId(source.getParentId());
        response.setRootId(source.getRootId());
        response.setContent(source.getContent());
        response.setLikeCount(source.getLikeCount());
        response.setReplyCount(source.getReplyCount());
        response.setLiked(false);
        response.setCreatedAt(source.getCreatedAt());
        List<ClientCommentResponse> replies = source.getReplies() == null
            ? Collections.emptyList()
            : source.getReplies().stream().map(this::toPublicComment).collect(Collectors.toList());
        response.setReplies(replies);
        return response;
    }

    private PageResponse<ClientCommentResponse> enrichLikedState(PageResponse<ClientCommentResponse> source, String phone) {
        PageResponse<ClientCommentResponse> copy = copyCommentPage(source);
        if (!StringUtils.hasText(phone)) {
            return copy;
        }

        List<ClientCommentResponse> allComments = flattenComments(copy.getItems());
        Set<Long> likedCommentIds = buildLikedCommentIdsByResponses(phone, allComments);
        applyLikedState(copy.getItems(), likedCommentIds);
        return copy;
    }

    private PageResponse<ClientCommentResponse> copyCommentPage(PageResponse<ClientCommentResponse> source) {
        if (source == null) {
            return new PageResponse<>(Collections.emptyList(), 1, 0, 0);
        }
        return new PageResponse<>(copyCommentList(source.getItems()), source.getPage(), source.getSize(), source.getTotal());
    }

    private List<ClientCommentResponse> copyCommentList(List<ClientCommentResponse> comments) {
        if (comments == null || comments.isEmpty()) {
            return Collections.emptyList();
        }

        return comments.stream()
            .map(this::copyComment)
            .collect(Collectors.toList());
    }

    private ClientCommentResponse copyComment(ClientCommentResponse source) {
        if (source == null) {
            return null;
        }

        ClientCommentResponse response = new ClientCommentResponse();
        response.setId(source.getId());
        response.setSpaceId(source.getSpaceId());
        response.setUserId(source.getUserId());
        response.setNickname(source.getNickname());
        response.setMaskedPhone(source.getMaskedPhone());
        response.setParentId(source.getParentId());
        response.setRootId(source.getRootId());
        response.setContent(source.getContent());
        response.setLikeCount(source.getLikeCount());
        response.setReplyCount(source.getReplyCount());
        response.setLiked(source.getLiked() != null && source.getLiked());
        response.setCreatedAt(source.getCreatedAt());
        response.setReplies(copyCommentList(source.getReplies()));
        return response;
    }

    private List<ClientCommentResponse> flattenComments(List<ClientCommentResponse> comments) {
        List<ClientCommentResponse> result = new ArrayList<>();
        if (comments == null || comments.isEmpty()) {
            return result;
        }

        for (ClientCommentResponse comment : comments) {
            result.add(comment);
            if (comment.getReplies() != null && !comment.getReplies().isEmpty()) {
                result.addAll(flattenComments(comment.getReplies()));
            }
        }
        return result;
    }

    private Set<Long> buildLikedCommentIdsByResponses(String phone, List<ClientCommentResponse> comments) {
        if (!StringUtils.hasText(phone) || !phone.matches("^1\\d{10}$")) {
            return new HashSet<>();
        }

        AppUser user = appUserMapper.selectOne(new LambdaQueryWrapper<AppUser>().eq(AppUser::getPhone, phone));
        if (user == null) {
            return new HashSet<>();
        }

        List<Long> commentIds = comments.stream()
            .map(ClientCommentResponse::getId)
            .collect(Collectors.toList());
        if (commentIds.isEmpty()) {
            return new HashSet<>();
        }

        return likeMapper.selectList(new LambdaQueryWrapper<SpaceCommentLike>()
                .eq(SpaceCommentLike::getUserId, user.getId())
                .eq(SpaceCommentLike::getLiked, 1)
                .in(SpaceCommentLike::getCommentId, commentIds))
            .stream()
            .map(SpaceCommentLike::getCommentId)
            .collect(Collectors.toSet());
    }

    private void applyLikedState(List<ClientCommentResponse> comments, Set<Long> likedCommentIds) {
        if (comments == null || comments.isEmpty()) {
            return;
        }

        for (ClientCommentResponse comment : comments) {
            comment.setLiked(likedCommentIds.contains(comment.getId()));
            applyLikedState(comment.getReplies(), likedCommentIds);
        }
    }

    private PageResponse<ClientCommentResponse> readCommentListFromRedis(String cacheKey) {
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (!StringUtils.hasText(cached)) {
            return null;
        }

        try {
            return objectMapper.readValue(
                cached,
                new com.fasterxml.jackson.core.type.TypeReference<PageResponse<ClientCommentResponse>>() {
                }
            );
        } catch (Exception ignored) {
            return null;
        }
    }

    private void writeCommentListToRedis(String cacheKey, PageResponse<ClientCommentResponse> result) {
        try {
            redisTemplate.opsForValue().set(
                cacheKey,
                objectMapper.writeValueAsString(result),
                hotKeyDetector.ttlDuration(30, 120, cacheKey)
            );
        } catch (Exception ignored) {
            // 写缓存失败不影响评论列表返回。
        }
    }

    private String normalizeContent(String content) {
        String value = content == null ? "" : content.trim();
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(400, "评论内容不能为空");
        }
        if (value.length() > 500) {
            throw new BusinessException(400, "评论内容不能超过 500 字");
        }
        return value;
    }

    private void validatePhone(String phone) {
        if (!StringUtils.hasText(phone) || !phone.matches("^1\\d{10}$")) {
            throw new BusinessException(400, "手机号格式不正确");
        }
    }

    private long safeCount(Long value) {
        return value == null ? 0L : value;
    }

    private String resolveNickname(AppUser user) {
        if (user == null) {
            return "匿名用户";
        }
        if (StringUtils.hasText(user.getNickname())) {
            return user.getNickname();
        }
        return "用户-" + maskPhone(user.getPhone());
    }

    private String maskPhone(String phone) {
        if (!StringUtils.hasText(phone) || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
