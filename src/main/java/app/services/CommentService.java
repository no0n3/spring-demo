package app.services;

import app.core.repos.CommentRepository;
import app.core.repos.LikeRepository;
import app.core.repos.UpdateRepository;
import app.core.repos.UserRepository;
import app.http.pojos.*;
import app.pojo.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Component
public class CommentService {

    private final CommentRepository commentRepository;
    private final UpdateRepository updateRepository;
    private final LikeRepository likeRepository;
    private final ImageService imageService;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    CommentService(
            final ImageService imageService,
            final UserRepository userRepository,
            final NotificationService notificationService,
            final LikeRepository likeRepository,
            final UpdateRepository updateRepository,
            final CommentRepository commentRepository
    ) {
        this.imageService = imageService;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.likeRepository = likeRepository;
        this.updateRepository = updateRepository;
        this.commentRepository = commentRepository;
    }

    public List<CommentResponse> findByUpdateIdPaged(final Page page, final int updateId, final Integer userId) {
        final List<Comment> comments = commentRepository.findByUpdateIdPaged(updateId, page);
        final List<CommentResponse> response = createCommentResult(comments, userId);

        return response;
    }

    public CommentResponse findById(final int id, final Integer userId) {
        final Comment comment = commentRepository.findById(id);
        final List<CommentResponse> response = createCommentResult(Collections.singletonList(comment), userId);

        return response.get(0);
    }

    public List<CommentResponse> createCommentResult(final List<Comment> comments, final Integer userId) {
        final Set<Integer> likedUpdateIds = getLikedComments(comments, userId);
        final Map<Integer, Image> userImages = getUserImages(comments);
        final Map<Integer, User> users = getUserCommentOwners(comments);

        final List<CommentResponse> response = new ArrayList<>();
        for (final Comment comment: comments) {
            final CommentResponse item = CommentResponse.createFromComment(comment);
            item.setLiked(likedUpdateIds.contains(comment.getId()));
            item.setUser(users.get(comment.getUserId()));
            final Image image = userImages.get(comment.getUserId());
            item.setImageId(null != image ? image.getId() : -1);

            response.add(item);
        }

        return response;
    }

    private Map<Integer, User> getUserCommentOwners(final List<Comment> comments) {
        final Map<Integer, User> result = new HashMap<>();
        final Set<Integer> userIds = getUserIds(comments);
        final List<User> users = userRepository.findByIds(userIds);
        for (final User user : users) {
            result.put(user.getId(), user);
        }

        return result;
    }

    private Map<Integer, Image> getUserImages(final List<Comment> comments) {
        final Set<Integer> userIds = getUserIds(comments);

        return imageService.getImageForUsers(userIds);
    }

    private Set<Integer> getUserIds(final List<Comment> comments) {
        final Set<Integer> userIds = new HashSet<>();
        for (final Comment comment : comments) {
            userIds.add(comment.getUserId());
        }

        return userIds;
    }

    private Set<Integer> getLikedComments(List<Comment> comments, Integer userId) {
        List<Like> likes = Collections.EMPTY_LIST;
        if (userId != null) {
            likes = likeRepository.findCommentLikesByIds(getCommentIds(comments), userId);
        }
        Set<Integer> likedUpdateIds = new HashSet<>();
        for (Like like : likes) {
            likedUpdateIds.add(like.getItemId());
        }

        return likedUpdateIds;
    }

    private Set<Integer> getCommentIds(List<Comment> updates) {
        Set<Integer> result = new HashSet<>();
        for (Comment update : updates) {
            result.add(update.getId());
        }

        return result;
    }

    @Transactional
    public Comment addNew(final CommentResource commentResource, final int userId) {
        final Comment comment = new Comment();
        comment.setContent(commentResource.getContent());
        comment.setUpdateId(commentResource.getUpdateId());
        comment.setUserId(userId);

        int newCommentId = commentRepository.add(comment);
        comment.setId(newCommentId);

        updateRepository.incrementUpdateComments(commentResource.getUpdateId());

        sendUpdateNotification(commentResource.getUpdateId(), userId);

        return comment;
    }

    private void sendUpdateNotification(final int updateId, final int userId) {
        final Update update = this.updateRepository.findById(updateId);
        final int updateOwnerId = update.getUserId();

        final Notification notification = Notification.createForUpdate(update, userId);
        notification.setType(NotificationType.UPDATE_COMMENT);

        if (updateOwnerId != userId) {
            this.notificationService.sendNotification(updateOwnerId, notification);
        }
    }

}
