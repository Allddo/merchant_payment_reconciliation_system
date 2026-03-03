package com.capgemini.mprs;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootTest
class MprsApplicationTests {

	@Test
	void encryptPassword() {
        System.out.println(new BCryptPasswordEncoder().encode("1234"));
	}

}
