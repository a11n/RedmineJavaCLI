package de.ad.tools.redmine.cli.command;

import com.taskadapter.redmineapi.RedmineManager;
import de.ad.tools.redmine.cli.Configuration;

import java.awt.*;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;

public class OpenCommand extends RedmineCommand {

  static final String NO_DESKTOP_SUPPORT_MESSAGE =
      "Cannot open issue #%d in default browser.";
  static final String SUCCESS_MESSAGE =
      "Opened issue #%d in default browser.";
  
  private static final String NAME = "open";
  private static final String DESCRIPTION = "Open issue in default browser.";
  private static final Argument[] ARGUMENTS =
      new Argument[] {
          new NumberArgument("id", "The ID of the issue to open.", false) };

  private Browser browser;

  public OpenCommand(Configuration configuration, PrintStream out,
      RedmineManager redmineManager, Browser browser) {
    super(NAME, DESCRIPTION, "", ARGUMENTS, configuration, out,
        redmineManager);

    this.browser = browser;
  }

  @Override
  public void process(String[] arguments) throws Exception {
    super.process(arguments);

    Integer id = ((NumberArgument)getArguments()[0]).getValue();

    assertBrowserIsSupported(id);

    String uri =
        String.format("%s/issues/%d", configuration.getServer(), id);
    browser.browse(new URI(uri));
    
    println(SUCCESS_MESSAGE, id);
  }

  private void assertBrowserIsSupported(Integer id) throws Exception {
    if (!browser.isSupported()) {
      throw new Exception(String.format(NO_DESKTOP_SUPPORT_MESSAGE, id));
    }
  }

  /**
   * Wraps static calls to Desktop for a better testability.
   */
  public static class Browser {

    private final boolean isSupported;
    private final Desktop desktop;

    public Browser() {
      isSupported = Desktop.isDesktopSupported();

      //Avoid java.awt.HeadlessException when running in CI environment
      if (isSupported) {
        desktop = Desktop.getDesktop();
      } else {
        desktop = null;
      }
    }

    public Browser(boolean isSupported, Desktop desktop) {
      this.isSupported = isSupported;
      this.desktop = desktop;
    }

    public boolean isSupported() {
      return isSupported;
    }

    public void browse(URI uri) throws IOException {
      if (desktop != null) {
        desktop.browse(uri);
      }
    }
  }
}
