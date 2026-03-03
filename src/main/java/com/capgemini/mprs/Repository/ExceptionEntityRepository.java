package com.capgemini.mprs.Repository;

import com.capgemini.mprs.Entity.ExceptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

public interface ExceptionEntityRepository extends JpaRepository<ExceptionEntity, Instant> {

    @Query(value = """
       SELECT e
       FROM ExceptionEntity e
       WHERE e.path = :path
       ORDER BY e.exception_id ASC
       
       """)
    public List<ExceptionEntity> findAllByPath(@Param("path") String path);
    // may return page in the future, but for now it returns list.
    @Query(value = """
       SELECT e
       FROM ExceptionEntity e
       WHERE e.path LIKE '/api/v1/transactions%'
       ORDER BY e.exception_id ASC
       
       """)
    public List<ExceptionEntity> findTransactionExceptions();
    // may return page in the future, but for now it returns list.

    @Query(value = """
       SELECT e
       FROM ExceptionEntity e
       WHERE e.path LIKE '/api/v1/payouts%'
       ORDER BY e.exception_id ASC
       
       """)
    public List<ExceptionEntity> findPayoutExceptions();
}


