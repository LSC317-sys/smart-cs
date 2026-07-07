package com.smartcs.chat.mapper;

import com.smartcs.chat.entity.ChatHistory;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

@Mapper
public interface ChatHistoryMapper {

    @Insert("INSERT INTO chat_history (user_id, session_id, question, answer, model, tokens_used, latency_ms) " +
            "VALUES (#{userId}, #{sessionId}, #{question}, #{answer}, #{model}, #{tokensUsed}, #{latencyMs})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ChatHistory chatHistory);

    @Select("SELECT * FROM chat_history WHERE id = #{id}")
    ChatHistory selectById(Long id);

    @Select("SELECT * FROM chat_history WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<ChatHistory> selectByUserId(Long userId);

    @Select("SELECT * FROM chat_history WHERE session_id = #{sessionId} ORDER BY created_at ASC")
    List<ChatHistory> selectBySessionId(String sessionId);

    @Update("UPDATE chat_history SET rating = #{rating} WHERE id = #{id}")
    int updateRating(@Param("id") Long id, @Param("rating") Integer rating);

    @Delete("DELETE FROM chat_history WHERE id = #{id}")
    int deleteById(Long id);

    @Delete("DELETE FROM chat_history WHERE user_id = #{userId}")
    int deleteByUserId(Long userId);

    @Select("SELECT * FROM chat_history WHERE user_id = #{userId} AND " +
            "(question LIKE CONCAT('%', #{keyword}, '%') OR answer LIKE CONCAT('%', #{keyword}, '%')) " +
            "ORDER BY created_at DESC")
    List<ChatHistory> search(@Param("userId") Long userId, @Param("keyword") String keyword);

    @Select("SELECT rating, COUNT(*) as count FROM chat_history WHERE user_id = #{userId} AND rating > 0 GROUP BY rating")
    List<Map<String, Object>> countByRating(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM chat_history WHERE user_id = #{userId}")
    int countByUserId(@Param("userId") Long userId);
}