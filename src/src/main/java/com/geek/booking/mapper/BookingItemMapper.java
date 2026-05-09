package com.geek.booking.mapper;


import com.geek.booking.dto.response.BookingItemResponse;
import com.geek.booking.entity.BookingItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BookingItemMapper {

    @Mapping(source = "ticketCategory.name", target = "categoryName")
    @Mapping(target = "subtotal", expression = "java(item.getUnitPrice().multiply(java.math.BigDecimal.valueOf(item.getQuantity())))")
    BookingItemResponse toResponse(BookingItem item);

    List<BookingItemResponse> toResponseList(List<BookingItem> items);
}