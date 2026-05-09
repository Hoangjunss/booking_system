package com.geek.booking.dto.request.concern;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ConcertUpdateRequest {
    private String name;
    private String description;
    private String venue;
    private LocalDateTime eventDate;
    private String status;
}