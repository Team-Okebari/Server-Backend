package com.okebari.artbite.membership.controller;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.okebari.artbite.AbstractContainerBaseTest;
import com.okebari.artbite.auth.vo.CustomUserDetails;
import com.okebari.artbite.domain.membership.Membership;
import com.okebari.artbite.domain.membership.MembershipPlanType;
import com.okebari.artbite.domain.membership.MembershipRepository;
import com.okebari.artbite.domain.membership.MembershipStatus;
import com.okebari.artbite.domain.user.User;
import com.okebari.artbite.domain.user.UserRepository;
import com.okebari.artbite.domain.user.UserRole;
import com.okebari.artbite.membership.dto.EnrollMembershipRequestDto;
import com.okebari.artbite.membership.service.MembershipService;

@SpringBootTest
@AutoConfigureMockMvc
class MembershipControllerTest extends AbstractContainerBaseTest {

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private MembershipService membershipService;

	@Autowired
	private MembershipRepository membershipRepository;

	// Test users
	private User regularUser;
	private User adminUser;
	private UserDetails regularUserDetails;
	private UserDetails adminUserDetails;

	@BeforeEach
	void setUp() {
		userRepository.deleteAll(); // Clear users before each test
		membershipRepository.deleteAll(); // Clear memberships before each test

		regularUser = User.builder()
			.email("testuser@example.com")
			.password("password")
			.username("Test User")
			.role(UserRole.USER)
			.enabled(true)
			.accountNonExpired(true)
			.accountNonLocked(true)
			.credentialsNonExpired(true)
			.tokenVersion(1)
			.build();
		userRepository.saveAndFlush(regularUser);
		regularUserDetails = new CustomUserDetails(regularUser);

		adminUser = User.builder()
			.email("admin@example.com")
			.password("password")
			.username("Admin User")
			.role(UserRole.ADMIN)
			.enabled(true)
			.accountNonExpired(true)
			.accountNonLocked(true)
			.credentialsNonExpired(true)
			.tokenVersion(1)
			.build();
		userRepository.saveAndFlush(adminUser);
		adminUserDetails = new CustomUserDetails(adminUser);
	}

	@Test
	@DisplayName("일반 사용자의 멤버십 가입 성공")
	void enrollMembership_success() throws Exception {
		EnrollMembershipRequestDto requestDto = new EnrollMembershipRequestDto();
		requestDto.setAutoRenew(true);

		mockMvc.perform(post("/api/memberships/enroll")
				.with(user(regularUserDetails)) // Authenticate as regular user
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestDto)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.status").value("ACTIVE"));
	}

