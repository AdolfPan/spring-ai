/*
 * Copyright 2023 the original author or authors.
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

package org.springframework.ai.core.prompt;

import org.antlr.runtime.Token;
import org.antlr.runtime.TokenStream;
import org.springframework.ai.core.prompt.messages.Message;
import org.springframework.ai.core.prompt.messages.UserMessage;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.compiler.STLexer;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PromptTemplate implements PromptOperations {

	private ST st;

	private Map<String, Object> dynamicModel = new HashMap<>();

	protected String template;

	protected TemplateFormat templateFormat = TemplateFormat.ST;

	private OutputParser outputParser;

	public PromptTemplate(String template) {
		this.template = template;
		// If the template string is not valid, an exception will be thrown
		try {
			this.st = new ST(this.template, '{', '}');
		}
		catch (Exception ex) {
			throw new IllegalArgumentException("The template string is not valid.", ex);
		}
	}

	public PromptTemplate(String template, Map<String, Object> model) {
		this.template = template;
		// If the template string is not valid, an exception will be thrown
		try {
			this.st = new ST(this.template, '{', '}');
			for (Entry<String, Object> entry : model.entrySet()) {
				add(entry.getKey(), entry.getValue());
			}
		}
		catch (Exception ex) {
			throw new IllegalArgumentException("The template string is not valid.", ex);
		}
	}

	public OutputParser getOutputParser() {
		return outputParser;
	}

	public void setOutputParser(OutputParser outputParser) {
		Objects.requireNonNull(outputParser, "Output Parser can not be null");
		this.outputParser = outputParser;
	}

	public void add(String name, Object value) {
		this.st.add(name, value);
		this.dynamicModel.put(name, value);
	}

	public String getTemplate() {
		return this.template;
	}

	public TemplateFormat getTemplateFormat() {
		return this.templateFormat;
	}

	// Render Methods
	public String render() {
		return st.render();
	}

	@Override
	public String render(Map<String, Object> model) {
		validate(model);
		for (Entry<String, Object> stringObjectEntry : model.entrySet()) {
			if (st.getAttribute(stringObjectEntry.getKey()) == null) {
				st.add(stringObjectEntry.getKey(), stringObjectEntry.getValue());
			}
		}
		return st.render().trim();
	}

	@Override
	public List<Message> createMessages() {
		return List.of(new UserMessage(render()));
	}

	@Override
	public List<Message> createMessages(Map<String, Object> model) {
		return List.of(new UserMessage(render(model)));
	}

	@Override
	public Prompt create() {
		return new Prompt(render(new HashMap<>()));
	}

	@Override
	public Prompt create(Map<String, Object> model) {
		return new Prompt(render(model));
	}

	protected Set<String> getInputVariables() {
		TokenStream tokens = this.st.impl.tokens;
		return IntStream.range(0, tokens.range())
			.mapToObj(tokens::get)
			.filter(token -> token.getType() == STLexer.ID)
			.map(Token::getText)
			.collect(Collectors.toSet());
	}

	protected void validate(Map<String, Object> model) {
		Set<String> dynamicVariableNames = new HashSet<>(this.dynamicModel.keySet());
		Set<String> modelVariables = new HashSet<>(model.keySet());
		modelVariables.addAll(dynamicVariableNames);
		Set<String> missingEntries = new HashSet<>(getInputVariables());
		missingEntries.removeAll(modelVariables);
		if (!missingEntries.isEmpty()) {
			throw new IllegalStateException(
					"All template variables were not replaced. Missing variable names are " + missingEntries);
		}
	}

}
