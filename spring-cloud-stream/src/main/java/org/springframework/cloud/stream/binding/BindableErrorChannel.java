/*
 * Copyright 2016 the original author or authors.
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.springframework.integration.channel.PublishSubscribeChannel;

/**
 * A {@link Bindable} component that represents an error channel.
 *
 * @author Ilayaperumal Gopinathan
 */
public class BindableErrorChannel extends BindableAdapter {

	private final String name;

	private final PublishSubscribeChannel errorChannel;

	public BindableErrorChannel(String name, PublishSubscribeChannel errorChannel) {
		this.name = name;
		this.errorChannel = errorChannel;
	}

	@Override
	public void bindOutputs(ChannelBindingService adapter) {
		adapter.bindProducer(errorChannel, name);
	}

	@Override
	public void unbindOutputs(ChannelBindingService adapter) {
		adapter.unbindProducers(name);
	}

	@Override
	public Set<String> getOutputs() {
		return new HashSet<String>(Arrays.asList(name));
	}
}
