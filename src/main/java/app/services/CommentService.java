package app.services;

import app.core.repos.CommentRepository;
import app.core.repos.UpdateRepository;
import app.http.pojos.CommentResource;
import app.http.pojos.UpdateResource;
import app.pojo.Comment;
import app.pojo.Update;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CommentService {
    @Autowired
    private CommentRepository commentRepository;

    public Comment addNew(CommentResource commentResource, int userId) {
        final Comment comment = new Comment();
        comment.setContent(commentResource.getContent());
        comment.setUpdateId(commentResource.getUpdateId());
        comment.setUserId(userId);

        int newCommentId = commentRepository.add(comment);
        comment.setId(newCommentId);

        return comment;
    }
}