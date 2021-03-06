package de.ad.tools.redmine.cli.command;

import de.ad.tools.redmine.cli.Configuration;
import de.ad.tools.redmine.cli.util.PrintUtil;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public class Command {
  static final String TOO_FEW_ARGUMENTS_MESSAGE =
      "Command '%s' requires %d argument(s). Found %d.";
  static final String TOO_MANY_ARGUMENTS_MESSAGE =
      "Command '%s' accepts up to %d argument(s). Found %d.";
  static final String INVALID_OPTION_MESSAGE =
      "'%s' is not a valid option.";

  private final String name;
  private final String description;
  private final String longDescription;
  private final Argument[] arguments;
  private final Option[] options;

  protected final Configuration configuration;
  private final PrintStream out;

  @Deprecated
  protected Command(String name, String description, String longDescription,
      Argument[] arguments, Configuration configuration, PrintStream out) {
    this.name = name;
    this.description = description;
    this.longDescription = longDescription;
    this.arguments = arguments;
    this.options = new Option[0];
    this.configuration = configuration;
    this.out = out;
  }

  protected Command(String name, String description, String longDescription,
      Argument[] arguments,
      Option[] options, Configuration configuration, PrintStream out) {
    this.name = name;
    this.description = description;
    this.longDescription = longDescription;
    this.arguments = arguments;
    this.options = options;
    this.configuration = configuration;
    this.out = out;
  }

  public void process(String[] arguments) throws Exception {
    List<String> suppliedArguments = filterArguments(arguments);
    List<String> suppliedOptions = filterOptions(arguments);

    validateArguments(suppliedArguments);
    validateOptions(suppliedOptions);

    assignArguments(suppliedArguments);
    assignOptions(suppliedOptions);
  }

  public final String getName() {
    return name;
  }

  public final String getDescription() {
    return description;
  }

  public String getLongDescription() {
    return longDescription;
  }

  public final Argument[] getArguments() {
    return arguments;
  }

  public Option[] getOptions() {
    return options;
  }

  protected final void println(String s, Object... args) {
    out.println(String.format(s, args));
  }

  protected final void println() {
    out.println();
  }

  protected final void printTable(String[][] table) {
    PrintUtil.printTable(out, table);
  }

  protected final void printTable(String[] header, String[][] table) {
    PrintUtil.printTable(out, header, table);
  }

  protected final void printHeading(String heading) {
    PrintUtil.printHeading(out, heading);
  }

  private List<String> filterArguments(String[] arguments) {
    return Arrays.stream(arguments)
        .filter(a -> !a.startsWith("--"))
        .collect(toList());
  }

  private List<String> filterOptions(String[] arguments) {
    return Arrays.stream(arguments)
        .filter(o -> o.startsWith("--"))
        .collect(toList());
  }

  private void validateArguments(List<String> suppliedArguments)
      throws Exception {
    int requiredArgumentsCount = getRequiredArgumentsCount();
    int argumentCount = getArguments().length;

    if (requiredArgumentsCount > suppliedArguments.size()) {
      throw new IllegalArgumentException(
          String.format(
              TOO_FEW_ARGUMENTS_MESSAGE,
              getName(),
              requiredArgumentsCount, suppliedArguments.size()));
    } else if (argumentCount < suppliedArguments.size()) {
      throw new IllegalArgumentException(
          String.format(
              TOO_MANY_ARGUMENTS_MESSAGE,
              getName(),
              argumentCount, suppliedArguments.size()));
    }
  }

  private void validateOptions(List<String> suppliedOptions) throws Exception {
    List<String> availableOptions =
        Arrays.stream(options).map(Option::getName).collect(toList());

    for (String suppliedOption : suppliedOptions)
      if (!Option.isOption(suppliedOption) || !availableOptions.contains(
          Option.getName(suppliedOption))) {
        throw new IllegalArgumentException(
            String.format(INVALID_OPTION_MESSAGE, suppliedOption));
      }
  }

  private void assignArguments(List<String> arguments) {
    Argument[] commandArguments = getArguments();
    for (int i = 0; i < arguments.size(); i++) {
      commandArguments[i].setValueOrThrow(arguments.get(i));
    }
  }

  private void assignOptions(List<String> suppliedOptions) {
    Map<String, Option> options =
        Arrays.stream(getOptions()).collect(toMap(Option::getName, o -> o));

    suppliedOptions.forEach(
        o -> options.get(Option.getName(o)).setValue(Option.getValue(o)));
  }

  private int getRequiredArgumentsCount() {
    return (int) Arrays.stream(arguments).filter(a -> !a.isOptional()).count();
  }

  public static abstract class Argument<T> {
    private final String name;
    private final String description;
    private final boolean isOptional;
    private T value;

    public Argument(String name, String description, boolean isOptional) {
      this.name = name;
      this.description = description;
      this.isOptional = isOptional;
    }

    public String getName() {
      return name;
    }

    public String getDescription() {
      return description;
    }

    public boolean isOptional() {
      return isOptional;
    }

    public T getValue() {
      return value;
    }

    protected void setValue(T value) {
      this.value = value;
    }

    public abstract void setValueOrThrow(String value);
  }

  public static final class TextArgument extends Argument<String> {
    public TextArgument(String name, String description, boolean isOptional) {
      super(name, description, isOptional);
    }

    @Override public void setValueOrThrow(String value) {
      setValue(value);
    }
  }

  public static final class NumberArgument extends Argument<Integer> {
    static final String INVALID_TYPE_MESSAGE =
        "Supplied argument '%s' is not of type number.";

    public NumberArgument(String name, String description, boolean isOptional) {
      super(name, description, isOptional);
    }

    @Override public void setValueOrThrow(String value) {
      try {
        Integer integerValue = Integer.valueOf(value);

        setValue(integerValue);
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException(
            String.format(INVALID_TYPE_MESSAGE, value));
      }
    }
  }

  public static final class BooleanArgument extends Argument<Boolean> {
    static final String INVALID_TYPE_MESSAGE =
        "Supplied argument '%s' is not of type boolean.";

    public BooleanArgument(String name, String description,
        boolean isOptional) {
      super(name, description, isOptional);
    }

    @Override public void setValueOrThrow(String value) {
      if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
        Boolean booleanValue = Boolean.parseBoolean(value);

        setValue(booleanValue);
      } else {
        throw new IllegalArgumentException(
            String.format(INVALID_TYPE_MESSAGE, value));
      }
    }
  }

  public static final class Option {
    private static final Pattern OPTION_PATTERN = Pattern.compile(
        "^--(?<name>[a-z]+)=(?<value>[A-Za-z0-9 ]+|\"[A-Za-z0-9 ]+\")$");

    private final String name;
    private final String description;
    private String value;

    public Option(String name, String description) {
      this.name = name;
      this.description = description;
    }

    public String getName() {
      return name;
    }

    public String getDescription() {
      return description;
    }

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }

    public static boolean isOption(String optionStatement) {
      return OPTION_PATTERN.matcher(optionStatement).matches();
    }

    public static String getName(String optionStatement) {
      Matcher matcher = OPTION_PATTERN.matcher(optionStatement);

      matcher.find();

      return matcher.group("name");
    }

    public static String getValue(String optionStatement) {
      Matcher matcher = OPTION_PATTERN.matcher(optionStatement);

      matcher.find();

      return matcher.group("value");
    }
  }
}
