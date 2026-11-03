package com.appasadito.AsaditoRg;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:AsaditoRgApplicationTests;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE", "spring.datasource.username=sa", "spring.datasource.password="})
class AsaditoRgApplicationTests {

	@Test
	void contextLoads() {
	}

}
