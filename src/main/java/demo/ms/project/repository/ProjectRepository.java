package demo.ms.project.repository;

import demo.ms.common.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ProjectRepository extends JpaRepository<Project,Integer> {
    List<Project> findByIdIn(Collection<Integer> ids);
}
