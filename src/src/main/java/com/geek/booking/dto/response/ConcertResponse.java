package com.geek.booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import com.geek.booking.enums.ConcertStatus;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConcertResponse {
    private Long id;
    private String name;
    private String description;
    private String venue;
    private LocalDateTime eventDate;
    private ConcertStatus status;
    private List<TicketCategoryResponse> ticketCategories;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}