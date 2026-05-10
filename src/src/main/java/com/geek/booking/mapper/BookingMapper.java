package com.geek.booking.mapper;

import com.geek.booking.dto.response.BookingResponse;
import com.geek.booking.entity.Booking;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", uses = BookingItemMapper.class)
public interface BookingMapper {


    @Mapping(source = "id", target = "bookingId")
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "concert.id", target = "concertId")
    @Mapping(source = "concert.name", target = "concertName")
    @Mapping(source = "items", target = "items")
    BookingResponse toResponse(Booking booking);
}