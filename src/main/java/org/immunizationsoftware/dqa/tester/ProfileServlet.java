package org.immunizationsoftware.dqa.tester;

import static org.immunizationsoftware.dqa.transform.ScenarioManager.SCENARIO_FULL_RECORD_FOR_PROFILING;
import static org.immunizationsoftware.dqa.transform.ScenarioManager.createTestCaseMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.eclipse.jetty.server.Request;
import org.immunizationsoftware.dqa.tester.profile.CompatibilityConformance;
import org.immunizationsoftware.dqa.tester.profile.CompatibilityInteroperability;
import org.immunizationsoftware.dqa.tester.profile.MessageAcceptStatus;
import org.immunizationsoftware.dqa.tester.profile.ProfileField;
import org.immunizationsoftware.dqa.tester.profile.ProfileFieldType;
import org.immunizationsoftware.dqa.tester.profile.ProfileLine;
import org.immunizationsoftware.dqa.tester.profile.ProfileManager;
import org.immunizationsoftware.dqa.tester.profile.ProfileUsage;
import org.immunizationsoftware.dqa.tester.profile.ProfileUsageValue;
import org.immunizationsoftware.dqa.tester.profile.Usage;
import org.immunizationsoftware.dqa.transform.TestCaseMessage;
import org.immunizationsoftware.dqa.transform.Transformer;

