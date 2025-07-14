package com.movie.service.recommend;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.movie.dto.recommand.*;
import com.movie.entity.member.Member;
import com.movie.entity.movie.Movie;
import com.movie.entity.reservation.Reservation;
import com.movie.repository.member.MemberRepository;
import com.movie.repository.movie.MovieRepository;
import com.movie.repository.reservation.ReservationRepository;
import lombok.RequiredArgsConstructor;
import net.minidev.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class RecommendService {
    private final MovieRepository movieRepository;
    private final MemberRepository memberRepository;
    private final ReservationRepository reservationRepository;

    private final RedisTemplate<String, Object> redisTemplate;
    private static final Duration TTL = Duration.ofHours(1);

    //tmdb api
    @Value("${tmdb.api-key}")
    private String TMDB_API_KEY;

    @Value("${tmdb.api.url}")
    private String TMDB_URL;

    //영화정보 매핑
    public List<RecommendMovieDto> getMovieInfo(){
        List<Movie> movies = movieRepository.findAll();
        List<RecommendMovieDto> recommendMovieDtoList = new ArrayList<>();
        for(Movie movie : movies){
            RecommendMovieDto recommendMovieDto = new RecommendMovieDto();
            recommendMovieDto.setMovieId(movie.getMovieId());
            recommendMovieDto.setTitle(movie.getMovieTitle());
            recommendMovieDto.setGenres(movie.getGenre());
            recommendMovieDto.setActors(movie.getMovieCast());
            recommendMovieDto.setOpenDate(movie.getOpenDate());
            recommendMovieDto.setDetailInfo(movie.getDetailInfo());
            recommendMovieDto.parseDetail(movie.getDetailInfo());
            recommendMovieDtoList.add(recommendMovieDto);
        }
        return recommendMovieDtoList;
    }
    //유저 정보 매핑
    public List<RecommendUserDto> getUserInfo(){
        List<Member> members = memberRepository.findAll();
        List<RecommendUserDto> memberIds = new ArrayList<>();
        for(Member member : members){
            RecommendUserDto recommendUserDto = new RecommendUserDto();
            recommendUserDto.setUserId(member.getMemberId());
            memberIds.add(recommendUserDto);
        }
        return memberIds;
    }

    //모든 유저가 본 영화 맵핑 협업추천에 쓸거
    public List<InteractionDTO> getInteraction(){
        List<Reservation> reservations = reservationRepository.findAll();
        List<InteractionDTO> interactionDTOS = new ArrayList<>();
        for(Reservation reservation : reservations){
            InteractionDTO interactionDTO = new InteractionDTO();
            interactionDTO.setUserId(reservation.getMember().getMemberId());
            interactionDTO.setMovieId(reservation.getSchedule().getMovie().getMovieId());
            interactionDTOS.add(interactionDTO);
        }
        return interactionDTOS;
    }
    //컨텐츠추천이랑 watchedMovieId 보낼거
    public List<Long> getMovieIdOneUser(String memberId){
        List<Reservation> reservations = reservationRepository.findAllByMember_memberIdOrderByReservedAtDesc(memberId);
        List<Long> movieIds = new ArrayList<>();
        for(Reservation reservation : reservations){
            Long movieId = reservation.getSchedule().getMovie().getMovieId();
            movieIds.add(movieId);
        }
        return movieIds;
    }

    //받은 UserRecommendationResultDto -> movieid, title, poster만 보이게 보내줌
    public List<RecommendResponseDto> setResultToResponse(List<UserRecommendationResultDto> resultDtos){
        List<Long> movieIds = resultDtos.stream()
                .map(UserRecommendationResultDto::getMovieId)
                .collect(Collectors.toList());

        List<Movie> movies = movieRepository.findAllById(movieIds);

        Map<Long, Movie> movieMap = movies.stream()
                .collect(Collectors.toMap(Movie::getMovieId, m -> m));

        List<RecommendResponseDto> responseDtos = new ArrayList<>();

        for(UserRecommendationResultDto resultDto : resultDtos){
            Movie movie = movieMap.get(resultDto.getMovieId());
            if(movie !=null){
                RecommendResponseDto responseDto = new RecommendResponseDto();
                responseDto.setMovieId(movie.getMovieId());
                responseDto.setMovieTitle(movie.getMovieTitle());
                responseDto.setMoviePoster(movie.getMoviePoster());
                int year = Integer.parseInt(movie.getOpenDate().substring(0,4));
                responseDto.setYear(year);
                responseDtos.add(responseDto);
            }
        }
        return responseDtos;
    }

    /// ////////////////////////////////////////////////////
    //하이브리드추천
    public List<RecommendResponseDto> getRecommendHybrid(@AuthenticationPrincipal UserDetails userDetails) throws JsonProcessingException {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("watchedMovieIds", getMovieIdOneUser(userDetails.getUsername()));
        requestBody.put("interactions", getInteraction());
        requestBody.put("targetMemberId", userDetails.getUsername());

        //먼저 redis에 있으면 가져옴
        String key = "recommend:hybrid:"+userDetails.getUsername();
        if(redisTemplate.hasKey(key)){
            System.out.println(" 하이브리드 추천 redis에서 결과 가져옴니다");
            List<UserRecommendationResultDto>resultDto =  (List<UserRecommendationResultDto>)redisTemplate.opsForValue().get(key);
            return setResultToResponse(resultDto);
        }else {
            //없으면 lightfm ㄱㄱ
            List<UserRecommendationResultDto> resultDto = gotoLightfm("recommend-hybrid", "hybrid", requestBody);
            //redis 한시간 저장
            Boolean success = redisTemplate.opsForValue().setIfAbsent(key, resultDto, TTL);
            System.out.println("하이브리드 redis 저장 성공?: "+success);
            return setResultToResponse(resultDto);
        }
    }

    //협업추천
    public List<RecommendResponseDto> getRecommendCollabo(@AuthenticationPrincipal UserDetails userDetails) throws JsonProcessingException {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("watchedMovieIds", getMovieIdOneUser(userDetails.getUsername()));
        requestBody.put("interactions", getInteraction());
        requestBody.put("targetMemberId", userDetails.getUsername());

        //redis
        String key = "recommend:collabo:"+userDetails.getUsername();
        if(redisTemplate.hasKey(key)){
            List<UserRecommendationResultDto> resultDto = (List<UserRecommendationResultDto>) redisTemplate.opsForValue().get(key);
            System.out.println(" 협업 추천 redis에서 결과 가져옴니다");
            return setResultToResponse(resultDto);
        }else {
            List<UserRecommendationResultDto> resultDto = gotoLightfm("recommend", "collaba", requestBody);
            Boolean success = redisTemplate.opsForValue().setIfAbsent(key, resultDto, TTL);
            System.out.println("협업 redis 저장 성공?: "+success);
            return setResultToResponse(resultDto);
        }
    }
    //컨텐츠기반추천
    public List<RecommendResponseDto> getRecommendContent(@AuthenticationPrincipal UserDetails userDetails) throws JsonProcessingException {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("movieIds", getMovieIdOneUser(userDetails.getUsername()));

        String key = "recommend:content:"+userDetails.getUsername();
        if(redisTemplate.hasKey(key)){
            List<UserRecommendationResultDto> resultDto = (List<UserRecommendationResultDto>)redisTemplate.opsForValue().get(key);
            System.out.println("콘텐츠 추천 redis에서 결과 가져옴니다");
            return setResultToResponse(resultDto);
        }else {
            List<UserRecommendationResultDto> resultDto = gotoLightfm("recommend-content", "content", requestBody);
            Boolean success = redisTemplate.opsForValue().setIfAbsent(key, resultDto, TTL);
            System.out.println("콘텐츠 redis 저장 성공?: "+success);
            return setResultToResponse(resultDto);
        }
    }

    //lightfm 가는 함수
    public List<UserRecommendationResultDto> gotoLightfm(String url, String name,Map<String, Object> requestBody) throws JsonProcessingException {
        RestTemplate restTemplate = new RestTemplate();
        String pythonApiUrl = "http://127.0.0.1:5000/"+url; //파이선 주소

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(requestBody); // Map -> JSON 문자열 변환

        HttpEntity<String> requestEntity = new HttpEntity<>(json, headers);
        System.out.println("요청 JSON = " + requestEntity);
        ResponseEntity<String> response =restTemplate.postForEntity(pythonApiUrl, requestEntity, String.class);
        // 받은 JSON을 다시 DTO로 변환
        List<UserRecommendationResultDto> resultDto = mapper.readValue(response.getBody(), new TypeReference<List<UserRecommendationResultDto>>() {});
        System.out.println(name+" 추천결과:"+resultDto);

        return resultDto;
    }

    /// //////////////////////////////////정보가 모자라서 추가를 위해 tmdbapi로 영화 받아옴 hybrid로 뽑은 상위3개의 tmdbid 뽑아옴
    public List<TmdbDto> searchMovieInTMDB(String memberId, String title, int year){ //하나씩이라고 생각하면됨 movieid 하나당
        String key = "recommend:tmdb:"+memberId;
        RestTemplate restTemplate = new RestTemplate();
        System.out.println("Title"+title);
        String url = TMDB_URL + "search/movie?api_key=" + TMDB_API_KEY + "&query=" + title + "&year=" + year + "&language=ko";
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> jsonResponse = objectMapper.readValue(response.getBody(), Map.class); //받아오는 json
            List<Map<String, Object>> results = (List<Map<String, Object>>) jsonResponse.get("results"); //결과를 map 형식으로 list로 받음
            if (!results.isEmpty() && results != null) {
                Map<String, Object> result = results.get(0);
                System.out.println("응답 본문: " + result);

                List<Map<String, Object>> movieList = searchSimilarInTMDB((Integer) result.get("id"));
                //tmdb에서 가져온 리스트 ->dto 변환
                List<TmdbDto> recommendList = movieList.stream()
                        .map(movie -> {
                            TmdbDto dto = new TmdbDto();
                            dto.setMovieId(((Number) movie.get("id")).longValue());
                            dto.setMovieTitle((String) movie.get("title"));
                            String fullUrl = "https://image.tmdb.org/t/p/w500" + (String) movie.get("poster_path");
                            dto.setMoviePoster(fullUrl);
                            dto.setDetail((String) movie.get("overview"));
                            dto.setReleasedDate((String) movie.get("release_date"));
                            dto.setVoteAverage((Double) movie.get("vote_average"));
                            Object genreObj = movie.get("genre_ids");
                            if (genreObj instanceof List<?>) {
                                List<Integer> genreIds = (List<Integer>) genreObj;
                                dto.setGenres(genreIds.stream()
                                        .map(id -> genreIdToName(id)) //id ->한글
                                        .collect(Collectors.joining(",")));
                            }
                            return dto;
                        }).collect(Collectors.toList());
                Boolean success = redisTemplate.opsForValue().setIfAbsent(key,recommendList,TTL);
                System.out.println("tmdb 레디스 저장 성공?"+success);
                return recommendList;
            } else {
                // 빈 리스트 할당하거나 로그 출력
                List<TmdbDto> recommendList = new ArrayList<>();
                System.out.println("movieList가 null입니다.");
                return recommendList;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    //tmdb에서 뽑아온 영화3개로 유사추천영화 각5개씩 15개뽑아옴
    public List<Map<String, Object>> searchSimilarInTMDB(int movieId){
        RestTemplate restTemplate = new RestTemplate();
        String url = TMDB_URL + "movie/" + movieId + "/similar?api_key=" + TMDB_API_KEY + "&language=ko";

        System.out.println("url: "+url);
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> jsonResponse = objectMapper.readValue(response.getBody(), Map.class);
            List<Map<String, Object>> results = (List<Map<String, Object>>) jsonResponse.get("results");
            System.out.println("응답 본문reuslt: " + results);
            // 넘 옛날영화말고 1990~2024안에 줄거리 있는거만 뽑음
            List<Map<String, Object>> filtered = results.stream()
                    .filter((movie -> {
                        String dateStr = (String) movie.get("release_date");
                        String overview = (String) movie.get("overview");

                        if(dateStr == null || dateStr.isEmpty() || overview == null || overview.trim().isEmpty()){
                            return false;
                        }
                        try{
                            int year = Integer.parseInt(dateStr.substring(0,4));
                            return year >= 1990 && year <=2024;
                        }catch (Exception e){
                            return false; //날짜 조건 아닐시 제외
                        }
                    })).collect(Collectors.toList());
            return filtered;

        } catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public String genreIdToName(int id) {
        switch (id) {
            case 28: return "액션";
            case 12: return "모험";
            case 16: return "애니메이션";
            case 35: return "코미디";
            case 80: return "범죄";
            case 99: return "다큐멘터리";
            case 18: return "드라마";
            case 10751: return "가족";
            case 14: return "판타지";
            case 36: return "역사";
            case 27: return "공포";
            case 10402: return "음악";
            case 9648: return "미스터리";
            case 10749: return "로맨스";
            case 878: return "SF";
            case 10770: return "TV 영화";
            case 53: return "스릴러";
            case 10752: return "전쟁";
            case 37: return "서부";
            default: return "알 수 없음";
        }
    }
}
