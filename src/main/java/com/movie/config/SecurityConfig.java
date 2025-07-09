package com.movie.config;

import com.movie.service.member.CustomOAuth2UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;

    public SecurityConfig(CustomOAuth2UserService customOAuth2UserService) {
        this.customOAuth2UserService = customOAuth2UserService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/email/**", "/login/oauth2/code/**")
                        .csrfTokenRepository(new HttpSessionCsrfTokenRepository())
                ) 
                .formLogin(form -> form
                        .loginPage("/members/auth") // 통합된 인증 페이지 URL
                        .loginProcessingUrl("/login") // 로그인 처리 URL
                        .successHandler(new SavedRequestAwareAuthenticationSuccessHandler())
                        .usernameParameter("memberId") // 사용자명
                        .passwordParameter("password") // 비밀번호 파라미터
                        .failureUrl("/members/login/error")
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/members/auth")
                        .defaultSuccessUrl("/")
                        .failureUrl("/members/login/error?error=oauth2_error")
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                        .redirectionEndpoint(redirection -> redirection
                                .baseUri("/login/oauth2/code/*")
                        )
                        .authorizationEndpoint(authorization -> authorization
                                .baseUri("/oauth2/authorization")
                        )
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/css/**", "/js/**", "/img/**", "/uploads/**").permitAll()
                        .requestMatchers("/", "/main", "/members/**", "/item/**", "/images/**","/cinema/**").permitAll()
                        .requestMatchers("/api/email/**").permitAll() // 이메일 인증 API 허용
                        .requestMatchers("/login/oauth2/code/**").permitAll() // OAuth2 콜백 허용
                        .requestMatchers("/mypage/**").authenticated()
                        .requestMatchers("/admin/**").permitAll()
                        .anyRequest().permitAll()
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}