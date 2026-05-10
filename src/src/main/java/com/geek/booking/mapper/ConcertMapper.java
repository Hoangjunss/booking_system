package com.geek.booking.mapper;

import com.geek.booking.dto.request.concern.ConcertCreateRequest;
import com.geek.booking.dto.response.ConcertResponse;
import com.geek.booking.entity.Concert;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", uses = TicketCategoryMapper.class)
public interface ConcertMapper {

    @Mapping(source = "ticketCategories", target = "ticketCategories")
    ConcertResponse toResponse(Concert concert);



    List<ConcertResponse> toResponseList(List<Concert> concerts);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "DRAFT")
    @Mapping(target = "ticketCategories", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Concert toEntity(ConcertCreateRequest request);
}