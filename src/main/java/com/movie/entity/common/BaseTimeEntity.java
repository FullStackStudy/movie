package com.movie.entity.common;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@EntityListeners(value = {AuditingEntityListener.class}) //auditind을 하기 위해 인테테 리스너 추가
@MappedSuperclass//부모클래스 상속받는 자식 클래세에 매핑정보만 제공
@Getter
@Setter
public abstract class BaseTimeEntity {
    @CreatedDate//생성 시 자동 저장
    @Column(updatable = false)
    private LocalDateTime regTime;

    @LastModifiedDate // 변경 시 자동 저장
    private LocalDateTime updateTime;
} 