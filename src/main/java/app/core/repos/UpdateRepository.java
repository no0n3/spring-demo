package app.core.repos;

import app.core.DB;
import app.core.repos.intefaces.UpdateRepositoryInterface;
import app.http.pojos.Page;
import app.pojo.Comment;
import app.pojo.Favorite;
import app.pojo.Update;
import app.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class UpdateRepository extends BaseRepository implements UpdateRepositoryInterface {
    public static final int PAGE_SIZE = 10;

    @Autowired
    private DB db;

    @Override
    public int add(final Update update) {
        final String sql = "INSERT INTO `updates` (`content`, `user_id`, `created_at`) VALUES " +
                "(:content, :userId, :createdAt)";
        update.setCreatedAt(System.currentTimeMillis() / 1000);

        KeyHolder holder = new GeneratedKeyHolder();
        db.getJdbcTemplate().update(sql, new BeanPropertySqlParameterSource(update), holder);

        return Integer.parseInt(holder.getKeys().get("GENERATED_KEY").toString());
    }

    @Override
    public void addFavorite(final int updateId, final int userId) {
        final String sql = "INSERT INTO `favorites` (`update_id`, `user_id`, `favorited_at`) VALUES " +
                "(:updateId, :userId, :favoritedAt)";
        final Map<String, Integer> params = new HashMap<>();
        params.put("userId", userId);
        params.put("updateId", updateId);
        params.put("favoritedAt", (int) System.currentTimeMillis() / 1000);

        db.getJdbcTemplate().update(
                sql,
                new MapSqlParameterSource(params)
        );
    }

    @Override
    public Favorite findFavoriteByUpdateIdAndUserId(final int updateId, final int userId) {
        final String sql = "SELECT * FROM `favorites` WHERE `update_id` = :updateId AND `user_id` = :userId LIMIT 1";
        final Map<String, Integer> params = new HashMap<>();
        params.put("updateId", updateId);
        params.put("userId", userId);

        try {
            final Favorite result = db.getJdbcTemplate().queryForObject(
                    sql,
                    new MapSqlParameterSource(params),
                    getFavoriteMapper()
            );

            return result;
        } catch (Exception e) {
            return null;
        }
    }

    public int updateFavorites(final int updateId) {
        final String sql = "UPDATE `updates` SET `favorites` = `favorites` + 1 WHERE `id` = :id";

        KeyHolder holder = new GeneratedKeyHolder();
        int updated = db.getJdbcTemplate().update(sql, new MapSqlParameterSource("id", updateId));

        return updated;
    }

    @Override
    public List<Update> findPaged(final Page page) {
        return findPagedByUserId(page, null);
    }

    @Override
    public List<Update> findPagedByUserId(final Page page, final Integer targetUserId) {
        final String sql = "SELECT * " +
                " FROM `updates` " +
                " WHERE (:userId IS NULL OR `user_id` = :userId) " +
                " ORDER BY `created_at` DESC " +
                " LIMIT :offset, :limit ";
        final Map<String, Integer> params = new HashMap<>();
        params.put("userId", targetUserId);
        params.put("offset", (page.getPage() - 1) * PAGE_SIZE);
        params.put("limit", PAGE_SIZE);

        final List<Update> updates = db.query(
                sql,
                new MapSqlParameterSource(params),
                getMapper()
        );

        return updates;
    }

    @Override
    public List<Update> findPagedByTag(final Page page, final String tag) {
        if (!isValidTag(tag)) {
            return this.getEmptyList(Update.class);
        }

        final String sql = "SELECT u.* " +
                " FROM `updates` AS u " +
                " JOIN `update_tags` ut ON ut.update_id = u.id " +
                " JOIN `tags` t ON ut.tag_id = t.id " +
                " WHERE t.`name` = :tag " +
                " ORDER BY u.`created_at` DESC " +
                " LIMIT :offset, :limit ";
        final Map<String, Object> params = new HashMap<>();
        params.put("tag", tag.trim());
        params.put("offset", (page.getPage() - 1) * PAGE_SIZE);
        params.put("limit", PAGE_SIZE);

        final List<Update> updates = db.query(
                sql,
                new MapSqlParameterSource(params),
                getMapper()
        );

        return updates;
    }

    private boolean isValidTag(final String tag) {
        return null != tag && !tag.trim().equals("");
    }

    @Override
    public Update findById(final int id) {
        final String sql = "SELECT * FROM `updates` WHERE `id` = :id LIMIT 1";
        try {
            final Update result = db.getJdbcTemplate().queryForObject(
                    sql,
                    new MapSqlParameterSource("id", id),
                    getMapper()
            );

            return result;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Update findByIdAndUserId(final int id, final int userId) {
        final String sql = "SELECT * FROM `updates` WHERE `id` = :id AND `user_id` = :userId LIMIT 1";
        final Map<String, Integer> params = new HashMap<>();
        params.put("id", id);
        params.put("userId", userId);

        try {
            final Update result = db.getJdbcTemplate().queryForObject(
                    sql,
                    new MapSqlParameterSource(params),
                    getMapper()
            );

            return result;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public List<Update> findByUserId(final int userId) {
        final String sql = "SELECT * FROM `updates` WHERE `user_id` = :userId ORDER BY `created_at` DESC";
        final List<Update> updates = db.query(
                sql,
                new MapSqlParameterSource("userId", userId),
                getMapper()
        );

        return updates;
    }

    @Override
    public void incrementUpdateComments(final int updateId) {
        final String sql = "UPDATE `updates` SET `comments` = `comments` + 1 WHERE `id` = :id";

        db.getJdbcTemplate().update(sql, new MapSqlParameterSource("id", updateId));
    }

    @Override
    public void incrementUpdateLikes(final int updateId) {
        final String sql = "UPDATE `updates` SET `likes` = `likes` + 1 WHERE `id` = :id";

        db.getJdbcTemplate().update(sql, new MapSqlParameterSource("id", updateId));
    }

    @Override
    public void decrementUpdateLikes(final int updateId) {
        final String sql = "UPDATE `updates` SET `likes` = `likes` - 1 WHERE `id` = :id";

        db.getJdbcTemplate().update(sql, new MapSqlParameterSource("id", updateId));
    }

    @Override
    public boolean exists(final int id) {
        final String sql = "SELECT count(*) FROM `updates` WHERE `id` = :id";
        try {
            final int count = db.getJdbcTemplate().queryForObject(
                    sql,
                    new MapSqlParameterSource("id", id),
                    (rs, row) -> rs.getInt(1)
            );

            return 0 < count;
        } catch (Exception e) {
            return false;
        }
    }

    private RowMapper<Update> getMapper() {
        return (ResultSet rs, int rowNum) -> {
            final Update update = new Update();
            update.setId(rs.getInt("id"));
            update.setUserId(rs.getInt("user_id"));
            update.setCreatedAt(rs.getInt("created_at"));
            update.setContent(rs.getString("content"));
            update.setLikes(rs.getInt("likes"));
            update.setCommentsCount(rs.getInt("comments"));

            return update;
        };
    }

    private RowMapper<Favorite> getFavoriteMapper() {
        return (ResultSet rs, int rowNum) -> {
            final Favorite favorite = new Favorite();
            favorite.setId(rs.getInt("id"));
            favorite.setUpdateId(rs.getInt("update_id"));
            favorite.setUserId(rs.getInt("user_id"));

            return favorite;
        };
    }

}
