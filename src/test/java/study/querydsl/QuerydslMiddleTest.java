package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.*;

@SpringBootTest
@Transactional
public class QuerydslMiddleTest {
    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(em);
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);

        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    /**
     * 프로젝션과 결과 반환 - 기본
     * 프로젝션: select 대상 지정
     */

    /**
     * 프로젝션 대상이 하나
     */
    @Test
    public void simpleProjection() {
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
        /**
         * 프로젝션 대상이 하나면 타입을 명확하게 지정할 수 있음
         * 프로젝션 대상이 둘 이상이면 튜플이나 DTO로 조회
         */
    }


    /**
     * 튜플 조회
     * 프로젝션 대상이 둘 이상일 때 사용
     */
    @Test
    public void tupleProjection() {
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username = " + username);
            System.out.println("age = " + age);
        }
    }

    /**
     * 순수 JPA에서 DTO 조회 코드
     *
     * 순수 JPA에서 DTO를 조회할 때는 new 명령어를 사용해야함
     * DTO의 package이름을 다 적어줘야해서 지저분함
     * 생성자 방식만 지원함
     */
    @Test
    public void findDtoByJPQL() {
        List<MemberDto> rseult = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
                .getResultList();

        for (MemberDto memberDto : rseult) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * Querydsl 빈 생성(Bean population)
     *
     * 결과를 DTO 반환할 때 사용
     * 다음 3가지 방법 지원
     *
     * 프로퍼티 접근
     * 필드 직접 접근
     * 생성자 사용
     */

    /**
     * 프로퍼티 접근 - Setter
     */
    @Test
    public void findDtoBySetter() {
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * 필드 직접 접근
     * Getter Setter 가 없어도 가능
     * 즉 MemberDto 에 있는 username, age 필드에 직접 접근
     */
    @Test
    public void findDtoByField() {
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * 별칭이 다를 때
     */
    @Test
    public void findUserDtoByField() {
        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"),
                        member.age))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
        /**
         * UserDto에서 username이 아니라 name으로 해서 별칭이 다를경우에는 값이 담기지 않는다, 매칭실패
         * userDto = UserDto(name=null, age=10)
         * userDto = UserDto(name=null, age=20)
         * userDto = UserDto(name=null, age=30)
         * userDto = UserDto(name=null, age=40)
         */
    }

    /**
     * 프로퍼티나, 필드 접근 생성 방식에서 이름이 다를 때 해결 방안
     * ExpressionUtils.as(source,alias) : 필드나, 서브 쿼리에 별칭 적용
     * username.as("memberName") : 필드에 별칭 적용
     */
    @Test
    public void findUserDtoByField_Ver2() {
        QMember memberSub = new QMember("memberSub");

        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"),

                        ExpressionUtils.as(JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub), "age") // 서브쿼리 별칭해서 dto 필드에 as 해줌
                ))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    /**
     * 생성자 사용
     * 주의 할 점: constructor를 사용하면 MemberDto의 생성자의 타입과 memeber 객체에 있는 필드 타입을 맞춰야 한다.
     * 안 맞추면 에러 발생함.
     * com.querydsl.core.types.ExpressionException: No constructor found for class study.querydsl.dto.MemberDto with parameters: [class java.lang.String, class java.lang.Integer]
     *
     */
    @Test
    public void findDtoByConstructor() {
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findDtoByConstructor_2() {
        List<UserDto> result = queryFactory
                .select(Projections.constructor(UserDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    /**
     * 프로젝션과 결과 반환 - @QueryProjection
     * 생성자 사용하는 케이스 비슷하지만 다르다.
     * 생성자를 사용한 프로젝션은 컴파일 오류를 못잡음 -> 런타임(실행)되어서야 오류가 발생!
     * @QueryProjection을 사용한 프로젝션은 컴파일 시점에서 오류를 잡음 QMemberDto에서 미리 생성자를 만들어 둠!
     *
     * 단,
     * ./gradlew compileQuerydsl
     * QMemberDto 생성 확인
     * 위 과정을 해주어야 한다.
     *
     * 이 방법은 컴파일러로 타입을 체크할 수 있으므로 가장 안전한 방법이다. 다만 DTO에 QueryDSL
     * 어노테이션을 유지해야 하는 점과 DTO까지 Q 파일을 생성해야 하는 단점이 있다.
     *
     * 단점
     * Q파일을 생성해야함?!
     * DTO가 Querydsl에 의존성을 가지게 됨.
     */
    @Test
    public void findDtoByQueryProjection() {
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     *  distinct 
     *  -> JPQL의 distinct와 같다.
     */
    @Test
    public void distinct() {
        List<String> result = queryFactory
                .select(member.username).distinct()
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println(" s = " + s);
        }
    }

    /**
     * 동적 쿼리 - BooleanBuilder 사용
     * 동적 쿼리를 해결하는 두가지 방식
     *  BooleanBuilder
     *  Where 다중 파라미터 사용(실무 추천)
     */

    /**
     * BooleanBuilder 사용
     */
    @Test
    public void dynamicQuery_BooleanBuilder() {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember1(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {

        BooleanBuilder builder = new BooleanBuilder();
        if (usernameCond != null) {
            builder.and(member.username.eq(usernameCond));
        }

        if (ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }

        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    /**
     * Where 다중 파라미터 사용
     * where 절에 파라미터를 메소드로 뺌.
     *
     * where 조건에 null 값은 무시된다.
     * 메서드를 다른 쿼리에서도 재활용 할 수 있다.(조립이 가능)
     * 쿼리 자체의 가독성이 높아진다. 코드가 깔끔해진다.
     *
     * null 체크는 주의해서 처리해야함
     */
    @Test
    public void dynamicQuery_WhereParam() {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember2(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory
                .selectFrom(member)
                .where(usernameEq(usernameCond), ageEq(ageCond))
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameCond) {
        return usernameCond != null ? member.username.eq(usernameCond) : null;
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;
    }

    private BooleanExpression allEq(String usernameCond, Integer ageCond) {
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }


    /**
     * 수정, 삭제 벌크 연산
     */


    /**
     * 쿼리 한번으로 대량 데이터 수정
     */
    @Test
    @Commit
    public void bulkUpdate() {

        //member1 = 10 -> DB member1
        //member2 = 20 -> DB member2
        //member3 = 30 -> DB member3
        //member4 = 40 -> DB member4

        long count = queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();

        // 아래 영속성 컨텍스트의 내용과 DB의 데이터가 일치하지 않는 문제를 해결하는 방법
        // 벌크 연산 후 반드시, '영속성 컨텍스트를 비워주자'
        em.flush();
        em.clear();

        //1 member1 = 10 -> 1 DB 비회원
        //2 member2 = 20 -> 2 DB 비회원
        //3 member3 = 30 -> 3 DB member3
        //4 member4 = 40 -> 4 DB member4

        List<Member> result = queryFactory
                .selectFrom(member)
                .fetch();

        for (Member member1 : result) {
            System.out.println("member1 = " + member1);
        }
        /**
         * H2 DB Member 테이블에 저장 된 데이터
         * MEMBER_ID  	AGE  	USERNAME  	TEAM_ID
         * 3	10	비회원	1
         * 4	20	비회원	1
         * 5	30	member3	2
         * 6	40	member4	2
         *
         * 인텔리 j 콘솔 화면 출력 결과
         * member1 = Member(id=3, username=member1, age=10)
         * member1 = Member(id=4, username=member2, age=20)
         * member1 = Member(id=5, username=member3, age=30)
         * member1 = Member(id=6, username=member4, age=40)
         */
        // 업데이트를 하려고 DB에 값을 가져왔는데 영속성 컨텍스트에 이미 해당 member가 있으면 DB에서 가져온 값을 버린다.
    }


    /**
     * 기존 숫자에 1 더하기(1 빼기)
     */
    @Test
    @Commit
    public void bulkAddandSub() {
        long count = queryFactory
                .update(member)
                .set(member.age, member.age.add(1)) //벌크덧셈
//                .set(member.age, member.age.add(-1)) //벌크뺄셈 
                .execute();

        /**
         * update
         *    member
         * set
         *    age=age+?
         */
    }


    /**
     * 기존 나이 숫자에 2 곱하기
     */
    @Test
    @Commit
    public void bulkMultiply() {
        long count = queryFactory
                .update(member)
                .set(member.age, member.age.multiply(2))    //벌크곱셈
                .execute();

        /**
         * update
         *    member
         * set
         *    age=age*?
         */
    }

    /**
     * 쿼리 한 번으로 대량 데이터 삭제
     */
    @Test
    @Commit
    public void bulkDelete() {
        queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();

        /**
         * delete
         * from
         *   member
         * where
         *   age>?
         */
    }
    /**
     * 주의: JPQL 배치와 마찬가지로, 영속성 컨텍스트에 있는 엔티티를 무시하고 실행되기 때문에 배치 쿼리를
     * 실행하고 나면 영속성 컨텍스트를 초기화 하는 것이 안전하다.
     */

    @Test
    public void sqlFunction() {

        List<String> result = queryFactory
                .select(Expressions.stringTemplate(
                        "function('replace', {0}, {1}, {2})",
                        member.username, "member", "M"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void sqlFunction2() {
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
//                .where(member.username.eq(Expressions.stringTemplate("function('lower', {0})", member.username)))
                /**
                 * lower 같은 ansi 표준 함수들은 querydsl이 상당부분 내장하고 있다. 따라서 다음과 같이 처리해도
                 * 결과는 같다
                 */
                .where(member.username.eq(member.username.lower()))
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
        /**
         * select
         *    member0_.username as col_0_0_
         * from
         *    member member0_
         * where
         *    member0_.username=lower(member0_.username)
         */

    }

}
