package com.movie.service.movie;

import com.movie.entity.movie.Movie;
import com.movie.repository.movie.MovieRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.github.bonigarcia.wdm.WebDriverManager;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class MovieCrawlingService {

    private final MovieRepository movieRepository;

    public void crawlAndSaveMovies() {
        log.info("🎬 영화 크롤링 시작 - 메가박스 영화 정보를 수집합니다...");
        try {
            String url = "https://www.megabox.co.kr/movie";
            List<Movie> movies = new ArrayList<>();

            // Selenium으로 크롤링
            try {
                log.info("🤖 Selenium으로 메가박스 영화 페이지 접속 시도: {}", url);
                movies = parseMovieChartWithSelenium(url);
                log.info("✅ Selenium 크롤링 완료: {}개 영화 수집", movies.size());
            } catch (Exception e) {
                log.error("❌ Selenium 크롤링 실패: {}", e.getMessage(), e);
                throw new RuntimeException("영화 정보 크롤링에 실패했습니다.", e);
            }

            saveMovies(movies);
            log.info("🎉 영화 크롤링 완료: 총 {}개 영화 저장됨", movies.size());

        } catch (Exception e) {
            log.error("💥 영화 크롤링 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("영화 정보 크롤링에 실패했습니다.", e);
        }
    }

    private List<Movie> parseMovieChartWithSelenium(String url) {
        List<Movie> movies = new ArrayList<>();
        WebDriver driver = null;

        try {
            driver = createWebDriver();
            log.info("🌐 메가박스 영화 페이지 접속: {}", url);
            driver.get(url);

            // 페이지 로딩 대기 (단축)
            Thread.sleep(2000);

            // JavaScript 실행 완료 대기 (단축)
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

            // 영화 목록이 로드될 때까지 대기
            try {
                wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("#movieList li")));
                log.info("영화 목록 요소 발견");
            } catch (Exception e) {
                log.warn("영화 목록 요소를 찾을 수 없습니다: {}", e.getMessage());
            }

            // 메가박스 영화 목록 구조에 맞게 정확한 선택자 사용
            List<WebElement> movieCards = driver.findElements(By.cssSelector("#movieList li"));
            log.info("발견된 영화 카드 수: {}", movieCards.size());

            // 병렬 처리를 위한 ExecutorService 생성 (스레드 풀 크기: 3)
            ExecutorService executor = Executors.newFixedThreadPool(3);
            List<CompletableFuture<Movie>> futures = new ArrayList<>();

            for (WebElement card : movieCards) {
                CompletableFuture<Movie> future = CompletableFuture.supplyAsync(() -> {
                try {
                    String title = extractMovieTitleSelenium(card);
                    if (title == null || title.trim().isEmpty()) {
                            return null;
                    }

                    // 이미 DB에 있는 영화는 건너뜀
                    if (movieRepository.existsByMovieTitle(title)) {
                        log.debug("이미 존재하는 영화 건너뜀: {}", title);
                            return null;
                    }

                    String posterUrl = extractPosterUrlSelenium(card);
                    String rating = extractRatingSelenium(card);
                    String openDate = extractOpenDateSelenium(card);
                    String content = extractContentSelenium(card);
                    String detailUrl = extractDetailUrlSelenium(card);

                    // 상세 정보 크롤링
                    MovieDetailInfo detailInfo = null;
                    if (detailUrl != null && !detailUrl.isEmpty()) {
                        try {
                            detailInfo = crawlMovieDetail(detailUrl, title);
                                log.debug("상세 정보 크롤링 완료: {}", title);
                        } catch (Exception e) {
                                log.warn("상세 정보 크롤링 실패: {} - {}", title, e.getMessage());
                            }
                    }

                    Movie movie = new Movie();
                    movie.setMovieTitle(title);

                    // 상세 정보가 있으면 사용, 없으면 기본값
                    if (detailInfo != null) {
                        movie.setGenre(detailInfo.getGenre() != null ? detailInfo.getGenre() : "기타");
                        movie.setDetailInfo(detailInfo.getDetailInfo() != null ? detailInfo.getDetailInfo() : "상세정보 없음");
                        movie.setOpenDate(detailInfo.getOpenDate() != null ? detailInfo.getOpenDate() : (openDate != null ? openDate : "개봉일 정보 없음"));
                        movie.setMovieCast(detailInfo.getCast() != null ? detailInfo.getCast() : "출연진 정보 없음");
                        movie.setMovieContent(detailInfo.getContent() != null ? detailInfo.getContent() : (content != null ? content : "줄거리 정보 없음"));
                        movie.setMovieRating(detailInfo.getRating() != null ? detailInfo.getRating() : (rating != null ? rating : "?"));
                        movie.setMovieDirector(detailInfo.getDirector() != null ? detailInfo.getDirector() : "감독 정보 없음");
                    } else {
                        movie.setGenre("기타");
                        movie.setDetailInfo("상세정보 없음");
                        movie.setOpenDate(openDate != null ? openDate : "개봉일 정보 없음");
                        movie.setMovieCast("출연진 정보 없음");
                        movie.setMovieContent(content != null ? content : "줄거리 정보 없음");
                        movie.setMovieRating(rating != null ? rating : "?");
                        movie.setMovieDirector("감독 정보 없음");
                    }

                    movie.setMoviePoster(posterUrl);
                    movie.setMoviePrice(12000);
                    movie.setRegDate(LocalDateTime.now());

                        log.info("영화 파싱 완료: {}", title);
                        return movie;
                    } catch (Exception e) {
                        log.warn("영화 파싱 중 오류: {}", e.getMessage());
                        return null;
                    }
                }, executor);

                futures.add(future);
            }

            // 모든 비동기 작업 완료 대기
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
            );

            try {
                allFutures.get(5, TimeUnit.MINUTES); // 최대 5분 대기
            } catch (Exception e) {
                log.warn("일부 영화 파싱이 시간 초과되었습니다: {}", e.getMessage());
            }

            // 결과 수집
            for (CompletableFuture<Movie> future : futures) {
                try {
                    Movie movie = future.get(1, TimeUnit.SECONDS);
                    if (movie != null) {
                    movies.add(movie);
                    }
                } catch (Exception e) {
                    log.warn("영화 결과 수집 실패: {}", e.getMessage());
                }
            }

            // ExecutorService 종료
            executor.shutdown();
            try {
                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        } catch (Exception e) {
            log.error("Selenium 크롤링 실패: {}", e.getMessage());
        } finally {
            if (driver != null) {
            driver.quit();
            }
        }

        return movies;
    }

    private String extractMovieTitleSelenium(WebElement card) {
        try {
            WebElement titleElement = card.findElement(By.cssSelector(".tit-area .tit"));
            String title = titleElement.getText().trim();
            if (!title.isEmpty()) {
                log.debug("영화 제목 추출: {}", title);
                return title;
            }
        } catch (Exception e) {
            log.warn("영화 제목 추출 실패: {}", e.getMessage());
        }
        return null;
    }

    private String extractPosterUrlSelenium(WebElement card) {
        try {
            // 메가박스 구조: .movie-list-info .poster
            WebElement imgElement = card.findElement(By.cssSelector(".movie-list-info .poster"));
            String src = imgElement.getAttribute("src");
            if (src == null || src.isEmpty()) {
                src = imgElement.getAttribute("data-src");
            }
            log.debug("포스터 URL 추출: {}", src);
            return src != null ? src : "default-poster.jpg";
        } catch (Exception e) {
            log.warn("포스터 URL 추출 실패: {}", e.getMessage());
        }
        return "default-poster.jpg";
    }

    private String extractRatingSelenium(WebElement card) {
        try {
            // 메가박스 구조: .rate-date .rate에서 예매율 추출
            WebElement ratingElement = card.findElement(By.cssSelector(".rate-date .rate"));
            String ratingText = ratingElement.getText().trim();
            log.debug("예매율 원본 텍스트: {}", ratingText);
            
            // "예매율 17.6%" 형태에서 숫자 부분만 추출
            if (ratingText.contains("예매율")) {
                String rating = ratingText.replace("예매율", "").replace("%", "").trim();
                log.debug("예매율 추출: {}", rating);
            return rating;
            }
            
            return ratingText;
        } catch (Exception e) {
            log.warn("예매율 추출 실패: {}", e.getMessage());
        }
        return "?";
    }

    private String extractOpenDateSelenium(WebElement card) {
        try {
            // 메가박스 구조: .rate-date .date
            WebElement dateElement = card.findElement(By.cssSelector(".rate-date .date"));
            String date = dateElement.getText().trim();
            // "개봉일 2025.07.30" 형태에서 날짜만 추출
            if (date.contains("개봉일")) {
                date = date.replace("개봉일", "").trim();
            }
            log.debug("개봉일 추출: {}", date);
            return date;
        } catch (Exception e) {
            log.warn("개봉일 추출 실패: {}", e.getMessage());
        }
        return null;
    }

    private String extractContentSelenium(WebElement card) {
        try {
            // 메가박스 구조: .movie-score .summary
            WebElement contentElement = card.findElement(By.cssSelector(".movie-score .summary"));
            String content = contentElement.getText().trim();
            log.debug("줄거리 추출: {}", content.length() > 50 ? content.substring(0, 50) + "..." : content);
            return content;
        } catch (Exception e) {
            log.warn("줄거리 추출 실패: {}", e.getMessage());
        }
        return null;
    }

    private String extractDetailUrlSelenium(WebElement card) {
        try {
            // 메가박스 구조: data-no 속성에서 영화 번호 추출
            // card는 li 요소이므로, .btn-util 안의 .btn-like 버튼을 찾아야 함
            WebElement buttonElement = card.findElement(By.cssSelector(".btn-util .btn-like"));
            String dataNo = buttonElement.getAttribute("data-no");
            if (dataNo != null && !dataNo.isEmpty()) {
                // data-no를 사용하여 상세 페이지 URL 생성 (올바른 메가박스 URL 구조)
                String href = "https://www.megabox.co.kr/movie-detail?rpstMovieNo=" + dataNo;
                log.debug("상세 페이지 URL 추출: {} (data-no: {})", href, dataNo);
                return href;
            }
        } catch (Exception e) {
            log.warn("상세 페이지 URL 추출 실패: {}", e.getMessage());
            // 대안 방법 시도
            try {
                WebElement buttonElement = card.findElement(By.cssSelector("button[data-no]"));
                String dataNo = buttonElement.getAttribute("data-no");
                if (dataNo != null && !dataNo.isEmpty()) {
                    String href = "https://www.megabox.co.kr/movie-detail?rpstMovieNo=" + dataNo;
                    log.debug("대안 방법으로 상세 페이지 URL 추출: {} (data-no: {})", href, dataNo);
                    return href;
                }
            } catch (Exception e2) {
                log.warn("대안 방법도 실패: {}", e2.getMessage());
            }
        }
        return null;
    }

    private MovieDetailInfo crawlMovieDetail(String detailUrl, String movieTitle) {
        MovieDetailInfo detailInfo = new MovieDetailInfo();

        try {
            log.debug("🔍 영화 상세 정보 크롤링 시작: {} - {}", movieTitle, detailUrl);
            
            // Selenium을 사용하여 동적 콘텐츠 로드
            WebDriver driver = null;
            try {
                driver = createWebDriver();
                driver.get(detailUrl);
                
                // 페이지가 완전히 로드될 때까지 대기 (단축)
                Thread.sleep(2000);
                
                // 영화 정보가 로드될 때까지 대기 (단축)
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(8));
                try {
                    // 먼저 contentData 영역이 로드될 때까지 대기
                    wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("#contentData")));
                    log.debug("contentData 요소 발견");
                    
                    // 추가 대기 시간 (AJAX 콘텐츠 로딩을 위해) (단축)
                    Thread.sleep(1000);
                    
                    // 영화 정보 요소 확인
                    try {
                        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".movie-info")));
                        log.debug("영화 정보 요소 발견");
                    } catch (Exception e) {
                        log.warn("영화 정보 요소를 찾을 수 없습니다: {}", e.getMessage());
                        // 대안: infoContent 클래스 확인
                        try {
                            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".infoContent")));
                            log.debug("infoContent 요소 발견");
                        } catch (Exception e2) {
                            log.warn("infoContent 요소도 찾을 수 없습니다: {}", e2.getMessage());
                        }
                    }
                } catch (Exception e) {
                    log.warn("contentData 요소를 찾을 수 없습니다: {}", e.getMessage());
                }

                // 1. 영화 제목 - 상세 페이지에서 확인
                try {
                    WebElement titleElement = driver.findElement(By.cssSelector(".movie-detail-cont .title"));
                    if (titleElement != null) {
                        log.debug("영화 제목 확인: {}", titleElement.getText().trim());
                    }
                } catch (Exception e) {
                    log.warn("영화 제목 확인 실패: {}", e.getMessage());
                }

                // 2. 줄거리 (movie_content) - movie-summary .txt에서 추출
                try {
                    WebElement contentElement = driver.findElement(By.cssSelector(".movie-summary .txt"));
                    if (contentElement != null) {
                        String content = contentElement.getText().trim();
                        detailInfo.setContent(content);
                        log.debug("줄거리 추출: {}", content.length() > 100 ? content.substring(0, 100) + "..." : content);
                    }
                } catch (Exception e) {
                    log.warn("줄거리 추출 실패: {}", e.getMessage());
                }

                // 3. 영화 정보 추출 - 실제 HTML 구조에 맞게 정확한 선택자 사용
                
                // 감독 정보 추출 - .movie-info .line p에서 "감독" 포함된 요소 찾기
                String director = extractInfoFromSelenium(driver, ".movie-info .line p", "감독");
                if (director != null) {
                    detailInfo.setDirector(director);
                    log.debug("감독 추출: {}", director);
                }
                
                // 장르 정보 추출 - .movie-info .line p에서 "장르" 포함된 요소 찾기
                String genre = extractInfoFromSelenium(driver, ".movie-info .line p", "장르");
                if (genre != null) {
                    detailInfo.setGenre(genre);
                    log.debug("장르 추출: {}", genre);
                }
                
                // 등급 정보 추출 - .movie-info .line p에서 "등급" 포함된 요소 찾기
                String detailInfoText = extractInfoFromSelenium(driver, ".movie-info .line p", "등급");
                if (detailInfoText != null) {
                    detailInfo.setDetailInfo(detailInfoText);
                    log.debug("등급 추출: {}", detailInfoText);
                }
                
                // 개봉일 정보 추출 - .movie-info .line p에서 "개봉일" 포함된 요소 찾기
                String openDate = extractInfoFromSelenium(driver, ".movie-info .line p", "개봉일");
                if (openDate != null) {
                    detailInfo.setOpenDate(openDate);
                    log.debug("개봉일 추출: {}", openDate);
                }
                
                // 출연진 정보 추출 - .movie-info p에서 "출연진" 포함된 요소 찾기 (line 밖에 있음)
                String cast = extractInfoFromSelenium(driver, ".movie-info p", "출연진");
                if (cast != null) {
                    detailInfo.setCast(cast);
                    log.debug("출연진 추출: {}", cast);
                }
                
            } catch (Exception e) {
                log.warn("Selenium 크롤링 실패: {}", e.getMessage());
            } finally {
                if (driver != null) {
                    driver.quit();
                }
            }

            // 4. 평점 - 메인 페이지에서 가져온 평점 사용 (예: 17.6)
            // 평점은 메인 페이지에서 이미 추출되었으므로 여기서는 설정하지 않음

            log.debug("✅ 영화 상세 정보 크롤링 완료: {}", movieTitle);

        } catch (Exception e) {
            log.warn("영화 상세 정보 크롤링 실패: {} - {}", movieTitle, e.getMessage());
        }

        return detailInfo;
    }

    private void saveMovies(List<Movie> movies) {
        for (Movie movie : movies) {
            try {
                // 필수 필드 유효성 검사 및 기본값 설정
                validateAndSetDefaults(movie);
                
                movieRepository.save(movie);
                log.info("영화 저장 완료: {}", movie.getMovieTitle());
            } catch (Exception e) {
                log.error("영화 저장 실패: {} - {}", movie.getMovieTitle(), e.getMessage(), e);
            }
        }
    }

    private void validateAndSetDefaults(Movie movie) {
        // 필수 필드 검증 및 기본값 설정
        if (movie.getMovieTitle() == null || movie.getMovieTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("영화 제목은 필수입니다.");
        }

        // 장르 필드 검증
        if (movie.getGenre() == null || movie.getGenre().trim().isEmpty()) {
            movie.setGenre("기타");
            log.warn("영화 '{}'의 장르가 없어 기본값 '기타'로 설정", movie.getMovieTitle());
        }

        // 출연진 필드 검증
        if (movie.getMovieCast() == null || movie.getMovieCast().trim().isEmpty()) {
            movie.setMovieCast("출연진 정보 없음");
            log.warn("영화 '{}'의 출연진 정보가 없어 기본값으로 설정", movie.getMovieTitle());
        }

        // 감독 필드 검증
        if (movie.getMovieDirector() == null || movie.getMovieDirector().trim().isEmpty()) {
            movie.setMovieDirector("감독 정보 없음");
            log.warn("영화 '{}'의 감독 정보가 없어 기본값으로 설정", movie.getMovieTitle());
        }

        // 평점 필드 검증
        if (movie.getMovieRating() == null || movie.getMovieRating().trim().isEmpty()) {
            movie.setMovieRating("?");
            log.warn("영화 '{}'의 평점이 없어 기본값 '?'로 설정", movie.getMovieTitle());
        }

        // 등록일 필드 검증
        if (movie.getRegDate() == null) {
            movie.setRegDate(LocalDateTime.now());
            log.warn("영화 '{}'의 등록일이 없어 현재 시간으로 설정", movie.getMovieTitle());
        }

        // 영화 가격 필드 검증
        if (movie.getMoviePrice() == null) {
            movie.setMoviePrice(12000);
            log.warn("영화 '{}'의 가격이 없어 기본값 12000원으로 설정", movie.getMovieTitle());
        }

        // 줄거리 필드 검증 (nullable이지만 기본값 설정)
        if (movie.getMovieContent() == null || movie.getMovieContent().trim().isEmpty()) {
            movie.setMovieContent("줄거리 정보가 없습니다.");
            log.warn("영화 '{}'의 줄거리가 없어 기본값으로 설정", movie.getMovieTitle());
        }

        // 상세정보 필드 검증 (nullable이지만 기본값 설정)
        if (movie.getDetailInfo() == null || movie.getDetailInfo().trim().isEmpty()) {
            movie.setDetailInfo("상세정보가 없습니다.");
            log.warn("영화 '{}'의 상세정보가 없어 기본값으로 설정", movie.getMovieTitle());
        }

        // 개봉일 필드 검증 (nullable이지만 기본값 설정)
        if (movie.getOpenDate() == null || movie.getOpenDate().trim().isEmpty()) {
            movie.setOpenDate("개봉일 정보 없음");
            log.warn("영화 '{}'의 개봉일이 없어 기본값으로 설정", movie.getMovieTitle());
        }

        // 포스터 URL 필드 검증 (nullable이지만 기본값 설정)
        if (movie.getMoviePoster() == null || movie.getMoviePoster().trim().isEmpty()) {
            movie.setMoviePoster("default-poster.jpg");
            log.warn("영화 '{}'의 포스터 URL이 없어 기본값으로 설정", movie.getMovieTitle());
        }

        log.debug("영화 '{}' 유효성 검사 완료", movie.getMovieTitle());
    }

    private WebDriver createWebDriver() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        // 성능 최적화 옵션 추가
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-plugins");
        options.addArguments("--disable-images"); // 이미지 로딩 비활성화로 속도 향상
        options.addArguments("--disable-web-security");
        options.addArguments("--disable-features=VizDisplayCompositor");
        return new ChromeDriver(options);
    }

    private String extractInfoFromSelenium(WebDriver driver, String selector, String label) {
        try {
            // Selenium으로 특정 선택자에 해당하는 요소들을 찾아서 정보 추출
            List<WebElement> elements = driver.findElements(By.cssSelector(selector));
            log.debug("선택자 '{}'로 {}개 요소 발견", selector, elements.size());
            
            for (WebElement element : elements) {
                String elementText = element.getText().trim();
                // HTML에서 &nbsp;를 공백으로 변환
                elementText = elementText.replace("\u00A0", " ").trim();
                
                log.debug("요소 텍스트: '{}'", elementText);
            
                if (elementText.contains(label)) {
                    // "감독 : 필감성" 또는 "감독&nbsp;: 필감성" 형태 처리
                    String result = elementText.replace(label, "").replace(":", "").replace("：", "").trim();
                    if (!result.isEmpty()) {
                        // 장르의 경우 "/" 이후 부분 제거 (예: "드라마, 코미디 / 113 분" -> "드라마, 코미디")
                        if (label.equals("장르") && result.contains("/")) {
                            result = result.substring(0, result.indexOf("/")).trim();
                }
                        log.debug("{} 정보 추출 성공: '{}'", label, result);
                        return result;
                    }
                }
            }
            
            // 디버깅을 위해 페이지의 전체 HTML 구조 확인
            if (elements.isEmpty()) {
                log.debug("선택자 '{}'로 요소를 찾을 수 없습니다. 페이지 HTML 구조 확인 중...", selector);
                try {
                    String pageSource = driver.getPageSource();
                    if (pageSource.contains("movie-info")) {
                        log.debug("페이지에 movie-info 클래스가 존재합니다.");
                        // movie-info 영역의 실제 HTML 출력
                        try {
                            WebElement movieInfoElement = driver.findElement(By.cssSelector(".movie-info"));
                            log.debug("movie-info 영역 HTML: {}", movieInfoElement.getAttribute("outerHTML"));
        } catch (Exception e) {
                            log.debug("movie-info 요소를 찾을 수 없습니다.");
                        }
                    } else {
                        log.debug("페이지에 movie-info 클래스가 없습니다.");
                    }
                } catch (Exception e) {
                    log.debug("페이지 소스 확인 실패: {}", e.getMessage());
                }
            }
            
            return null;
        } catch (Exception e) {
            log.warn("Selenium에서 {} 정보 추출 실패: {}", label, e.getMessage());
            return null;
        }
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