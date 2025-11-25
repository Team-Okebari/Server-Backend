package com.okebari.artbite.payment.toss.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "슬라이스 응답 DTO")
public class SliceResponseDto<T> {
	@Schema(description = "데이터 목록")
	private List<T> data;
	@Schema(description = "슬라이스 정보")
	private SliceInfo sliceInfo;
}
