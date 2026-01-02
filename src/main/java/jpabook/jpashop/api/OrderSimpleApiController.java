package jpabook.jpashop.api;

import jpabook.jpashop.domain.Order;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.OrderSearch;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * xToOne 관계
 * Order
 * Order -> Member
 * Order -> Delivery
 */
@RestController
@RequiredArgsConstructor
public class OrderSimpleApiController {

    private final OrderRepository orderRepository;

    /**
     * 문제점
     * Order 는 Member 를 가지고 있음
     * Order 의 ToString 을 호출기 Member 로 감.
     * Member 에서도 ToString 을 하는데 다시 Order 감.
     * 즉 순환 적으로 참조하기 때문에 무한 루프가 발생됨.
     * 따라서 한쪽에 json ignore 처리를 해주어야 함.
     *
     * 또한 entity 수정시 api spec 또한 수정되기 때문에 별도의 DTO 로 응답하는 것이 맞음
     */
    @GetMapping("/api/v1/simple-orders")
    public List<Order> orderV1() {
        return orderRepository.findAllByString(new OrderSearch());
    }
}
