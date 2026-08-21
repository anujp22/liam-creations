package com.codewithanuj.catalog.order.service;

import com.codewithanuj.catalog.order.dto.OrderCreateRequest;
import com.codewithanuj.catalog.order.dto.OrderItemRequest;
import com.codewithanuj.catalog.order.dto.OrderResponseDto;
import com.codewithanuj.catalog.order.model.CustomerOrder;
import com.codewithanuj.catalog.order.model.OrderStatus;
import com.codewithanuj.catalog.order.repository.CustomerOrderRepository;
import com.codewithanuj.catalog.product.model.Product;
import com.codewithanuj.catalog.product.model.ProductCategory;
import com.codewithanuj.catalog.product.model.ProductStatus;
import com.codewithanuj.catalog.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private CustomerOrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderCodeGenerator orderCodeGenerator;

    @InjectMocks
    private OrderService orderService;

    @BeforeEach
    void stubSave() {
        lenient().when(orderCodeGenerator.next()).thenReturn("LC-1000");
        lenient().when(orderRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private Product product(String number, String title, String price, String salePrice) {
        Product product = new Product(number, title, "d", new BigDecimal(price), "INR",
                ProductStatus.IN_STOCK, false, null, ProductCategory.BRIDAL_SAREES);
        if (salePrice != null) {
            product.setSalePrice(new BigDecimal(salePrice));
        }
        return product;
    }

    private OrderCreateRequest request(List<OrderItemRequest> items) {
        return new OrderCreateRequest(items, "Asha", "9876543210", "asha@example.com",
                "12 Rose Street", "Deliver after 6pm");
    }

    private CustomerOrder captureSaved() {
        ArgumentCaptor<CustomerOrder> saved = ArgumentCaptor.forClass(CustomerOrder.class);
        verify(orderRepository).saveAndFlush(saved.capture());
        return saved.getValue();
    }

    @Test
    void pricesEveryLineFromTheDatabaseAndSumsTheTotalItself() {
        when(productRepository.findAllById(any()))
                .thenReturn(List.of(product("PRD-1", "Red Saree", "1000.00", null)));

        OrderResponseDto dto = orderService.placeOrder(
                request(List.of(new OrderItemRequest("PRD-1", 3))));

        assertThat(dto.total()).isEqualByComparingTo("3000.00");
        assertThat(dto.items()).singleElement().satisfies(item -> {
            assertThat(item.unitPrice()).isEqualByComparingTo("1000.00");
            assertThat(item.quantity()).isEqualTo(3);
            assertThat(item.lineTotal()).isEqualByComparingTo("3000.00");
        });
    }

    @Test
    void usesTheSalePriceWhenOneIsSet() {
        // The storefront shows the sale price, so the order must charge it. Charging the
        // full price after advertising a discount is the worst possible version of this.
        when(productRepository.findAllById(any()))
                .thenReturn(List.of(product("PRD-1", "Red Saree", "1000.00", "750.00")));

        OrderResponseDto dto = orderService.placeOrder(
                request(List.of(new OrderItemRequest("PRD-1", 2))));

        assertThat(dto.total()).isEqualByComparingTo("1500.00");
    }

    @Test
    void snapshotsTheTitleAndPriceOntoTheOrderLine() {
        when(productRepository.findAllById(any()))
                .thenReturn(List.of(product("PRD-1", "Red Saree", "1000.00", null)));

        orderService.placeOrder(request(List.of(new OrderItemRequest("PRD-1", 1))));

        assertThat(captureSaved().getItems()).singleElement().satisfies(item -> {
            assertThat(item.getTitle()).isEqualTo("Red Saree");
            assertThat(item.getUnitPrice()).isEqualByComparingTo("1000.00");
        });
    }

    @Test
    void mergesTheSameProductListedTwiceIntoOneLine() {
        when(productRepository.findAllById(any()))
                .thenReturn(List.of(product("PRD-1", "Red Saree", "100.00", null)));

        OrderResponseDto dto = orderService.placeOrder(request(List.of(
                new OrderItemRequest("PRD-1", 2),
                new OrderItemRequest("PRD-1", 3))));

        assertThat(dto.items()).singleElement()
                .satisfies(item -> assertThat(item.quantity()).isEqualTo(5));
        assertThat(dto.total()).isEqualByComparingTo("500.00");
    }

    @Test
    void keepsTheLinesInTheOrderTheCustomerSawThemIn() {
        // findAllById returns whatever order the database feels like; the stored lines
        // should still read like the cart did.
        when(productRepository.findAllById(any())).thenReturn(List.of(
                product("PRD-2", "Second", "20.00", null),
                product("PRD-1", "First", "10.00", null)));

        OrderResponseDto dto = orderService.placeOrder(request(List.of(
                new OrderItemRequest("PRD-1", 1),
                new OrderItemRequest("PRD-2", 1))));

        assertThat(dto.items()).extracting("productNumber").containsExactly("PRD-1", "PRD-2");
    }

    @Test
    void rejectsTheWholeOrderWhenAProductNoLongerExists() {
        when(productRepository.findAllById(any()))
                .thenReturn(List.of(product("PRD-1", "Red Saree", "100.00", null)));

        assertThatThrownBy(() -> orderService.placeOrder(request(List.of(
                new OrderItemRequest("PRD-1", 1),
                new OrderItemRequest("PRD-GONE", 1)))))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409")
                .hasMessageContaining("PRD-GONE");

        verify(orderRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsTheWholeOrderWhenAProductWasSoftDeleted() {
        // A deleted product is invisible on the storefront but still sitting in the table,
        // so findAllById returns it. Filtering it out is the service's job.
        Product deleted = product("PRD-1", "Red Saree", "100.00", null);
        deleted.setDeleted(true);
        when(productRepository.findAllById(any())).thenReturn(List.of(deleted));

        assertThatThrownBy(() -> orderService.placeOrder(request(List.of(new OrderItemRequest("PRD-1", 1)))))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No longer available");

        verify(orderRepository, never()).saveAndFlush(any());
    }

    @Test
    void refusesToSumAcrossCurrencies() {
        Product inr = product("PRD-1", "Red Saree", "100.00", null);
        Product usd = product("PRD-2", "Imported", "100.00", null);
        usd.setCurrency("USD");
        when(productRepository.findAllById(any())).thenReturn(List.of(inr, usd));

        assertThatThrownBy(() -> orderService.placeOrder(request(List.of(
                new OrderItemRequest("PRD-1", 1),
                new OrderItemRequest("PRD-2", 1)))))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("different currencies");
    }

    @Test
    void storesTheOrderAsNewWithAGeneratedCode() {
        when(productRepository.findAllById(any()))
                .thenReturn(List.of(product("PRD-1", "Red Saree", "100.00", null)));

        OrderResponseDto dto = orderService.placeOrder(request(List.of(new OrderItemRequest("PRD-1", 1))));

        assertThat(dto.orderCode()).isEqualTo("LC-1000");
        assertThat(dto.status()).isEqualTo(OrderStatus.NEW);
    }

    @Test
    void trimsCustomerDetailsAndDropsBlankOptionalFields() {
        when(productRepository.findAllById(any()))
                .thenReturn(List.of(product("PRD-1", "Red Saree", "100.00", null)));

        orderService.placeOrder(new OrderCreateRequest(
                List.of(new OrderItemRequest("PRD-1", 1)),
                "  Asha  ", "  9876543210  ", "   ", "  12 Rose Street  ", "   "));

        CustomerOrder saved = captureSaved();
        assertThat(saved.getCustomerName()).isEqualTo("Asha");
        assertThat(saved.getCustomerPhone()).isEqualTo("9876543210");
        assertThat(saved.getCustomerAddress()).isEqualTo("12 Rose Street");
        assertThat(saved.getCustomerEmail()).as("a blank email is absent, not empty").isNull();
        assertThat(saved.getNotes()).isNull();
    }

    @Test
    void movesAnOrderToAnyStatusIncludingBackwards() {
        // Deliberate: the owner must be able to undo a mis-tap. See OrderService.updateStatus.
        CustomerOrder order = new CustomerOrder("LC-1000", "Asha", "9876543210", null,
                "12 Rose Street", null, new BigDecimal("100.00"), "INR");
        order.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findByOrderCode("LC-1000")).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrderResponseDto dto = orderService.updateStatus("LC-1000", OrderStatus.CONFIRMED);

        assertThat(dto.status()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void reports404ForAnUnknownOrderCode() {
        when(orderRepository.findByOrderCode("LC-9999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getByCode("LC-9999"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }
}
