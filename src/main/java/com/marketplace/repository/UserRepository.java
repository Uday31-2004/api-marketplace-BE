package com.marketplace.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.marketplace.models.User;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByEmail(String email);

    Optional<User> findByVerificationToken(String verificationToken);
}
