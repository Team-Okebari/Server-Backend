package com.okebari.artbite.domain.membership;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MembershipInducementImageRepository extends JpaRepository<MembershipInducementImage, Long> {
}
