package org.drugis.addis.interventions.model;

import org.drugis.addis.interventions.controller.viewAdapter.AbstractInterventionViewAdapter;
import org.drugis.addis.interventions.controller.viewAdapter.InterventionSetViewAdapter;

import javax.persistence.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Daan on 06/10/2016.
 */
@Entity
@PrimaryKeyJoinColumn(name = "interventionSetId", referencedColumnName = "id")
public class InterventionSet extends AbstractIntervention {
  @ElementCollection
  @CollectionTable(
      name="InterventionSetItem",
      joinColumns=@JoinColumn(name="interventionSetId")
  )
  @Column(name="interventionId")
  private Set<Integer> interventionIds = new HashSet<>();

  public InterventionSet() {
    super();
  }

  public InterventionSet(Integer id, Integer project, String name, String motivation, Set<Integer> interventionIds) {
    super(id, project, name, motivation);
    this.interventionIds = interventionIds;
  }

  public Set<Integer> getInterventionIds() {
    return Collections.unmodifiableSet(this.interventionIds);
  }

  @Override
  public AbstractInterventionViewAdapter toViewAdapter() {
    return new InterventionSetViewAdapter(this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    InterventionSet that = (InterventionSet) o;

    return interventionIds.equals(that.interventionIds);

  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + interventionIds.hashCode();
    return result;
  }
}