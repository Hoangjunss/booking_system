package com.geek.booking.integration;

import com.geek.booking.dto.request.booking.BookingCreateRequest;
import com.geek.booking.dto.request.booking.BookingItemRequest;
import com.geek.booking.dto.response.BookingCreationResult;
import com.geek.booking.dto.response.BookingResponse;
import com.geek.booking.entity.*;
import com.geek.booking.enums.BookingStatus;
import com.geek.booking.enums.ConcertStatus;
import com.geek.booking.enums.DiscountType;
import com.geek.booking.enums.UserRole;
import com.geek.booking.exception.BusinessException;
import com.geek.booking.exception.InsufficientInventoryException;
import com.geek.booking.repository.*;
import com.geek.booking.service.BookingService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
@SpringBootTest
@Testcontainers
@Transactional
class BookingServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConcertRepository concertRepository;

    @Autowired
    private TicketCategoryRepository categoryRepository;

    @Autowired
    private VoucherRepository voucherRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private EntityManager entityManager;

    private Long userId;
    private Long categoryId;
    private Long concertId;
    private Long voucherId;

    private BookingItemRequest createItemRequest(Long categoryId, int quantity) {
        BookingItemRequest req = new BookingItemRequest();
        req.setTicketCategoryId(categoryId);
        req.setQuantity(quantity);
        return req;
    }

    @BeforeEach
    void setup() {
        User user = User.builder()
                .email("test@example.com")
                .name("Test User")
                .password("encoded")
                .role(UserRole.CUSTOMER)
                .build();
        user = userRepository.save(user);
        userId = user.getId();

        Concert concert = Concert.builder()
                .name("Test Concert")
                .venue("Test Venue")
                .eventDate(LocalDateTime.now().plusDays(5))
                .status(ConcertStatus.PUBLISHED)
                .build();
        concert = concertRepository.save(concert);
        concertId = concert.getId();

        TicketCategory category = TicketCategory.builder()
                .concert(concert)
                .name("VIP")
                .price(BigDecimal.valueOf(100))
                .totalQuantity(10)
                .availableQuantity(10)
                .build();
        category = categoryRepository.save(category);
        categoryId = category.getId();

        Voucher voucher = Voucher.builder()
                .code("TEST10")
                .discountType(DiscountType.PERCENT)
                .discountValue(BigDecimal.valueOf(10))
                .usageLimit(5)
                .usedCount(0)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validTo(LocalDateTime.now().plusDays(1))
                .minOrderValue(BigDecimal.ZERO)
                .build();
        voucher = voucherRepository.save(voucher);
        voucherId = voucher.getId();
    }

    @Test
    void shouldCreateBookingAndDecreaseInventory() {
        BookingCreateRequest request = new BookingCreateRequest();
        request.setItems(List.of(createItemRequest(categoryId, 2)));
        request.setIdempotencyKey("inv-test-1");

        BookingCreationResult result = bookingService.createBooking(request, userId);
        assertThat(result.isDuplicate()).isFalse();
        BookingResponse response = result.getBookingResponse();
        assertThat(response.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(response.getTotalPrice()).isEqualTo(BigDecimal.valueOf(200));

        entityManager.flush();
        entityManager.clear();

        TicketCategory updated = categoryRepository.findById(categoryId).orElseThrow();
        assertThat(updated.getAvailableQuantity()).isEqualTo(8);
    }

    @Test
    void shouldReturnExistingBookingWhenDuplicateIdempotencyKey() {
        String idempotencyKey = "dup-key-123";
        BookingCreateRequest request = new BookingCreateRequest();
        request.setItems(List.of(createItemRequest(categoryId, 1)));
        request.setIdempotencyKey(idempotencyKey);

        BookingCreationResult first = bookingService.createBooking(request, userId);
        BookingCreationResult second = bookingService.createBooking(request, userId);

        assertThat(second.isDuplicate()).isTrue();
        assertThat(second.getBookingResponse().getBookingId()).isEqualTo(first.getBookingResponse().getBookingId());
        long count = bookingRepository.findAll().stream()
                .filter(b -> b.getIdempotencyKey().equals(idempotencyKey))
                .count();
        assertThat(count).isEqualTo(1);
    }

    @Test
    void shouldThrowExceptionWhenInsufficientInventoryAndRollback() {
        BookingCreateRequest request = new BookingCreateRequest();
        request.setItems(List.of(createItemRequest(categoryId, 20)));
        request.setIdempotencyKey("oversell-test");

        assertThatThrownBy(() -> bookingService.createBooking(request, userId))
                .isInstanceOf(InsufficientInventoryException.class)
                .hasMessageContaining("Not enough tickets");

        TicketCategory category = categoryRepository.findById(categoryId).orElseThrow();
        assertThat(category.getAvailableQuantity()).isEqualTo(10);
        boolean bookingExists = bookingRepository.findByIdempotencyKey("oversell-test").isPresent();
        assertThat(bookingExists).isFalse();
    }

    @Test
    void shouldEnforceUniqueConstraintOnIdempotencyKey() {
        User user = userRepository.findById(userId).orElseThrow();
        Concert concert = concertRepository.findById(concertId).orElseThrow();
        String key = "unique-key-test";
        Booking booking1 = Booking.builder()
                .user(user)
                .concert(concert)
                .idempotencyKey(key)
                .status(BookingStatus.PENDING)
                .totalPrice(BigDecimal.TEN)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();
        bookingRepository.save(booking1);
        Booking booking2 = Booking.builder()
                .user(user)
                .concert(concert)
                .idempotencyKey(key)
                .status(BookingStatus.PENDING)
                .totalPrice(BigDecimal.TEN)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();
        assertThatThrownBy(() -> bookingRepository.saveAndFlush(booking2))
                .hasStackTraceContaining("idempotency_key");
    }

    @Test
    void shouldEnforceUniqueUserVoucherConstraint() {
        BookingCreateRequest request = new BookingCreateRequest();
        request.setItems(List.of(createItemRequest(categoryId, 1)));
        request.setVoucherCode("TEST10");
        request.setIdempotencyKey("voucher-first");
        bookingService.createBooking(request, userId);

        BookingCreateRequest request2 = new BookingCreateRequest();
        request2.setItems(List.of(createItemRequest(categoryId, 1)));
        request2.setVoucherCode("TEST10");
        request2.setIdempotencyKey("voucher-second");
        assertThatThrownBy(() -> bookingService.createBooking(request2, userId))
                .hasMessageContaining("already used this voucher");
    }

    @Test
    void shouldNotCreateBookingForUnpublishedConcert() {
        Concert concert = concertRepository.findById(concertId).orElseThrow();
        concert.setStatus(ConcertStatus.DRAFT);
        concertRepository.save(concert);
        entityManager.flush();
        entityManager.clear();

        BookingCreateRequest request = new BookingCreateRequest();
        request.setItems(List.of(createItemRequest(categoryId, 1)));
        request.setIdempotencyKey("draft-test");

        assertThatThrownBy(() -> bookingService.createBooking(request, userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Concert is not available for booking");
    }

}