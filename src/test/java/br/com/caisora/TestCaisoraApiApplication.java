package br.com.caisora;

import org.springframework.boot.SpringApplication;

public class TestCaisoraApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(CaisoraApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
