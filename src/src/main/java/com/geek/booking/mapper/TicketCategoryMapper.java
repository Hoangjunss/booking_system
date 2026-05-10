package com.geek.booking.mapper;

import com.geek.booking.dto.request.ticketCategory.TicketCategoryCreateRequest;
import com.geek.booking.dto.response.TicketCategoryResponse;
import com.geek.booking.entity.TicketCategory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.util.List;

@Mapper(componentModel = "spring")
public interface TicketCategoryMapper {

    TicketCategoryResponse toResponse(TicketCategory category);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "concert", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    TicketCategory toEntity(TicketCategoryCreateRequest request);
}