	@Test
	@DisplayName("멤버십 가입 실패: 이미 활성 멤버십이 존재")
	void enrollMembership_alreadyActive() throws Exception {
		// 먼저 멤버십을 가입시킵니다.
		EnrollMembershipRequestDto enrollRequest = new EnrollMembershipRequestDto();
		enrollRequest.setAutoRenew(true);
		membershipService.enrollMembership(regularUser.getId(), enrollRequest);

		// 다시 가입을 시도합니다.
		EnrollMembershipRequestDto reEnrollRequest = new EnrollMembershipRequestDto();
		reEnrollRequest.setAutoRenew(false);

		mockMvc.perform(post("/api/memberships/enroll")
				.with(user(regularUserDetails))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(reEnrollRequest)))
			.andExpect(status().isConflict()) // Expect 409 Conflict
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("M002"));
	}

	@Test
	@DisplayName("멤버십 재가입 성공: 만료된 멤버십")
	void enrollMembership_reEnrollExpired() throws Exception {
		// 만료된 멤버십을 생성합니다.
		Membership expiredMembership = Membership.builder()
			.user(regularUser)
			.status(MembershipStatus.EXPIRED)
			.planType(MembershipPlanType.DEFAULT_MEMBER_PLAN)
			.startDate(LocalDateTime.now().minusMonths(2))
			.endDate(LocalDateTime.now().minusMonths(1))
			.consecutiveMonths(1)
			.autoRenew(false)
			.build();
		membershipRepository.save(expiredMembership);

		EnrollMembershipRequestDto requestDto = new EnrollMembershipRequestDto();
		requestDto.setAutoRenew(true);

		mockMvc.perform(post("/api/memberships/enroll")
				.with(user(regularUserDetails))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestDto)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.status").value("ACTIVE"))
			.andExpect(jsonPath("$.data.consecutiveMonths").value(expiredMembership.getConsecutiveMonths() + 1));
	}

	@Test
	@DisplayName("멤버십 재가입 성공: 취소된 멤버십")
	void enrollMembership_reEnrollCanceled() throws Exception {
		// 취소된 멤버십을 생성합니다.
		Membership canceledMembership = Membership.builder()
			.user(regularUser)
			.status(MembershipStatus.CANCELED)
			.planType(MembershipPlanType.DEFAULT_MEMBER_PLAN)
			.startDate(LocalDateTime.now().minusMonths(2))
			.endDate(LocalDateTime.now().plusMonths(1)) // Canceled but end_date might be in future
			.consecutiveMonths(1)
			.autoRenew(false)
			.build();
		membershipRepository.save(canceledMembership);

		EnrollMembershipRequestDto requestDto = new EnrollMembershipRequestDto();
		requestDto.setAutoRenew(true);

		mockMvc.perform(post("/api/memberships/enroll")
				.with(user(regularUserDetails))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestDto)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.status").value("ACTIVE"))
			.andExpect(jsonPath("$.data.consecutiveMonths").value(canceledMembership.getConsecutiveMonths() + 1));
	}

	@Test
	@DisplayName("멤버십 취소 성공")
	void cancelMembership_success() throws Exception {
		// 먼저 활성 멤버십을 생성합니다.
		EnrollMembershipRequestDto enrollRequest = new EnrollMembershipRequestDto();
		enrollRequest.setAutoRenew(true);
		membershipService.enrollMembership(regularUser.getId(), enrollRequest);

		transactionTemplate.execute(txStatus -> {
			try {
				User updatedUser = transactionTemplate.execute(status -> {
					User user = userRepository.findById(regularUser.getId()).orElseThrow();
					Hibernate.initialize(user.getMemberships());
					return user;
				});

				CustomUserDetails updatedUserDetails = new CustomUserDetails(updatedUser);
				mockMvc.perform(post("/api/memberships/cancel")
						.with(user(updatedUserDetails)))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.success").value(true))
					.andExpect(jsonPath("$.data").doesNotExist());
				// 취소되었는지 확인
				Membership canceled = membershipRepository.findTopByUserAndStatusOrderByStartDateDesc(regularUser,
						MembershipStatus.CANCELED)
					.orElseThrow(() -> new AssertionError("Canceled membership not found"));
				assertThat(canceled.getStatus()).isEqualTo(MembershipStatus.CANCELED);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			return null;
		});
	}

	@Test
	@DisplayName("멤버십 취소 실패: 멤버십이 없는 경우")
	void cancelMembership_notMember() throws Exception {
		// 멤버십이 없는 상태에서 취소 시도
		mockMvc.perform(post("/api/memberships/cancel")
				.with(user(regularUserDetails)))
			.andExpect(status().isNotFound()) // Expect 404 Not Found
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("M001"));
	}

	@Test
	@DisplayName("멤버십 상태 조회 성공: 활성 멤버십")
	void getMembershipStatus_active() throws Exception {
		// 활성 멤버십 생성
		membershipService.enrollMembership(regularUser.getId(), new EnrollMembershipRequestDto() {{
			setAutoRenew(true);
		}});

		mockMvc.perform(get("/api/memberships/status")
				.with(user(regularUserDetails)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.status").value("ACTIVE"));
	}

	@Test
	@DisplayName("멤버십 상태 조회 성공: 취소된 멤버십")
	void getMembershipStatus_canceled() throws Exception {
		// 취소된 멤버십 생성
		Membership canceledMembership = Membership.builder()
			.user(regularUser)
			.status(MembershipStatus.CANCELED)
			.planType(MembershipPlanType.DEFAULT_MEMBER_PLAN)
			.startDate(LocalDateTime.now().minusMonths(2))
			.endDate(LocalDateTime.now().plusMonths(1))
			.consecutiveMonths(1)
			.autoRenew(false)
			.build();
		membershipRepository.save(canceledMembership);

		mockMvc.perform(get("/api/memberships/status")
				.with(user(regularUserDetails)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.status").value("CANCELED"));
	}

	@Test
	@DisplayName("멤버십 상태 조회 성공: 만료된 멤버십")
	void getMembershipStatus_expired() throws Exception {
		// 만료된 멤버십 생성
		Membership expiredMembership = Membership.builder()
			.user(regularUser)
			.status(MembershipStatus.EXPIRED)
			.planType(MembershipPlanType.DEFAULT_MEMBER_PLAN)
			.startDate(LocalDateTime.now().minusMonths(2))
			.endDate(LocalDateTime.now().minusMonths(1))
			.consecutiveMonths(1)
			.autoRenew(false)
			.build();
		membershipRepository.save(expiredMembership);

		mockMvc.perform(get("/api/memberships/status")
				.with(user(regularUserDetails)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.status").value("EXPIRED"));
	}

	@Test
	@DisplayName("멤버십 상태 조회 성공: 정지된 멤버십")
	void getMembershipStatus_banned() throws Exception {
		// 정지된 멤버십 생성
		Membership bannedMembership = Membership.builder()
			.user(regularUser)
			.status(MembershipStatus.BANNED)
			.planType(MembershipPlanType.DEFAULT_MEMBER_PLAN)
			.startDate(LocalDateTime.now().minusMonths(2))
			.endDate(LocalDateTime.now().minusMonths(1))
			.consecutiveMonths(1)
			.autoRenew(false)
			.build();
		membershipRepository.save(bannedMembership);

		mockMvc.perform(get("/api/memberships/status")
				.with(user(regularUserDetails)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.status").value("BANNED"));
	}

	@Test
	@DisplayName("관리자의 멤버십 정지 성공")
	void banMembership_success() throws Exception {
		// 활성 멤버십 생성
		membershipService.enrollMembership(regularUser.getId(), new EnrollMembershipRequestDto() {{
			setAutoRenew(true);
		}});

		mockMvc.perform(post("/api/memberships/{userId}/ban", regularUser.getId())
				.with(user(adminUserDetails))) // Authenticate as admin
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true));

		// 정지되었는지 확인
		Membership banned = membershipRepository.findTopByUserAndStatusOrderByStartDateDesc(regularUser,
				MembershipStatus.BANNED)
			.orElseThrow(() -> new AssertionError("Banned membership not found"));
		assertThat(banned.getStatus()).isEqualTo(MembershipStatus.BANNED);
	}

	@Test
	@DisplayName("일반 사용자의 멤버십 정지 실패: 권한 없음")
	void banMembership_unauthorized() throws Exception {
		// 활성 멤버십 생성
		membershipService.enrollMembership(regularUser.getId(), new EnrollMembershipRequestDto() {{
			setAutoRenew(true);
		}});

		mockMvc.perform(post("/api/memberships/{userId}/ban", regularUser.getId())
				.with(user(regularUserDetails))) // Authenticate as regular user
			.andExpect(status().isForbidden()) // Expect 403 Forbidden
			.andExpect(jsonPath("$.success").value(false));
	}

	@Test
	@DisplayName("관리자의 멤버십 정지 해제 성공")
	void unbanMembership_success() throws Exception {
		// 정지된 멤버십 생성
		Membership bannedMembership = Membership.builder()
			.user(regularUser)
			.status(MembershipStatus.BANNED)
			.planType(MembershipPlanType.DEFAULT_MEMBER_PLAN)
			.startDate(LocalDateTime.now().minusMonths(2))
			.endDate(LocalDateTime.now().minusMonths(1))
			.consecutiveMonths(1)
			.autoRenew(false)
			.build();
		membershipRepository.save(bannedMembership);

		mockMvc.perform(post("/api/memberships/{userId}/unban", regularUser.getId())
				.with(user(adminUserDetails))) // Authenticate as admin
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true));

		// 정지 해제되었는지 확인 (EXPIRED 상태로 변경됨)
		Membership unbanned = membershipRepository.findTopByUserAndStatusOrderByStartDateDesc(regularUser,
				MembershipStatus.EXPIRED)
			.orElseThrow(() -> new AssertionError("Unbanned membership (EXPIRED) not found"));
		assertThat(unbanned.getStatus()).isEqualTo(MembershipStatus.EXPIRED);
	}

	@Test
	@DisplayName("일반 사용자의 멤버십 정지 해제 실패: 권한 없음")
	void unbanMembership_unauthorized() throws Exception {
		// 정지된 멤버십 생성
		Membership bannedMembership = Membership.builder()
			.user(regularUser)
			.status(MembershipStatus.BANNED)
			.planType(MembershipPlanType.DEFAULT_MEMBER_PLAN)
			.startDate(LocalDateTime.now().minusMonths(2))
			.endDate(LocalDateTime.now().minusMonths(1))
			.consecutiveMonths(1)
			.autoRenew(false)
			.build();
		membershipRepository.save(bannedMembership);

		mockMvc.perform(post("/api/memberships/{userId}/unban", regularUser.getId())
				.with(user(regularUserDetails))) // Authenticate as regular user
			.andExpect(status().isForbidden()) // Expect 403 Forbidden
			.andExpect(jsonPath("$.success").value(false));
	}
}
