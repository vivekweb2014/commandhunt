package com.wirehall.commandbuilder.service.auth;


import com.wirehall.commandbuilder.dto.User;
import com.wirehall.commandbuilder.exception.ResourceNotFoundException;
import com.wirehall.commandbuilder.model.auth.CustomUserPrincipal;
import com.wirehall.commandbuilder.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * The principal is always the result of the UserDetailsService that returned the object
 * <p>
 * NOTE:
 * There is often some confusion about UserDetailsService.
 * It is purely a DAO for user data and performs no other function other than to supply that data
 * to other components within the framework. In particular, it does not authenticate the user,
 * which is done by the AuthenticationManager. In many cases it makes more sense to implement AuthenticationProvider
 * directly if you require a custom authentication process.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email : " + email));

        return CustomUserPrincipal.create(user);
    }

    public UserDetails loadUserById(Object id) {
        User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return CustomUserPrincipal.create(user);
    }
}