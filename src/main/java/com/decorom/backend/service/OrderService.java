package com.decorom.backend.service;

import com.decorom.backend.entity.Order;
import com.decorom.backend.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    public Order saveOrder(Order order) {
        return orderRepository.save(order);
    }

    public Optional<Order> getOrderById(UUID id) {
        return orderRepository.findById(id);
    }
}
