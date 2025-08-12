package com.marketplace.auth;

import lombok.*;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    private String id;
    private String name;
    private String email;
    private String password;
    private boolean verified = false;

    private String verificationToken;
    private LocalDateTime tokenExpiry;

}
