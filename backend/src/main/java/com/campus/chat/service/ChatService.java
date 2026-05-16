package com.campus.chat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.chat.dto.ChatMessageResponse;
import com.campus.chat.dto.ConversationListItemResponse;
import com.campus.chat.dto.SendMessageRequest;
import com.campus.chat.dto.StartConversationRequest;
import com.campus.chat.entity.ChatMessage;
import com.campus.chat.entity.Conversation;
import com.campus.chat.mapper.ChatMessageMapper;
import com.campus.chat.mapper.ConversationMapper;
import com.campus.product.entity.Product;
import com.campus.product.entity.ProductImage;
import com.campus.product.mapper.ProductImageMapper;
import com.campus.product.mapper.ProductMapper;
import com.campus.user.entity.User;
import com.campus.user.mapper.UserMapper;
import com.campus.wanted.entity.Wanted;
import com.campus.wanted.mapper.WantedMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ChatService {

    public static final String CTX_GENERAL = "GENERAL";
    public static final String CTX_PRODUCT = "PRODUCT";
    public static final String CTX_WANTED = "WANTED";

    private static final int PREVIEW_MAX = 200;
    private static final int MESSAGE_PAGE_SIZE = 50;

    private final ConversationMapper conversationMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final UserMapper userMapper;
    private final ProductMapper productMapper;
    private final ProductImageMapper productImageMapper;
    private final WantedMapper wantedMapper;

    public ChatService(
        ConversationMapper conversationMapper,
        ChatMessageMapper chatMessageMapper,
        UserMapper userMapper,
        ProductMapper productMapper,
        ProductImageMapper productImageMapper,
        WantedMapper wantedMapper
    ) {
        this.conversationMapper = conversationMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.userMapper = userMapper;
        this.productMapper = productMapper;
        this.productImageMapper = productImageMapper;
        this.wantedMapper = wantedMapper;
    }

    @Transactional
    public Long startOrGet(Long currentUserId, StartConversationRequest request) {
        Long peerId = request.getPeerUserId();
        if (Objects.equals(currentUserId, peerId)) {
            throw new IllegalArgumentException("不能与自己发起会话");
        }
        User peer = userMapper.selectById(peerId);
        if (peer == null) {
            throw new IllegalArgumentException("对方用户不存在");
        }
        String ctxType = StringUtils.hasText(request.getContextType())
            ? request.getContextType().trim().toUpperCase()
            : CTX_GENERAL;
        long ctxId = request.getContextId() != null ? request.getContextId() : 0L;

        validateContext(currentUserId, peerId, ctxType, ctxId);

        long ua = Math.min(currentUserId, peerId);
        long ub = Math.max(currentUserId, peerId);

        Conversation existing = conversationMapper.selectOne(new LambdaQueryWrapper<Conversation>()
            .eq(Conversation::getUserAId, ua)
            .eq(Conversation::getUserBId, ub)
            .eq(Conversation::getContextType, ctxType)
            .eq(Conversation::getContextId, ctxId));
        if (existing != null) {
            return existing.getId();
        }

        LocalDateTime now = LocalDateTime.now();
        Conversation conv = new Conversation();
        conv.setUserAId(ua);
        conv.setUserBId(ub);
        conv.setContextType(ctxType);
        conv.setContextId(ctxId);
        conv.setLastMessagePreview(null);
        conv.setLastMessageAt(now);
        conv.setCreatedAt(now);
        conv.setUpdatedAt(now);
        conversationMapper.insert(conv);
        return conv.getId();
    }

    private void validateContext(Long currentUserId, Long peerId, String ctxType, long ctxId) {
        if (CTX_GENERAL.equals(ctxType)) {
            if (ctxId != 0) {
                throw new IllegalArgumentException("普通会话 contextId 应为 0");
            }
            return;
        }
        if (CTX_PRODUCT.equals(ctxType)) {
            Product p = productMapper.selectById(ctxId);
            if (p == null) {
                throw new IllegalArgumentException("商品不存在");
            }
            if (!Objects.equals(p.getSellerId(), peerId)) {
                throw new IllegalArgumentException("会话对象与商品卖家不一致");
            }
            return;
        }
        if (CTX_WANTED.equals(ctxType)) {
            Wanted w = wantedMapper.selectById(ctxId);
            if (w == null) {
                throw new IllegalArgumentException("求购不存在");
            }
            if (!Objects.equals(w.getUserId(), peerId)) {
                throw new IllegalArgumentException("会话对象与求购发布者不一致");
            }
            if (Objects.equals(currentUserId, w.getUserId())) {
                throw new IllegalArgumentException("不能就自己的求购发起「我有这个」会话");
            }
            return;
        }
        throw new IllegalArgumentException("不支持的 contextType");
    }

    public List<ConversationListItemResponse> listConversations(Long currentUserId) {
        List<Conversation> list = conversationMapper.selectList(new LambdaQueryWrapper<Conversation>()
            .and(w -> w.eq(Conversation::getUserAId, currentUserId).or().eq(Conversation::getUserBId, currentUserId))
            .orderByDesc(Conversation::getLastMessageAt)
            .orderByDesc(Conversation::getUpdatedAt));

        if (list.isEmpty()) {
            return List.of();
        }

        Set<Long> peerIds = list.stream()
            .map(c -> peerOf(currentUserId, c))
            .collect(Collectors.toSet());
        Map<Long, User> userMap = userMapper.selectBatchIds(peerIds).stream()
            .collect(Collectors.toMap(User::getId, u -> u));

        Set<Long> productIds = list.stream()
            .filter(c -> CTX_PRODUCT.equals(c.getContextType()))
            .map(Conversation::getContextId)
            .collect(Collectors.toSet());
        Map<Long, Product> productMap = productIds.isEmpty()
            ? Map.of()
            : productMapper.selectBatchIds(productIds).stream().collect(Collectors.toMap(Product::getId, p -> p));

        Map<Long, String> productCover = new java.util.HashMap<>();
        if (!productIds.isEmpty()) {
            List<ProductImage> imgs = productImageMapper.selectList(new LambdaQueryWrapper<ProductImage>()
                .in(ProductImage::getProductId, productIds));
            Map<Long, List<ProductImage>> grouped = imgs.stream().collect(Collectors.groupingBy(ProductImage::getProductId));
            for (Map.Entry<Long, List<ProductImage>> e : grouped.entrySet()) {
                e.getValue().stream().min(Comparator.comparingInt(ProductImage::getSortOrder))
                    .ifPresent(pi -> productCover.put(e.getKey(), pi.getImageUrl()));
            }
        }

        Set<Long> wantedIds = list.stream()
            .filter(c -> CTX_WANTED.equals(c.getContextType()))
            .map(Conversation::getContextId)
            .collect(Collectors.toSet());
        Map<Long, Wanted> wantedMap = wantedIds.isEmpty()
            ? Map.of()
            : wantedMapper.selectBatchIds(wantedIds).stream().collect(Collectors.toMap(Wanted::getId, w -> w));

        List<ConversationListItemResponse> out = new ArrayList<>();
        for (Conversation c : list) {
            Long peerId = peerOf(currentUserId, c);
            User peer = userMap.get(peerId);
            String title = null;
            String cover = null;
            if (CTX_PRODUCT.equals(c.getContextType())) {
                Product p = productMap.get(c.getContextId());
                title = p != null ? p.getTitle() : null;
                cover = productCover.get(c.getContextId());
            } else if (CTX_WANTED.equals(c.getContextType())) {
                Wanted w = wantedMap.get(c.getContextId());
                title = w != null ? w.getTitle() : null;
            }
            out.add(ConversationListItemResponse.builder()
                .id(c.getId())
                .peerUserId(peerId)
                .peerNickname(peer != null ? peer.getNickname() : null)
                .peerAvatarUrl(peer != null ? peer.getAvatarUrl() : null)
                .contextType(c.getContextType())
                .contextId(c.getContextId())
                .contextTitle(title)
                .contextCoverUrl(cover)
                .lastMessagePreview(c.getLastMessagePreview())
                .lastMessageAt(c.getLastMessageAt())
                .unreadCount(countUnread(c.getId(), currentUserId))
                .build());
        }
        return out;
    }

    private long countUnread(Long conversationId, Long currentUserId) {
        Long c = chatMessageMapper.selectCount(new LambdaQueryWrapper<ChatMessage>()
            .eq(ChatMessage::getConversationId, conversationId)
            .ne(ChatMessage::getSenderId, currentUserId)
            .eq(ChatMessage::getReadFlag, false));
        return c == null ? 0 : c;
    }

    private Long peerOf(Long me, Conversation c) {
        return Objects.equals(c.getUserAId(), me) ? c.getUserBId() : c.getUserAId();
    }

    public void assertParticipant(Long conversationId, Long userId) {
        Conversation c = conversationMapper.selectById(conversationId);
        if (c == null) {
            throw new IllegalArgumentException("会话不存在");
        }
        if (!Objects.equals(c.getUserAId(), userId) && !Objects.equals(c.getUserBId(), userId)) {
            throw new IllegalArgumentException("无权访问该会话");
        }
    }

    public List<ChatMessageResponse> listMessages(Long conversationId, Long currentUserId, int page, int size) {
        assertParticipant(conversationId, currentUserId);
        Page<ChatMessage> pg = chatMessageMapper.selectPage(
            new Page<>(page, size),
            new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getConversationId, conversationId)
                .orderByDesc(ChatMessage::getCreatedAt)
        );
        List<ChatMessage> rows = new ArrayList<>(pg.getRecords());
        Collections.reverse(rows);

        Set<Long> senderIds = rows.stream().map(ChatMessage::getSenderId).collect(Collectors.toSet());
        Map<Long, User> senderMap = senderIds.isEmpty()
            ? Map.of()
            : userMapper.selectBatchIds(senderIds).stream().collect(Collectors.toMap(User::getId, u -> u));

        List<ChatMessageResponse> out = new ArrayList<>();
        for (ChatMessage m : rows) {
            User s = senderMap.get(m.getSenderId());
            out.add(ChatMessageResponse.builder()
                .id(m.getId())
                .senderId(m.getSenderId())
                .senderNickname(s != null ? s.getNickname() : null)
                .content(m.getContent())
                .createdAt(m.getCreatedAt())
                .mine(Objects.equals(m.getSenderId(), currentUserId))
                .build());
        }
        return out;
    }

    /**
     * 最近消息（时间正序），默认最多 50 条。
     */
    public List<ChatMessageResponse> latestMessages(Long conversationId, Long currentUserId) {
        assertParticipant(conversationId, currentUserId);
        List<ChatMessage> rows = chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
            .eq(ChatMessage::getConversationId, conversationId)
            .orderByDesc(ChatMessage::getCreatedAt)
            .last("LIMIT " + MESSAGE_PAGE_SIZE));
        Collections.reverse(rows);

        Set<Long> senderIds = rows.stream().map(ChatMessage::getSenderId).collect(Collectors.toSet());
        Map<Long, User> senderMap = senderIds.isEmpty()
            ? Map.of()
            : userMapper.selectBatchIds(senderIds).stream().collect(Collectors.toMap(User::getId, u -> u));

        List<ChatMessageResponse> out = new ArrayList<>();
        for (ChatMessage m : rows) {
            User s = senderMap.get(m.getSenderId());
            out.add(ChatMessageResponse.builder()
                .id(m.getId())
                .senderId(m.getSenderId())
                .senderNickname(s != null ? s.getNickname() : null)
                .content(m.getContent())
                .createdAt(m.getCreatedAt())
                .mine(Objects.equals(m.getSenderId(), currentUserId))
                .build());
        }
        return out;
    }

    @Transactional
    public Long sendMessage(Long conversationId, Long senderId, SendMessageRequest request) {
        assertParticipant(conversationId, senderId);
        String text = request.getContent().trim();
        if (text.length() > 2000) {
            throw new IllegalArgumentException("消息过长");
        }
        LocalDateTime now = LocalDateTime.now();
        ChatMessage msg = new ChatMessage();
        msg.setConversationId(conversationId);
        msg.setSenderId(senderId);
        msg.setContent(text);
        msg.setReadFlag(false);
        msg.setCreatedAt(now);
        chatMessageMapper.insert(msg);

        String preview = text.length() > PREVIEW_MAX ? text.substring(0, PREVIEW_MAX) + "…" : text;
        conversationMapper.update(null, new LambdaUpdateWrapper<Conversation>()
            .eq(Conversation::getId, conversationId)
            .set(Conversation::getLastMessagePreview, preview)
            .set(Conversation::getLastMessageAt, now)
            .set(Conversation::getUpdatedAt, now));

        return msg.getId();
    }

    @Transactional
    public void markRead(Long conversationId, Long readerId) {
        assertParticipant(conversationId, readerId);
        chatMessageMapper.update(null, new LambdaUpdateWrapper<ChatMessage>()
            .eq(ChatMessage::getConversationId, conversationId)
            .ne(ChatMessage::getSenderId, readerId)
            .eq(ChatMessage::getReadFlag, false)
            .set(ChatMessage::getReadFlag, true));
    }

    @Transactional
    public Long sendMessage(Long conversationId, Long senderId, String content) {
        assertParticipant(conversationId, senderId);
        String text = content.trim();
        if (text.length() > 2000) {
            throw new IllegalArgumentException("消息过长");
        }
        LocalDateTime now = LocalDateTime.now();
        ChatMessage msg = new ChatMessage();
        msg.setConversationId(conversationId);
        msg.setSenderId(senderId);
        msg.setContent(text);
        msg.setReadFlag(false);
        msg.setCreatedAt(now);
        chatMessageMapper.insert(msg);

        String preview = text.length() > PREVIEW_MAX ? text.substring(0, PREVIEW_MAX) + "…" : text;
        conversationMapper.update(null, new LambdaUpdateWrapper<Conversation>()
            .eq(Conversation::getId, conversationId)
            .set(Conversation::getLastMessagePreview, preview)
            .set(Conversation::getLastMessageAt, now)
            .set(Conversation::getUpdatedAt, now));

        return msg.getId();
    }

    public Long getOtherUserId(Long conversationId, Long userId) {
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new IllegalArgumentException("会话不存在");
        }
        return Objects.equals(conversation.getUserAId(), userId) ? conversation.getUserBId() : conversation.getUserAId();
    }

    public ChatMessageResponse getMessageById(Long messageId) {
        ChatMessage message = chatMessageMapper.selectById(messageId);
        if (message == null) {
            throw new IllegalArgumentException("消息不存在");
        }
        User sender = userMapper.selectById(message.getSenderId());
        return ChatMessageResponse.builder()
            .id(message.getId())
            .senderId(message.getSenderId())
            .senderNickname(sender != null ? sender.getNickname() : null)
            .content(message.getContent())
            .createdAt(message.getCreatedAt())
            .mine(false) // 这里 mine 字段在返回给客户端时会根据接收者进行设置
            .build();
    }
}
