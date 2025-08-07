// package com.melog.melog.clova.domain;

// import java.time.LocalDateTime;

// import jakarta.persistence.Entity;
// import jakarta.persistence.EnumType;
// import jakarta.persistence.Enumerated;
// import jakarta.persistence.GeneratedValue;
// import jakarta.persistence.GenerationType;
// import jakarta.persistence.Id;
// import jakarta.persistence.Table;
// import lombok.AccessLevel;
// import lombok.AllArgsConstructor;
// import lombok.Builder;
// import lombok.Getter;
// import lombok.NoArgsConstructor;

// import com.melog.melog.clova.domain.model.ClovaEndpoint;

// @Entity
// @Table(name = "clova_api_history")
// @Getter
// @NoArgsConstructor(access = AccessLevel.PROTECTED)
// @AllArgsConstructor
// @Builder
// public class ClovaApiHistory {

//     @Id
//     @GeneratedValue(strategy = GenerationType.IDENTITY)
//     private Long id;

//     @Enumerated(EnumType.STRING)
//     private ClovaEndpoint endpoint;

//     private String requestData;
//     private String responseData;
//     private String errorMessage;
//     private Long responseTime; // 응답 시간 (ms)
//     private LocalDateTime createdAt;
//     private String userId; // API를 호출한 사용자 ID (선택사항)
// }
