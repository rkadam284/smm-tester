package org.immregistries.smm.tester.certify;

import static org.immregistries.smm.transform.ScenarioManager.SCENARIO_1_R_ADMIN_CHILD;
import static org.immregistries.smm.transform.ScenarioManager.createTestCaseMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.immregistries.smm.tester.run.TestRunner;
import org.immregistries.smm.tester.transform.Issue;
import org.immregistries.smm.transform.TestCaseMessage;

public class CAAdvanced extends CertifyArea {

  public CAAdvanced(CertifyRunner certifyRunner) {
    super("C", VALUE_TEST_SECTION_TYPE_ADVANCED, certifyRunner);
  }

  @Override
  public void prepareUpdates() {
    Map<Integer, List<Issue>> issueMap = new HashMap<Integer, List<Issue>>();
    for (Issue issue : Issue.values()) {
      int priority = issue.getPriority();
      List<Issue> issueList = issueMap.get(priority);
      if (issueList == null) {
        issueList = new ArrayList<Issue>();
        issueMap.put(priority, issueList);
      }
      issueList.add(issue);
    }

    int count = 0;
    for (int i = 0; i < areaScore.length; i++) {
      int priority = i + 1;
      List<Issue> issueList = issueMap.get(priority);
      if (issueList != null && issueList.size() > 0) {
        for (Issue issue : issueList) {
          count++;
          TestCaseMessage testCaseMessage = createTestCaseMessage(SCENARIO_1_R_ADMIN_CHILD);
          testCaseMessage.setDescription(issue.getName());
          testCaseMessage.setFieldName(issue.getFieldName());
          testCaseMessage.addCauseIssues(issue.getName());
          if (issue.getFieldName().equals("")) {
            testCaseMessage.setAssertResult(TestRunner.ASSERT_RESULT_ACCEPT_ACCEPT_OR_ERROR);
          } else {
            testCaseMessage.setAssertResult(TestRunner.ASSERT_RESULT_ERROR_LOCATION_IS_ + issue.getFieldName());
          }
          registerIfHasIssue(count, priority, testCaseMessage);
        }
      }
    }
  }

  public void registerIfHasIssue(int count, int masterCount, TestCaseMessage testCaseMessage) {
    testCaseMessage.setTestCaseSet(certifyRunner.testCaseSet);
    testCaseMessage.setTestCaseCategoryId(areaLetter + "." + makeTwoDigits(masterCount) + "." + makeTwoDigits(count));
    testCaseMessage.setTestCaseNumber(certifyRunner.uniqueMRNBase + testCaseMessage.getTestCaseCategoryId());
    testCaseMessage.setTestPosition(certifyRunner.incrementingInt.next());
    testCaseMessage.setTestType(VALUE_TEST_TYPE_UPDATE);
    certifyRunner.transformer.transform(testCaseMessage);
    if (testCaseMessage.hasIssue()) {
      certifyRunner.register(testCaseMessage);
      updateList.add(testCaseMessage);
    }
  }

  @Override
  public void sendUpdates() {
    runUpdates(true);
  }

  @Override
  public void prepareQueries() {
    doPrepareQueries();
  }

  @Override
  public void sendQueries() {
    runQueries();
  }

}