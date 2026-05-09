package com.geek.booking.repository;


import com.geek.booking.entity.TicketCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface TicketCategoryRepository extends JpaRepository<TicketCategory, Long>, JpaSpecificationExecutor<TicketCategory> {

    /**
     * Finds a ticketCategory category with pessimistic write lock.
     * Used during booking to prevent overselling.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT tc FROM TicketCategory tc WHERE tc.id = :id")
    Optional<TicketCategory> findByIdWithPessimisticLock(@Param("id") Long id);

    List<TicketCategory> findByConcertId(Long concertId);
}