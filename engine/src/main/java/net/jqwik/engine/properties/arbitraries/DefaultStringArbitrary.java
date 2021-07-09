package net.jqwik.engine.properties.arbitraries;

import java.util.*;

import net.jqwik.api.*;
import net.jqwik.api.arbitraries.*;
import net.jqwik.engine.properties.arbitraries.exhaustive.*;
import net.jqwik.engine.properties.arbitraries.randomized.*;
import net.jqwik.engine.properties.shrinking.*;

import static java.util.Arrays.*;

public class DefaultStringArbitrary extends TypedCloneable implements StringArbitrary {

	private CharacterArbitrary characterArbitrary = new DefaultCharacterArbitrary();

	private int minLength = 0;
	private int maxLength = RandomGenerators.DEFAULT_COLLECTION_SIZE;
	private Set<Character> excludedChars = new HashSet<>();
	private RandomDistribution lengthDistribution = null;

	@Override
	public RandomGenerator<String> generator(int genSize) {
		return RandomGenerators.strings(randomCharacterGenerator(), minLength, maxLength, genSize, lengthDistribution);
	}

	@Override
	public Optional<ExhaustiveGenerator<String>> exhaustive(long maxNumberOfSamples) {
		return ExhaustiveGenerators.strings(
			effectiveCharacterArbitrary(),
			minLength,
			maxLength,
			maxNumberOfSamples
		);
	}

	@Override
	public EdgeCases<String> edgeCases(int maxEdgeCases) {
		// Optimization. Already handled by EdgeCases.concat(..)
		if (maxEdgeCases <= 0) {
			return EdgeCases.none();
		}

		EdgeCases<String> emptyStringEdgeCases =
			hasEmptyStringEdgeCase() ? emptyStringEdgeCase() : EdgeCases.none();

		int effectiveMaxEdgeCases = maxEdgeCases - emptyStringEdgeCases.size();
		EdgeCases<String> singleCharEdgeCases =
			hasSingleCharEdgeCases() ? fixedSizedEdgeCases(1, effectiveMaxEdgeCases) : EdgeCases.none();

		effectiveMaxEdgeCases = effectiveMaxEdgeCases - singleCharEdgeCases.size();
		EdgeCases<String> fixedSizeEdgeCases =
			hasMultiCharEdgeCases() ? fixedSizedEdgeCases(minLength, effectiveMaxEdgeCases) : EdgeCases.none();

		return EdgeCasesSupport.concat(asList(singleCharEdgeCases, emptyStringEdgeCases, fixedSizeEdgeCases), maxEdgeCases);
	}

	private boolean hasEmptyStringEdgeCase() {
		return minLength <= 0;
	}

	private boolean hasMultiCharEdgeCases() {
		return minLength <= maxLength && minLength > 1;
	}

	private boolean hasSingleCharEdgeCases() {
		return minLength <= 1 && maxLength >= 1;
	}

	private EdgeCases<String> emptyStringEdgeCase() {
		return EdgeCases.fromSupplier(() -> new ShrinkableString(Collections.emptyList(), minLength, maxLength));
	}

	private EdgeCases<String> fixedSizedEdgeCases(int fixedSize, int maxEdgeCases) {
		return EdgeCasesSupport.mapShrinkable(
			effectiveCharacterArbitrary().edgeCases(maxEdgeCases),
			shrinkableChar -> {
				List<Shrinkable<Character>> shrinkableChars = new ArrayList<>(Collections.nCopies(fixedSize, shrinkableChar));
				return new ShrinkableString(shrinkableChars, minLength, maxLength);
			}
		);
	}

	@Override
	public StringArbitrary ofMinLength(int minLength) {
		DefaultStringArbitrary clone = typedClone();
		clone.minLength = minLength;
		return clone;
	}

	@Override
	public StringArbitrary ofMaxLength(int maxLength) {
		DefaultStringArbitrary clone = typedClone();
		clone.maxLength = maxLength;
		return clone;
	}

	@Override
	public StringArbitrary withLengthDistribution(RandomDistribution distribution) {
		DefaultStringArbitrary clone = typedClone();
		clone.lengthDistribution = distribution;
		return clone;
	}

	@Override
	public StringArbitrary withChars(char... chars) {
		DefaultStringArbitrary clone = typedClone();
		clone.characterArbitrary = clone.characterArbitrary.with(chars);
		return clone;
	}

	@Override
	public StringArbitrary withChars(CharSequence chars) {
		DefaultStringArbitrary clone = typedClone();
		clone.characterArbitrary = clone.characterArbitrary.with(chars);
		return clone;
	}

	@Override
	public StringArbitrary withCharRange(char from, char to) {
		DefaultStringArbitrary clone = typedClone();
		clone.characterArbitrary = clone.characterArbitrary.range(from, to);
		return clone;
	}

	@Override
	public StringArbitrary ascii() {
		DefaultStringArbitrary clone = typedClone();
		clone.characterArbitrary = characterArbitrary.ascii();
		return clone;
	}

	@Override
	public StringArbitrary alpha() {
		DefaultStringArbitrary clone = typedClone();
		clone.characterArbitrary = clone.characterArbitrary
									   .range('A', 'Z')
									   .range('a', 'z');
		return clone;
	}

	@Override
	public StringArbitrary numeric() {
		DefaultStringArbitrary clone = typedClone();
		clone.characterArbitrary = clone.characterArbitrary.range('0', '9');
		return clone;
	}

	@Override
	public StringArbitrary whitespace() {
		DefaultStringArbitrary clone = typedClone();
		clone.characterArbitrary = clone.characterArbitrary.whitespace();
		return clone;
	}

	@Override
	public StringArbitrary all() {
		return this.withCharRange(Character.MIN_VALUE, Character.MAX_VALUE);
	}

	@Override
	public StringArbitrary excludeChars(char... charsToExclude) {
		DefaultStringArbitrary clone = typedClone();
		Set<Character> excludedChars = new HashSet<>(this.excludedChars);
		for (char c : charsToExclude) {
			excludedChars.add(c);
		}
		clone.excludedChars = excludedChars;
		return clone;
	}

	private RandomGenerator<Character> randomCharacterGenerator() {
		return effectiveCharacterArbitrary()
				   .generator(1, false)
				   .injectDuplicates(0.01);
	}

	private Arbitrary<Character> effectiveCharacterArbitrary() {
		Arbitrary<Character> characterArbitrary = this.characterArbitrary;
		if (!excludedChars.isEmpty()) {
			characterArbitrary = characterArbitrary.filter(c -> !excludedChars.contains(c));
		}
		return characterArbitrary;
	}

}
