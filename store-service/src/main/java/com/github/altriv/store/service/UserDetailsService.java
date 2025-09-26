package com.github.altriv.store.service;

import com.github.altriv.store.config.StoreAdminCredentials;
import com.github.altriv.store.config.StorePredefinedUsersCredentials;
import com.github.altriv.store.entity.UserEntity;
import com.github.altriv.store.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class UserDetailsService implements ReactiveUserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StoreAdminCredentials storeAdmin;
    private final StorePredefinedUsersCredentials predefinedUsers;

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(userEntity -> User.builder()
                        .username(userEntity.getUsername())
                        .password(userEntity.getPassword())
                        .roles(userEntity.getRole())
                        .build()
                );
    }

    @EventListener(ApplicationReadyEvent.class)
    public void setUpUsers() {
        List<UserEntity> simpleUsers = predefinedUsers.credentials().stream()
                .map(credentials -> credentials.split(":"))
                .filter(userCredentials -> userCredentials.length == 2)
                .map(userCredentials -> new UserEntity(null, userCredentials[0], passwordEncoder.encode(userCredentials[1]), "USER"))
                .toList();
        List<UserEntity> allUsers = new ArrayList<>();
        allUsers.add(new UserEntity(null, storeAdmin.username(), passwordEncoder.encode(storeAdmin.password()), "ADMIN"));
        allUsers.addAll(simpleUsers);

        Flux.fromIterable(allUsers)
                .flatMap(userEntity -> userRepository.findByUsername(userEntity.getUsername()).switchIfEmpty(userRepository.save(userEntity)))
                .subscribe();
    }
}
