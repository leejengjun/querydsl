package study.querydsl.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.repository.support.Querydsl4RepositorySupport;

import java.util.List;

import static org.springframework.util.StringUtils.hasText;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.team;

/**
 * Querydsl 지원 클래스 직접 만들기
 * 스프링 데이터가 제공하는 QuerydslRepositorySupport 가 지닌 한계를 극복하기 위해 직접 Querydsl 지원 클래스를 만들어보자.
 *
 * 장점
 * 스프링 데이터가 제공하는 페이징을 편리하게 변환
 * 페이징과 카운트 쿼리 분리 가능
 * 스프링 데이터 Sort 지원
 * select() , selectFrom() 으로 시작 가능
 * EntityManager , QueryFactory 제공
 */
@Repository
public class MemberTestRepository extends Querydsl4RepositorySupport {

    public MemberTestRepository() {
        super(Member.class);
    }

    public List<Member> basicSelect() {
        return select(member)
                .from(member)
                .fetch();
    }

    public List<Member> basicSelectFrom() {
        return selectFrom(member)
                .fetch();
    }

    public Page<Member> searchPageByApplyPage(MemberSearchCondition condition, Pageable pageable) {
        JPAQuery<Member> query = selectFrom(member)
                .leftJoin(member.team, team)
                .where(usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                );

        List<Member> content = getQuerydsl().applyPagination(pageable, query)
                .fetch();

        /**
         * fetchResult() deprecated 대안
         */
        JPAQuery<Long> queryCount = select(member.count())
                .from(member)
                .where(usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                );

        return PageableExecutionUtils.getPage(content, pageable, queryCount::fetchOne);
    }

    public Page<Member> applyPagiantion(MemberSearchCondition condition, Pageable pageable) {
        return applyPagination(pageable, query -> query
                    .selectFrom(member)
                    .leftJoin(member.team, team)
                    .where(usernameEq(condition.getUsername()),
                            teamNameEq(condition.getTeamName()),
                            ageGoe(condition.getAgeGoe()),
                            ageLoe(condition.getAgeLoe())
                    )
        );
    }

    public Page<Member> applyPagiantion2(MemberSearchCondition condition, Pageable pageable) {
        return applyPagination(pageable, contentquery -> contentquery
                .selectFrom(member)
                .leftJoin(member.team, team)
                .where(usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())

                ), countQuery -> countQuery
                .select(member.id)
                .from(member)
                .leftJoin(member.team, team)
                .where(usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe()))
        );
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
}
