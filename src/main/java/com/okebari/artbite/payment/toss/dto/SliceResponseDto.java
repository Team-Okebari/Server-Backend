package com.okebari.artbite.payment.toss.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SliceResponseDto<T> {
	private List<T> data;
	private SliceInfo sliceInfo;
}
