package com.capgemini.mprs.Service;

import com.capgemini.mprs.Entity.ExceptionEntity;
import com.capgemini.mprs.Repository.ExceptionEntityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExceptionEntityService {
    private final ExceptionEntityRepository repository;
    @Autowired
    public ExceptionEntityService(ExceptionEntityRepository repository) {
        this.repository = repository;
    }

    public void createException(Integer status, String Error, String message, String path) {
        ExceptionEntity e = new ExceptionEntity(status, Error, message, path );
        repository.save(e);
    }

    // Might return page in future.
    public List<ExceptionEntity> selectAllByPath(String path, Pageable pageable) {
        return repository.findAllByPath(path);
    }


    public List<ExceptionEntity> findTransactionExceptions() {
        return repository.findTransactionExceptions();
    }

    public List<ExceptionEntity> findPayoutExceptions() {
        return repository.findPayoutExceptions();
    }
}
