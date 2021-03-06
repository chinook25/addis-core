package org.drugis.addis.subProblems.service.impl;

import org.drugis.addis.scenarios.Scenario;
import org.drugis.addis.scenarios.repository.ScenarioRepository;
import org.drugis.addis.subProblems.SubProblem;
import org.drugis.addis.subProblems.repository.SubProblemRepository;
import org.drugis.addis.subProblems.service.SubProblemService;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

/**
 * Created by joris on 8-5-17.
 */
@Service
public class SubProblemServiceImpl implements SubProblemService {
  @Inject
  private SubProblemRepository subProblemRepository;

  @Inject
  private ScenarioRepository scenarioRepository;

  @Override
  public void createMCDADefaults(Integer projectId, Integer analysisId, String scenarioState) {
    SubProblem subProblem = subProblemRepository.create(analysisId, "{}", "Default");
    scenarioRepository.create(analysisId, subProblem.getId(), Scenario.DEFAULT_TITLE, scenarioState);
  }

  @Override
  public SubProblem createSubProblem(Integer analysisId, String definition, String title, String scenarioState) {
    SubProblem subProblem = subProblemRepository.create(analysisId, definition, title);
    scenarioRepository.create(analysisId, subProblem.getId(), Scenario.DEFAULT_TITLE, scenarioState);
    return subProblem;
  }
}
