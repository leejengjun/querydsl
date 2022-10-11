package study.querydsl.repository;

import com.querydsl.core.QueryResults;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Wildcard;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.data.support.PageableExecutionUtils;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.entity.Member;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import java.util.List;

import static org.springframework.util.StringUtils.hasText;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

//public class MemberRepositoryImpl extends QuerydslRepositorySupport implements MemberRepositoryCustom {
public class MemberRepositoryImpl  implements MemberRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public MemberRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

//    public MemberRepositoryImpl(EntityManager em) {
//        super(Member.class);
//        this.queryFactory = new JPAQueryFactory(em);
//    }


    @Override
    public List<MemberTeamDto> search(MemberSearchCondition condition) {

        /**
         * 리포지토리 지원 - QuerydslRepositorySupport
         * 실무에서 잘 사용 안 함.
         *
         * 장점
         * getQuerydsl().applyPagination() 스프링 데이터가 제공하는 페이징을 Querydsl로 편리하게 변환
         * 가능(단! Sort는 오류발생)
         * from() 으로 시작 가능(최근에는 QueryFactory를 사용해서 select() 로 시작하는 것이 더 명시적)
         * EntityManager 제공
         *
         * 한계
         * Querydsl 3.x 버전을 대상으로 만듬
         * Querydsl 4.x에 나온 JPAQueryFactory로 시작할 수 없음
         *   - select로 시작할 수 없음 (from으로 시작해야함)
         * QueryFactory 를 제공하지 않음
         * 스프링 데이터 Sort 기능이 정상 동작하지 않음
         */
//        List<MemberTeamDto> result = from(member)
//                .leftJoin(member.team, team)
//                .where(
//                        usernameEq(condition.getUsername()),
//                        teamNameEq(condition.getTeamName()),
//                        ageGoe(condition.getAgeGoe()),
//                        ageLoe(condition.getAgeLoe())
//                )
//                .select(new QMemberTeamDto(
//                        member.id,
//                        member.username,
//                        member.age,
//                        team.id,
//                        team.name
//                ))
//                .fetch();
        /////////////////리포지토리 지원 - QuerydslRepositorySupport *End* ////////////////////////////////

        return queryFactory
                .select(new QMemberTeamDto(
                        member.id,
                        member.username,
                        member.age,
                        team.id,
                        team.name
                ))
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
                .fetch();
    }

    private BooleanExpression usernameEq(String username) {
        return hasText(username) ? member.username.eq(username) : null;
    }

    private BooleanExpression teamNameEq(String teamName) {
        return hasText(teamName) ? team.name.eq(teamName) : null;
    }

    private BooleanExpression ageGoe(Integer ageGoe) {
        return ageGoe != null ? member.age.goe(ageGoe) : null;
    }

    private BooleanExpression ageLoe(Integer ageLoe) {
        return ageLoe != null ? member.age.loe(ageLoe) : null;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 단순한 페이징, fetchResults() 사용
     */
    @Override
    public Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition, Pageable pageable) {
        QueryResults<MemberTeamDto> result = queryFactory
                .select(new QMemberTeamDto(
                        member.id,
                        member.username,
                        member.age,
                        team.id,
                        team.name
                ))
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetchResults();

        List<MemberTeamDto> content = result.getResults();
        long total = result.getTotal();

        return new PageImpl<>(content, pageable, total);

        /**
         * Querydsl이 제공하는 fetchResults() 를 사용하면 내용과 전체 카운트를 한번에 조회할 수 있다.(실제
         * 쿼리는 2번 호출)
         * fetchResult() 는 카운트 쿼리 실행시 필요없는 order by 는 제거한다
         * -> Querydsl 에서는 fetchResult()를 deprecated 했다.
         *
         *  .fetchResults() deprecated 대안
         */
//        List<MemberTeamDto> results = queryFactory
//                .select(new QMemberTeamDto(
//                        member.id,
//                        member.username,
//                        member.age,
//                        team.id,
//                        team.name
//                ))
//                .from(member)
//                .leftJoin(member.team, team)
//                .where(
//                        usernameEq(condition.getUsername()),
//                        teamNameEq(condition.getTeamName()),
//                        ageGoe(condition.getAgeGoe()),
//                        ageLoe(condition.getAgeLoe())
//                )
//                .offset(pageable.getOffset())
//                .limit(pageable.getPageSize())
//                .fetch();
//
//        int total = results.size();
//
//        return new PageImpl<>(results, pageable, total);
    }

    /**
     * 리포지토리 지원 - QuerydslRepositorySupport
     * 실무에서 사용하기에는 한계가 있다. 잘 안 씀.
     */
