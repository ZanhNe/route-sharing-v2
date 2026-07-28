package com.zanh.route_sharing.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// import com.capstone.ridesharing.entity.NguoiDung; // Giả định Entity bạn sắp tạo
// import com.capstone.ridesharing.repository.NguoiDungRepository; // Giả định Repo bạn sắp tạo

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    // private final NguoiDungRepository nguoiDungRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        // Mock data tạm thời
        return new User(username, "password", new ArrayList<>());
    }
}