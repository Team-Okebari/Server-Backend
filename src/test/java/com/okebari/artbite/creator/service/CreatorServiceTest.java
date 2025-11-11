package com.okebari.artbite.creator.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.okebari.artbite.creator.domain.Creator;
import com.okebari.artbite.creator.dto.CreatorRequest;
import com.okebari.artbite.creator.exception.CreatorNotFoundException;
import com.okebari.artbite.creator.mapper.CreatorMapper;
import com.okebari.artbite.creator.repository.CreatorRepository;

@ExtendWith(MockitoExtension.class)
class CreatorServiceTest {

	@Mock
	private CreatorRepository creatorRepository;

	@Mock
	private CreatorMapper creatorMapper;

	@InjectMocks
	private CreatorService creatorService;

	@Test
	void createSavesCreatorAndReturnsId() {
		CreatorRequest request = new CreatorRequest("name", "bio", "job", null, null, null, null, null, null, null);
		Creator entity = Creator.builder().name("name").bio("bio").jobTitle("job").build();

		when(creatorMapper.toEntity(request)).thenReturn(entity);
		when(creatorRepository.save(entity)).thenReturn(setId(entity, 10L));

		assertThat(creatorService.create(request)).isEqualTo(10L);
		verify(creatorRepository).save(entity);
	}

	@Test
	void updateThrowsWhenCreatorNotFound() {
		when(creatorRepository.findById(1L)).thenReturn(Optional.empty());
		CreatorRequest request = new CreatorRequest("name", "bio", "job", null, null, null, null, null, null, null);
		assertThatThrownBy(() -> creatorService.update(1L, request))
			.isInstanceOf(CreatorNotFoundException.class);
	}

	@Test
	void listReturnsSummaries() {
		Creator creator = Creator.builder().name("name").bio("bio").jobTitle("job").build();
		when(creatorRepository.findAll()).thenReturn(List.of(creator));
		when(creatorMapper.toSummaryList(List.of(creator))).thenReturn(List.of());

		assertThat(creatorService.list()).isEmpty();
	}

	private Creator setId(Creator creator, Long id) {
		try {
			var field = Creator.class.getDeclaredField("id");
			field.setAccessible(true);
			field.set(creator, id);
		} catch (ReflectiveOperationException ignored) {
		}
		return creator;
	}
}
