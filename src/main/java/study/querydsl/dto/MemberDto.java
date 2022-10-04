package study.querydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MemberDto {

    private String username;
    private int age;

//    public MemberDto() { //생성자를 만들면 반드시 기본 생성자를 선언해야함. 이 코드를 생략 하려면 위에 @NoArgsConstructor 어노미테이션을 적용
//    }  

    @QueryProjection
    public MemberDto(String username, int age) {
        this.username = username;
        this.age = age;
    }
}
