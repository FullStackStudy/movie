package com.movie.controller.recommand;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.movie.dto.recommand.RecommendResponseDto;
import com.movie.dto.recommand.TmdbDto;
import com.movie.entity.member.Member;
import com.movie.repository.member.MemberRepository;
import com.movie.service.recommend.RecommendService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/recommend")
public class RecommendController {
    private final RecommendService recommendService;
    private final MemberRepository memberRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    @GetMapping({"","/"})
    public String recommendMain(@AuthenticationPrincipal UserDetails userDetails, Model model) throws JsonProcessingException {
        String tmdbkey = "recommend:tmdb:"+userDetails.getUsername();

        List<RecommendResponseDto> hybridList = recommendService.getRecommendHybrid(userDetails);
        List<RecommendResponseDto> collaboList = recommendService.getRecommendCollabo(userDetails);
        List<RecommendResponseDto> contentList = recommendService.getRecommendContent(userDetails);
        List<TmdbDto> tmdbDtoList = new ArrayList<>();
        //레디스에 저장되어있음 여기서 가져옴
        if(redisTemplate.hasKey(tmdbkey)){
            tmdbDtoList = (List<TmdbDto>) redisTemplate.opsForValue().get(tmdbkey);
            System.out.println("tmdb추천 redis에서 결과 가져옴니다");
        }else {
            //hybrid에서 가져온 리스트에서 상위 3개만 tmdb에서 아이디 뽑아옴
            for (int i = 0; i < Math.min(hybridList.size(), 3); i++) {
                System.out.println("하이브리드 아이디:" + hybridList.get(i).getMovieTitle());
                List<TmdbDto> tmdbList = recommendService.searchMovieInTMDB(userDetails.getUsername(), hybridList.get(i).getMovieTitle(), hybridList.get(i).getYear());
                System.out.println("덜아오니?" + tmdbList);
                tmdbDtoList.addAll(tmdbList);
            }
        }
        System.out.println("합쳐진 리스트 크기: " + tmdbDtoList.size());
        Member member = memberRepository.findById(userDetails.getUsername()).orElseThrow(()-> new NullPointerException("아이디 없음"));
        model.addAttribute("hybridList", hybridList);
        model.addAttribute("collaboList", collaboList);
        model.addAttribute("contentList", contentList);
        model.addAttribute("nickname", !member.getNickname().isEmpty()? member.getNickname() : member.getMemberId());
        model.addAttribute("tmdbList", tmdbDtoList);
        return "recommend/recommendMain";
    }

}
