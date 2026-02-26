package com.capgemini.mprs.exception;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ExceptionEntityRepository extends JpaRepository<ExceptionEntity, Instant> {

    @Query(value = """
       SELECT e
       FROM ExceptionEntity e
       WHERE e.path = :path
       ORDER BY e.exception_id ASC
       
       """)
    public List<ExceptionEntity> findAllByPath(@Param("path") String path);
    // may return page in the future, but for now it returns list.
}


