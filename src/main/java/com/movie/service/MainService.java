package com.movie.service;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MainService {

    public List<String> crowlingMovieVideo(){
        List<String> infoVideo = new ArrayList<>();

        try {
            String url = "https://www.cgv.co.kr/";
            Document doc = Jsoup.connect(url).timeout(5000).get();

            org.jsoup.nodes.Element wrap = doc.getElementById("ctl00_PlaceHolderContent_divMovieSelection_wrap");
            if (wrap != null) {
                Element contents = wrap.selectFirst(".contents");
                if (contents != null) {
                    Element videoWrap = contents.selectFirst(".video_wrap");
                    if (videoWrap != null) {
                        Element video = videoWrap.selectFirst("video");
                        if (video != null) {
                            Element source = video.selectFirst("source");
                            if (source != null) {
                                String videoUrl = source.attr("src");
                                infoVideo.add(videoUrl);
                                System.out.println("동영상 URL: " + videoUrl);
                            } else {
                                System.out.println("source 태그가 없습니다.");
                            }
                        } else {
                            System.out.println("video 태그가 없습니다.");
                        }
                        // 🎬 제목 가져오기
                        Element titleElement = doc.getElementById("ctl00_PlaceHolderContent_AD_MOVIE_NM");
                        if (titleElement != null) {
                            String movieTitle  = titleElement.text();
                            infoVideo.add(movieTitle);
                            System.out.println("영화 제목: " + movieTitle);
                        } else {
                            System.out.println("영화 제목 태그를 찾지 못했습니다.");
                        }
                    } else {
                        System.out.println("video_wrap 요소가 없습니다.");
                    }
                } else {
                    System.out.println("contents 요소가 없습니다.");
                }
            } else {
                System.out.println("ctl00_PlaceHolderContent_divMovieSelection_wrap 요소가 없습니다.");
                String videoUrl = "https://www.youtube.com/embed/Xb96_61kMS8";
                String movieTitle = "전지적 독자시점";
                infoVideo.add(videoUrl);
                infoVideo.add(movieTitle);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    return infoVideo;
    }
}
