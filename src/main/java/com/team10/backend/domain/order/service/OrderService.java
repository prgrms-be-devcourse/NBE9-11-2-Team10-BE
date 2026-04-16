package com.team10.backend.domain.order.service;

import com.team10.backend.domain.order.dto.OrderCreateRequest;
import com.team10.backend.domain.order.dto.OrderResponse;
import com.team10.backend.domain.order.entity.Order;
import com.team10.backend.domain.order.entity.OrderDelivery;
import com.team10.backend.domain.order.entity.OrderProducts;
import com.team10.backend.domain.order.repository.OrderRepository;
import com.team10.backend.domain.product.entity.Product;
import com.team10.backend.domain.product.repository.ProductRepository;
import com.team10.backend.domain.user.entity.User;
import com.team10.backend.domain.user.repository.UserRepository;
import com.team10.backend.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.team10.backend.global.exception.ErrorCode.PRODUCT_NOT_FOUND;
import static com.team10.backend.global.exception.ErrorCode.USER_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Transactional
    public OrderResponse createOrder(OrderCreateRequest req) {

        // 1. 주문자 조회
        User user = findUser(req);

        // 2. 배송 정보 엔티티 생성
        OrderDelivery delivery = deliveryInfo(req);

        // 3. 주문 상품(OrderProducts) 리스트 생성
        List<OrderProducts> orderProductsList = getOrderProductList(req);

        // 예: ORD20240414-a1b2c3d4
        // 토스 페이먼츠에서 단순히 orderId로 1,2,3 증가하는 형식이면 안된다.
        //제약 사항: 영문 대소문자, 숫자, 특수문자(-, _)를 포함한 6자 이상 64자 이하의 문자열이어야 한다.
        String orderNumber = "ORD-" + LocalDate.now().toString().replace("-", "")
                + "-" + UUID.randomUUID().toString().substring(0, 8);

        //최종적으로 주문 생성
        Order order = Order.createOrder(user, orderNumber, delivery, orderProductsList);

        // 5. 저장
        // Order 엔티티에 cascade = CascadeType.ALL 설정을 했기 때문에,
        // order만 save해도 연관된 OrderDelivery와 OrderProducts,Payment가 모두 함께 DB에 저장됩니다.
        orderRepository.save(order);

        //응답할때 OrderNumber(토스 페이먼츠가 원하는 형식), totalAmout, userId을 돌려준다.
        return OrderResponse.from(order);
    }

    //유저 찾기
    public User findUser(OrderCreateRequest request) {
        return userRepository.findById(request.userId())
                .orElseThrow(() ->new BusinessException(USER_NOT_FOUND,"해당 유저 정보를 찾을 수 없습니다."));
    }

    //order-delivery 테이블에 배송지, 운송장 번호 생성
    public OrderDelivery deliveryInfo(OrderCreateRequest request) {
        return  OrderDelivery.builder()
                .delivery_address(request.deliveryAddress())
                .tracking_number(null) // 송장 번호를 여기서 만들어야 하나?
                .build();
    }

    //order-product테이블에 상품id, 상품 수량, 상품 가격을 넣는다.
    public List<OrderProducts> getOrderProductList(OrderCreateRequest request) {
        return request.orderProducts().stream()
                .map(orderProdctReq->{
                    Product product = productRepository.findById(orderProdctReq.productId())
                            .orElseThrow(() -> new BusinessException(PRODUCT_NOT_FOUND,"상품을 찾을 수 없습니다. ID: " + orderProdctReq.productId()));

                    //todo 재고 감소 로직

                    //OrderProduct 테이블에 저장
                    return OrderProducts.builder()
                            .product(product)
                            .quantity(orderProdctReq.quantity())
                            .orderPrice(product.getPrice())
                            .build();
                }).toList();
    }

}
