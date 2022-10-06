package study.querydsl.dto;

import lombok.Data;

/**
 * 동적 쿼리에서 조건 객체
 */
@Data
public class MemberSearchCondition {
    //회원명, 팀명, 나이(ageGoe, ageLoe)

    private String username;
    private String teamName;
    private Integer ageGoe;
    private Integer ageLoe;

}
