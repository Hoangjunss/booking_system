package com.geek.booking.mapper;


import com.geek.booking.dto.request.voucher.VoucherCreateRequest;
import com.geek.booking.dto.response.VoucherResponse;
import com.geek.booking.entity.Voucher;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;



@Mapper(componentModel = "spring")
public interface VoucherMapper {

    VoucherResponse toResponse(Voucher voucher);
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "usedCount", constant = "0")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Voucher toEntity(VoucherCreateRequest request);
}