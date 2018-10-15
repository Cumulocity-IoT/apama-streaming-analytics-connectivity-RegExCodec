/*
 * Copyright © 2017 Software AG, Darmstadt, Germany and/or its licensors
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.softwareag.samples;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;


import com.softwareag.connectivity.AbstractSimpleCodec;
import com.softwareag.connectivity.Message;
import com.softwareag.connectivity.PluginConstructorParameters.CodecConstructorParameters;
import com.softwareag.connectivity.util.MapExtractor;


/**
 * Performs a single regular expression replacement/substitution on strings in one or more  
 * payload and/or metadata keys. 
 * 
 * Any primitive non-string object referenced in any of the rules will be converted to a string implicitly.
 * 
 * Any key that is missing in a given message, or has an unsupported type will be ignored.
 * 
 * The regex and replacement string format are as described by Java's {@link java.util.regex.Matcher}, 
 * using the replaceAll method.  
 * 
 * The transformation of each message is O(n) where n is the number of replacement regular expressions configured. 
 * 
 */
public class RegexCodec extends AbstractSimpleCodec {

	enum Direction {
		towardsHost, towardsTransport, bidirectional
	}

	private final Direction direction;

	/** Empty means this applies to all event types */
	private final Set<String> hostMessageTypes;
	
	/** Never empty, for now keys must be explicitly listed */
	private final List<String> keys;

	/** Turn on for debugging */
	private final boolean warnOnMissingKey;
	
	// configuration specific to regular expressions
	private final Pattern pattern;
	private final String replacement;


	/**
	 * Constructor, passing parameters to AbstractSimpleCodec.
	 *
	 * The config and chainId are available as members on this
	 * class for the sub-class to refer to if needed.  This class
	 * does not require or use any configuration.
	 *
	 * @param logger Is a logger object which can be used to log to the host log file.
	 * @param params see product documentation
	 */
	public RegexCodec(org.slf4j.Logger logger, CodecConstructorParameters params) throws Exception {
		super(logger, params);
		
		MapExtractor config = new MapExtractor(params.getConfig(), "config");
		
		// Message type config
		hostMessageTypes = new HashSet<String>(config.getList("hostMessageTypes", String.class, true));
		
		// Config for which keys to process
		keys = config.getList("keys", String.class, false);
		
		// Message direction config
		direction = config.get("direction", Direction.bidirectional);

		// Config for debuging
		warnOnMissingKey = config.get("warnOnMissingKey", false);
		
		// Config for regular expression flags
		int flags = 0;
		for (String flag: config.getList("flags", String.class, true)) {
			flags = flags | Pattern.class.getField(flag).getInt(null);
		}
		
		// Config for regular expression and the replacement
		pattern = Pattern.compile(config.getStringDisallowEmpty("regex"), flags);
		replacement = config.getStringAllowEmpty("repl");
		
		// raise an error if any unexpected config options were specified
		config.checkNoItemsRemaining();
	}

	/**
	 * Applies a regular expression to a message from the transport.
	 *
	 * If this method throws then the exception will be logged and the message dropped.
	 *
	 * @param message The message, guaranteed to be non-null and have a non-null payload.  
	 * @return a map as a payload of a message with the regex applied.
	 */
	@Override
	public Message transformMessageTowardsHost(Message message) {
		if (direction != Direction.towardsTransport) {
			transformMessageBidirectional(message);
		}
		return message;
	}

	/**
	 * Applies a regular expression to a message from the host.
	 *
	 * If this method throws then the exception will be logged and the message dropped.
	 *
	 * @param message The message, guaranteed to be non-null and have a non-null payload.  
	 * @return a map as a payload of a message with the regex applied.
	 */
	@Override
	public Message transformMessageTowardsTransport(Message message) {
		if (direction != Direction.towardsHost) {
			transformMessageBidirectional(message);
		}
		return message;
	}
	
	/**
	 * Unlike some other codecs, this one can operate on the metadata of null-payload messages.
	 *
	 * If this method throws then the exception will be logged and the message dropped.
	 *
	 * @param message The message, may be null.
	 * @return a map as a payload of a message with the regex applied.
	 */
	@Override
	public Message transformNullPayloadTowardsHost(Message message) {
		return transformMessageTowardsHost(message);
	}
	

	/**
	 * Unlike some other codecs, this one can operate on the metadata of null-payload messages.
	 *
	 * If this method throws then the exception will be logged and the message dropped.
	 *
	 * @param message The message, may be null.  
	 * @return a map as a payload of a message with the regex applied.
	 */
	@Override
	public Message transformNullPayloadTowardsTransport(Message message) {
		return transformMessageTowardsTransport(message);
	}
	
	private void transformMessageBidirectional(Message message) {
		if (!hostMessageTypes.isEmpty() && !hostMessageTypes.contains(message.getMetadataMap().get(Message.HOST_MESSAGE_TYPE))) {
			if (logger.isDebugEnabled()) {
				logger.debug("Ignoring message whose event type does not match: '" +
					message.getMetadataMap().get(Message.HOST_MESSAGE_TYPE) + "'");
			}
			return;
		}

		// allows us to visit metadata and payload using the same algorithm
		Map<Object,Object> m = new HashMap<Object, Object>();
		m.put("payload", message.getPayload());
		m.put("metadata", message.getMetadataMap());
		
		for (String field: keys) {
			boolean found = visit(m, Arrays.asList(field.split("[.]")));
			if (!found && warnOnMissingKey) {
				logger.warn("Failed to find key '" + field + "' in message " + message);
			}
		}
		
		// in case the payload itself got changed when visited
		message.setPayload(m.get("payload"));
	}
	
	@SuppressWarnings("unchecked")
	private boolean visit(Map<Object,Object> m, List<String> field) {
		Object o = m.get(field.get(0));
		
		if (field.size() == 1) {
			if (o==null) return false;
			
			if (o instanceof String || o instanceof Number || o instanceof Boolean) {
				String input = o.toString();
				String output = applyRegex(input, pattern, replacement);
				Map<String, Object> map = new HashMap<String, Object>();
				map.put("input", input);
				map.put("output", output);
				m.put(field.get(0), map);
			}
			else {
				logger.debug("Ignoring non-primitive field value: " + field.get(0) + " of type " + o.getClass());
			}
			return true;
		}
		
		if (o instanceof Map) {
			visit((Map<Object,Object>)o, field.subList(1, field.size()));
		}
		else if (o instanceof List) {
			// limited support for lists for now - allow map items to be found inside list entries, but not primitives
			for (Object item: ((List<Object>)o))
				if (item instanceof Map)
					return visit((Map<Object,Object>)item, field.subList(1, field.size()));
		}
		else if (o != null) {
			logger.debug("Ignoring non-map value we cannot iterate into: " + field + " of type " + o.getClass());
		}
		return false;
	}
	
	private static String applyRegex(String input, Pattern pattern, String replacement) {
		try {
			return pattern.matcher(input).replaceAll(replacement);
		}
		catch (Exception e) {
			// make sure the end user gets enough context to understand what went wrong, since there could be many 
			// different instances of this codec with different expressions
			throw new IllegalArgumentException("Failed to apply regex '" + pattern + "' -> '" + replacement + "': " + e.getMessage(), e);
		}
	}
	
	/** Identifies the version of the API this plug-in was built against. */
	public static final String CONNECTIVITY_API_VERSION = com.softwareag.connectivity.ConnectivityPlugin.CONNECTIVITY_API_VERSION;

}
