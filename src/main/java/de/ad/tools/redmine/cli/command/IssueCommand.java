package de.ad.tools.redmine.cli.command;

import com.taskadapter.redmineapi.IssueManager;
import com.taskadapter.redmineapi.RedmineManager;
import com.taskadapter.redmineapi.bean.Issue;
import de.ad.tools.redmine.cli.Configuration;

import java.io.PrintStream;

import static de.ad.tools.redmine.cli.util.DateUtil.getTimeDifferenceAsText;

public class IssueCommand extends RedmineCommand {

  private static final String NAME = "issue";
  private static final String DESCRIPTION = "Display issue details.";
  private static final Argument[] ARGUMENTS =
      new Argument[] { new Argument("id", "The ID of the issue to display.",
          false) };

  public IssueCommand(Configuration configuration, PrintStream out,
      RedmineManager redmineManager) {
    super(NAME, DESCRIPTION, "", ARGUMENTS, configuration, out,
        redmineManager);
  }

  @Override
  public void process(String[] arguments) throws Exception {
    super.process(arguments);

    IssueManager issueManager = redmineManager.getIssueManager();

    String id = getArguments()[0].getValue();
    Issue issue = issueManager.getIssueById(Integer.valueOf(id));

    printHeader(issue);
    printDetails(issue);
    printDescription(issue);
  }

  private void printHeader(Issue issue) {
    println("%s #%d", issue.getTracker().getName(), issue.getId());
    println(issue.getSubject());
    String createdText = getTimeDifferenceAsText(issue.getCreatedOn());
    String updatedText = "";

    if (issue.getCreatedOn().before(issue.getUpdatedOn())) {
      updatedText = getTimeDifferenceAsText(issue.getUpdatedOn());
      updatedText = String.format("Updated %s ago.", updatedText);
    }
    println("Added by %s %s ago. %s", issue.getAuthor().getFullName(),
        createdText, updatedText);
    println();
  }

  private void printDetails(Issue issue) {
    String[][] details = new String[1][3];
    String[] header = new String[] { "Status", "Priority", "Assignee" };

    String assignee =
        issue.getAssignee() != null ?
            issue.getAssignee().getFullName() : "(not assigned)";
    details[0] =
        new String[] { issue.getStatusName(), issue.getPriorityText(),
            assignee };

    printTable(header, details);
    println();
  }

  private void printDescription(Issue issue) {
    println("Description\n¯¯¯¯¯¯¯¯¯¯¯");

    if (issue.getDescription().length() > 0) {
      println(issue.getDescription());
    } else {
      println("(not set)");
    }
    println();
  }
}
