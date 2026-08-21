package com.codewithanuj.catalog.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * A public, unauthenticated order submission.
 *
 * <p>Notice what is <strong>not</strong> here: prices and totals. The client sends only
 * what it wants and how many; the server looks up every price itself. Anything else
 * would let a caller name their own total.
 *
 * <p>The size caps are not cosmetic. This endpoint takes personal data from anyone on
 * the internet, so the columns it writes to are bounded and so is the item list.
 */
public record OrderCreateRequest(
        @NotEmpty(message = "an order must contain at least one item")
        @Size(max = 50, message = "an order may contain at most 50 different products")
        @Valid
        List<OrderItemRequest> items,

        @NotBlank(message = "name is required")
        @Size(max = 120)
        String customerName,

        @NotBlank(message = "phone is required")
        @Size(max = 30)
        String customerPhone,

        // Optional — the WhatsApp number is the real point of contact.
        @Email(message = "email must be a valid address")
        @Size(max = 200)
        String customerEmail,

        @NotBlank(message = "delivery address is required")
        @Size(max = 500)
        String customerAddress,

        @Size(max = 1000)
        String notes
) {
}
