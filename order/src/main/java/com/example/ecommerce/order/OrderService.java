package com.example.ecommerce.order;

import com.example.ecommerce.customer.CustomerClient;
import com.example.ecommerce.exception.BusinessException;
import com.example.ecommerce.orderline.OrderLineRequest;
import com.example.ecommerce.orderline.OrderLineService;
import com.example.ecommerce.product.ProductClient;
import com.example.ecommerce.product.PurchaseRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository repository;
    private final CustomerClient customerClient;
    private final ProductClient productClient;
    private final OrderMapper mapper;
    private final OrderLineService orderLineService;

    public Integer createOrder(OrderRequest request) {
        // checking the customer --> using OpenFeign
        var customer = customerClient.findCustomerById(request.customerId())
                .orElseThrow(() ->
                        new BusinessException("Cannot create order:: No Customer exists with the provided ID"));

        // purchase the products --> using product-ms (RestTemplate)
        this.productClient.purchaseProducts(request.products());

        var order = this.repository.save(mapper.toOrder(request));

        // persist order line
        for (PurchaseRequest purchaseRequest : request.products()) {
            orderLineService.saveOrderLine(
                    new OrderLineRequest(
                            null,
                            order.getId(),
                            purchaseRequest.productId(),
                            purchaseRequest.quantity()
                    )
            );
        }

        // todo start payment process

        // todo send order confirmation --> notification-ms (kafka)

        return null;

    }
}
