package com.movie.config;

import com.movie.service.CustomOAuth2UserService;
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
                        .ignoringRequestMatchers("/api/**", "/members/new", "/members/login", "/members/logout")
                        .csrfTokenRepository(new HttpSessionCsrfTokenRepository())
                ) // API 엔드포인트와 특정 페이지에서 CSRF 검증 제외
                .formLogin(form -> form
                        .loginPage("/members/login") // 사용할 로그인 페이지 URL
                        .successHandler(new SavedRequestAwareAuthenticationSuccessHandler())
                        .usernameParameter("memberId") // 사용자명
                        .failureUrl("/members/login/error")
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/members/login")
                        .defaultSuccessUrl("/main")
                        .failureUrl("/members/login/error")
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                        .redirectionEndpoint(redirection -> redirection
                                .baseUri("/login/oauth2/code/*")
                        )
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/main")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/css/**", "/js/**", "/img/**", "/uploads/**").permitAll()
                        .requestMatchers("/", "/main", "/members/**", "/item/**", "/images/**").permitAll()
                        .requestMatchers("/api/email/**").permitAll() // 이메일 인증 API 허용
                        .requestMatchers("/mypage/**").authenticated()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().permitAll()
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}