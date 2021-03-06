package org.drugis.addis.interventions.controller.command;

import org.drugis.addis.interventions.model.DoseConstraint;
import org.drugis.addis.interventions.model.FixedDoseIntervention;
import org.drugis.addis.interventions.model.InvalidConstraintException;
import org.drugis.trialverse.util.Namespaces;

import java.net.URI;

/**
 * Created by daan on 5-4-16.
 */
public class FixedInterventionCommand extends AbstractInterventionCommand {
  ConstraintCommand fixedDoseConstraint;

  public FixedInterventionCommand() {
  }

  @Override
  public FixedDoseIntervention toIntervention() throws InvalidConstraintException {
    return new FixedDoseIntervention(null, this.getProjectId(), this.getName(), this.getMotivation(),
            URI.create(this.getSemanticInterventionUri()),
            this.getSemanticInterventionLabel(),
            new DoseConstraint(this.fixedDoseConstraint.getLowerBound(), this.fixedDoseConstraint.getUpperBound()));
  }

  public FixedInterventionCommand(Integer projectId, String name, String motivation, String semanticInterventionLabel,
                                  String semanticInterventionUri, ConstraintCommand fixedDoseConstraint) {
    super(projectId, name, motivation, semanticInterventionLabel, semanticInterventionUri);
    this.fixedDoseConstraint = fixedDoseConstraint;
  }

  public ConstraintCommand getFixedDoseConstraint() {
    return fixedDoseConstraint;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    FixedInterventionCommand that = (FixedInterventionCommand) o;

    return fixedDoseConstraint.equals(that.fixedDoseConstraint);

  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + fixedDoseConstraint.hashCode();
    return result;
  }
}
