package jpabook.jpashop.api;

import jpabook.jpashop.domain.Member;
import jpabook.jpashop.service.MemberService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MemberApiController {

    private final MemberService memberService;

    /**
     *
     * 문제점 : entity 를 외부에 노출
     * 1. Domain Entity 에 presentation 계층을 위한 검증 로직이 들어감.
     * 어떤 api 에서는 특정 필드가 반드시 들어가야 하지만 다른 api 에서는 빈 필드여도 될 수 있음
     * <p>
     * 2. 만약 entity 스펙이 바뀔 경우 api 스펙 또한 바뀌어 버림
     * entity 필드명이 name -> username 으로 변경될 경우 api 스펙 또한 변경되어 버림
     * 즉 entity 는 여러 곳에 공통적으로 쓰이는 객체인데 이 스펙이 바뀜으로 인해 api 스펙이 바뀌어버림
     *
     *
     */
    @PostMapping("/v1/members")
    public CreateMemberResponse saveMemberV1(@RequestBody @Valid Member member) {
        Long id = memberService.join(member);
        return new CreateMemberResponse(id);
    }

    /**
     * 장점
     * entity 수정이 일어날 경우 compile 단계에서 오류가 발생함. 따라서 미리 오류를 잡을 수 있고 entity 와 api spec 이 분리되어 영향을 안미침
     */
    @PostMapping("/v2/members")
    public CreateMemberResponse saveMemberV2(@RequestBody @Valid CreateMemberRequest request) {
        Member member = new Member();
        member.setName(request.getName());

        Long id = memberService.join(member);
        return new CreateMemberResponse(id);
    }

    /**
     *
     * command 와 query 를 분리하자.
     *
     * update 할 경우 member 를 쿼리하는 꼴이 되어 버림.
     * update 는 변경성 메서드인데 update 에서 변경된 member 의 상태를 return 하게 되면 이는 결국 update 와 조회를 한번에 하는 꼴이됨.
     * 즉 command 와 query 가 같이 있는 꼴이 되어버림.
     * 따라서 update 한 뒤 반환 값을 없애거나 pk 만 반환 하고 이후 select 를 다시 날림. -> command 와 query 를 분리
     * 단순 pk 로 조회 하는 것은 traffic 이 몰리는 api 가 아닌 경우 크게 성능 이슈가 안됨
     * 이렇게 할 경유 유지보수성이 증대됨
     *
     * command : create, update, delete 즉 시스템의 상태를 변경하는 작업
     * query : select 즉 시스템의 상태를 조회하는 작업
     *
     */
    @PutMapping("/v2/members/{id}")
    public UpdateMemberResponse updateMemberV2(
            @PathVariable Long id,
            @RequestBody @Valid UpdateMemberRequest request) {
        memberService.update(id, request.getName());

        Member findMember = memberService.findOne(id);

        return new UpdateMemberResponse(findMember.getId(), findMember.getName());
    }

    @Data
    static class CreateMemberResponse {
        private Long id;

        public CreateMemberResponse(Long id) {
            this.id = id;
        }
    }

    @Data
    static class CreateMemberRequest {
        private String name;
    }

    @Data
    static class UpdateMemberRequest {
        private String name;
    }

    @Data
    @AllArgsConstructor// dto 에는 lombok annotation 막 써도 되지만 entity 에는 getter 정도만 사용 ...
    static class UpdateMemberResponse {
        private Long id;
        private String name;
    }
}
