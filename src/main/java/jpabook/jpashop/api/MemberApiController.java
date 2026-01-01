package jpabook.jpashop.api;

import jpabook.jpashop.domain.Member;
import jpabook.jpashop.service.MemberService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
     *
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
}
