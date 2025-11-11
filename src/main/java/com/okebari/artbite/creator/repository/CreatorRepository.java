package com.okebari.artbite.creator.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.okebari.artbite.creator.domain.Creator;

/**
 * Creator 엔티티용 JPA 레포지토리.
 */
public interface CreatorRepository extends JpaRepository<Creator, Long> {
}

