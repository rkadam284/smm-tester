package org.immregistries.smm.tester.manager.hl7.segments;

import org.immregistries.smm.tester.manager.hl7.Cardinality;
import org.immregistries.smm.tester.manager.hl7.HL7Component;
import org.immregistries.smm.tester.manager.hl7.ItemType;
import org.immregistries.smm.tester.manager.hl7.UsageType;
import org.immregistries.smm.tester.manager.hl7.ValueSet;
import org.immregistries.smm.tester.manager.hl7.conformance.ExactValue;
import org.immregistries.smm.tester.manager.hl7.conformance.MatchValue;
import org.immregistries.smm.tester.manager.hl7.datatypes.CE;
import org.immregistries.smm.tester.manager.hl7.datatypes.CWE;
import org.immregistries.smm.tester.manager.hl7.datatypes.ID;
import org.immregistries.smm.tester.manager.hl7.datatypes.LA2;
import org.immregistries.smm.tester.manager.hl7.datatypes.NM;
import org.immregistries.smm.tester.manager.hl7.datatypes.ST;
import org.immregistries.smm.tester.manager.hl7.datatypes.TS;
import org.immregistries.smm.tester.manager.hl7.datatypes.XCN;
import org.immregistries.smm.tester.manager.hl7.predicates.IsNotValuedAs;
import org.immregistries.smm.tester.manager.hl7.predicates.IsValued;
import org.immregistries.smm.tester.manager.hl7.predicates.IsValuedAs;

public class RXAforRSP extends RXA
{

  @Override
  public HL7Component makeAnother() {
    return new RXAforRSP(this);
  }

  public RXAforRSP(RXAforRSP copy) {
    super(copy);
  }

  public RXAforRSP(UsageType usageType, Cardinality cardinality) {
    super(usageType, cardinality);
  }

  @Override
  public void init() {
    super.init();
    dateTimeEndOfAdministration.setUsageType(UsageType.O);
    administeredAmount.setUsageType(UsageType.O);
    administeredUnits.setUsageType(UsageType.O);
    administrationNotes.setUsageType(UsageType.O);
    administeringProvider.setUsageType(UsageType.O);
    administeredAtLocation.setUsageType(UsageType.O);
    substanceLotNumber.setUsageType(UsageType.O);
    substanceExpirationDate.setUsageType(UsageType.O);
    substanceManufacturerName.setUsageType(UsageType.O);
    actionCodeRXA.setUsageType(UsageType.O);
  }
}
