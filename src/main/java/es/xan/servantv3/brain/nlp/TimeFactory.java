package es.xan.servantv3.brain.nlp;

import es.xan.servantv3.api.State;
import es.xan.servantv3.api.StateMachine;
import es.xan.servantv3.api.Transition;

import io.vertx.core.json.JsonObject;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeFactory {

    enum TimeDetector implements State<TimeBuilder> {
        _INIT() {
            public Transition[] getTrans() {
                return new Transition[]{new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "a"), (i, s) -> TimeDetector.valueOf("_DOS"))};
            }
        },
        _DOS() {
            public Transition[] getTrans() {
                return new Transition[]{new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "las") || matches(x, "la"), (i, s) -> TimeDetector.HOUR)};
            }
        },
        HOUR() {
            public Transition[] getTrans() {
                return new Transition[]{
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "una"), (i, s) -> {
                            i.addHour(1);
                            return TimeDetector.SEPARATOR;
                        }),
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "dos"), (i, s) -> {
                            i.addHour(2);
                            return TimeDetector.SEPARATOR;
                        }),
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "tres"), (i, s) -> {
                            i.addHour(3);
                            return TimeDetector.SEPARATOR;
                        }),
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "cuatro"), (i, s) -> {
                            i.addHour(4);
                            return TimeDetector.SEPARATOR;
                        }),
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "cinco"), (i, s) -> {
                            i.addHour(5);
                            return TimeDetector.SEPARATOR;
                        }),
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "seis"), (i, s) -> {
                            i.addHour(6);
                            return TimeDetector.SEPARATOR;
                        }),
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "siete"), (i, s) -> {
                            i.addHour(7);
                            return TimeDetector.SEPARATOR;
                        }),
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "ocho"), (i, s) -> {
                            i.addHour(8);
                            return TimeDetector.SEPARATOR;
                        }),
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "nueve"), (i, s) -> {
                            i.addHour(9);
                            return TimeDetector.SEPARATOR;
                        }),
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "diez"), (i, s) -> {
                            i.addHour(10);
                            return TimeDetector.SEPARATOR;
                        }),
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "once"), (i, s) -> {
                            i.addHour(11);
                            return TimeDetector.SEPARATOR;
                        }),
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "doce"), (i, s) -> {
                            i.addHour(12);
                            return TimeDetector.SEPARATOR;
                        }),
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "trece"), (i, s) -> {
                            i.addHour(13);
                            return TimeDetector.SEPARATOR;
                        }),
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "catorce"), (i, s) -> {
                            i.addHour(14);
                            return TimeDetector.SEPARATOR;
                        }),
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "quince"), (i, s) -> {
                            i.addHour(15);
                            return TimeDetector.SEPARATOR;
                        }),
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "dieciseis"), (i, s) -> {
                            i.addHour(16);
                            return TimeDetector.SEPARATOR;
                        }),
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "diecisiete"), (i, s) -> {
                            i.addHour(17);
                            return TimeDetector.SEPARATOR;
                        }),
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "dieciocho"), (i, s) -> {
                            i.addHour(18);
                            return TimeDetector.SEPARATOR;
                        }),
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "diecinueve"), (i, s) -> {
                            i.addHour(19);
                            return TimeDetector.SEPARATOR;
                        }),
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "veinte"), (i, s) -> {
                            i.addHour(20);
                            return TimeDetector.SEPARATOR;
                        }),
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "veintiuno"), (i, s) -> {
                            i.addHour(21);
                            return TimeDetector.SEPARATOR;
                        }),
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "veintiuna"), (i, s) -> {
                            i.addHour(21);
                            return TimeDetector.SEPARATOR;
                        }),
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "veintidos"), (i, s) -> {
                            i.addHour(22);
                            return TimeDetector.SEPARATOR;
                        }),
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "veintitres"), (i, s) -> {
                            i.addHour(23);
                            return TimeDetector.SEPARATOR;
                        }),
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "veinticuatro"), (i, s) -> {
                            i.addHour(24);
                            return TimeDetector.SEPARATOR;
                        })
                };
            }
        },
        SEPARATOR() {
            public Transition[] getTrans() {
                return new Transition[]{
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "y"), (i, s) -> {
                            i.positive();
                            return TimeDetector.MINUTES;
                        }),
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "menos"), (i, s) -> {
                            i.less();
                            return TimeDetector.MINUTES;
                        })
                };
            }
        },
        MINUTES() {
            public Transition[] getTrans() {
                return new Transition[]{
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "uno"), (i, s) -> {
                            i.addMinutes(1);
                            return TimeDetector.FINALIZE;
                        }),
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "una"), (i, s) -> {
                            i.addMinutes(1);
                            return TimeDetector.FINALIZE;
                        }),
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "dos"), (i, s) -> {
                            i.addMinutes(2);
                            return TimeDetector.FINALIZE;
                        }),
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "tres"), (i, s) -> {
                            i.addMinutes(3);
                            return TimeDetector.FINALIZE;
                        }),
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "cuatro"), (i, s) -> {
                            i.addMinutes(4);
                            return TimeDetector.FINALIZE;
                        }),
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "cinco"), (i, s) -> {
                            i.addMinutes(5);
                            return TimeDetector.FINALIZE;
                        }),
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "seis"), (i, s) -> {
                            i.addMinutes(6);
                            return TimeDetector.FINALIZE;
                        }),
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "siete"), (i, s) -> {
                            i.addMinutes(7);
                            return TimeDetector.FINALIZE;
                        }),
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "ocho"), (i, s) -> {
                            i.addMinutes(8);
                            return TimeDetector.FINALIZE;
                        }),
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "nueve"), (i, s) -> {
                            i.addMinutes(9);
                            return TimeDetector.FINALIZE;
                        }),
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "diez"), (i, s) -> {
                            i.addMinutes(10);
                            return TimeDetector.FINALIZE;
                        }),
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "once"), (i, s) -> {
                            i.addMinutes(11);
                            return TimeDetector.FINALIZE;
                        }),
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "doce"), (i, s) -> {
                            i.addMinutes(12);
                            return TimeDetector.FINALIZE;
                        }),
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "trece"), (i, s) -> {
                            i.addMinutes(13);
                            return TimeDetector.FINALIZE;
                        }),
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "catorce"), (i, s) -> {
                            i.addMinutes(14);
                            return TimeDetector.FINALIZE;
                        }),
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "quince"), (i, s) -> {
                            i.addMinutes(15);
                            return TimeDetector.FINALIZE;
                        }),
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "cuarto"), (i, s) -> {
                            i.addMinutes(15);
                            return TimeDetector.FINALIZE;
                        }),
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "dieciseis"), (i, s) -> {
                            i.addMinutes(16);
                            return TimeDetector.FINALIZE;
                        }),
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "diecisiete"), (i, s) -> {
                            i.addMinutes(17);
                            return TimeDetector.FINALIZE;
                        }),
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "dieciocho"), (i, s) -> {
                            i.addMinutes(18);
                            return TimeDetector.FINALIZE;
                        }),
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "diecinueve"), (i, s) -> {
                            i.addMinutes(19);
                            return TimeDetector.FINALIZE;
                        }),
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "veinte"), (i, s) -> {
                            i.addMinutes(20);
                            return TimeDetector.FINALIZE;
                        }),
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "veintiuno"), (i, s) -> {
                            i.addMinutes(21);
                            return TimeDetector.FINALIZE;
                        }),
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "veintiuna"), (i, s) -> {
                            i.addMinutes(21);
                            return TimeDetector.FINALIZE;
                        }),
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "veintidos"), (i, s) -> {
                            i.addMinutes(22);
                            return TimeDetector.FINALIZE;
                        }),
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "veintitres"), (i, s) -> {
                            i.addMinutes(23);
                            return TimeDetector.FINALIZE;
                        }),
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "veinticuatro"), (i, s) -> {
                            i.addMinutes(24);
                            return TimeDetector.FINALIZE;
                        }),
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "veinticinco"), (i, s) -> {
                            i.addMinutes(25);
                            return TimeDetector.FINALIZE;
                        }),
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "veintiseis"), (i, s) -> {
                            i.addMinutes(26);
                            return TimeDetector.FINALIZE;
                        }),
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "veintisiete"), (i, s) -> {
                            i.addMinutes(27);
                            return TimeDetector.FINALIZE;
                        }),
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "veintiocho"), (i, s) -> {
                            i.addMinutes(28);
                            return TimeDetector.FINALIZE;
                        }),
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "veintinueve"), (i, s) -> {
                            i.addMinutes(29);
                            return TimeDetector.FINALIZE;
                        }),
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "treinta"), (i, s) -> {
                            i.addMinutes(30);
                            return TimeDetector.FINALIZE_OR_SEPARATOR;
                        }),
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "media"), (i, s) -> {
                            i.addMinutes(30);
                            return TimeDetector.FINALIZE;
                        }),
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "cuarenta"), (i, s) -> {
                            i.addMinutes(40);
                            return TimeDetector.FINALIZE_OR_SEPARATOR;
                        }),
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "cincuenta"), (i, s) -> {
                            i.addMinutes(50);
                            return TimeDetector.FINALIZE_OR_SEPARATOR;
                        })
                };
            }
        },
        FINALIZE(),
        FINALIZE_OR_SEPARATOR() {
            public Transition[] getTrans() {
                return new Transition[]{
                        new Transition<TimeBuilder, TimeDetector>(x -> matches(x, "y"), (i, s) -> TimeDetector.MINUTES),
                        new Transition<TimeBuilder, TimeDetector>(x -> true, (i, s) -> TimeDetector.FINALIZE)
                };
            }
        };

        private final Transition<TimeBuilder, TimeDetector>[] mTransitions;

        TimeDetector(Transition<TimeBuilder, TimeDetector>...items) {
            this.mTransitions = items;
        }

        public Transition[] getTrans(){
            return this.mTransitions;
        }

    }

    private static final class TimeBuilder {
        private int mHour = -1;
        private int mMinutes = -1;

        private boolean positive = true;

        private static TimeBuilder newInstance() {
            return new TimeBuilder();
        }

        public void less() {
            this.positive = false;
        }

        public void positive() {
            this.positive = true;
        }

        public void addHour(int i) {
            this.mHour = i;
        }

        public void addMinutes(int i) {
            this.mMinutes += i;
        }

        public long computeTime() {
            if (this.mHour < 0 ||this.mMinutes < 0)
                return 0;

            int hour = this.mHour;
            int min = this.mMinutes;
            if (!this.positive) {
                hour -= 1;
                if (hour < 0)
                    hour = 23;

                min = 60 - min;
            }

            final LocalDateTime localNow = LocalDateTime.now();
            final ZoneId currentZone = ZoneId.of("Europe/Madrid");
            final ZonedDateTime zonedNow = ZonedDateTime.of(localNow, currentZone);
            ZonedDateTime zonedNextScheduled = zonedNow.withHour(hour).withMinute(min).withSecond(0);
            if(zonedNow.compareTo(zonedNextScheduled) > 0)
                zonedNextScheduled = zonedNextScheduled.plusDays(1);

            final Duration duration = Duration.between(zonedNow, zonedNextScheduled);
            return duration.getSeconds();
        }
    }

    private static boolean matches(JsonObject entry, String expected) {
        String actual = entry.getString("entry");
        return actual.equals(expected);
    }

    public static long findTimeAndTransform(String input) {
        if (input.toLowerCase().contains(" at ")) {
            return addtimingInfo(input);
//            if (input.toLowerCase().contains("every day")) {
//                addEveryDayInfo(translation);
//            }
        } else if (input.toLowerCase().contains(" in ")) {
            return addtimingInfoIn(input);
        }

        TimeBuilder instance = TimeBuilder.newInstance();
        StateMachine<TimeBuilder> stateMachine = new StateMachine<>(TimeDetector._INIT, instance);

        Scanner scanner = new Scanner(input);
        while (scanner.hasNext()) {
            Map<String, Object> value = new HashMap<>();
            value.put("entry" , scanner.next());
            State current = stateMachine.process(new JsonObject(value));
            if (current == TimeDetector.FINALIZE) break;
        }

        return instance.computeTime();
    }

    protected static long addtimingInfoIn(String message) {
        final int indexOf = message.indexOf(" in ");

        final Pattern compile = Pattern.compile("(\\d+):(\\d+)");
        final Matcher matcher = compile.matcher(message);

        if (matcher.find(indexOf)) {
            final String str_min = matcher.group(1);
            final String str_sec = matcher.group(2);

            final int min = Integer.parseInt(str_min);
            final int sec = Integer.parseInt(str_sec);

            return min * 60 + sec;
        }

        return 0;
    }

    protected static long addtimingInfo(String message) {
        final int indexOf = message.indexOf(" at ");

        final Pattern compile = Pattern.compile("(\\d+):(\\d+)");
        final Matcher matcher = compile.matcher(message);

        if (matcher.find(indexOf)) {
            final String str_hour = matcher.group(1);
            final String str_min = matcher.group(2);

            final int hour = Integer.parseInt(str_hour);
            final int min = Integer.parseInt(str_min);

            final LocalDateTime localNow = LocalDateTime.now();
            final ZoneId currentZone = ZoneId.of("Europe/Madrid");
            final ZonedDateTime zonedNow = ZonedDateTime.of(localNow, currentZone);
            ZonedDateTime zonedNextScheduled = zonedNow.withHour(hour).withMinute(min).withSecond(0);
            if(zonedNow.compareTo(zonedNextScheduled) > 0)
                zonedNextScheduled = zonedNextScheduled.plusDays(1);

            final Duration duration = Duration.between(zonedNow, zonedNextScheduled);
            return duration.getSeconds();
        }

        return 0;
    }
}
