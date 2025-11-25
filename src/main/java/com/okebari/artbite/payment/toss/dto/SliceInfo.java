package com.okebari.artbite.payment.toss.dto;

import org.springframework.data.domain.Pageable;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "슬라이스(페이지) 정보 DTO")
public class SliceInfo {
	@Schema(description = "현재 슬라이스 번호 (0부터 시작)", example = "0")
	private final int getNumber; // 현재 슬라이스 번호
	@Schema(description = "현재 슬라이스 크기 (한 페이지당 요소 수)", example = "10")
	private final int getSize; // 현재 슬라이스 크기
	@Schema(description = "현재 슬라이스가 가지고 있는 엔티티의 개수", example = "7")
	private final int getNumberOfElements; // 현재 슬라이스가 가지고 있는 엔티티의 개수
	@Schema(description = "다음 슬라이스의 존재 유무", example = "true")
	private final boolean hasNext; // 다음 슬라이스의 존재 유무
	@Schema(description = "이전 슬라이스의 존재 유무", example = "false")
	private final boolean hasPrevious; // 이전 슬라이스의 존재 유무

	public SliceInfo(Pageable pageable, int getNumberOfElements, boolean hasNext) {
		this.getNumber = pageable.getPageNumber();
		this.getSize = pageable.getPageSize();
		this.getNumberOfElements = getNumberOfElements;
		this.hasNext = hasNext;
		this.hasPrevious = pageable.hasPrevious();
	}
}
