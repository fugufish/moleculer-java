/**
 * This software is licensed under MIT license.<br>
 * <br>
 * Copyright 2017 Andras Berkes [andras.berkes@programmer.net]<br>
 * <br>
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:<br>
 * <br>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.<br>
 * <br>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package services.moleculer.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.api.async.RedisStringAsyncCommands;
import com.lambdaworks.redis.cluster.RedisClusterClient;
import com.lambdaworks.redis.event.Event;
import com.lambdaworks.redis.event.EventBus;
import com.lambdaworks.redis.resource.DefaultClientResources;
import com.lambdaworks.redis.resource.EventLoopGroupProvider;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;
import rx.Observable;

/**
 * 
 */
public final class RedisUtilities {

	// --- GET/SET CONNECTIONS ---

	public static final RedisStringAsyncCommands<String, String> getAsyncCommands(String[] urls, String password,
			boolean useSSL, boolean startTLS, NioEventLoopGroup group) {

		// Open new connection
		List<RedisURI> redisURIs = parseURLs(urls, password, useSSL, startTLS);
		DefaultClientResources clientResources = createClientResources(null, group);
		RedisStringAsyncCommands<String, String> commands;
		if (urls.length > 1) {

			// Clustered client
			RedisClusterClient client = RedisClusterClient.create(clientResources, redisURIs);
			commands = client.connect().async();

		} else {

			// Single connection
			RedisClient client = RedisClient.create(clientResources, redisURIs.get(0));
			commands = client.connect().async();
		}
		return commands;
	}

	// --- PRIVATE UTILITIES ---

	public static final List<RedisURI> parseURLs(String[] urls, String password, boolean useSSL, boolean startTLS) {
		ArrayList<RedisURI> list = new ArrayList<>(urls.length);
		for (String url : urls) {
			url = url.trim();
			if (url.startsWith("redis://")) {
				url = url.substring(8);
			}
			if (url.endsWith("/")) {
				url = url.substring(0, url.length() - 1);
			}
			int i = url.indexOf(':');
			String host = "localhost";
			int port = 6379;
			if (i > -1) {
				host = url.substring(0, i);
				port = Integer.parseInt(url.substring(i + 1));
			} else {
				host = url;
			}
			RedisURI.Builder builder = RedisURI.builder().withHost(host).withPort(port).withSsl(useSSL)
					.withStartTls(startTLS);
			if (password != null && !password.isEmpty()) {
				builder.withPassword(password);
			}
			list.add(builder.build());
		}
		return list;
	}

	public static final DefaultClientResources createClientResources(EventBus eventBus, NioEventLoopGroup group) {
		DefaultClientResources.Builder builder = DefaultClientResources.builder();
		builder.eventLoopGroupProvider(new EventLoopGroupProvider() {
			
			@Override
			public final int threadPoolSize() {
				return 1;
			}
			
			@Override
			public final Future<Boolean> shutdown(long quietPeriod, long timeout, TimeUnit timeUnit) {
				return null;
			}
			
			@Override
			public final Future<Boolean> release(EventExecutorGroup eventLoopGroup, long quietPeriod, long timeout, TimeUnit unit) {
				return null;
			}
			
			@SuppressWarnings("unchecked")
			@Override
			public final <T extends EventLoopGroup> T allocate(Class<T> type) {
				return (T) group;
			}
			
		});
		builder.ioThreadPoolSize(1);
		if (eventBus == null) {
			eventBus = new EventBus() {

				@Override
				public final void publish(Event event) {

					// Ignore
				}

				@Override
				public final Observable<Event> get() {

					// Ignore
					return null;
				}
			};
		}
		builder.eventBus(eventBus);
		return builder.build();
	}

}