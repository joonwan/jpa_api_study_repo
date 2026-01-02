package jpabook.jpashop.api;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.OrderSearch;
import jpabook.jpashop.repository.OrderSimpleQueryDto;
import jpabook.jpashop.repository.order.simplequery.OrderSimpleQueryRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

/**
 * xToOne 관계
 * Order
 * Order -> Member
 * Order -> Delivery
 *
 * 쿼리 방식 진행 순서
 *
 * 1. 우선 entity 를 dto 로 변환하는 방식을 사용하자
 * 2. 필요시 fetch join 으로 성능 최적화 하 -> 대부분의 성능 이슈가 해결된다.
 * 3. 그래도 안되면 DTO 로 직접 조회하는 방식을 사용하자.
 * 4. 최후의 방법에서는 JPA 에서 제공하는 Native SQL 이나 Spring JDBC Template 를 사용햐 SQL 을 직접 사용하자.
 */
@RestController
@RequiredArgsConstructor
public class OrderSimpleApiController {

    private final OrderRepository orderRepository;
    private final OrderSimpleQueryRepository orderSimpleQueryRepository;

    /**
     * 문제점
     * Order 는 Member 를 가지고 있음
     * Order 의 ToString 을 호출기 Member 로 감.
     * Member 에서도 ToString 을 하는데 다시 Order 감.
     * 즉 순환 적으로 참조하기 때문에 무한 루프가 발생됨.
     * 따라서 한쪽에 json ignore 처리를 해주어야 함.
     *
     * 또한 entity 수정시 api spec 또한 수정되기 때문에 별도의 DTO 로 응답하는 것이 맞음
     *
     * 정리
     * entity 를 그대로 반환할 경우 양방향 연관관계가 걸린 부분을 json ignore 처리를 해주어야 한다. 안하면 무한 루프 걸린다.
     * 간단한 application 이 아닐 경우 DTO 로 변환해서 반환하는 것이 좋은 방법이다.
     *
     * 지연 로딩을 피하기 위해서 즉시 로딩으로 변경하면 안됨.
     * 즉시 로딩을 할 경우 성능 최적화의 여지가 줄어듬. jpql 날라갈 경우 성능 최적화할 여지가 없어지지만 em.find() 같은 경우 내부적으로 join 을 해버림
     */
    @GetMapping("/api/v1/simple-orders")
    public List<Order> orderV1() {
        return orderRepository.findAllByString(new OrderSearch());
    }

    /**
     *
     * N + 1 문제 발생 (최악의 경우 - 연관된 entity 들 모두 영속성 context 에 없을 경우)
     * 주문리스트 쿼리 1번.
     * 주문 리스트 순회 -> member, delivery 각각 쿼리 나감
     * 예제는 order 2개
     * 각각 member조회 쿼리, delivery query 나감
     * 1 (order) + 2 (orders size) * 2(member query + delivery query)
     *
     * eager 로 해도 n + 1 문제 그대로 발생 -> fetch join 해야 해결 됨.
     */
    @GetMapping("/api/v2/simple-orders")
    public List<SimpleOrderDto> orderV2() {

        List<Order> orders = orderRepository.findAllByString(new OrderSearch());

        System.out.println("order size = " + orders.size());

        return orders.stream()
                .map(SimpleOrderDto::new)
                .collect(toList());
    }

    @GetMapping("/api/v3/simple-orders")
    public List<SimpleOrderDto> orderV3() {
        return orderRepository.findAllWithMemberDelivery()
                .stream()
                .map(SimpleOrderDto::new)
                .collect(toList());
    }

    /**
     * dto 변환 없이 JPA 에서 바로 DTO 로 조회해서 꺼내버리기
     * 조금더 성능 최적화가 됨. -> 원하는 데이터만 가져오기 때문임
     * 하지만 재사용성이 v3 보다는 떨어짐 그리고 v3 과 v4 가 크게 성능차이가 나지 않음
     * 만약 너무 많이 트래픽이 몰리는 api 일 경우에는 v4 를 고민해볼 필요가 있음
     *
     * orderRepository 는 entity 조회용 -> 최적화를 위해서 fetch join 정도 까지만
     * 통계용 APi 등 복잡한 api 구현시 화면에 fit 하게 최적하 하기 위해서 DTO 로 바로 조회시에는 별도의 QueryRepository 로 해당 로직을 분리 시켜 유지보수성을 향상 시키자.
     * @return
     */
    @GetMapping("/api/v4/simple-orders")
    public List<OrderSimpleQueryDto> orderV4() {
        return orderSimpleQueryRepository.findOrderDtos();
    }

    @Data
    static class SimpleOrderDto {
        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;

        public SimpleOrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName();
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getDelivery().getAddress();
        }
    }
}
