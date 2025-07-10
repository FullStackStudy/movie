package com.movie.service.movie;

import com.movie.entity.movie.Movie;
import com.movie.repository.movie.MovieRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.github.bonigarcia.wdm.WebDriverManager;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class MovieCrawlingService {

    private final MovieRepository movieRepository;

    public void crawlAndSaveMovies() {
        log.info("🎬 영화 크롤링 시작 - CGV 영화 정보를 수집합니다...");
        try {
            String url = "http://www.cgv.co.kr/movies/?lt=1&ft=0";
            List<Movie> allMovies = new ArrayList<>();
            
            // Jsoup 으로 시도
            Document doc = null;
            List<Movie> jsoupMovies = new ArrayList<>();
            try {
                log.info("📡 Jsoup으로 CGV 영화 차트 페이지 접속 시도: {}", url);
                doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                        .timeout(15000)
                        .get();
                
                jsoupMovies = parseMovieChartWithJsoup(doc);
                allMovies.addAll(jsoupMovies);
                log.info("✅ Jsoup 크롤링 완료: {}개 영화 수집", jsoupMovies.size());
                
            } catch (IOException e) {
                log.warn("⚠️ Jsoup 크롤링 실패: {}", e.getMessage());
            }
            
            // Selenium 으로도 무조건 실행
            try {
                log.info("🤖 Selenium으로 CGV 영화 차트 페이지 접속 시도: {}", url);
                List<Movie> seleniumMovies = parseMovieChartWithSelenium(url);
                allMovies.addAll(seleniumMovies);
                log.info("✅ Selenium 크롤링 완료: {}개 영화 수집", seleniumMovies.size());
            } catch (Exception e) {
                log.error("❌ Selenium 크롤링 실패: {}", e.getMessage(), e);
            }
            
            // 중복 제거 (제목 기준)
            List<Movie> uniqueMovies = removeDuplicates(allMovies);
            log.info("🔄 중복 제거: {}개 → {}개 영화", allMovies.size(), uniqueMovies.size());
            
            saveMovies(uniqueMovies);
            log.info("🎉 영화 크롤링 완료: 총 {}개 영화 저장됨 (Jsoup: {}, Selenium: {}, 중복제거 후: {})", 
                    uniqueMovies.size(), jsoupMovies.size(), 
                    allMovies.size() - jsoupMovies.size(), uniqueMovies.size());

        } catch (Exception e) {
            log.error("💥 영화 크롤링 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("영화 정보 크롤링에 실패했습니다.", e);
        }
    }

    private List<Movie> parseMovieChartWithSelenium(String url) {
        List<Movie> movies = new ArrayList<>();
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        WebDriver driver = new ChromeDriver(options);

        try {
            driver.get(url);
            Thread.sleep(2000);

            // 더보기 버튼 반복 클릭
            while (true) {
                try {
                    WebElement moreBtn = driver.findElement(By.cssSelector(".btn-more-fontbold"));
                    if (moreBtn.isDisplayed()) {
                        moreBtn.click();
                        Thread.sleep(1500);
                    } else {
                        break;
                    }
                } catch (NoSuchElementException e) {
                    break;
                }
            }

            // 영화 데이터 추출
            List<WebElement> movieCards = driver.findElements(By.cssSelector(".sect-movie-chart ol li"));
            for (WebElement card : movieCards) {
                try {
                    String title = card.findElement(By.cssSelector(".box-contents strong.title")).getText();
                    String openDate = card.findElement(By.cssSelector(".txt-info")).getText();
                    String posterUrl = card.findElement(By.cssSelector(".thumb-image img")).getAttribute("src");
                    // 평점은 상세페이지에서 가져올 예정이므로 임시로 0.0 설정
                    String rating = "0.0";

                    // 이미 DB에 있는 영화는 건너뜀
                    if (movieRepository.existsByMovieTitle(title)) continue;

                    // 상세 페이지 URL 추출
                    String detailUrl = null;
                    try {
                        WebElement linkElement = card.findElement(By.cssSelector("a"));
                        String href = linkElement.getAttribute("href");
                        if (href.startsWith("/")) {
                            detailUrl = "http://www.cgv.co.kr" + href;
                        } else if (href.startsWith("http")) {
                            detailUrl = href;
                        }
                    } catch (Exception e) {
                        log.warn("상세 페이지 URL 추출 실패: {} - {}", title, e.getMessage());
                    }

                    // 상세 정보 크롤링
                    MovieDetailInfo detailInfo = null;
                    if (detailUrl != null && !detailUrl.isEmpty()) {
                        try {
                            detailInfo = crawlMovieDetail(detailUrl, title);
                            log.debug("Selenium 상세 정보 크롤링 완료: {}", title);
                        } catch (Exception e) {
                            log.warn("Selenium 상세 정보 크롤링 실패: {} - {}", title, e.getMessage());
                        }
                    }

                    Movie movie = new Movie();
                    movie.setMovieTitle(title);
                    
                    // 상세 정보가 있으면 사용, 없으면 기본값
                    if (detailInfo != null) {
                        movie.setGenre(detailInfo.getGenre());
                        movie.setDetailInfo(detailInfo.getDetailInfo());
                        movie.setOpenDate(detailInfo.getOpenDate());
                        movie.setMovieCast(detailInfo.getCast());
                        movie.setMovieContent(detailInfo.getContent());
                        // 상세페이지에서 가져온 평점 사용
                        movie.setMovieRating(detailInfo.getRating());
                        movie.setMovieDirector(detailInfo.getDirector()); // 추가
                    } else {
                        movie.setGenre("기타");
                        movie.setDetailInfo("상세정보 없음");
                        movie.setOpenDate(openDate);
                        movie.setMovieCast("출연진 정보 없음");
                        movie.setMovieContent("줄거리 정보 없음");
                        // 기본 평점 사용
                        movie.setMovieRating(rating);
                        movie.setMovieDirector("감독 정보가 없습니다."); // 추가
                    }
                    movie.setMoviePoster(posterUrl);
                    movie.setMoviePrice(12000);
                    movie.setRegDate(LocalDateTime.now());

                    movies.add(movie);
                    log.info("Selenium 영화 파싱 완료: {}", title);
                } catch (Exception e) {
                    log.warn("영화 파싱 중 오류: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Selenium 크롤링 실패: {}", e.getMessage());
        } finally {
            driver.quit();
        }
        
        return movies;
    }

    private List<Movie> parseMovieChartWithJsoup(Document doc) {
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
        
        log.info("Jsoup 으로 발견된 영화 요소 수: {}", movieElements.size());
        
        for (Element movieElement : movieElements) {
            try {
                Movie movie = parseMovieElement(movieElement);
                if (movie != null) {
                    movies.add(movie);
                }
            } catch (Exception e) {
                log.warn("영화 파싱 중 오류: {}", e.getMessage());
            }
        }
        
        return movies;
    }

    private Movie parseMovieElement(Element movieElement) {
        try {
            String title = extractMovieTitle(movieElement);
            if (title == null || title.trim().isEmpty()) {
                return null;
            }

            // 이미 DB에 있는 영화는 건너뜀
            if (movieRepository.existsByMovieTitle(title)) {
                log.debug("이미 존재하는 영화 건너뜀: {}", title);
                return null;
            }

            String posterUrl = extractPosterUrl(movieElement);
            String rating = extractRatingAsString(movieElement);
            String detailUrl = extractDetailUrl(movieElement);

            // 상세 정보 크롤링
            MovieDetailInfo detailInfo = null;
            if (detailUrl != null && !detailUrl.isEmpty()) {
                try {
                    detailInfo = crawlMovieDetail(detailUrl, title);
                    log.debug("Jsoup 상세 정보 크롤링 완료: {}", title);
                } catch (Exception e) {
                    log.warn("Jsoup 상세 정보 크롤링 실패: {} - {}", title, e.getMessage());
                }
            }

            Movie movie = new Movie();
            movie.setMovieTitle(title);
            
            // 상세 정보가 있으면 사용, 없으면 기본값
            if (detailInfo != null) {
                movie.setGenre(detailInfo.getGenre());
                movie.setDetailInfo(detailInfo.getDetailInfo());
                movie.setOpenDate(detailInfo.getOpenDate());
                movie.setMovieCast(detailInfo.getCast());
                movie.setMovieContent(detailInfo.getContent());
                movie.setMovieRating(detailInfo.getRating());
                movie.setMovieDirector(detailInfo.getDirector());
            } else {
                movie.setGenre("기타");
                movie.setDetailInfo("상세정보 없음");
                movie.setOpenDate("개봉일 정보 없음");
                movie.setMovieCast("출연진 정보 없음");
                movie.setMovieContent("줄거리 정보 없음");
                movie.setMovieRating(rating);
                movie.setMovieDirector("감독 정보가 없습니다.");
            }
            
            movie.setMoviePoster(posterUrl);
            movie.setMoviePrice(12000);
            movie.setRegDate(LocalDateTime.now());

            log.info("Jsoup 영화 파싱 완료: {}", title);
            return movie;
        } catch (Exception e) {
            log.warn("영화 요소 파싱 중 오류: {}", e.getMessage());
            return null;
        }
    }

    private String extractMovieTitle(Element movieElement) {
        try {
            // 여러 선택자로 시도
            Element titleElement = movieElement.selectFirst("strong.title");
            if (titleElement == null) {
                titleElement = movieElement.selectFirst(".title");
            }
            if (titleElement == null) {
                titleElement = movieElement.selectFirst("h3");
            }
            if (titleElement == null) {
                titleElement = movieElement.selectFirst("a");
            }
            
            if (titleElement != null) {
                return titleElement.text().trim();
            }
        } catch (Exception e) {
            log.warn("영화 제목 추출 실패: {}", e.getMessage());
        }
        return null;
    }

    private String extractPosterUrl(Element movieElement) {
        try {
            Element imgElement = movieElement.selectFirst("img");
            if (imgElement != null) {
                String src = imgElement.attr("src");
                if (src.isEmpty()) {
                    src = imgElement.attr("data-src");
                }
                return src;
            }
        } catch (Exception e) {
            log.warn("포스터 URL 추출 실패: {}", e.getMessage());
        }
        return "default-poster.jpg";
    }

    private String extractRatingAsString(Element movieElement) {
        try {
            Element ratingElement = movieElement.selectFirst(".score, .rating, [class*=score]");
            if (ratingElement != null) {
                return ratingElement.text().trim();
            }
        } catch (Exception e) {
            log.warn("평점 추출 실패: {}", e.getMessage());
        }
        return "?";
    }

    private String extractDetailUrl(Element movieElement) {
        try {
            Element linkElement = movieElement.selectFirst("a");
            if (linkElement != null) {
                String href = linkElement.attr("href");
                if (href.startsWith("/")) {
                    return "http://www.cgv.co.kr" + href;
                } else if (href.startsWith("http")) {
                    return href;
                }
            }
        } catch (Exception e) {
            log.warn("상세 페이지 URL 추출 실패: {}", e.getMessage());
        }
        return null;
    }

    private MovieDetailInfo crawlMovieDetail(String detailUrl, String movieTitle) {
        MovieDetailInfo detailInfo = new MovieDetailInfo();

        try {
            log.debug("🔍 영화 상세 정보 크롤링 시작: {} - {}", movieTitle, detailUrl);
            Document doc = Jsoup.connect(detailUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .timeout(10000)
                    .get();

            // 1. rating
            Element ratingElement = doc.selectFirst("div.egg-gage.small span.percent");
            detailInfo.setRating(ratingElement != null ? ratingElement.text().trim() : "?");

            // 2. cast
            Elements castLinks = doc.select("dt:contains(배우) + dd.on a");
            String cast = castLinks.stream()
                    .map(Element::text)
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.joining(", "));
            detailInfo.setCast(cast.isEmpty() ? "출연진 정보 없음" : cast);

            // 3. genre (dt에 직접 들어있는 텍스트에서 추출)
            Element genreDt = doc.selectFirst("dt:matchesOwn(^\\s*장르)");
            if (genreDt != null) {
                String genreText = genreDt.text().trim();
                int idx = genreText.indexOf("장르");
                if (idx != -1) {
                    genreText = genreText.substring(idx + 2).replace(":", "").trim();
                }
                // 값이 비어있으면 "기타"로 대체
                detailInfo.setGenre(genreText.isEmpty() ? "기타" : genreText);
            } else {
                detailInfo.setGenre("기타");
            }

            // 4. 상세정보 (등급, 상영시간, 국가)
            Element infoElement = doc.selectFirst("dt:contains(기본 정보) + dd.on");
            detailInfo.setDetailInfo(infoElement != null ? infoElement.text().trim() : "기본 정보 없음");

            // 5. 감독 (여러 명일 경우 콤마로 연결)
            Elements directorLinks = doc.select("dt:contains(감독) + dd a");
            String director = directorLinks.stream()
                    .map(Element::text)
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.joining(", "));
            detailInfo.setDirector(director.isEmpty() ? "감독 정보 없음" : director);

            // 6. 개봉일 (openDate)
            Element openDateElement = doc.selectFirst("dt:contains(개봉) + dd");
            detailInfo.setOpenDate(openDateElement != null ? openDateElement.text().trim() : "개봉일 정보 없음");

            // 7. 줄거리
            Element contentElement = doc.selectFirst(".sect-story-movie");
            detailInfo.setContent(contentElement != null ? contentElement.text().trim() : "줄거리 정보 없음");

        } catch (Exception e) {
            log.warn("영화 상세 정보 크롤링 실패: {} - {}", movieTitle, e.getMessage());
        }

        return detailInfo;
    }

    private void saveMovies(List<Movie> movies) {
        for (Movie movie : movies) {
            try {
                movieRepository.save(movie);
                log.info("영화 저장 완료: {}", movie.getMovieTitle());
            } catch (Exception e) {
                log.error("영화 저장 실패: {} - {}", movie.getMovieTitle(), e.getMessage());
            }
        }
    }

    private List<Movie> removeDuplicates(List<Movie> movies) {
        List<Movie> uniqueMovies = new ArrayList<>();
        for (Movie movie : movies) {
            boolean isDuplicate = uniqueMovies.stream()
                    .anyMatch(existing -> existing.getMovieTitle().equals(movie.getMovieTitle()));
            if (!isDuplicate) {
                uniqueMovies.add(movie);
            }
        }
        return uniqueMovies;
    }

    @Setter
    @Getter
    private static class MovieDetailInfo {
        private String genre;
        private String detailInfo;
        private String openDate;
        private String cast;
        private String content;
        private String rating;
        private String director;
    }
} 