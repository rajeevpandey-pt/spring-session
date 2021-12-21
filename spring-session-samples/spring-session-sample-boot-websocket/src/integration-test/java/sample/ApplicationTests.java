/*
 * Copyright 2014-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sample;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.GenericContainer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.sockjs.client.RestTemplateXhrTransport;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Rob Winch
 * @author Vedran Pavic
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ApplicationTests {

	private static final String DOCKER_IMAGE = "redis:5.0.14";

	@Value("${local.server.port}")
	private String port;

	@Autowired
	private WebSocketHandler webSocketHandler;

	@Test
	void run() {
		List<Transport> transports = new ArrayList<>(2);
		transports.add(new WebSocketTransport(new StandardWebSocketClient()));
		transports.add(new RestTemplateXhrTransport());

		SockJsClient sockJsClient = new SockJsClient(transports);
		ListenableFuture<WebSocketSession> wsSession = sockJsClient.doHandshake(this.webSocketHandler,
				"ws://localhost:" + this.port + "/sockjs");

		assertThatExceptionOfType(ExecutionException.class)
				.isThrownBy(() -> wsSession.get().sendMessage(new TextMessage("a")));
	}

	@TestConfiguration
	static class Config {

		@Bean
		GenericContainer redisContainer() {
			GenericContainer redisContainer = new GenericContainer(DOCKER_IMAGE).withExposedPorts(6379);
			redisContainer.start();
			return redisContainer;
		}

		@Bean
		LettuceConnectionFactory redisConnectionFactory() {
			return new LettuceConnectionFactory(redisContainer().getContainerIpAddress(),
					redisContainer().getFirstMappedPort());
		}

	}

}
