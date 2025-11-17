package com.okebari.artbite.creator.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "note_creator")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
/**
 * Creator 도메인: 노트 외부에서 재사용 가능한 작가 정보를 보관한다.
 * 노트는 creator_id FK로 이 엔티티를 참조하고, 관리자 화면에서 독립적으로 CRUD 된다.
 */
public class Creator {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 60)
	private String name;

	@Column(length = 100)
	private String bio;

	@Column(name = "job_title", length = 60)
	private String jobTitle;

	@Column(name = "profile_image_url", length = 500)
	private String profileImageUrl;

	@Column(name = "instagram_url", length = 500)
	private String instagramUrl;

	@Column(name = "youtube_url", length = 500)
	private String youtubeUrl;

	@Column(name = "behance_url", length = 500)
	private String behanceUrl;

	@Column(name = "x_url", length = 500)
	private String xUrl;

	@Column(name = "blog_url", length = 500)
	private String blogUrl;

	@Column(name = "news_url", length = 500)
	private String newsUrl;

	@Builder
	private Creator(String name, String bio, String jobTitle, String instagramUrl, String youtubeUrl,
		String behanceUrl, String xUrl, String blogUrl, String newsUrl, String profileImageUrl) {
		this.name = name;
		this.bio = bio;
		this.jobTitle = jobTitle;
		this.profileImageUrl = profileImageUrl;
		this.instagramUrl = instagramUrl;
		this.youtubeUrl = youtubeUrl;
		this.behanceUrl = behanceUrl;
		this.xUrl = xUrl;
		this.blogUrl = blogUrl;
		this.newsUrl = newsUrl;
	}

	public void updateProfile(String name, String bio, String jobTitle, String profileImageUrl) {
		this.name = name;
		this.bio = bio;
		this.jobTitle = jobTitle;
		this.profileImageUrl = profileImageUrl;
	}

	public void updateSocialLinks(String instagram, String youtube, String behance,
		String x, String blog, String news) {
		this.instagramUrl = instagram;
		this.youtubeUrl = youtube;
		this.behanceUrl = behance;
		this.xUrl = x;
		this.blogUrl = blog;
		this.newsUrl = news;
	}
}
