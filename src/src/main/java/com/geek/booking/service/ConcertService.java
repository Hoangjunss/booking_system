package com.geek.booking.service;


import com.geek.booking.dto.request.concern.ConcertCreateRequest;
import com.geek.booking.dto.request.concern.ConcertUpdateRequest;
import com.geek.booking.dto.response.ConcertResponse;
import com.geek.booking.entity.Concert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


import java.time.LocalDateTime;
import java.util.List;

public interface ConcertService {
    ConcertResponse createConcert(ConcertCreateRequest request);
    ConcertResponse updateConcert(Long concertId, ConcertUpdateRequest request);
    void publishConcert(Long concertId);
    void unpublishConcert(Long concertId);
    ConcertResponse getConcertById(Long concertId);
    Concert getConcertEntityById(Long concertId);
    Page<ConcertResponse> getAllConcerts(String name, String venue, String status,
                                         LocalDateTime fromDate, LocalDateTime toDate, Pageable pageable);
    List<ConcertResponse> getPublishedConcerts(); // giữ lại nếu cần
}