package org.drugis.addis.analyses;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.drugis.addis.interventions.Intervention;
import org.drugis.addis.outcomes.Outcome;
import org.drugis.addis.util.ObjectToStringDeserializer;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Created by connor on 3/11/14.
 */
@Entity
public class SingleStudyBenefitRiskAnalysis extends AbstractAnalysis {

  @Id
  @SequenceGenerator(name="analysis_sequence", sequenceName = "shared_analysis_id_seq", allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "analysis_sequence")
  private Integer id;
  private String name;

  @JsonRawValue
  private String problem;

  private Integer studyId;

  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  @JoinTable(name = "singleStudyBenefitRiskAnalysis_Outcome",
          joinColumns = {@JoinColumn(name = "analysisId", referencedColumnName = "id")},
          inverseJoinColumns = {@JoinColumn(name = "outcomeId", referencedColumnName = "id")})
  private List<Outcome> selectedOutcomes;

  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  @JoinTable(name = "singleStudyBenefitRiskAnalysis_Intervention",
          joinColumns = {@JoinColumn(name = "analysisId", referencedColumnName = "id")},
          inverseJoinColumns = {@JoinColumn(name = "interventionId", referencedColumnName = "id")})
  private List<Intervention> selectedInterventions;

  public SingleStudyBenefitRiskAnalysis() {
  }

  public SingleStudyBenefitRiskAnalysis(Integer id, Integer projectId, String name, List<Outcome> selectedOutcomes, List<Intervention> selectedInterventions, String problem) {
    this.id = id;
    this.projectId = projectId;
    this.name = name;
    this.selectedOutcomes = selectedOutcomes == null ? this.selectedOutcomes : selectedOutcomes;
    this.selectedInterventions = selectedInterventions == null ? this.selectedInterventions : selectedInterventions;
    this.problem = problem;
  }

  public SingleStudyBenefitRiskAnalysis(Integer id, Integer projectId, String name, List<Outcome> selectedOutcomes, List<Intervention> selectedInterventions) {
    this(id, projectId, name, selectedOutcomes, selectedInterventions, null);
  }

  public SingleStudyBenefitRiskAnalysis(Integer projectId, String name, List<Outcome> selectedOutcomes, List<Intervention> selectedInterventions) {
    this(null, projectId, name, selectedOutcomes, selectedInterventions, null);
  }

  public SingleStudyBenefitRiskAnalysis(Integer projectId, String name, List<Outcome> selectedOutcomes, List<Intervention> selectedInterventions, String problem) {
    this(null, projectId, name, selectedOutcomes, selectedInterventions, problem);
  }

  public Integer getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public Integer getStudyId() {
    return studyId;
  }

  public List<Outcome> getSelectedOutcomes() {
    return selectedOutcomes == null ? Collections.EMPTY_LIST : Collections.unmodifiableList(selectedOutcomes);
  }

  public List<Intervention> getSelectedInterventions() {
    return selectedInterventions == null ? Collections.EMPTY_LIST : Collections.unmodifiableList(selectedInterventions);
  }

  public void setStudyId(Integer studyId) {
    this.studyId = studyId;
  }

  @JsonRawValue
  public String getProblem() {
    return problem;
  }

  @JsonDeserialize(using = ObjectToStringDeserializer.class)
  public void setProblem(String problem) {
    this.problem = problem;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    SingleStudyBenefitRiskAnalysis analysis = (SingleStudyBenefitRiskAnalysis) o;

    if (id != null ? !id.equals(analysis.id) : analysis.id != null) return false;
    if (!name.equals(analysis.name)) return false;
    if (problem != null ? !problem.equals(analysis.problem) : analysis.problem != null) return false;
    if (!getProjectId().equals(analysis.getProjectId())) return false;
    if (!selectedInterventions.equals(analysis.selectedInterventions)) return false;
    if (!selectedOutcomes.equals(analysis.selectedOutcomes)) return false;
    if (studyId != null ? !studyId.equals(analysis.studyId) : analysis.studyId != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = id != null ? id.hashCode() : 0;
    result = 31 * result + getProjectId().hashCode();
    result = 31 * result + name.hashCode();
    result = 31 * result + (problem != null ? problem.hashCode() : 0);
    result = 31 * result + (studyId != null ? studyId.hashCode() : 0);
    result = 31 * result + selectedOutcomes.hashCode();
    result = 31 * result + selectedInterventions.hashCode();
    return result;
  }

}