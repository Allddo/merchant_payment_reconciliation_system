package com.capgemini.mprs.payout;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PayoutRepositoryImpl implements PayoutBatchOps{

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public void saveInBatches(List<Payout> chunk){

        //Marks each Transaction entity to be inserted and stores it in the persistence context
        for(Payout p: chunk){
            em.persist(p);
        }

        em.flush();     //Forces JPA to immediately execute all pending SQL statements
        em.clear();     //Detaches all managed entities to free memory and reset the persistence context

    }

}
