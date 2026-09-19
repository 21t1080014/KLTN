package com.dungochung.shopdongho.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.dungochung.shopdongho.entity.AuditLogEntity;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {
	@Query("SELECT a FROM AuditLogEntity a WHERE (:action IS NULL OR a.action = :action) "
			+ "AND (:actor IS NULL OR LOWER(a.actor) LIKE :actor) ORDER BY a.auditId DESC")
	Page<AuditLogEntity> search(@Param("action") String action, @Param("actor") String actorLike, Pageable pageable);
}
