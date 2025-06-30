package com.movie.service;

import com.movie.entity.Movie;
import com.movie.repository.MovieRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class MovieCrawlingService {

    private final MovieRepository movieRepository;

    public void crawlAndSaveMovies() {
        try {
            String url = "http://www.cgv.co.kr/movies/?lt=1&ft=0";
            Document doc = Jsoup.connect(url)
                    // userAgent : 크롤링 할때 웹 서버의 요청을 보내기 위한 신분증
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .timeout(15000)
                    .get();

            List<Movie> movies = parseMovieChart(doc);
            saveMovies(movies);

            log.info("영화 크롤링 완료: {}개 영화 저장됨", movies.size());

        } catch (IOException e) {
            log.error("영화 크롤링 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("영화 정보 크롤링에 실패했습니다.", e);
        }
    }

    private List<Movie> parseMovieChart(Document doc) {
        List<Movie> movies = new ArrayList<>();
        
        Elements movieElements = doc.select("div.sect-movie-chart ol li");
        if (movieElements.isEmpty()) {
            movieElements = doc.select("div.sect-movie-chart li");
        }
        if (movieElements.isEmpty()) {
            movieElements = doc.select("ul.lst-item li");
        }
        if (movieElements.isEmpty()) {
            movieElements = doc.select("div.box-contents");
        }
        
        log.info("발견된 영화 요소 수: {}", movieElements.size());
        
        for (Element movieElement : movieElements) {
            try {
                Movie movie = parseMovieElement(movieElement);
                if (movie != null) {
                    movies.add(movie);
                }
            } catch (Exception e) {
                log.warn("영화 정보 파싱 중 오류: {}", e.getMessage());
            }
        }
        
        return movies;
    }

    private Movie parseMovieElement(Element movieElement) {
        try {
            // 영화 제목 추출
            String movieTitle = extractMovieTitle(movieElement);
            if (movieTitle == null || movieTitle.trim().isEmpty()) {
                return null;
            }
            
            log.debug("영화 제목 발견: {}", movieTitle);
            
            // 이미 존재하는 영화인지 확인
            if (movieRepository.existsByMovieTitle(movieTitle)) {
                log.info("이미 존재하는 영화: {}", movieTitle);
                return null;
            }

            // 포스터 이미지 URL 추출
            String posterUrl = extractPosterUrl(movieElement);
            
            // 평점 추출
            double rating = extractRating(movieElement);

            // 영화 상세 페이지 URL 추출
            String detailUrl = extractDetailUrl(movieElement);
            
            // 상세 정보 크롤링
            MovieDetailInfo detailInfo = null;
            if (detailUrl != null && !detailUrl.isEmpty()) {
                try {
                    detailInfo = crawlMovieDetail(detailUrl, movieTitle);
                    log.debug("상세 정보 크롤링 완료: {}", movieTitle);
                } catch (Exception e) {
                    log.warn("상세 정보 크롤링 실패: {} - {}", movieTitle, e.getMessage());
                }
            }

            // Movie 객체 생성
            Movie movie = new Movie();
            movie.setMovieTitle(movieTitle);
            
            // 상세 정보가 있으면 사용, 없으면 기본값
            if (detailInfo != null) {
                movie.setGenre(detailInfo.getGenre());
                movie.setDetailInfo(detailInfo.getDetailInfo());
                movie.setOpenDate(detailInfo.getOpenDate());
                movie.setMovieCast(detailInfo.getCast());
                movie.setMovieContent(detailInfo.getContent());
            } else {
                movie.setGenre("장르 정보가 없습니다.");
                movie.setDetailInfo("기본정보가 없습니다.");
                movie.setOpenDate("개봉일 정보가 없습니다.");
                movie.setMovieCast("정보 없음");
                movie.setMovieContent("줄거리 정보가 없습니다.");
            }
            
            movie.setMovieRating(rating);
            movie.setMoviePoster(posterUrl);
            movie.setMoviePrice(12000);
            movie.setRegDate(LocalDateTime.now());

            log.info("영화 정보 파싱 완료: {}", movieTitle);
            return movie;

        } catch (Exception e) {
            log.error("영화 요소 파싱 중 오류: {}", e.getMessage());
            return null;
        }
    }

    // 제목
    private String extractMovieTitle(Element movieElement) {
        Element titleElement = movieElement.select("strong.title").first();
        if (titleElement == null) {
            titleElement = movieElement.select("div.box-contents strong.title").first();
        }
        if (titleElement == null) {
            titleElement = movieElement.select("a strong").first();
        }
        if (titleElement == null) {
            titleElement = movieElement.select("h3.title").first();
        }
        if (titleElement == null) {
            titleElement = movieElement.select("span.title").first();
        }
        
        if (titleElement != null) {
            return titleElement.text().trim();
        }
        
        return null;
    }

    // 포스터 URL
    private String extractPosterUrl(Element movieElement) {
        Element posterElement = movieElement.select("span.thumb-image img").first();
        if (posterElement == null) {
            posterElement = movieElement.select("div.box-image img").first();
        }
        if (posterElement == null) {
            posterElement = movieElement.select("img").first();
        }
        
        if (posterElement != null) {
            String posterUrl = posterElement.attr("src");
            if (posterUrl.startsWith("//")) {
                posterUrl = "https:" + posterUrl;
            }
            log.debug("포스터 URL: {}", posterUrl);
            return posterUrl;
        }
        
        return "";
    }

    // 평점
    private double extractRating(Element movieElement) {
        Element ratingElement = movieElement.select("span.percent").first();
        if (ratingElement == null) {
            ratingElement = movieElement.select("span.txt-info strong").first();
        }
        if (ratingElement == null) {
            ratingElement = movieElement.select("div.score strong").first();
        }
        if (ratingElement == null) {
            ratingElement = movieElement.select("span.rate").first();
        }
        
        if (ratingElement != null) {
            String ratingText = ratingElement.text().replace("%", "").trim();
            try {
                double rating = Double.parseDouble(ratingText);
                log.debug("평점: {}", rating);
                return rating;
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        
        return 0.0;
    }

    // 예매율
    private String extractReserveInfo(Element movieElement) {
        Element reserveElement = movieElement.select("span.rank").first();
        if (reserveElement == null) {
            reserveElement = movieElement.select("span.txt-info").first();
        }
        if (reserveElement == null) {
            reserveElement = movieElement.select("span.reserve").first();
        }
        
        if (reserveElement != null) {
            String reserveInfo = reserveElement.text().trim();
            log.debug("예매율 정보: {}", reserveInfo);
            return reserveInfo;
        }
        
        return "";
    }

    // 개봉일
    private String extractReleaseDate(Element movieElement) {
        Element dateElement = movieElement.select("span.txt-info").first();
        if (dateElement == null) {
            dateElement = movieElement.select("span.txt-date").first();
        }
        if (dateElement == null) {
            dateElement = movieElement.select("span.date").first();
        }
        
        if (dateElement != null) {
            String releaseDate = dateElement.text().trim();
            log.debug("개봉일: {}", releaseDate);
            return releaseDate;
        }
        
        return "";
    }

    // 상세 페이지 URL 추출
    private String extractDetailUrl(Element movieElement) {
        Element linkElement = movieElement.select("a").first();
        if (linkElement != null) {
            String href = linkElement.attr("href");
            if (href.startsWith("/")) {
                return "http://www.cgv.co.kr" + href;
            } else if (href.startsWith("http")) {
                return href;
            }
        }
        return null;
    }

    // 영화 상세 정보 크롤링
    private MovieDetailInfo crawlMovieDetail(String detailUrl, String movieTitle) {
        try {
            Document doc = Jsoup.connect(detailUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .timeout(10000)
                    .get();

            MovieDetailInfo detailInfo = new MovieDetailInfo();

            // spec 영역에서 장르, 출연진, 기본정보, 개봉일 추출 (dl의 자식 순회)
            Element spec = doc.selectFirst("div.spec > dl");
            String genre = "";
            String cast = "";
            String detail = "";
            String openDate = "";
            if (spec != null) {
                Elements children = spec.children(); // spec.children() : dl 의 자식요소를 list 로 가져옴
                for (int i = 0; i < children.size(); i++) {
                    Element el = children.get(i); // 자식 요소 확인
                    if (el.tagName().equals("dt")) { // <dt> 인 경우
                        String dtText = el.text().replace("\u00a0", "").trim();
                        // <dt> 에서 (\u00a0) 공백(&nbsp) 제거 및 양쪽 공백도 제거
                        if (i + 1 < children.size() && children.get(i + 1).tagName().equals("dd")) {
                            // 바로 다음 요소가 <dd> 인 경우
                            Element dd = children.get(i + 1); // <dd> 요소 추출
                            if (dtText.contains("장르")) { // <dt> 안에 "장르" 가 포함되 있을시
                                // <dt> 에서 "장르" 와 공백을 삭제 후 genre 에 저장
                                genre = dtText.replace("장르", "").replace(":", "").replace("\u00a0", "").replace(" ", "").trim();
                            } else if (dtText.contains("배우")) {
                                Elements actorLinks = dd.select("a"); // a query
                                List<String> actorNames = new ArrayList<>();
                                for (Element a : actorLinks) {
                                    actorNames.add(a.text().trim());
                                }
                                cast = String.join(", ", actorNames);
                            } else if (dtText.contains("기본 정보")) {
                                detail = dd.text().trim();
                            } else if (dtText.contains("개봉")) {
                                openDate = dd.text().trim();
                            }
                        }
                    }
                }
            }
            detailInfo.setGenre(genre.isEmpty() ? "장르 정보가 없습니다." : genre);
            // 출연진 정보가 없는 공연 일 경우 제목 첫 단어만 출연진에 등재
            detailInfo.setCast(cast.isEmpty() ? (movieTitle != null && !movieTitle.isEmpty() ? movieTitle.split("\\s+")[0] : "출연진 정보가 없습니다.") : cast);
            detailInfo.setDetailInfo(detail.isEmpty() ? "기본정보가 없습니다." : detail);
            detailInfo.setOpenDate(openDate.isEmpty() ? "개봉일 정보가 없습니다." : openDate);

            // 줄거리 추출
            Element storyElement = doc.select("div.sect-story-movie").first();
            if (storyElement == null) {
                storyElement = doc.select("div.story").first();
            }
            if (storyElement == null) {
                storyElement = doc.select("div.txt-story").first();
            }
            if (storyElement != null) {
                String story = storyElement.text().trim();
                if (story.length() > 1000) {
                    story = story.substring(0, 1000) + "...";
                }
                detailInfo.setContent(story);
            } else {
                detailInfo.setContent("줄거리 정보가 없습니다.");
            }

/*            // 추출값 로그
            log.info("장르: {}", genre);
            log.info("배우: {}", cast);
            log.info("기본정보: {}", detail);
            log.info("개봉일: {}", openDate);*/

            return detailInfo;

        } catch (IOException e) {
            log.error("상세 정보 크롤링 중 오류: {}", e.getMessage());
            return null;
        }
    }

    // DB에 저장
    private void saveMovies(List<Movie> movies) {
        for (Movie movie : movies) {
            try {
                movieRepository.save(movie);
                log.info("영화 저장 완료: {}", movie.getMovieTitle());
            } catch (Exception e) {
                log.error("영화 저장 중 오류: {} - {}", movie.getMovieTitle(), e.getMessage());
            }
        }
    }

    // 영화 상세 정보를 담는 내부 클래스
    @Setter
    @Getter
    private static class MovieDetailInfo {
        private String genre;
        private String detailInfo;
        private String openDate;
        private String cast;
        private String content;

    }
}