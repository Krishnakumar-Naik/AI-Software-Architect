package com.aisoftwarearchitect.core.repository;

import com.aisoftwarearchitect.core.domain.Conversation;
import com.aisoftwarearchitect.core.domain.Project;
import com.aisoftwarearchitect.core.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    List<Conversation> findByProjectAndUserOrderByUpdatedAtDesc(Project project, User user);
}
