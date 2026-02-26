package com.capgemini.mprs.exception;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
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


}
