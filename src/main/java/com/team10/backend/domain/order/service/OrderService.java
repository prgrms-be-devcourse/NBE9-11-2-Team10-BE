package com.team10.backend.domain.order.service;

import com.team10.backend.domain.order.dto.OrderCreateRequest;
import com.team10.backend.domain.order.dto.search.OrderDetailResponse;
import com.team10.backend.domain.order.dto.search.buyer.OrderListResponse;
import com.team10.backend.domain.order.dto.OrderResponse;
import com.team10.backend.domain.order.dto.search.buyer.OrderSummaryResponse;
import com.team10.backend.domain.order.dto.search.seller.SellerOrderListResponse;
import com.team10.backend.domain.order.dto.search.seller.SellerOrderSummaryResponse;
import com.team10.backend.domain.order.entity.Order;
import com.team10.backend.domain.order.entity.OrderDelivery;
import com.team10.backend.domain.order.entity.OrderProducts;
import com.team10.backend.domain.order.repository.OrderProductRepository;
import com.team10.backend.domain.order.repository.OrderRepository;
import com.team10.backend.domain.product.entity.Product;
import com.team10.backend.domain.product.repository.ProductRepository;
import com.team10.backend.domain.user.entity.User;
import com.team10.backend.domain.user.enums.Role;
import com.team10.backend.domain.user.repository.UserRepository;
import com.team10.backend.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.team10.backend.global.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderProductRepository orderProductRepository;

    @Transactional
    public OrderResponse createOrder(OrderCreateRequest req) {

        // 1. 주문자 조회
        User user = findUser(req.userId());

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
    public User findUser(Long userId) {
        return userRepository.findById(userId)
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

    // [Buyer] 주문한 내역 전체 조회
    @Transactional(readOnly = true)
    public OrderListResponse getBuyerOrderList(Long userId) {
        // 유저 있는지 없는지 확인
        User user = findUser(userId);
        if (user.getRole() != Role.BUYER) {
            //판매자 ID일 경우
            throw new BusinessException(ACCESS_DENIED);
        }

        // 해당 유저의 모든 주문 내역을 보여준다. 최신순
        List<Order> userOrderList = orderRepository.findAllByUserIdOrderByCreatedAtDesc(userId);

        // 3. 주문 엔티티 리스트를 DTO 리스트로 변환
        List<OrderSummaryResponse> summaryResponses = userOrderList.stream()
                .map(OrderSummaryResponse::from)
                .toList();

        // 4. 회원 정보와 주문 목록을 결합하여 반환
        return OrderListResponse.of(user, summaryResponses);
    }

    // [Seller] 나에게 들어온 판매 내역 전체 조회
    @Transactional(readOnly = true)
    public SellerOrderListResponse getSellerOrderList(Long sellerId) {
        // 판매자 존재 확인
        User seller = findUser(sellerId);
        if (seller.getRole() != Role.SELLER) {
            //구매자 ID일 경우
            throw new BusinessException(ACCESS_DENIED);
        }

        // 이 판매자가 등록한 상품들이 포함된 '주문 상품(OrderProducts)'들을 가져옴
        // OrderProducts를 통해 Order에 접근하는 방식이 정확합니다.
        List<OrderProducts> sellerSales = orderProductRepository.findAllBySellerId(sellerId);

        //  OrderProducts 리스트를 SellerOrderSummaryResponse 리스트로 변환
        List<SellerOrderSummaryResponse> summaryResponses = sellerSales.stream()
                .map(SellerOrderSummaryResponse::from) // 위에서 만든 정적 팩토리 메서드 활용
                .toList();

        return SellerOrderListResponse.of(seller, summaryResponses);
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderDetail(Long curUserId, String orderNumber) {

        // 1. 주문 상세 조회 (없으면 예외 발생)
        Order order = orderRepository.findByOrderNumberWithDetails(orderNumber)
                .orElseThrow(() -> new BusinessException(ORDER_NOT_FOUND));

        User user = findUser(curUserId);

        if (user.getRole() == Role.BUYER) {
            // 구매자라면: 주문서의 주인인지 확인
            if (!order.getUser().getId().equals(curUserId)) {
                throw new BusinessException(ACCESS_DENIED);
            }
        }else if (user.getRole()  == Role.SELLER) {
            // 판매자라면: order_product 테이블에서 해당 주문 안에 '내 상품'이 하나라도 포함되어 있는지 확인한다.
            boolean isSellerOfThisOrder = order.getOrderProducts().stream()
                    .anyMatch(op -> op.getProduct().getUser().getId().equals(curUserId));

            if (!isSellerOfThisOrder) {
                throw new BusinessException(ACCESS_DENIED);
            }
        }

        // 3. DTO 변환 및 반환
        return OrderDetailResponse.from(order);
    }
}
