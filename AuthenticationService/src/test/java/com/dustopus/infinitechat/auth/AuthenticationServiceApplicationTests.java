package com.dustopus.infinitechat.auth;

import com.dustopus.infinitechat.auth.model.User;
import com.dustopus.infinitechat.auth.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AuthenticationServiceApplicationTests {
    @Autowired
    private UserService userService;

    @Test
    void contextLoads() {
    }

    @Test
    void testUserService() {
        // Note: requires a running database with data
        // User user = userService.getById(1L);
        // System.out.println(user);
    }
}
