package com.geek.booking.concurrent;

import com.geek.booking.dto.request.booking.BookingCreateRequest;
import com.geek.booking.dto.request.booking.BookingItemRequest;
import com.geek.booking.dto.response.BookingCreationResult;
import com.geek.booking.entity.*;
import com.geek.booking.enums.ConcertStatus;
import com.geek.booking.enums.DiscountType;
import com.geek.booking.enums.UserRole;
import com.geek.booking.exception.InsufficientInventoryException;
import com.geek.booking.repository.*;
import com.geek.booking.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class ConcurrentBookingTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.datasource.hikari.maximumPoolSize", () -> "30");
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

    private BookingItemRequest createItemRequest(Long categoryId, int quantity) {
        BookingItemRequest item = new BookingItemRequest();
        item.setTicketCategoryId(categoryId);
        item.setQuantity(quantity);
        return item;
    }

    @Test
    void concurrentBooking_shouldNotOversell() throws Exception {
        // Tạo dữ liệu riêng cho test này
        String uniqueId = UUID.randomUUID().toString();
        User user = User.builder()
                .email("oversell_" + uniqueId + "@example.com")
                .name("Oversell User")
                .password("encoded")
                .role(UserRole.CUSTOMER)
                .build();
        user = userRepository.save(user);
        Long userId = user.getId();

        Concert concert = Concert.builder()
                .name("Concert Oversell " + uniqueId)
                .venue("Test Venue")
                .eventDate(LocalDateTime.now().plusDays(5))
                .status(ConcertStatus.PUBLISHED)
                .build();
        concert = concertRepository.save(concert);

        TicketCategory category = TicketCategory.builder()
                .concert(concert)
                .name("General")
                .price(BigDecimal.valueOf(50))
                .totalQuantity(5)
                .availableQuantity(5)
                .build();
        category = categoryRepository.save(category);
        Long categoryId = category.getId();

        int totalTickets = 5;
        int numberOfThreads = 30;
        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        List<Future<BookingCreationResult>> futures = new ArrayList<>();

        for (int i = 0; i < numberOfThreads; i++) {
            String key = UUID.randomUUID().toString();
            BookingCreateRequest request = new BookingCreateRequest();
            request.setItems(List.of(createItemRequest(categoryId, 1)));
            request.setIdempotencyKey(key);
            futures.add(executor.submit(() -> {
                startLatch.await();
                return bookingService.createBooking(request, userId);
            }));
        }

        startLatch.countDown();
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        int successCount = 0, failureCount = 0;
        for (Future<BookingCreationResult> future : futures) {
            try {
                future.get();
                successCount++;
            } catch (ExecutionException e) {
                if (e.getCause() instanceof InsufficientInventoryException) failureCount++;
                else throw new RuntimeException(e);
            }
        }
        assertThat(successCount).isEqualTo(totalTickets);
        assertThat(failureCount).isEqualTo(numberOfThreads - totalTickets);

        TicketCategory updated = categoryRepository.findById(categoryId).orElseThrow();
        assertThat(updated.getAvailableQuantity()).isZero();
        assertThat(bookingRepository.findByUserId(userId)).hasSize(totalTickets);
    }


    @Test
    void concurrentVoucherUsage_shouldRespectLimit() throws Exception {
        int usageLimit = 3;
        int numberOfThreads = 5; // 5 users, chỉ 3 voucher thành công
        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        List<Future<BookingCreationResult>> futures = new ArrayList<>();
        List<Long> userIds = new ArrayList<>();

        Voucher voucher = voucherRepository.save(Voucher.builder()
                .code("VOUCHER_LIMIT_" + UUID.randomUUID())
                .discountType(DiscountType.PERCENT)
                .discountValue(BigDecimal.valueOf(10))
                .usageLimit(usageLimit)
                .usedCount(0)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validTo(LocalDateTime.now().plusDays(1))
                .minOrderValue(BigDecimal.ZERO)
                .build());

        Concert concert = concertRepository.save(Concert.builder()
                .name("Concert Voucher " + UUID.randomUUID())
                .venue("Venue")
                .eventDate(LocalDateTime.now().plusDays(5))
                .status(ConcertStatus.PUBLISHED)
                .build());
        TicketCategory category = categoryRepository.save(TicketCategory.builder()
                .concert(concert)
                .name("General")
                .price(BigDecimal.valueOf(50))
                .totalQuantity(20)
                .availableQuantity(20)
                .build());

        for (int i = 0; i < numberOfThreads; i++) {
            User user = userRepository.save(User.builder()
                    .email("voucher_user_" + UUID.randomUUID() + "@example.com")
                    .name("Voucher User " + i)
                    .password("encoded")
                    .role(UserRole.CUSTOMER)
                    .build());
            userIds.add(user.getId());
        }

        for (int i = 0; i < numberOfThreads; i++) {
            final Long userId = userIds.get(i);
            String idempotencyKey = UUID.randomUUID().toString();
            BookingCreateRequest request = new BookingCreateRequest();
            request.setItems(List.of(createItemRequest(category.getId(), 1)));
            request.setVoucherCode(voucher.getCode());
            request.setIdempotencyKey(idempotencyKey);
            futures.add(executor.submit(() -> {
                startLatch.await();
                return bookingService.createBooking(request, userId);
            }));
        }

        startLatch.countDown();
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        int successCount = 0, failureCount = 0;
        for (Future<BookingCreationResult> future : futures) {
            try {
                future.get();
                successCount++;
            } catch (ExecutionException e) {
                failureCount++;
            }
        }
        assertThat(successCount).isEqualTo(usageLimit);
        assertThat(failureCount).isEqualTo(numberOfThreads - usageLimit);

        Voucher updated = voucherRepository.findById(voucher.getId()).orElseThrow();
        assertThat(updated.getUsedCount()).isEqualTo(usageLimit);
    }
}