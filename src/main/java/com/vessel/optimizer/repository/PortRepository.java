package com.vessel.optimizer.repository;

import com.vessel.optimizer.model.Port;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PortRepository extends JpaRepository<Port, Long> {
    Optional<Port> findByCode(String code);
    List<Port> findByIsActiveTrue();
    List<Port> findByCodeIn(List<String> codes);
    boolean existsByCode(String code);
}