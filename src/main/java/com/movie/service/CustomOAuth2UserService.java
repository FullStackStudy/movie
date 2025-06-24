package com.movie.service;

import com.movie.entity.Member;
import com.movie.repository.MemberRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

// 소셜로그인 인증 서비스
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final MemberRepository memberRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails()
                .getUserInfoEndpoint().getUserNameAttributeName();

        OAuthAttributes attributes = OAuthAttributes.of(registrationId, userNameAttributeName, oAuth2User.getAttributes());

        if (attributes == null) {
            throw new OAuth2AuthenticationException("지원하지 않는 OAuth2 제공자입니다: " + registrationId);
        }

        // 이메일 검증
        if (attributes.getEmail() == null || attributes.getEmail().trim().isEmpty()) {
            throw new OAuth2AuthenticationException("이메일 정보를 가져올 수 없습니다.");
        }

        Member member = saveOrUpdate(attributes);

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(member.getRole().toString())),
                attributes.getAttributes(),
                attributes.getNameAttributeKey()
        );
    }

    private Member saveOrUpdate(OAuthAttributes attributes) {
        Optional<Member> existingMember = memberRepository.findById(attributes.getEmail());

        Member member;
        if (existingMember.isPresent()) {
            member = updateExistingUser(existingMember.get(), attributes);
        } else {
            member = attributes.toEntity();
        }

        return memberRepository.save(member);
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

        public static OAuthAttributes of(String registrationId, String userNameAttributeName, Map<String, Object> attributes) {
            if ("google".equals(registrationId)) {
                return ofGoogle(userNameAttributeName, attributes);
            }
            return null;
        }

        private static OAuthAttributes ofGoogle(String userNameAttributeName, Map<String, Object> attributes) {
            OAuthAttributes oAuthAttributes = new OAuthAttributes();
            oAuthAttributes.name = (String) attributes.get("name");
            oAuthAttributes.email = (String) attributes.get("email");
            oAuthAttributes.picture = (String) attributes.get("picture");
            oAuthAttributes.attributes = attributes;
            oAuthAttributes.nameAttributeKey = userNameAttributeName;
            return oAuthAttributes;
        }

        public Member toEntity() {
            Member member = new Member();
            member.setMemberId(email);
            member.setName(name != null ? name : "사용자");
            member.setNickname(name != null ? name : "사용자");
            member.setEmail(null);
            member.setProfile(picture);
            member.setRole(com.movie.constant.Role.USER);
            member.setRegDate(LocalDate.now());
            member.setGrade("일반");
            member.setPassword("OAUTH2_USER");
            member.setBirth(LocalDate.of(1990, 1, 1));
            member.setPhone("000-0000-0000");
            member.setAddress("OAuth2 사용자");
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