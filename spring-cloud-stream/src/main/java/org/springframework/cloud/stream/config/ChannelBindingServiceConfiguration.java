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

package org.springframework.cloud.stream.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.binder.BinderFactory;
import org.springframework.cloud.stream.binding.BinderAwareChannelResolver;
import org.springframework.cloud.stream.binding.BinderAwareRouterBeanPostProcessor;
import org.springframework.cloud.stream.binding.ChannelBindingService;
import org.springframework.cloud.stream.binding.ChannelFactory;
import org.springframework.cloud.stream.binding.ContextStartAfterRefreshListener;
import org.springframework.cloud.stream.binding.DefaultChannelFactory;
import org.springframework.cloud.stream.binding.InputBindingLifecycle;
import org.springframework.cloud.stream.binding.MessageConverterConfigurer;
import org.springframework.cloud.stream.binding.OutputBindingLifecycle;
import org.springframework.cloud.stream.tuple.spel.TuplePropertyAccessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.convert.converter.Converter;
import org.springframework.expression.PropertyAccessor;
import org.springframework.integration.config.IntegrationEvaluationContextFactoryBean;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.json.JsonPropertyAccessor;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.core.DestinationResolutionException;
import org.springframework.messaging.core.DestinationResolver;

/**
 * Configuration class that provides necessary beans for {@link MessageChannel} binding.
 *
 * @author Dave Syer
 * @author David Turanski
 * @author Marius Bogoevici
 * @author Ilayaperumal Gopinathan
 */
@Configuration
@EnableConfigurationProperties(ChannelBindingServiceProperties.class)
public class ChannelBindingServiceConfiguration {

	@Bean
	// This conditional is intentionally not in an autoconfig (usually a bad idea) because
	// it is used to detect a ChannelBindingService in the parent context (which we know
	// already exists).
	@ConditionalOnMissingBean(ChannelBindingService.class)
	public ChannelBindingService bindingService(
			ChannelBindingServiceProperties channelBindingServiceProperties,
			BinderFactory<MessageChannel> binderFactory) {
		return new ChannelBindingService(channelBindingServiceProperties, binderFactory);
	}

	@Bean
	public MessageConverterConfigurer messageConverterConfigurer(ChannelBindingServiceProperties channelBindingServiceProperties) {
		return new MessageConverterConfigurer(channelBindingServiceProperties);
	}

	@Bean
	public ChannelFactory channelFactory(ChannelBindingServiceProperties channelBindingServiceProperties) {
		return new DefaultChannelFactory(messageConverterConfigurer(channelBindingServiceProperties));
	}

	@Bean
	@DependsOn("bindingService")
	public OutputBindingLifecycle outputBindingLifecycle() {
		return new OutputBindingLifecycle();
	}

	@Bean
	@DependsOn("bindingService")
	public InputBindingLifecycle inputBindingLifecycle() {
		return new InputBindingLifecycle();
	}

	@Bean
	@DependsOn("bindingService")
	public ContextStartAfterRefreshListener contextStartAfterRefreshListener() {
		return new ContextStartAfterRefreshListener();
	}

	@Bean
	public BinderAwareChannelResolver binderAwareChannelResolver(
			BinderFactory<MessageChannel> binderFactory) {
		return new BinderAwareChannelResolver(binderFactory, new Properties());
	}

	@Bean
	@ConfigurationPropertiesBinding
	public Converter<String,BindingProperties> bindingPropertiesConverter() {
		return new BindingPropertiesConverter();
	}

	// IMPORTANT: Nested class to avoid instantiating all of the above early
	@Configuration
	protected static class PostProcessorConfiguration {

		private BinderAwareChannelResolver binderAwareChannelResolver;

		@Bean
		public BinderAwareRouterBeanPostProcessor binderAwareRouterBeanPostProcessor(
				final ConfigurableListableBeanFactory beanFactory) {
			// IMPORTANT: Lazy delegate to avoid instantiating all of the above early
			return new BinderAwareRouterBeanPostProcessor(
					new DestinationResolver<MessageChannel>() {

						@Override
						public MessageChannel resolveDestination(String name)
								throws DestinationResolutionException {
							if (PostProcessorConfiguration.this.binderAwareChannelResolver == null) {
								PostProcessorConfiguration.this.binderAwareChannelResolver = BeanFactoryUtils
										.beanOfType(beanFactory,
												BinderAwareChannelResolver.class);
							}
							return PostProcessorConfiguration.this.binderAwareChannelResolver
									.resolveDestination(name);
						}

					});
		}

		/**
		 * Adds property accessors for use in SpEL expression evaluation
		 */
		@Bean
		public static BeanPostProcessor propertyAccessorBeanPostProcessor() {
			final Map<String, PropertyAccessor> accessors = new HashMap<>();
			accessors.put("tuplePropertyAccessor", new TuplePropertyAccessor());
			accessors.put("jsonPropertyAccessor", new JsonPropertyAccessor());
			return new BeanPostProcessor() {

				@Override
				public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
					if (IntegrationContextUtils.INTEGRATION_EVALUATION_CONTEXT_BEAN_NAME.equals(beanName)) {
						IntegrationEvaluationContextFactoryBean factoryBean = (IntegrationEvaluationContextFactoryBean) bean;
						Map<String, PropertyAccessor> factoryBeanAccessors = factoryBean.getPropertyAccessors();
						for (Map.Entry<String, PropertyAccessor> entry : accessors.entrySet()) {
							if (!factoryBeanAccessors.containsKey(entry.getKey())) {
								factoryBeanAccessors.put(entry.getKey(), entry.getValue());
							}
						}
						factoryBean.setPropertyAccessors(factoryBeanAccessors);
					}
					return bean;
				}

				@Override
				public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
					return bean;
				}
			};
		}
	}

}
