package com.geek.booking.repository;

import com.geek.booking.entity.Concert;
import com.geek.booking.enums.ConcertStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ConcertRepository extends JpaRepository<Concert, Long> , JpaSpecificationExecutor<Concert> {
    List<Concert> findByStatus(ConcertStatus status);
}