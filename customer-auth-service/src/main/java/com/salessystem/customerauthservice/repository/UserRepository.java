package com.salessystem.customerauthservice.repository;

import com.salessystem.customerauthservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for User database operations.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by their GitHub unique ID.
     *
     * @param githubId GitHub ID string
     * @return Optional containing the User if found
     */
    Optional<User> findByGithubId(String githubId);

    /**
     * Finds a user by their email address.
     *
     * @param email User email string
     * @return Optional containing the User if found
     */
    Optional<User> findByEmail(String email);
}