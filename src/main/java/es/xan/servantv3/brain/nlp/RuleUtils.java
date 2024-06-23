package es.xan.servantv3.brain.nlp;

import es.xan.servantv3.brain.UserContext;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class RuleUtils {
	
	private static final String I18NS_SEPARATOR = "\\|\\|";

	public static Predicate<Pair<String, UserContext>> isContextFree() {
		return (Pair<String, UserContext> pair) -> {
			return "".equals(pair.getRight().getAttention());
		};
	}

	public static Predicate<Pair<String, UserContext>> isContext(String context) {
		return (Pair<String, UserContext> pair) -> {
			return context.equals(pair.getRight().getAttention());
		};
	}
	
	public static Predicate<Pair<String, UserContext>> messageIs(String...options) {
		return (Pair<String, UserContext> pair) -> {
			return Arrays.stream(options).
					flatMap(Pattern.compile(I18NS_SEPARATOR)::splitAsStream).
					parallel().
					anyMatch(it -> pair.getLeft().toLowerCase().equals(it));
		};
	}

	public static Predicate<Pair<String, UserContext>> messageStartsWith(String expected) {
		return (Pair<String, UserContext> text) -> text.getLeft().toLowerCase().startsWith(expected);
	}

	public static String findNumber(String[] tokens) {
		return Arrays.stream(tokens).filter(NumberUtils::isCreatable).findFirst().orElse("");
	}

	public static String concatStrings(String[] tokens) {
		StringJoiner joiner = new StringJoiner(" ");
		for(int i = 0; i < tokens.length; i++) {
			joiner.add(tokens[i]);
		}

		return joiner.toString();
	}
	
	public static Predicate<Pair<String, UserContext>> messageContains(String...options) {
		return (Pair<String, UserContext> text) -> {
			return Arrays.stream(options).
					flatMap(Pattern.compile(I18NS_SEPARATOR)::splitAsStream).
					parallel().
					anyMatch(it -> text.getLeft().toLowerCase().contains(it));
		};
	}

	public static Function<String[],String> send(String input) {
		return (String[] tokens) -> {
			return input;
		};
	}
	
	public static Function<String[],String> nextTokenTo(String input) {
		return (String[] tokens) -> {
			for (int i=0; i < tokens.length - 1; i++) {
				if (input.equals(tokens[i].toLowerCase())) {
					return tokens[i + 1];
				}
			}
			
			return "";
		};
	}
	
	public static Predicate<String[]> contains(String input) {
		return (String[] tokens) -> {
			for (int i=0; i < tokens.length; i++) {
				if (input.equals(tokens[i].toLowerCase())) {
					return true;
				}
			}
			
			return false;
		};
		
	}
}
