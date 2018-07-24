'use strict';
define([], function() {
  var dependencies = ['$state'];
  var PageTitleService = function(
    $state
  ) {
    var validStateControllerCombinations = {
      BenefitRisk: 'BenefitRiskController',
      'BenefitRiskCreationStep-1': 'BenefitRiskStep1Controller',
      'BenefitRiskCreationStep-2': 'BenefitRiskStep2Controller',
      dataset: 'DatasetController',
      'dataset.concepts':'ConceptsController',
      'dataset.study': 'StudyController',
      datasetHistory: 'DatasetHistoryController',
      datasets: 'DatasetsController',
      editReport: 'EditReportController',
      networkMetaAnalysis: 'NetworkMetaAnalysisContainerController',
      project: 'SingleProjectController',
      projectAnalyses: 'SingleProjectController',
      projectReport: 'SingleProjectController',
      projects: 'ProjectsController',
      search: 'SearchController',
      studyHistory: 'StudyHistoryController',
      versionedDataset: 'DatasetController',
      'versionedDataset.concepts': 'ConceptsController',
      'versionedDataset.study': 'StudyController'
    };

    function setPageTitle(controllerName, newTitle) {
      if (validStateControllerCombinations[$state.current.name] === controllerName) {
        document.title = newTitle;
      }
    }

    return {
      setPageTitle: setPageTitle
    };
  };

  return dependencies.concat(PageTitleService);
});
