package com.geek.booking.repository;

import com.geek.booking.entity.UserVoucherUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserVoucherUsageRepository extends JpaRepository<UserVoucherUsage, Long>, JpaSpecificationExecutor<UserVoucherUsage> {


    boolean existsByUserIdAndVoucherId(Long userId, Long voucherId);
}