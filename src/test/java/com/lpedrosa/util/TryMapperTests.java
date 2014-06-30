package com.lpedrosa.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

public class TryMapperTests {

    @Test
    public void shouldPerformComputationForSuccessCase() throws Throwable {
        // given
        Try<Integer> sumResult = Try.of(() -> Integer.parseInt("2"))
                                    .map(number -> number + 2);

        // when
        int result = sumResult.get();

        // then
        assertEquals(4, result);
    }

    @Test(expected = NumberFormatException.class)
    public void shouldShortCircuitWhenExceptionIsThrown() throws Throwable {
        // given
        Try<Integer> sumResult = Try.of(() -> Integer.parseInt("a"))
                                    .map(number -> number + 2);
        // when
        sumResult.get();

        // then
        fail("Should've thrown an exception");
    }

    @Test
    public void shouldReturnDefaultWhenExceptionIsThrown() {
        // given
        Try<String> upperCaseString = Try.of(() -> somethingThatMightFail())
                                         .map(String::toUpperCase);

        // when
        String resolved = upperCaseString.orElse("Something");

        // then
        assertEquals("Something", resolved);
    }

    @Test
    public void shouldKeepWrappingComputationsWhenProvidingDefaults() {
        // given
        Try<String> computation = Try.of(() -> somethingThatMightFail());

        // when
        String result = computation.orElseGet(this::somethingThatMightFail)
                                   .orElse("result");

        // then
        assertEquals("result", result);
    }

    @Test(expected=Exception.class)
    public void shouldShortCircuitWhenFlatMapFails() throws Throwable {
        // given
        Try<String> computation = Try.of(() -> Integer.toString(2))
                                     .flatMap(this::somethingThatAlreadyTriesAndFails)
                                     .map(this::somethingThatFails);
        // when
        computation.get();

        // then
        fail("Should've thrown an exception");
    }

    @Test
    public void shouldShortCircuitWhenSecondMapFails() throws Throwable {
        Integer computationResult = Try.success("a")
                                       .map(Integer::parseInt)
                                       .recoverWith((t) -> {
                                           System.out.println(t);
                                           return Try.failure(t);
                                       })
                                       .orElse(4);

        assertEquals(4, computationResult.intValue());
    }

    @Test
    public void recoverTests() throws Throwable {
        Try<Integer> integer = Try.of(() -> Integer.parseInt("a"))
                                  .recover((t) -> {
                                      if (NumberFormatException.class.isAssignableFrom(t.getClass())) {
                                          return 0;
                                      }
                                      return -1;
                                  });

        assertEquals(0, integer.get().intValue());
    }

    private String somethingThatMightFail() throws Exception {
        throw new Exception("I failed");
    }

    private String somethingThatFails(String s) {
        throw new RuntimeException("I failed");
    }

    private Try<String> somethingThatAlreadyTriesAndFails(String s) {
        Try<String> failure = Try.failure(new Exception("I failed"));
        return failure;
    }
}
