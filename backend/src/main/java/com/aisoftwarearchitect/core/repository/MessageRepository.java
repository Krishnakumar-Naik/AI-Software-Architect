package com.aisoftwarearchitect.core.repository;

import com.aisoftwarearchitect.core.domain.Conversation;
import com.aisoftwarearchitect.core.domain.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByConversationOrderByTimestampAsc(Conversation conversation);
}
