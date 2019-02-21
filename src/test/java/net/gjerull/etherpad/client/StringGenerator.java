package net.gjerull.etherpad.client;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

public class StringGenerator extends Generator<String> {
    private static final String[] TEMPLATES = {"Here is my %!", "Your % is", "Try with this %...", "Keep this %:", "I've found the %,"};
    private static final String[] TYPES = {"BTC address", "code", "credential", "hash", "key", "password", "token"};
    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghiklmnnopqrstuvwxyz0123456789,.:;!?()[]{}<>+-/*%=@#$&|^_'`~";
    private static final int SIZE = 30;

    public StringGenerator() {
        super(String.class);
    }

    @Override
    public String generate(SourceOfRandomness random, GenerationStatus status) {
        String template = TEMPLATES[random.nextInt(TEMPLATES.length)].replace("%", TYPES[random.nextInt(TYPES.length)]) + " ";
        StringBuilder string = new StringBuilder(template.length() + SIZE);
        string.append(template);
        for (int i = 0; i < SIZE; i++) {
            string.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return string.toString();
    }
}