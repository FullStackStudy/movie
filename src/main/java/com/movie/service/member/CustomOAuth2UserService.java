package com.movie.service.member;

import com.movie.entity.member.Member;
import com.movie.repository.member.MemberRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final MemberRepository memberRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        try {
            System.out.println("🔐 OAuth2 로그인 시작");
            System.out.println("📋 Registration ID: " + userRequest.getClientRegistration().getRegistrationId());
            
            OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
            OAuth2User oAuth2User = delegate.loadUser(userRequest);
            
            System.out.println("✅ OAuth2 사용자 정보 로드 성공");
            System.out.println("📧 사용자 속성: " + oAuth2User.getAttributes());

            String registrationId = userRequest.getClientRegistration().getRegistrationId();
            String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails()
                    .getUserInfoEndpoint().getUserNameAttributeName();

            System.out.println("🔍 Registration ID: " + registrationId);
            System.out.println("🔑 User Name Attribute: " + userNameAttributeName);

            OAuthAttributes attributes = OAuthAttributes.of(registrationId, userNameAttributeName, oAuth2User.getAttributes());

            if (attributes == null) {
                System.out.println("❌ 지원하지 않는 OAuth2 제공자: " + registrationId);
                throw new OAuth2AuthenticationException("지원하지 않는 OAuth2 제공자입니다: " + registrationId);
            }

            System.out.println("✅ OAuth 속성 파싱 성공");
            System.out.println("👤 이름: " + attributes.getName());
            System.out.println("📧 이메일: " + attributes.getEmail());
            System.out.println("🖼️ 프로필: " + attributes.getPicture());

            // 이메일 검증 (임시 이메일도 허용)
            if (attributes.getEmail() == null || attributes.getEmail().trim().isEmpty()) {
                System.out.println("❌ 이메일 정보 없음");
                throw new OAuth2AuthenticationException("이메일 정보를 가져올 수 없습니다.");
            }

            Member member = saveOrUpdate(attributes);
            System.out.println("✅ 회원 저장/업데이트 완료: " + member.getMemberId());

            // OIDC 사용자이면 OidcUser로 반환
            if (oAuth2User instanceof OidcUser) {
                OidcUser oidcUser = (OidcUser) oAuth2User;
                return new DefaultOidcUser(
                        Collections.singleton(new SimpleGrantedAuthority(member.getRole().toString())),
                        oidcUser.getIdToken(),
                        oidcUser.getUserInfo(),
                        "id" // nameAttributeKey를 "id"로 설정 (카카오 ID 사용)
                );
            } else {
                return new DefaultOAuth2User(
                    Collections.singleton(new SimpleGrantedAuthority(member.getRole().toString())),
                    attributes.getAttributes(),
                    "id" // nameAttributeKey를 "id"로 설정 (카카오 ID 사용)
            );
            }
        } catch (Exception e) {
            System.out.println("❌ OAuth2 로그인 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            throw new OAuth2AuthenticationException("OAuth2 로그인 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    private Member saveOrUpdate(OAuthAttributes attributes) {
        try {
            Optional<Member> existingMember = memberRepository.findById(attributes.getEmail());

            Member member;
            if (existingMember.isPresent()) {
                member = updateExistingUser(existingMember.get(), attributes);

                Member savedMember = memberRepository.save(member);
                return savedMember;
            } else {
                member = attributes.toEntity();

                // OAuth2 사용자는 중복 검사 없이 직접 저장
                Member savedMember = memberRepository.save(member);
                System.out.println("✅ 새 OAuth2 회원 생성 완료: " + savedMember.getMemberId());
                System.out.println("📋 저장된 회원 정보 - ID: " + savedMember.getMemberId() + ", 이름: " + savedMember.getName());
                
                // 저장 후 다시 조회하여 확인
                Optional<Member> verifyMember = memberRepository.findById(savedMember.getMemberId());
                System.out.println("🔍 저장 확인 조회: " + (verifyMember.isPresent() ? "성공" : "실패"));
                
                return savedMember;
            }
        } catch (Exception e) {
            System.out.println("❌ 회원 저장/업데이트 실패: " + e.getMessage());
            System.out.println("❌ 예외 타입: " + e.getClass().getSimpleName());
            e.printStackTrace();
            throw e;
        }
    }

    private Member updateExistingUser(Member member, OAuthAttributes attributes) {
        member.setName(attributes.getName());
        member.setNickname(attributes.getName());
        member.setProfile(attributes.getPicture());
        return member;
    }

    @Getter
    public static class OAuthAttributes {
        private Map<String, Object> attributes;
        private String nameAttributeKey;
        private String name;
        private String email;
        private String picture;

        public static OAuthAttributes of(String registrationId,
                                         String userNameAttributeName,
                                         Map<String, Object> attributes) {
            if ("google".equals(registrationId)) {
                return ofGoogle(userNameAttributeName, attributes);
            } else if ("kakao".equals(registrationId)) {
                return ofKakao(userNameAttributeName, attributes);
            }
            return null;
        }

        private static OAuthAttributes ofGoogle(String userNameAttributeName,
                                                Map<String, Object> attributes) {
            OAuthAttributes oAuthAttributes = new OAuthAttributes();
            oAuthAttributes.name = (String) attributes.get("name");
            oAuthAttributes.email = (String) attributes.get("email");
            oAuthAttributes.picture = (String) attributes.get("picture");
            oAuthAttributes.attributes = attributes;
            oAuthAttributes.nameAttributeKey = userNameAttributeName;
            return oAuthAttributes;
        }

        private static OAuthAttributes ofKakao(String userNameAttributeName,
                                               Map<String, Object> attributes) {
            OAuthAttributes oAuthAttributes = new OAuthAttributes();
            
            // 카카오는 attributes에서 kakao_account와 properties를 통해 정보를 제공
            Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
            Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");
            
            oAuthAttributes.name = properties != null ? (String) properties.get("nickname") : "카카오 사용자";
            
            // 카카오에서 실제 이메일을 제공하는 경우 우선 사용
            String email = null;
            if (kakaoAccount != null) {
                email = (String) kakaoAccount.get("email");
            }
            
            // 이메일이 없는 경우에만 카카오 ID를 사용
            if (email == null || email.trim().isEmpty()) {
                String kakaoId = String.valueOf(attributes.get("id"));
                email = kakaoId + "@kakao.com";
                System.out.println("⚠️ 카카오 이메일 정보 없음, 카카오 ID 사용: " + email);
            } else {
                System.out.println("✅ 카카오 실제 이메일 사용: " + email);
            }
            
            oAuthAttributes.email = email;
            oAuthAttributes.picture = properties != null ? (String) properties.get("profile_image") : null;
            oAuthAttributes.attributes = attributes;
            oAuthAttributes.nameAttributeKey = userNameAttributeName;
            
            return oAuthAttributes;
        }

        public Member toEntity() {
            Member member = new Member();
            member.setMemberId(email);
            member.setName(name != null ? name : "사용자");
            member.setNickname(name != null ? name : "닉네임 미입력");
            member.setProfile(picture != null ? picture : "default-profile.png");
            member.setRole(com.movie.constant.Role.USER);
            member.setRegDate(LocalDate.now());
            member.setGrade("일반");
            member.setPassword("OAUTH2_USER");
            member.setBirth(LocalDate.of(1990, 1, 1));
            member.setPhone("000-0000-0000");
            member.setAddress("주소 미입력");
            member.setPoint("0"); // 포인트 초기값 설정
            member.setReserve("예약 내역이 없습니다."); // 예약 내역 초기값 설정
            member.setInquiry("0"); // 문의 내역 초기값 설정
            
            System.out.println("🏗️ OAuth2 회원 엔티티 생성:");
            System.out.println("   - 이메일: " + member.getMemberId());
            System.out.println("   - 이름: " + member.getName());
            System.out.println("   - 닉네임: " + member.getNickname());
            System.out.println("   - 프로필: " + member.getProfile());
            System.out.println("   - 역할: " + member.getRole());
            System.out.println("   - 등급: " + member.getGrade());
            System.out.println("   - 포인트: " + member.getPoint());
            
            return member;
        }

        public Map<String, Object> getAttributes() {
            return attributes;
        }

        public String getNameAttributeKey() {
            return nameAttributeKey;
        }

        public String getName() {
            return name;
        }

        public String getEmail() {
            return email;
        }

        public String getPicture() {
            return picture;
        }
    }
} 