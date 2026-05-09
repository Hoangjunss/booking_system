package com.geek.booking.mapper;


import com.geek.booking.dto.request.user.UserVoucherUsageResponse;
import com.geek.booking.entity.UserVoucherUsage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserVoucherUsageMapper {


    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.email", target = "userEmail")
    @Mapping(source = "voucher.id", target = "voucherId")
    @Mapping(source = "voucher.code", target = "voucherCode")
    @Mapping(source = "booking.id", target = "bookingId")
    @Mapping(source = "usedAt", target = "usedAt")
    UserVoucherUsageResponse toResponse(UserVoucherUsage usage);
}