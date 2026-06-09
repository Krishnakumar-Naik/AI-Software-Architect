package com.aisoftwarearchitect.core.repository;

import com.aisoftwarearchitect.core.domain.Project;
import com.aisoftwarearchitect.core.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByOwner(User owner);
}
