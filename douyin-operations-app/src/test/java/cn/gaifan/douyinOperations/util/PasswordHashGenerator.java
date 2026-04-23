package cn.gaifan.douyinOperations.util;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
public class PasswordHashGenerator {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    public void generatePasswordHash() {
        String password = "admin123";
        String hash = passwordEncoder.encode(password);
        System.out.println("=".repeat(80));
        System.out.println("Password: " + password);
        System.out.println("Hash: " + hash);
        System.out.println("Verification: " + passwordEncoder.matches(password, hash));
        System.out.println("=".repeat(80));
    }
}
