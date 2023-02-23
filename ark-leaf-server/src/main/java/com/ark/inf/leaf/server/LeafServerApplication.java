package com.ark.inf.leaf.server;

import com.ark.inf.leaf.nacos.NacosConfig;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@EnableDubbo
@SpringBootApplication(scanBasePackages = {"com.ark"})
public class LeafServerApplication {

	public static void main(String[] args) {
		NacosConfig.init();
		SpringApplication.run(LeafServerApplication.class, args);
	}

}
