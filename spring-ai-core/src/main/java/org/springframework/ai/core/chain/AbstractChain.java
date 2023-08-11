package org.springframework.ai.core.chain;

import org.springframework.ai.core.memory.Memory;

import java.util.*;

public abstract class AbstractChain implements Chain {

	private Optional<Memory> memory = Optional.empty();

	protected Optional<Memory> getMemory() {
		return this.memory;
	}

	@Override
	public abstract List<String> getInputKeys();

	@Override
	public abstract List<String> getOutputKeys();

	// TODO validation of input/outputs

	@Override
	public Map<String, Object> apply(Map<String, Object> inputMap) {
		Map<String, Object> inputMapToUse = processBeforeApply(inputMap);
		Map<String, Object> outputMap = doApply(inputMapToUse);
		Map<String, Object> outputMapToUse = processAfterApply(inputMapToUse, outputMap);
		return outputMapToUse;
	}

	protected Map<String, Object> processBeforeApply(Map<String, Object> inputMap) {
		validateInputs(inputMap);
		return inputMap;
	}

	protected abstract Map<String, Object> doApply(Map<String, Object> inputMap);

	private Map<String, Object> processAfterApply(Map<String, Object> inputMap, Map<String, Object> outputMap) {
		validateOutputs(outputMap);
		Map<String, Object> combindedMap = new HashMap<>();
		combindedMap.putAll(inputMap);
		combindedMap.putAll(outputMap);
		return combindedMap;
	}

	protected void validateOutputs(Map<String, Object> outputMap) {
		Set<String> missingKeys = new HashSet<>(getOutputKeys());
		missingKeys.removeAll(outputMap.keySet());
		if (!missingKeys.isEmpty()) {
			throw new IllegalArgumentException("Missing some output keys: " + missingKeys);
		}
	}

	protected void validateInputs(Map<String, Object> inputMap) {
		Set<String> missingKeys = new HashSet<>(getInputKeys());
		missingKeys.removeAll(inputMap.keySet());
		if (!missingKeys.isEmpty()) {
			throw new IllegalArgumentException("Missing some input keys: " + missingKeys);
		}
	}

}