//    public Page<MemberTeamDto> searchPageSimple2(MemberSearchCondition condition, Pageable pageable) {
//        JPQLQuery<MemberTeamDto> jpaQuery = from(member)
//                .leftJoin(member.team, team)
//                .where(
//                        usernameEq(condition.getUsername()),
//                        teamNameEq(condition.getTeamName()),
//                        ageGoe(condition.getAgeGoe()),
//                        ageLoe(condition.getAgeLoe())
//                )
//                .select(new QMemberTeamDto(
//                        member.id,
//                        member.username,
//                        member.age,
//                        team.id,
//                        team.name
//                ));
//
//        JPQLQuery<MemberTeamDto> query = getQuerydsl().applyPagination(pageable, jpaQuery);
//
//        query.fetch();
//
//        List<MemberTeamDto> content = result.getResults();
//        long total = result.getTotal();
//
//        return new PageImpl<>(content, pageable, total);
//    }

    /**
     * 복잡한 페이징
     * 데이터 조회 쿼리와, 전체 카운트 쿼리를 분리
     */
    @Override
    public Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable) {

        List<MemberTeamDto> content = queryFactory
                .select(new QMemberTeamDto(
                        member.id,
                        member.username,
                        member.age,
                        team.id,
                        team.name
                ))
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        /**
         * 직접 토탈 카운트 쿼리를 날림
         */
//        long total = queryFactory
//                .select(Wildcard.count) //select count(*)
//                //.select(member.count()) //select count(member.id)
//                .from(member)
//                .leftJoin(member.team, team)
//                .where(
//                        usernameEq(condition.getUsername()),
//                        teamNameEq(condition.getTeamName()),
//                        ageGoe(condition.getAgeGoe()),
//                        ageLoe(condition.getAgeLoe())
//                )
//                .fetchOne();
//
//        return new PageImpl<>(content, pageable, total);

        /**
         * 전체 카운트를 조회 하는 방법을 최적화 할 수 있으면 이렇게 분리하면 된다. (예를 들어서 전체 카운트를
         * 조회할 때 조인 쿼리를 줄일 수 있다면 상당한 효과가 있다.)
         * 코드를 리펙토링해서 내용 쿼리과 전체 카운트 쿼리를 읽기 좋게 분리하면 좋다.
         */




        /**
         *  CountQuery 최적화
         *  PageableExecutionUtils.getPage()로 최적화
         *  스프링 데이터 라이브러리가 제공
         *  count 쿼리가 생략 가능한 경우 생략해서 처리
         *   페이지 시작이면서 컨텐츠 사이즈가 페이지 사이즈보다 작을 때
         *   마지막 페이지 일 때 (offset + 컨텐츠 사이즈를 더해서 전체 사이즈 구함)
         */
        JPAQuery<Long> countQuery = queryFactory
                .select(member.count())
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                );

//        return PageableExecutionUtils.getPage(content, pageable, ()-> countQuery.fetchOne());
        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }


    /**
     * 스프링 데이터 정렬(Sort)
     * 스프링 데이터 JPA는 자신의 정렬(Sort)을 Querydsl의 정렬(OrderSpecifier)로 편리하게 변경하는 기능을 제공한다.
     *
     * > 참고: 정렬( Sort )은 조건이 조금만 복잡해져도 Pageable 의 Sort 기능을 사용하기 어렵다. 루트 엔티티
     * 범위를 넘어가는 동적 정렬 기능이 필요하면 스프링 데이터 페이징이 제공하는 Sort 를 사용하기 보다는
     * 파라미터를 받아서 직접 처리하는 것을 권장한다.
     */

}
