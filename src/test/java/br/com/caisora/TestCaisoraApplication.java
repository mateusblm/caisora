package br.com.caisora;

import org.springframework.boot.SpringApplication;

public class TestCaisoraApplication {

    public static void main(String[] args) {
        SpringApplication.from(CaisoraApplication::main).with(TestcontainersConfiguration.class).run(args);
    }
}
