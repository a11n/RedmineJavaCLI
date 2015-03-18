package de.ad.tools.redmine.cli.command;

import com.taskadapter.redmineapi.IssueManager;
import com.taskadapter.redmineapi.RedmineManager;
import com.taskadapter.redmineapi.bean.Issue;
import com.taskadapter.redmineapi.bean.IssueStatus;
import com.taskadapter.redmineapi.bean.Tracker;
import com.taskadapter.redmineapi.bean.User;
import de.ad.tools.redmine.cli.Configuration;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static de.ad.tools.redmine.cli.test.TestHelper.resourceToByteArray;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UpdateIssueCommandTest {
  private Configuration configuration;
  private PrintStream out;
  private RedmineManager redmineManager;

  private IssueManager issueManager;

  private UpdateIssueCommand command;

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Before
  public void setUp() throws Exception {
    configuration = mock(Configuration.class);
    when(configuration.isConnected()).thenReturn(true);

    out = mock(PrintStream.class);

    redmineManager = mock(RedmineManager.class);
    issueManager = mock(IssueManager.class);

    when(redmineManager.getIssueManager()).thenReturn(issueManager);

    command = new UpdateIssueCommand(configuration, out, redmineManager);
  }

  @Test
  public void testUpdateWithInvalidKeyValue() throws Exception {
    String[] arguments = new String[] { "1", "thisIsNoKey" };

    exception.expect(Exception.class);
    exception.expectMessage(
        String.format(UpdateIssueCommand.INVALID_KEYVALUE_MESSAGE,
            "thisIsNoKey"));

    command.process(arguments);
  }

  @Test
  public void testUpdateWithInvalidKey() throws Exception {
    String[] arguments = new String[] { "1", "invalidKey=value" };

    exception.expect(Exception.class);
    exception.expectMessage(
        String.format(UpdateIssueCommand.INVALID_KEY_MESSAGE,
            "invalidKey"));

    command.process(arguments);
  }

  @Test
  public void testUpdateDescription() throws Exception {
    String[] arguments =
        new String[] { "1", "description=\"A new description.\"" };

    Issue issue = createMockIssue(1);
    when(issueManager.getIssueById(1)).thenReturn(issue);

    command.process(arguments);

    verify(issue).setDescription("\"A new description.\"");
    verify(issueManager).update(issue);

    verify(out).println(
        String.format(UpdateIssueCommand.ISSUE_UPDATE_SUCCESS_MESSAGE, 1));
  }

  @Test
  public void testUpdateSubject() throws Exception {
    String[] arguments =
        new String[] { "1", "subject=\"A new subject.\"" };

    Issue issue = createMockIssue(1);
    when(issueManager.getIssueById(1)).thenReturn(issue);

    command.process(arguments);

    verify(issue).setSubject("\"A new subject.\"");
    verify(issueManager).update(issue);

    verify(out).println(
        String.format(UpdateIssueCommand.ISSUE_UPDATE_SUCCESS_MESSAGE, 1));
  }
  
  @Test
  public void testUpdateStatus() throws Exception {
    String[] arguments = new String[] { "1", "status=New" };

    Issue issue = createMockIssue(1);
    when(issueManager.getIssueById(1)).thenReturn(issue);

    List<IssueStatus> statuses = createDummyStatuses();
    when(issueManager.getStatuses()).thenReturn(statuses);

    command.process(arguments);

    verify(issue).setStatusId(1);
    verify(issueManager).update(issue);

    verify(out).println(
        String.format(UpdateIssueCommand.ISSUE_UPDATE_SUCCESS_MESSAGE, 1));
  }

  @Test
  public void testUpdateWithInvalidStatus() throws Exception {
    String[] arguments = new String[] { "1", "status=invalid" };

    Issue issue = createMockIssue(1);
    when(issueManager.getIssueById(1)).thenReturn(issue);

    List<IssueStatus> statuses = createDummyStatuses();
    when(issueManager.getStatuses()).thenReturn(statuses);

    exception.expect(Exception.class);
    exception.expectMessage(
        String.format(UpdateIssueCommand.INVALID_STATUS_MESSAGE,
            "invalid"));

    command.process(arguments);
  }

  @Test
  public void testUpdateTracker() throws Exception {
    String[] arguments = new String[] { "1", "tracker=Bug" };

    Issue issue = createMockIssue(1);
    when(issueManager.getIssueById(1)).thenReturn(issue);

    List<Tracker> trackers = createDummyTrackers();
    when(issueManager.getTrackers()).thenReturn(trackers);

    command.process(arguments);

    verify(issue).setTracker(trackers.get(0));
    verify(issueManager).update(issue);

    verify(out).println(
        String.format(UpdateIssueCommand.ISSUE_UPDATE_SUCCESS_MESSAGE, 1));
  }

  @Test
  public void testUpdateWithInvalidTracker() throws Exception {
    String[] arguments = new String[] { "1", "tracker=invalid" };

    Issue issue = createMockIssue(1);
    when(issueManager.getIssueById(1)).thenReturn(issue);

    List<Tracker> trackers = createDummyTrackers();
    when(issueManager.getTrackers()).thenReturn(trackers);

    exception.expect(Exception.class);
    exception.expectMessage(
        String.format(UpdateIssueCommand.INVALID_TRACKER_MESSAGE,
            "invalid"));

    command.process(arguments);
  }

  private Issue createMockIssue(int id) {
    Tracker tracker = mock(Tracker.class);
    when(tracker.getName()).thenReturn("Bug");

    Date createdOn = Date.from(
        LocalDateTime.now()
            .minusHours(1)
            .atZone(ZoneId.systemDefault())
            .toInstant());
    Date updatedOn = Date.from(
        LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());

    User author = mock(User.class);
    when(author.getFullName()).thenReturn("John Doe");

    Issue issue = mock(Issue.class);
    when(issue.getTracker()).thenReturn(tracker);
    when(issue.getId()).thenReturn(id);
    when(issue.getSubject()).thenReturn("Subject of #" + id);
    when(issue.getCreatedOn()).thenReturn(createdOn);
    when(issue.getUpdatedOn()).thenReturn(updatedOn);
    when(issue.getAuthor()).thenReturn(author);
    when(issue.getAssignee()).thenReturn(author);
    when(issue.getDescription()).thenReturn("Description of #" + id);

    return issue;
  }

  private List<IssueStatus> createDummyStatuses() {
    IssueStatus newStatus = mock(IssueStatus.class);
    when(newStatus.getName()).thenReturn("New");
    when(newStatus.getId()).thenReturn(1);
    when(newStatus.isDefaultStatus()).thenReturn(true);
    when(newStatus.isClosed()).thenReturn(false);

    IssueStatus closedStatus = mock(IssueStatus.class);
    when(closedStatus.getName()).thenReturn("Closed");
    when(closedStatus.getId()).thenReturn(2);
    when(closedStatus.isDefaultStatus()).thenReturn(false);
    when(closedStatus.isClosed()).thenReturn(true);

    return Arrays.asList(newStatus, closedStatus);
  }

  private List<Tracker> createDummyTrackers() {
    Tracker bugTracker = mock(Tracker.class);
    when(bugTracker.getName()).thenReturn("Bug");
    when(bugTracker.getId()).thenReturn(1);

    Tracker featureTracker = mock(Tracker.class);
    when(featureTracker.getName()).thenReturn("Feature");
    when(featureTracker.getId()).thenReturn(2);

    return Arrays.asList(bugTracker, featureTracker);
  }
}