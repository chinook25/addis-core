package org.drugis.addis.interventions.controller.command;

import org.drugis.addis.interventions.model.AbstractIntervention;
import org.drugis.addis.interventions.model.BothDoseTypesIntervention;
import org.drugis.addis.interventions.model.DoseConstraint;
import org.drugis.addis.interventions.model.InvalidConstraintException;
import org.drugis.trialverse.util.Namespaces;

import java.net.URI;

/**
 * Created by daan on 5-4-16.
 */
public class BothDoseTypesInterventionCommand extends AbstractInterventionCommand {
  private ConstraintCommand bothDoseTypesMinConstraint; //TODO: refactor to shared property with titrated
  private ConstraintCommand bothDoseTypesMaxConstraint;

  public BothDoseTypesInterventionCommand(){}


  public BothDoseTypesInterventionCommand(Integer projectId, String name, String motivation, URI semanticInterventionUri,
                                          String semanticInterventionLabel, ConstraintCommand minConstraint, ConstraintCommand maxConstraint) {
    super(projectId, name, motivation, semanticInterventionLabel, semanticInterventionUri.toString());
    this.bothDoseTypesMinConstraint = minConstraint;
    this.bothDoseTypesMaxConstraint = maxConstraint;
  }

  public ConstraintCommand getBothDoseTypesMinConstraint() {
    return bothDoseTypesMinConstraint;
  }

  public ConstraintCommand getBothDoseTypesMaxConstraint() {
    return bothDoseTypesMaxConstraint;
  }

  @Override
  public AbstractIntervention toIntervention() throws InvalidConstraintException {
    DoseConstraint minConstraint = null;
    if (this.bothDoseTypesMinConstraint != null) {
      minConstraint = new DoseConstraint(this.bothDoseTypesMinConstraint.getLowerBound(), this.bothDoseTypesMinConstraint.getUpperBound());
    }
    DoseConstraint maxConstraint = null;
    if(this.bothDoseTypesMaxConstraint != null) {
      maxConstraint = new DoseConstraint(this.bothDoseTypesMaxConstraint.getLowerBound(), this.bothDoseTypesMaxConstraint.getUpperBound());
    }
    return new BothDoseTypesIntervention(null, this.getProjectId(), this.getName(), this.getMotivation(),
            URI.create(this.getSemanticInterventionUri()), this.getSemanticInterventionLabel(),
            minConstraint,
            maxConstraint);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    BothDoseTypesInterventionCommand that = (BothDoseTypesInterventionCommand) o;

    if (bothDoseTypesMinConstraint != null ? !bothDoseTypesMinConstraint.equals(that.bothDoseTypesMinConstraint) : that.bothDoseTypesMinConstraint != null)
      return false;
    return bothDoseTypesMaxConstraint != null ? bothDoseTypesMaxConstraint.equals(that.bothDoseTypesMaxConstraint) : that.bothDoseTypesMaxConstraint == null;

  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (bothDoseTypesMinConstraint != null ? bothDoseTypesMinConstraint.hashCode() : 0);
    result = 31 * result + (bothDoseTypesMaxConstraint != null ? bothDoseTypesMaxConstraint.hashCode() : 0);
    return result;
  }
}
