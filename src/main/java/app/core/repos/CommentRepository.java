package app.core.repos;

import app.core.DB;
import app.core.repos.intefaces.CommentRepositoryInterface;
import app.pojo.Comment;
import app.pojo.Update;
import app.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.util.List;

@Component
public class CommentRepository implements CommentRepositoryInterface {
    @Autowired
    private DB db;

    public int add(Comment comment) {
        final String sql = "INSERT INTO `comments` (`content`, `update_id`, `user_id`, `created_at`) VALUES " +
                "(:content, :update_id, :user_id, :created_at)";
        comment.setCreatedAt(System.currentTimeMillis() / 1000);

        KeyHolder holder = new GeneratedKeyHolder();
        db.getJdbcTemplate().update(sql, new BeanPropertySqlParameterSource(comment), holder);

        return Integer.parseInt(holder.getKeys().get("GENERATED_KEY").toString());
    }

    @Override
    public Comment findById(int id) {
        final String sql = "SELECT `id`, `content`, `user_id`, `update_id` FROM `comments` WHERE `id` = :id LIMIT 1";
        Comment result = db.getJdbcTemplate().queryForObject(
                sql,
                new MapSqlParameterSource("id", id),
                getMapper()
        );

        return result;
    }

    @Override
    public List<Comment> findByUpdateId(int updateId) {
        final String sql = "SELECT`id`, `content`, `user_id`, `update_id` FROM `updates` WHERE `user_id` = :updateId ORDER BY `created_at` DESC";
        List<Comment> comments = db.getJdbcTemplate().query(
                sql,
                new MapSqlParameterSource("updateId", updateId),
                getMapper()
        );

        return comments;
    }

    private RowMapper<Comment> getMapper() {
        return (ResultSet rs, int rowNum) -> {
            Comment comment = new Comment();
            comment.setId(rs.getInt("id"));
            comment.setContent(rs.getString("content"));
            comment.setUpdateId(rs.getInt("update_id"));
            comment.setUserId(rs.getInt("user_id"));

            return comment;
        };
    }

}
