package es.xan.servantv3.brain.nlp;

import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class RuleUtils {
	
	private static final String I18NS_SEPARATOR = "\\|\\|";
	
	public static Predicate<String> messageIs(String...options) {
		return (String text) -> {
			return Arrays.stream(options).
					flatMap(Pattern.compile(I18NS_SEPARATOR)::splitAsStream).
					parallel().
					anyMatch(it -> text.toLowerCase().equals(it));
		};
	}
	
	public static Predicate<String> messageContains(String...options) {
		return (String text) -> {
			return Arrays.stream(options).
					flatMap(Pattern.compile(I18NS_SEPARATOR)::splitAsStream).
					parallel().
					anyMatch(it -> text.toLowerCase().contains(it));
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
