package com.okebari.artbite.creator.exception;

import com.okebari.artbite.common.exception.BusinessException;
import com.okebari.artbite.common.exception.ErrorCode;

/**
 * 작가 정보를 찾지 못했을 때 사용하는 예외.
 */
public class CreatorNotFoundException extends BusinessException {

	public CreatorNotFoundException(Long creatorId) {
		super(ErrorCode.CREATOR_NOT_FOUND, "존재하지 않는 작가(" + creatorId + ")입니다.");
	}

	public CreatorNotFoundException() {
		super(ErrorCode.CREATOR_NOT_FOUND, "존재하지 않는 작가입니다.");
	}

	public CreatorNotFoundException(String message) {
		super(ErrorCode.CREATOR_NOT_FOUND, message);
	}
}
