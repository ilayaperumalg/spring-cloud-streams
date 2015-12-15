/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.stream.binding;

import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.SubscribableChannel;

/**
 * Class that {@link BindableProxyFactory} uses to create message channels.
 *
 * @author Marius Bogoevici
 * @author David Syer
 * @author Ilayaperumal Gopinathan
 */
public class DefaultChannelFactory implements ChannelFactory {

	private final MessageConverterConfigurer messageConverterConfigurer;

	public DefaultChannelFactory(MessageConverterConfigurer messageConverterConfigurer) {
		this.messageConverterConfigurer = messageConverterConfigurer;
	}

	@Override
	public PollableChannel createPollableBindableChannel(String name) throws Exception {
		PollableChannel pollableChannel = new QueueChannel();
		messageConverterConfigurer.configureMessageConverters(pollableChannel, name);
		return pollableChannel;
	}

	@Override
	public SubscribableChannel createSubscribableBindableChannel(String name) throws Exception {
		SubscribableChannel subscribableChannel = new DirectChannel();
		messageConverterConfigurer.configureMessageConverters(subscribableChannel, name);
		return subscribableChannel;
	}

	@Override
	public SubscribableChannel createSubscribableSharedChannel() throws Exception {
		return new DirectChannel();
	}

	@Override
	public PollableChannel createPollableSharedChannel() throws Exception {
		return new QueueChannel();
	}
}