public class ProfileServlet extends ClientServlet {

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    doGet(req, resp);
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    response.setContentType("text/html;charset=UTF-8");
    HttpSession session = request.getSession(true);
    String username = (String) session.getAttribute("username");
    if (username == null) {
      response.sendRedirect(Authenticate.APP_DEFAULT_HOME);
    } else {
      Authenticate.User user = (Authenticate.User) session.getAttribute("user");
      String action = request.getParameter("action");
      if (action == null) {
        action = "";
      }

      PrintWriter out = response.getWriter();
      try {
        initProfileManager(false);
        ProfileUsage profileUsageSelected = null;
        int profileUsageId = 0;
        if (request.getParameter("profileUsageId") != null) {
          profileUsageId = Integer.parseInt(request.getParameter("profileUsageId"));
        } else if (session.getAttribute("profileUsageId") != null) {
          profileUsageId = (Integer) session.getAttribute("profileUsageId");
        }
        if (profileUsageId > 0) {
          session.setAttribute("profileUsageId", profileUsageId);
        }
        ProfileUsage profileUsageCompare = null;
        int profileUsageIdCompare = 0;
        String profileUsageIdCompareString = request.getParameter("profileUsageIdCompare");
        if (profileUsageIdCompareString != null && !profileUsageIdCompareString.equals("")) {
          profileUsageIdCompare = Integer.parseInt(profileUsageIdCompareString);
        }

        if (profileManager == null) {
          request.setAttribute("message", "Unable to find profile details");
        }

        printHtmlHead(out, MENU_HEADER_PROFILE, request);

        if (profileManager != null) {
          String comparisonType = request.getParameter("comparisonType");
          if (comparisonType == null) {
            comparisonType = "C";
          }
          {
            out.println("    <h2>Compare Profiles</h2>");
            out.println("    <form action=\"ProfileServlet\" method=\"GET\">");
            out.println("      <table border=\"0\">");
            out.println("        <tr>");
            out.println("          <td>Usage Profile</td>");
            out.println("          <td>");
            out.println("            <select name=\"profileUsageId\">");
            out.println("              <option value=\"\">select</option>");
            {
              int i = 0;
              for (ProfileUsage profileUsage : profileManager.getProfileUsageList()) {
                i++;
                if (profileUsageId == i) {
                  out.println("              <option value=\"" + i + "\" selected=\"true\">" + profileUsage + "</option>");
                  profileUsageSelected = profileUsage;
                } else {
                  out.println("              <option value=\"" + i + "\">" + profileUsage + "</option>");
                }
              }
            }
            out.println("            </select>");
            out.println("          </td>");
            out.println("          <td>");
            out.println("            <input type=\"submit\" name=\"show\" value=\"View\"/>");
            if (user.isAdmin()) {
              out.println("            <input type=\"submit\" name=\"show\" value=\"Edit\"/>");
            }
            out.println("          </td>");
            out.println("        </tr>");
            out.println("        <tr>");
            out.println("          <td>Compare with</td>");
            out.println("          <td>");
            out.println("            <select name=\"profileUsageIdCompare\">");
            out.println("              <option value=\"\">select</option>");
            {
              int i = 0;
              for (ProfileUsage profileUsage : profileManager.getProfileUsageList()) {
                i++;
                if (profileUsageIdCompare == i) {
                  out.println("              <option value=\"" + i + "\" selected=\"true\">" + profileUsage + "</option>");
                  profileUsageCompare = profileUsage;
                } else {
                  out.println("              <option value=\"" + i + "\">" + profileUsage + "</option>");
                }
              }
            }
            out.println("            </select>");
            out.println("          </td>");
            out.println("          <td>");
            String checked = "";
            checked = comparisonType.equals("C") ? " checked=\"checked\"" : "";
            out.println("            <input type=\"radio\" name=\"comparisonType\" value=\"C\"" + checked + "> Conformance");
            checked = comparisonType.equals("I") ? " checked=\"checked\"" : "";
            out.println("            <input type=\"radio\" name=\"comparisonType\" value=\"I\"" + checked + "> Interoperability");
            out.println("            <input type=\"submit\" name=\"show\" value=\"Compare\"/>");
            out.println("          </td>");
            out.println("        </tr>");
            out.println("      </table>");
            out.println("    </form>");
          }

          String show = request.getParameter("show");
          if (show == null) {
            show = "";
          }
          if (show.equals("Compare")) {
            List<ProfileLine> profileLineList = profileManager.createProfileLines(profileUsageSelected);
            ProfileManager.updateMessageAcceptStatus(profileLineList);
            if (profileUsageCompare != null) {
              boolean compareConformance = comparisonType.equals("C");
              if (compareConformance) {

                Map<CompatibilityConformance, List<ProfileLine>> compatibilityMap = new HashMap<CompatibilityConformance, List<ProfileLine>>();
                for (ProfileLine profileLine : profileLineList) {
                  ProfileUsageValue profileUsageValueConformance = profileUsageCompare.getProfileUsageValueMap().get(profileLine.getField());
                  if (profileUsageValueConformance != null) {
                    CompatibilityConformance compatibility = ProfileManager.getCompatibilityConformance(profileLine.getProfileUsageValue(),
                        profileUsageValueConformance);
                    List<ProfileLine> pll = compatibilityMap.get(compatibility);
                    if (pll == null) {
                      pll = new ArrayList<ProfileLine>();
                      compatibilityMap.put(compatibility, pll);
                    }
                    pll.add(profileLine);
                  }
                }

                out.println("<h2>Conformance of " + profileUsageSelected + " in regards to " + profileUsageCompare + "</h2>");
                out.println("<table border=\"1\" cellspacing=\"0\">");
                out.println("  <tr>");
                out.println("    <th>Conformance</th>");
                out.println("    <th>Count</th>");
                out.println("  </tr>");
                printTotalRow(out, compatibilityMap, CompatibilityConformance.COMPATIBLE);
                printTotalRow(out, compatibilityMap, CompatibilityConformance.ALLOWANCE);
                printTotalRow(out, compatibilityMap, CompatibilityConformance.MAJOR_CONSTRAINT);
                printTotalRow(out, compatibilityMap, CompatibilityConformance.CONFLICT);
                printTotalRow(out, compatibilityMap, CompatibilityConformance.MAJOR_CONFLICT);
                printTotalRow(out, compatibilityMap, CompatibilityConformance.UNABLE_TO_DETERMINE);
                printTotalRow(out, compatibilityMap, CompatibilityConformance.NOT_DEFINED);
                out.println("</table>");

                printConformanceCompatibility(out, compatibilityMap, CompatibilityConformance.MAJOR_CONFLICT, profileUsageSelected,
                    profileUsageCompare);
                printConformanceCompatibility(out, compatibilityMap, CompatibilityConformance.CONFLICT, profileUsageSelected, profileUsageCompare);
                printConformanceCompatibility(out, compatibilityMap, CompatibilityConformance.MAJOR_CONSTRAINT, profileUsageSelected,
                    profileUsageCompare);
                printConformanceCompatibility(out, compatibilityMap, CompatibilityConformance.CONSTRAINT, profileUsageSelected, profileUsageCompare);
                printConformanceCompatibility(out, compatibilityMap, CompatibilityConformance.ALLOWANCE, profileUsageSelected, profileUsageCompare);
                printConformanceCompatibility(out, compatibilityMap, CompatibilityConformance.UNABLE_TO_DETERMINE, profileUsageSelected,
                    profileUsageCompare);
                printConformanceCompatibility(out, compatibilityMap, CompatibilityConformance.NOT_DEFINED, profileUsageSelected, profileUsageCompare);
              } else {
                Map<CompatibilityInteroperability, List<ProfileLine>> compatibilityMap = new HashMap<CompatibilityInteroperability, List<ProfileLine>>();
                for (ProfileLine profileLine : profileLineList) {
                  ProfileUsageValue profileUsageValueConformance = profileUsageCompare.getProfileUsageValueMap().get(profileLine.getField());
                  if (profileUsageValueConformance != null) {
                    CompatibilityInteroperability compatibility = ProfileManager.getCompatibilityInteroperability(profileLine.getProfileUsageValue(),
                        profileUsageValueConformance);
                    List<ProfileLine> pll = compatibilityMap.get(compatibility);
                    if (pll == null) {
                      pll = new ArrayList<ProfileLine>();
                      compatibilityMap.put(compatibility, pll);
                    }
                    pll.add(profileLine);
                  }
                }

                out.println("<h2>Interoperability of " + profileUsageSelected + " with " + profileUsageCompare + "</h2>");
                out.println("<table border=\"1\" cellspacing=\"0\">");
                out.println("  <tr>");
                out.println("    <th>Conformance</th>");
                out.println("    <th>Count</th>");
                out.println("  </tr>");
                printTotalRow(out, compatibilityMap, CompatibilityInteroperability.COMPATIBLE);
                printTotalRow(out, compatibilityMap, CompatibilityInteroperability.DATA_LOSS);
                printTotalRow(out, compatibilityMap, CompatibilityInteroperability.IF_CONFIGURED);
                printTotalRow(out, compatibilityMap, CompatibilityInteroperability.IF_POPULATED);
                printTotalRow(out, compatibilityMap, CompatibilityInteroperability.NO_PROBLEM);
                printTotalRow(out, compatibilityMap, CompatibilityInteroperability.PROBLEM);
                printTotalRow(out, compatibilityMap, CompatibilityInteroperability.MAJOR_PROBLEM);
                out.println("</table>");

                printConformanceInteroperability(out, compatibilityMap, CompatibilityInteroperability.MAJOR_PROBLEM, profileUsageSelected,
                    profileUsageCompare);
                printConformanceInteroperability(out, compatibilityMap, CompatibilityInteroperability.PROBLEM, profileUsageSelected,
                    profileUsageCompare);
                printConformanceInteroperability(out, compatibilityMap, CompatibilityInteroperability.IF_CONFIGURED, profileUsageSelected,
                    profileUsageCompare);
                printConformanceInteroperability(out, compatibilityMap, CompatibilityInteroperability.IF_POPULATED, profileUsageSelected,
                    profileUsageCompare);
                printConformanceInteroperability(out, compatibilityMap, CompatibilityInteroperability.DATA_LOSS, profileUsageSelected,
                    profileUsageCompare);
              }
            }
          } else if (show.equals("View")) {
            List<ProfileLine> profileLineList = profileManager.createProfileLines(profileUsageSelected);
            ProfileManager.updateMessageAcceptStatus(profileLineList);
            out.println("<h2>Profile for " + profileUsageSelected + "</h2>");
            {
              String lastDataTypeDef = "";
              boolean skip = false;
              for (ProfileLine profileLine : profileLineList) {
                ProfileField profileField = profileLine.getField();
                if (profileField.isDataType()) {
                  if (!lastDataTypeDef.equals(profileField.getDataTypeDef())) {
                    if (!lastDataTypeDef.equals("") && !skip) {
                      out.println("</table>");
                    }
                    skip = profileLine.getUsage() == Usage.NOT_DEFINED;
                    if (!skip) {
                      out.println("<h3>" + profileField.getDataTypeDef() + " Data Type</h3>");
                      out.println("<table border=\"1\" cellspacing=\"0\">");
                      out.println("  <tr>");
                      out.println("    <th>Field Name</th>");
                      out.println("    <th>Description</th>");
                      out.println("    <th>Type</th>");
                      out.println("    <th>Usage</th>");
                      out.println("  </tr>");
                    }
                  }
                  if (!skip) {
                    if (profileLine.getUsage() != Usage.NOT_DEFINED) {
                      out.println("  <tr>");
                      out.println("    <td>" + profileField.getFieldName() + "</td>");
                      if (profileField.getType() == ProfileFieldType.DATA_TYPE_FIELD) {
                        out.println("    <td>&nbsp;" + profileField.getDescription() + "</td>");
                      } else {
                        out.println("    <td>" + profileField.getDescription() + "</td>");
                      }
                      out.println("    <td>" + profileField.getType() + "</td>");
                      out.println("    <td>" + profileLine.getUsage() + "</td>");
                      out.println("  </tr>");
                    }
                  }
                  lastDataTypeDef = profileField.getDataTypeDef();
                }
              }
              if (!lastDataTypeDef.equals("")) {
                out.println("</table>");
              }
            }

            {
              boolean skip = false;
              String lastSegmentName = "";
              for (ProfileLine profileLine : profileLineList) {
                ProfileField profileField = profileLine.getField();
                if (!profileField.isDataType()) {
                  if (!lastSegmentName.equals(profileField.getSegmentName())) {
                    if (!lastSegmentName.equals("") && !skip) {
                      out.println("</table>");
                    }
                    skip = profileLine.getUsage() == Usage.NOT_DEFINED;
                    if (!skip) {
                      out.println("<h3>" + profileField.getSegmentName() + " Segment</h3>");
                      out.println("<table border=\"1\" cellspacing=\"0\">");
                      out.println("  <tr>");
                      out.println("    <th>Field</th>");
                      out.println("    <th>Description</th>");
                      out.println("    <th>Type</th>");
                      out.println("    <th>Usage</th>");
                      out.println("    <th>Expect Error</th>");
                      out.println("    <th>Test Cases</th>");
                      out.println("  </tr>");
                    }
                  }
                  if (!skip) {
                    if (profileLine.getUsage() != Usage.NOT_DEFINED) {
                      out.println("  <tr>");
                      out.println("    <td>" + profileField.getFieldName() + "</td>");
                      if (profileField.getType() == ProfileFieldType.FIELD_PART || profileField.getType() == ProfileFieldType.FIELD_SUB_PART
                          || profileField.getType() == ProfileFieldType.FIELD_VALUE || profileField.getType() == ProfileFieldType.FIELD_PART_VALUE
                          || profileField.getType() == ProfileFieldType.FIELD_SUB_PART_VALUE) {
                        out.println("    <td>&nbsp;" + profileField.getDescription() + "</td>");
                      } else {
                        out.println("    <td>" + profileField.getDescription() + "</td>");
                      }
                      out.println("    <td>" + profileField.getType() + "</td>");
                      out.println("    <td>" + profileLine.getUsage() + "</td>");
                      if (profileLine.getMessageAcceptStatus() == MessageAcceptStatus.ONLY_IF_PRESENT) {
                        out.println("    <td>If Empty</td>");
                      } else if (profileLine.getMessageAcceptStatus() == MessageAcceptStatus.ONLY_IF_ABSENT) {
                        out.println("    <td>If Valued</td>");
                      } else {
                        out.println("    <td>-</td>");
                      }
                      String link = "ProfileServlet?show=transforms&fieldName=" + URLEncoder.encode(profileField.getFieldName(), "UTF-8");
                      if (!profileLine.getField().isTransformPresentDefined() || !profileLine.getField().isTransformAbsentDefined()) {
                        out.println("    <td><a href=\"" + link + "\">-</a></td>");
                      } else {
                        out.println("    <td><a href=\"" + link + "\">Defined</a></td>");
                      }
                      out.println("  </tr>");
                    }
                  }
                  lastSegmentName = profileField.getSegmentName();
                }
              }
              if (!lastSegmentName.equals("")) {
                out.println("</table>");
              }
            }
          } else if (show.equals("Edit")) {
            List<ProfileLine> profileLineList = profileManager.createProfileLines(profileUsageSelected);
            if (action.equals("Save")) {
              for (ProfileLine profileLine : profileLineList) {
                int pos = profileLine.getField().getPos();
                String usage = request.getParameter("usage" + pos);
                if (usage != null && !usage.equals("")) {
                  profileLine.getProfileUsageValue().setUsage(Usage.readUsage(usage));
                  profileLine.setUsage(profileLine.getProfileUsageValue().getUsage());
                  String value = request.getParameter("value" + pos);
                  String comments = request.getParameter("comments" + pos);
                  profileLine.getProfileUsageValue().setValue(value);
                  profileLine.getProfileUsageValue().setComments(comments);
                }
              }
              if (profileUsageSelected.getFile() != null) {
                try {
                  profileManager.saveProfileUsage(profileUsageSelected, profileLineList);
                } catch (IOException ioe) {
                  ioe.printStackTrace();
                  out.println("<p>Unable to save: " + ioe.getMessage() + "</p>");
                }
              }
            }
            ProfileManager.updateMessageAcceptStatus(profileLineList);
            out.println("<h2>Edit Profile for " + profileUsageSelected + "</h2>");
            String fieldName = request.getParameter("fieldName");
            if (fieldName == null) {
              printEditTable(out, profileLineList, ProfileFieldType.SEGMENT, ProfileFieldType.SEGMENT_GROUP);
              printEditTable(out, profileLineList, ProfileFieldType.DATA_TYPE, null);
            } else {
              ProfileLine profileLineSelected = null;
              for (ProfileLine profileLine : profileLineList) {
                if (profileLine.getField().getFieldName().equals(fieldName)) {
                  profileLineSelected = profileLine;
                  break;
                }
              }
              if (profileLineSelected != null) {
                printEditTable(out, profileLineList, profileLineSelected, request.getParameter("fieldName"));
              }
            }
          } else if (show.equals("transforms")) {
            String fieldName = request.getParameter("fieldName");
            ProfileField profileFieldSelected = null;
            for (ProfileField profileField : profileManager.getProfileFieldList()) {
              if (profileField.getFieldName().equals(fieldName)) {
                profileFieldSelected = profileField;
                break;
              }
            }
            if (profileFieldSelected != null) {
              TestCaseMessage tcmFull = createTestCaseMessage(SCENARIO_FULL_RECORD_FOR_PROFILING);
              Transformer transformer = new Transformer();
              transformer.transform(tcmFull);
              TestCaseMessage testCaseMessagePresent = ProfileManager.getPresentTestCase(profileFieldSelected, tcmFull);
              TestCaseMessage testCaseMessageAbsent = ProfileManager.getAbsentTestCase(profileFieldSelected, tcmFull);
              out.println("<h2>" + profileFieldSelected.getFieldName() + "</h2>");
              String edit = request.getParameter("edit");
              if (edit == null) {
                edit = "";
              }
              out.println("<h3>Present Test Case</h3>");
              if (edit.equals("present")) {
                if (action.equals("Refresh") || action.equals("Save")) {
                  String additionalTransformations = request.getParameter("additionalTransformations");
                  testCaseMessagePresent.setAdditionalTransformations(additionalTransformations);
                  profileFieldSelected.setTransformsPresent(additionalTransformations);
                  if (action.equals("Save")) {
                    profileManager.writeTransforms();
                  }
                }
                String transformedMessage = null;
                String originalMessage = formatMessage(tcmFull.getMessageText());
                if (testCaseMessagePresent != tcmFull) {
                  transformedMessage = formatMessage(transformer.transformAddition(tcmFull, testCaseMessagePresent.getAdditionalTransformations()));
                }
                out.println("<p>Starting Message</p>");
                if (transformedMessage != null) {
                  out.println("<pre>" + addHovers(showDiff(originalMessage, transformedMessage)) + "</pre>");
                } else {
                  out.println("<pre>" + originalMessage + "</pre>");
                }
                out.println("<p>Transforms</p>");
                out.println("<form action=\"ProfileServlet\" method=\"POST\">");
                out.println("  <textarea name=\"additionalTransformations\" cols=\"70\" rows=\"10\" wrap=\"off\">"
                    + testCaseMessagePresent.getAdditionalTransformations() + "</textarea></td>");
                out.println("  <input type=\"hidden\" name=\"show\" value=\"transforms\"/>");
                out.println("  <input type=\"hidden\" name=\"fieldName\" value=\"" + profileFieldSelected.getFieldName() + "\"/>");
                out.println("  <input type=\"hidden\" name=\"edit\" value=\"present\"/>");
                out.println("  <br/>");
                out.println("  <input type=\"submit\" name=\"action\" value=\"Refresh\"/>");
                out.println("  <input type=\"submit\" name=\"action\" value=\"Save\"/>");
                out.println("</form>");
                if (testCaseMessagePresent != tcmFull) {
                  out.println("<p>Final Message</p>");
                  out.println("<pre>" + addHovers(showDiff(transformedMessage, originalMessage)) + "</pre>");
                }
              } else {
                if (testCaseMessagePresent == tcmFull) {
                  out.println("<p>Full Test Case</p>");
                } else if (!testCaseMessagePresent.hasIssue()) {
                  out.println("<p>Not Defined</p>");
                } else {
                  out.println("<pre>" + testCaseMessagePresent.getAdditionalTransformations() + "</pre>");
                }
                String link = "ProfileServlet?show=transforms&fieldName=" + URLEncoder.encode(profileFieldSelected.getFieldName(), "UTF-8")
                    + "&edit=present";
                out.println("<p><a href=\"" + link + "\">Edit</a></p>");
              }
              out.println("<h3>Absent Test Case</h3>");
              if (edit.equals("absent")) {
                if (action.equals("Refresh") || action.equals("Save")) {
                  String additionalTransformations = request.getParameter("additionalTransformations");
                  testCaseMessageAbsent.setAdditionalTransformations(additionalTransformations);
                  profileFieldSelected.setTransformsPresent(additionalTransformations);
                  if (action.equals("Save")) {
                    profileManager.writeTransforms();
                  }
                }
                String transformedMessage = null;
                String originalMessage = formatMessage(tcmFull.getMessageText());
                if (testCaseMessageAbsent != tcmFull) {
                  transformedMessage = formatMessage(transformer.transformAddition(tcmFull, testCaseMessageAbsent.getAdditionalTransformations()));
                }
                out.println("<p>Starting Message</p>");
                if (transformedMessage != null) {
                  out.println("<pre>" + addHovers(showDiff(originalMessage, transformedMessage)) + "</pre>");
                } else {
                  out.println("<pre>" + originalMessage + "</pre>");
                }
                out.println("<p>Transforms</p>");
                out.println("<form action=\"ProfileServlet\" method=\"POST\">");
                out.println("  <textarea name=\"additionalTransformations\" cols=\"70\" rows=\"10\" wrap=\"off\">"
                    + testCaseMessageAbsent.getAdditionalTransformations() + "</textarea></td>");
                out.println("  <input type=\"hidden\" name=\"show\" value=\"transforms\"/>");
                out.println("  <input type=\"hidden\" name=\"fieldName\" value=\"" + profileFieldSelected.getFieldName() + "\"/>");
                out.println("  <input type=\"hidden\" name=\"edit\" value=\"absent\"/>");
                out.println("  <br/>");
                out.println("  <input type=\"submit\" name=\"action\" value=\"Refresh\"/>");
                out.println("  <input type=\"submit\" name=\"action\" value=\"Save\"/>");
                out.println("</form>");
                if (testCaseMessageAbsent != tcmFull) {
                  out.println("<p>Final Message</p>");
                  out.println("<pre>" + addHovers(showDiff(transformedMessage, originalMessage)) + "</pre>");
                }
              } else {
                if (testCaseMessageAbsent == tcmFull) {
                  out.println("<p>Full Test Case</p>");
                } else if (!testCaseMessageAbsent.hasIssue()) {
                  out.println("<p>Not Defined</p>");
                } else {
                  out.println("<pre>" + testCaseMessageAbsent.getAdditionalTransformations() + "</pre>");
                }
                String link = "ProfileServlet?show=transforms&fieldName=" + URLEncoder.encode(profileFieldSelected.getFieldName(), "UTF-8")
                    + "&edit=absent";
                out.println("<p><a href=\"" + link + "\">Edit</a></p>");
              }
            }
          }
        }

        printHtmlFoot(out);
      } finally {
        out.close();
      }
    }
  }

  private void printEditTable(PrintWriter out, List<ProfileLine> profileLineList, ProfileLine profileLineSelected, String fieldName)
      throws UnsupportedEncodingException {
    out.println("<h3>" + profileLineSelected.getField().getDescription() + " Table</h3>");
    out.println("<form action=\"ProfileServlet\" method=\"POST\">");
    out.println("<table border=\"1\" cellspacing=\"0\">");
    out.println("  <tr>");
    out.println("    <th>Field Name</th>");
    out.println("    <th>Description</th>");
    out.println("    <th>Usage</th>");
    out.println("    <th>Value</th>");
    out.println("    <th>Comments</th>");
    out.println("  </tr>");
    boolean found = false;
    ProfileFieldType profileFieldType1 = null;
    ProfileFieldType profileFieldType2 = null;
    switch (profileLineSelected.getField().getType()) {
    case DATA_TYPE:
      profileFieldType1 = ProfileFieldType.DATA_TYPE_FIELD;
      profileFieldType2 = null;
      break;
    case DATA_TYPE_FIELD:
      profileFieldType1 = null;
      profileFieldType2 = null;
      break;
    case FIELD:
      profileFieldType1 = ProfileFieldType.FIELD_PART;
      profileFieldType2 = ProfileFieldType.FIELD_PART_VALUE;
      break;
    case FIELD_PART:
      profileFieldType1 = ProfileFieldType.FIELD_SUB_PART;
      profileFieldType2 = ProfileFieldType.FIELD_PART_VALUE;
      break;
    case FIELD_PART_VALUE:
      profileFieldType1 = null;
      profileFieldType2 = null;
      break;
    case FIELD_SUB_PART:
      profileFieldType1 = null;
      profileFieldType2 = null;
      break;
    case FIELD_SUB_PART_VALUE:
      profileFieldType1 = null;
      profileFieldType2 = null;
      break;
    case FIELD_VALUE:
      profileFieldType1 = null;
      profileFieldType2 = null;
      break;
    case SEGMENT:
      profileFieldType1 = ProfileFieldType.FIELD;
      profileFieldType2 = null;
      break;
    case SEGMENT_GROUP:
      profileFieldType1 = ProfileFieldType.SEGMENT;
      profileFieldType2 = null;
      break;
    }
    for (ProfileLine profileLine : profileLineList) {
      ProfileField profileField = profileLine.getField();

      if (found) {
        if (profileField.getType() == profileLineSelected.getField().getType()) {
          found = false;
        } else {
          if (profileField.getType() == profileFieldType1 || profileField.getType() == profileFieldType2) {
            String link = "ProfileServlet?show=Edit&fieldName=" + URLEncoder.encode(profileField.getFieldName(), "UTF-8");
            out.println("  <tr>");
            if (profileField.getType() == ProfileFieldType.FIELD || profileField.getType() == ProfileFieldType.FIELD_SUB_PART) {
              out.println("    <td><a href=\"" + link + "\">" + profileField.getFieldName() + "</a></td>");
              out.println("    <td><a href=\"" + link + "\">" + profileField.getDescription() + "</a></td>");
            } else {
              out.println("    <td>" + profileField.getFieldName() + "</td>");
              out.println("    <td>" + profileField.getDescription() + "</td>");
            }
            out.println("    <td>");
            out.println("      <select name=\"usage" + profileField.getPos() + "\">");
            out.println("        <option value=\"\">select</option>");
            for (Usage usage : Usage.values()) {
              if (usage == profileLine.getUsage()) {
                out.println("        <option value=\"" + usage + "\" selected=\"true\">" + usage + " " + usage.getDescription() + "</option>");
              } else {
                out.println("        <option value=\"" + usage + "\">" + usage + " " + usage.getDescription() + "</option>");
              }
            }
            out.println("      </select>");
            out.println("    </td>");
            if (fieldName != null) {
              out.println("  <input type=\"hidden\" name=\"fieldName\" value=\"" + fieldName + "\"/>");
            }
            out.println("    <td><input type=\"text\" size=\"5\" name=\"value" + profileField.getPos() + "\" value=\""
                + profileLine.getProfileUsageValue().getValue() + "\"/></td>");
            out.println("    <td><input type=\"text\" size=\"20\" name=\"comments" + profileField.getPos() + "\" value=\""
                + profileLine.getProfileUsageValue().getComments() + "\"/></td>");
            out.println("  </tr>");
          }
        }
      } else if (profileLine == profileLineSelected) {
        found = true;
      }
    }
    out.println("</table>");
    out.println("<input type=\"hidden\" name=\"show\" value=\"Edit\"/>");
    out.println("<input type=\"submit\" name=\"action\" value=\"Save\"/>");
    out.println("</form>");
  }

  private void printEditTable(PrintWriter out, List<ProfileLine> profileLineList, ProfileFieldType profileFieldType1,
      ProfileFieldType profileFieldType2) throws UnsupportedEncodingException {
    out.println("<h3>" + profileFieldType1 + " Table</h3>");
    out.println("<form action=\"ProfileServlet\" method=\"POST\">");
    out.println("<table border=\"1\" cellspacing=\"0\">");
    out.println("  <tr>");
    out.println("    <th>Field Name</th>");
    out.println("    <th>Description</th>");
    out.println("    <th>Usage</th>");
    out.println("    <th>Value</th>");
    out.println("    <th>Comments</th>");
    out.println("  </tr>");
    for (ProfileLine profileLine : profileLineList) {
      ProfileField profileField = profileLine.getField();
      if (profileField.getType() == profileFieldType1 || profileField.getType() == profileFieldType2) {
        String link = "ProfileServlet?show=Edit&fieldName=" + URLEncoder.encode(profileField.getFieldName(), "UTF-8");
        out.println("  <tr>");
        if (profileLine.getUsage() == Usage.NOT_DEFINED || profileLine.getField().getType() == ProfileFieldType.SEGMENT_GROUP) {
          out.println("    <td>" + profileField.getFieldName() + "</td>");
          out.println("    <td>" + profileField.getDescription() + "</td>");
        } else {
          out.println("    <td><a href=\"" + link + "\">" + profileField.getFieldName() + "</a></td>");
          out.println("    <td><a href=\"" + link + "\">" + profileField.getDescription() + "</a></td>");
        }
        out.println("    <td>");
        out.println("      <select name=\"usage" + profileField.getPos() + "\">");
        out.println("        <option value=\"\">select</option>");
        for (Usage usage : Usage.values()) {
          if (usage == profileLine.getUsage()) {
            out.println("        <option value=\"" + usage + "\" selected=\"true\">" + usage + " " + usage.getDescription() + "</option>");
          } else {
            out.println("        <option value=\"" + usage + "\">" + usage + " " + usage.getDescription() + "</option>");
          }
        }
        out.println("      </select>");
        out.println("    </td>");
        out.println("    <td><input type=\"text\" size=\"5\" name=\"value" + profileField.getPos() + "\" value=\""
            + profileLine.getProfileUsageValue().getValue() + "\"/></td>");
        out.println("    <td><input type=\"text\" size=\"20\" name=\"comments" + profileField.getPos() + "\" value=\""
            + profileLine.getProfileUsageValue().getComments() + "\"/></td>");
        out.println("  </tr>");
      }
    }
    out.println("</table>");
    out.println("<input type=\"hidden\" name=\"show\" value=\"Edit\"/>");
    out.println("<input type=\"submit\" name=\"action\" value=\"Save\"/>");
    out.println("</form>");
  }

  public void printTotalRow(PrintWriter out, Map<CompatibilityConformance, List<ProfileLine>> compatibilityMap, CompatibilityConformance c) {
    List<ProfileLine> pl = compatibilityMap.get(c);
    if (pl != null && pl.size() > 0) {
      out.println("  <tr>");
      out.println("    <td>" + c + "</td>");
      out.println("    <td>" + pl.size() + "</td>");
      out.println("  </tr>");
    }
  }

  public void printTotalRow(PrintWriter out, Map<CompatibilityInteroperability, List<ProfileLine>> compatibilityMap, CompatibilityInteroperability c) {
    List<ProfileLine> pl = compatibilityMap.get(c);
    if (pl != null && pl.size() > 0) {
      out.println("  <tr>");
      out.println("    <td>" + c + "</td>");
      out.println("    <td>" + pl.size() + "</td>");
      out.println("  </tr>");
    }
  }

  private static String showDiff(String message, String compareMessage) {
    int firstDiffPos = message.length();
    int lastDiffPos = message.length();
    for (int i = 0; i < message.length() && i < compareMessage.length(); i++) {
      if (message.charAt(i) != compareMessage.charAt(i)) {
        firstDiffPos = i;
        break;
      }
    }
    if (firstDiffPos == message.length()) {
      return message;
    }
    int i = message.length();
    int j = compareMessage.length();
    while (i > 0 && j > 0) {
      i--;
      j--;
      if (message.charAt(i) != compareMessage.charAt(j)) {
        lastDiffPos = i + 1;
        break;
      }
    }
    return message.substring(0, firstDiffPos) + "<b class=\"different\">" + message.substring(firstDiffPos, lastDiffPos) + "</b>"
        + message.substring(lastDiffPos);
  }

  private static String addHovers(String message) throws IOException {
    StringBuilder sb = new StringBuilder();
    BufferedReader reader = new BufferedReader(new StringReader(message));
    String line;
    String segmentName = "";
    int fieldCount = 0;
    while ((line = reader.readLine()) != null) {
      if (!line.startsWith("   ") && line.length() > 3 && line.charAt(3) == '|') {
        segmentName = line.substring(0, 3);
        fieldCount = 0;
      }
      for (int i = 0; i < line.length(); i++) {
        char c = line.charAt(i);
        if (line.charAt(i) == '|') {
          if (fieldCount > 0) {
            sb.append("</a>");
          } else {
            if (segmentName.equals("MSH") || segmentName.equals("FHS") || segmentName.equals("BHS")) {
              fieldCount++;
            }
          }
          fieldCount++;
          sb.append("<a class=\"hl7\" title=\"" + segmentName + "-" + fieldCount + "\">");
        }
        sb.append(line.charAt(i));
      }
      if (fieldCount > 0) {
        sb.append("</a>");
      }
      sb.append("\n");
    }
    reader.close();
    return sb.toString();
  }

  private static String formatMessage(String message) throws IOException {
    StringBuilder sb = new StringBuilder();
    BufferedReader reader = new BufferedReader(new StringReader(message));
    String line;
    while ((line = reader.readLine()) != null) {
      boolean first = true;
      while (line.length() > 200) {
        int i = first ? 200 : 197;
        while (i > 4 && line.charAt(i) != '|') {
          i--;
        }
        if (i == 4) {
          i = first ? 200 : 197;
        }
        if (!first) {
          sb.append("   ");
        }
        sb.append(line.substring(0, i));
        sb.append("\n");
        line = line.substring(i);
        first = false;
      }
      if (!first) {
        sb.append("   ");
      }
      sb.append(line);
      sb.append("\n");
    }
    reader.close();
    return sb.toString();
  }

  private void printConformanceCompatibility(PrintWriter out, Map<CompatibilityConformance, List<ProfileLine>> compatibilityMap,
      CompatibilityConformance c, ProfileUsage profileUsageSelected, ProfileUsage profileUsageCompare) {
    if (compatibilityMap.get(c) != null) {
      out.println("<h3>" + c + "</h3>");
      out.println("<table border=\"1\" cellspacing=\"0\">");
      out.println("  <tr>");
      out.println("    <th>Field</th>");
      out.println("    <th>Description</th>");
      out.println("    <th>" + profileUsageSelected + "</th>");
      out.println("    <th>" + profileUsageCompare + "</th>");
      out.println("  </tr>");
      for (ProfileLine profileLine : compatibilityMap.get(c)) {
        ProfileUsageValue profileUsageValueConformance = profileUsageCompare.getProfileUsageValueMap().get(profileLine.getField());
        if (profileUsageValueConformance != null) {
          out.println("  <tr>");
          out.println("    <td>" + profileLine.getField().getFieldName() + "</td>");
          out.println("    <td>" + profileLine.getField().getDescription() + "</td>");
          out.println("    <td>" + profileLine.getUsage() + "</td>");
          out.println("    <td>" + profileUsageValueConformance.getUsage() + "</td>");
          out.println("  </tr>");
        }
      }
      out.println("</table>");
    }
  }

  private void printConformanceInteroperability(PrintWriter out, Map<CompatibilityInteroperability, List<ProfileLine>> compatibilityMap,
      CompatibilityInteroperability c, ProfileUsage profileUsageSelected, ProfileUsage profileUsageCompare) {
    if (compatibilityMap.get(c) != null) {
      out.println("<h3>" + c + "</h3>");
      out.println("<table border=\"1\" cellspacing=\"0\">");
      out.println("  <tr>");
      out.println("    <th>Field</th>");
      out.println("    <th>Description</th>");
      out.println("    <th>" + profileUsageSelected + "</th>");
      out.println("    <th>" + profileUsageCompare + "</th>");
      out.println("  </tr>");
      for (ProfileLine profileLine : compatibilityMap.get(c)) {
        ProfileUsageValue profileUsageValueConformance = profileUsageCompare.getProfileUsageValueMap().get(profileLine.getField());
        if (profileUsageValueConformance != null) {
          out.println("  <tr>");
          out.println("    <td>" + profileLine.getField().getFieldName() + "</td>");
          out.println("    <td>" + profileLine.getField().getDescription() + "</td>");
          out.println("    <td>" + profileLine.getUsage() + "</td>");
          out.println("    <td>" + profileUsageValueConformance.getUsage() + "</td>");
          out.println("  </tr>");
        }
      }
      out.println("</table>");
    }
  }

}
