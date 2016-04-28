package org.drugis.addis.problems;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.tuple.Pair;
import org.drugis.addis.analyses.*;
import org.drugis.addis.analyses.repository.AnalysisRepository;
import org.drugis.addis.analyses.repository.SingleStudyBenefitRiskAnalysisRepository;
import org.drugis.addis.analyses.service.AnalysisService;
import org.drugis.addis.covariates.Covariate;
import org.drugis.addis.covariates.CovariateRepository;
import org.drugis.addis.exception.ResourceDoesNotExistException;
import org.drugis.addis.interventions.model.AbstractIntervention;
import org.drugis.addis.interventions.model.SimpleIntervention;
import org.drugis.addis.interventions.repository.InterventionRepository;
import org.drugis.addis.interventions.service.impl.InvalidTypeForDoseCheckException;
import org.drugis.addis.models.Model;
import org.drugis.addis.models.exceptions.InvalidModelException;
import org.drugis.addis.models.repository.ModelRepository;
import org.drugis.addis.outcomes.Outcome;
import org.drugis.addis.outcomes.repository.OutcomeRepository;
import org.drugis.addis.patavitask.PataviTask;
import org.drugis.addis.patavitask.repository.PataviTaskRepository;
import org.drugis.addis.problems.model.*;
import org.drugis.addis.problems.service.ProblemService;
import org.drugis.addis.problems.service.impl.PerformanceTableBuilder;
import org.drugis.addis.problems.service.impl.ProblemServiceImpl;
import org.drugis.addis.problems.service.model.AbstractMeasurementEntry;
import org.drugis.addis.problems.service.model.ContinuousMeasurementEntry;
import org.drugis.addis.projects.Project;
import org.drugis.addis.projects.repository.ProjectRepository;
import org.drugis.addis.security.Account;
import org.drugis.addis.trialverse.model.SemanticInterventionUriAndName;
import org.drugis.addis.trialverse.model.SemanticVariable;
import org.drugis.addis.trialverse.model.emun.CovariateOption;
import org.drugis.addis.trialverse.model.emun.CovariateOptionType;
import org.drugis.addis.trialverse.model.trialdata.*;
import org.drugis.addis.trialverse.service.MappingService;
import org.drugis.addis.trialverse.service.TrialverseService;
import org.drugis.addis.trialverse.service.TriplestoreService;
import org.drugis.addis.trialverse.service.impl.ReadValueException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

;

/**
 * Created by daan on 3/21/14.
 */
public class ProblemServiceTest {

  @Mock
  SingleStudyBenefitRiskAnalysisRepository singleStudyBenefitRiskAnalysisRepository;

  @Mock
  AnalysisRepository analysisRepository;

  @Mock
  ProjectRepository projectRepository;

  @Mock
  CovariateRepository covariateRepository;

  @Mock
  PerformanceTableBuilder performanceTablebuilder;

  @Mock
  InterventionRepository interventionRepository;

  @Mock
  ModelRepository modelRepository;

  @Mock
  OutcomeRepository outcomeRepository;

  @Mock
  PataviTaskRepository pataviTaskRepository;

  @Mock
  MappingService mappingService;

  @Mock
  TrialverseService trialverseService;

  @Mock
  private TriplestoreService triplestoreService;

  @Mock
  AnalysisService analysisService;

  @InjectMocks
  ProblemService problemService;

  private final String namespaceUid = "UID 1";
  private final String versionedUuid = "versionedUuid";
  private final Integer projectId = 101;
  private final Integer analysisId = 202;
  private final String projectDatasetUid = "projectDatasetUid";
  private final String projectDatasetVersion = "projectDatasetVersion";
  private final Account owner = mock(Account.class);
  private final Project project = new Project(projectId, owner, "project name", "desc", projectDatasetUid, projectDatasetVersion);
  ;
  private final SemanticVariable semanticOutcome = new SemanticVariable(URI.create("semanticOutcomeUri"), "semanticOutcomeLabel");
  private final Outcome outcome = new Outcome(303, project.getId(), "outcome name", "moti", semanticOutcome);
  private final URI fluoxConceptUri = URI.create("fluoxConceptUri");
  private final SemanticInterventionUriAndName fluoxConcept = new SemanticInterventionUriAndName(fluoxConceptUri, "fluox concept");
  private final Integer fluoxInterventionId = 401;
  private final AbstractIntervention fluoxIntervention = new SimpleIntervention(fluoxInterventionId, project.getId(),
          "fluoxetine", "moti", fluoxConcept.getUri(), fluoxConcept.getLabel());
  private final URI paroxConceptUri = URI.create("paroxConceptUri");
  private final SemanticInterventionUriAndName paroxConcept = new SemanticInterventionUriAndName(paroxConceptUri, "parox concept");
  private final Integer paroxInterventionId = 402;
  private final AbstractIntervention paroxIntervention = new SimpleIntervention(paroxInterventionId, project.getId(),
          "paroxetine", "moti", paroxConcept.getUri(), paroxConcept.getLabel());
  private final URI sertraConceptUri = URI.create("sertraConceptUri");
  private final SemanticInterventionUriAndName sertraConcept = new SemanticInterventionUriAndName(sertraConceptUri, "sertra concept");
  private final Integer sertraInterventionId = 403;
  private final AbstractIntervention sertraIntervention = new SimpleIntervention(sertraInterventionId, project.getId(),
          "sertraline", "moti", sertraConcept.getUri(), sertraConcept.getLabel());
  private final List<AbstractIntervention> allProjectInterventions = Arrays.asList(fluoxIntervention, paroxIntervention, sertraIntervention);

  @Before
  public void setUp() throws URISyntaxException, ResourceDoesNotExistException {
    problemService = new ProblemServiceImpl();
    MockitoAnnotations.initMocks(this);
    when(mappingService.getVersionedUuid(namespaceUid)).thenReturn(versionedUuid);
    when(projectRepository.get(projectId)).thenReturn(project);
    when(interventionRepository.query(project.getId())).thenReturn(allProjectInterventions);
  }

  @After
  public void cleanUp() throws URISyntaxException {
    verifyNoMoreInteractions(analysisRepository, projectRepository, singleStudyBenefitRiskAnalysisRepository,
            interventionRepository, trialverseService, triplestoreService, mappingService, modelRepository);
  }

  @Test
  public void testGetSingleStudyBenefitRiskProblem() throws ResourceDoesNotExistException, URISyntaxException, SQLException, IOException, ReadValueException, InvalidTypeForDoseCheckException {
    URI secondOutcomeUri = URI.create("http://secondSemantic");
    SemanticVariable secondSemanticOutcome = new SemanticVariable(secondOutcomeUri, "second semantic outcome");
    Outcome secondOutcome = new Outcome(-303, projectId, "second outcome", "very", secondSemanticOutcome);
    List<Outcome> outcomes = Arrays.asList(outcome, secondOutcome);
    //include interventions: fluox and sertra
    InterventionInclusion fluoxInclusion = new InterventionInclusion(analysisId, fluoxIntervention.getId());
    InterventionInclusion sertraInclusion = new InterventionInclusion(analysisId, sertraIntervention.getId());
    List<InterventionInclusion> interventionInclusions = Arrays.asList(fluoxInclusion, sertraInclusion);
    SingleStudyBenefitRiskAnalysis singleStudyAnalysis = new SingleStudyBenefitRiskAnalysis(analysisId, projectId, "single study analysis", outcomes, interventionInclusions);
    when(analysisRepository.get(analysisId)).thenReturn(singleStudyAnalysis);

    URI daanEtAlUri = URI.create("DaanEtAlUri");
    URI daanEtAlFluoxInstance = URI.create("daanEtAlFluoxInstance");
    URI daanEtAlFluoxArmUri = URI.create("daanEtAlFluoxArm");
    int daanEtAlFluoxSampleSize = 20;
    int daanEtAlFluoxRate = 30;
    URI variableUri = outcome.getSemanticOutcomeUri();
    URI variableConceptUri = outcome.getSemanticOutcomeUri();
    Measurement daanEtAlFluoxMeasurement1 = new Measurement(daanEtAlUri, variableUri, variableConceptUri, daanEtAlFluoxArmUri,
            daanEtAlFluoxSampleSize, daanEtAlFluoxRate, null, null);
    Measurement daanEtAlFluoxMeasurement2 = new Measurement(daanEtAlUri, secondOutcomeUri, variableConceptUri, daanEtAlFluoxArmUri,
            daanEtAlFluoxSampleSize, daanEtAlFluoxRate, null, null);
    AbstractSemanticIntervention simpleSemanticFluoxIntervention = new SimpleSemanticIntervention(daanEtAlFluoxInstance, fluoxConceptUri);

    TrialDataArm daanEtAlFluoxArm = new TrialDataArm(daanEtAlFluoxArmUri, "daanEtAlFluoxArm", daanEtAlFluoxInstance,
            simpleSemanticFluoxIntervention);
    daanEtAlFluoxArm.addMeasurement(daanEtAlFluoxMeasurement1);
    daanEtAlFluoxArm.addMeasurement(daanEtAlFluoxMeasurement2);

    URI daanEtAlSertraInstance = URI.create("daanEtAlSertraInstance");
    URI daanEtAlSertraArmUri = URI.create("daanEtAlSertraArm");
    int daanEtAlSertraSampleSize = 40;
    int daanEtAlSertraRate = 5;
    Measurement daanEtAlSertraMeasurement1 = new Measurement(daanEtAlUri, variableUri, variableConceptUri, daanEtAlSertraArmUri,
            daanEtAlSertraSampleSize, daanEtAlSertraRate, null, null);
    Measurement daanEtAlSertraMeasurement2 = new Measurement(daanEtAlUri, secondOutcomeUri, variableConceptUri, daanEtAlSertraArmUri,
            daanEtAlSertraSampleSize, daanEtAlSertraRate, null, null);
    AbstractSemanticIntervention simpleSemanticSertraIntervention = new SimpleSemanticIntervention(daanEtAlSertraInstance, sertraConceptUri);

    TrialDataArm daanEtAlSertraArm = new TrialDataArm(daanEtAlSertraArmUri, "daanEtAlSertraArm", daanEtAlSertraInstance,
            simpleSemanticSertraIntervention);
    daanEtAlSertraArm.addMeasurement(daanEtAlSertraMeasurement1);
    daanEtAlSertraArm.addMeasurement(daanEtAlSertraMeasurement2);

    URI daanEtAlParoxInstance = URI.create("daanEtAlParoxInstance");
    URI daanEtAlParoxArmUri = URI.create("daanEtAlParoxArm");
    int daanEtAlParoxSampleSize = 40;
    int daanEtAlParoxRate = 5;
    Measurement daanEtAlParoxMeasurement1 = new Measurement(daanEtAlUri, variableUri, variableConceptUri, daanEtAlParoxArmUri,
            daanEtAlParoxSampleSize, daanEtAlParoxRate, null, null);
    Measurement daanEtAlParoxMeasurement2 = new Measurement(daanEtAlUri, secondOutcomeUri, variableConceptUri, daanEtAlParoxArmUri,
            daanEtAlParoxSampleSize, daanEtAlParoxRate, null, null);
    AbstractSemanticIntervention simpleSemanticParoxIntervention = new SimpleSemanticIntervention(daanEtAlParoxInstance, paroxConceptUri);

    TrialDataArm unmatchedDaanEtAlParoxArm = new TrialDataArm(daanEtAlParoxArmUri, "daanEtAlParoxArm", daanEtAlParoxInstance,
            simpleSemanticParoxIntervention);
    unmatchedDaanEtAlParoxArm.addMeasurement(daanEtAlParoxMeasurement1);
    unmatchedDaanEtAlParoxArm.addMeasurement(daanEtAlParoxMeasurement2);

    // add matching result to arms
    daanEtAlFluoxArm.setMatchedProjectInterventionIds(Collections.singleton(fluoxIntervention.getId()));
    daanEtAlSertraArm.setMatchedProjectInterventionIds(Collections.singleton(sertraIntervention.getId()));
    List<TrialDataArm> daanEtAlArms = Arrays.asList(daanEtAlFluoxArm, daanEtAlSertraArm, unmatchedDaanEtAlParoxArm);
    TrialDataStudy daanEtAl = new TrialDataStudy(daanEtAlUri, "Daan et al", daanEtAlArms);

    // actually set study in analysis
    singleStudyAnalysis.setStudyGraphUri(daanEtAlUri);

    List<TrialDataStudy> studyResult = Collections.singletonList(daanEtAl);
    Set<URI> outcomeUris = new HashSet<>(Arrays.asList(outcome.getSemanticOutcomeUri(), secondOutcome.getSemanticOutcomeUri()));
    Set<URI> interventionUris = new HashSet<>(Arrays.asList(fluoxIntervention.getSemanticInterventionUri(), sertraIntervention.getSemanticInterventionUri()));

    when(triplestoreService.getSingleStudyData(versionedUuid, daanEtAl.getStudyUri(), project.getDatasetVersion(), outcomeUris, interventionUris)).thenReturn(studyResult);

    AbstractMeasurementEntry measurementEntry = mock(ContinuousMeasurementEntry.class);
    List<AbstractMeasurementEntry> performanceTable = Collections.singletonList(measurementEntry);

    List<AbstractIntervention> includedInterventions = Arrays.asList(fluoxIntervention, sertraIntervention);
    when(analysisService.getIncludedInterventions(singleStudyAnalysis)).thenReturn(includedInterventions);
    when(triplestoreService.findMatchingIncludedInterventions(includedInterventions, daanEtAlFluoxArm)).thenReturn(ImmutableSet.of(fluoxIntervention));
    when(triplestoreService.findMatchingIncludedInterventions(includedInterventions, daanEtAlSertraArm)).thenReturn(ImmutableSet.of(sertraIntervention));
    when(triplestoreService.findMatchingIncludedInterventions(includedInterventions, unmatchedDaanEtAlParoxArm)).thenReturn(Collections.emptySet());

    when(performanceTablebuilder.build(any())).thenReturn(performanceTable);
    when(mappingService.getVersionedUuid(project.getNamespaceUid())).thenReturn(versionedUuid);

    // execute
    SingleStudyBenefitRiskProblem actualProblem = (SingleStudyBenefitRiskProblem) problemService.getProblem(projectId, analysisId);

    verify(projectRepository).get(projectId);
    verify(analysisRepository).get(analysisId);
    verify(triplestoreService).getSingleStudyData(versionedUuid, daanEtAl.getStudyUri(), project.getDatasetVersion(), outcomeUris, interventionUris);
    verify(triplestoreService).findMatchingIncludedInterventions(includedInterventions, daanEtAlFluoxArm);
    verify(triplestoreService).findMatchingIncludedInterventions(includedInterventions, daanEtAlSertraArm);
    verify(triplestoreService).findMatchingIncludedInterventions(includedInterventions, unmatchedDaanEtAlParoxArm);
    verify(performanceTablebuilder).build(any());
    verify(mappingService).getVersionedUuid(project.getNamespaceUid());
    verify(interventionRepository).query(project.getId());

    Pair<Measurement, URI> pair1 = Pair.of(daanEtAlFluoxMeasurement1, daanEtAlFluoxArm.getDrugInstance());
    Pair<Measurement, URI> pair2 = Pair.of(daanEtAlFluoxMeasurement2, daanEtAlFluoxArm.getDrugInstance());
    Pair<Measurement, URI> pair3 = Pair.of(daanEtAlSertraMeasurement1, daanEtAlSertraArm.getDrugInstance());
    Pair<Measurement, URI> pair4 = Pair.of(daanEtAlSertraMeasurement2, daanEtAlSertraArm.getDrugInstance());
    Set<Pair<Measurement, URI>> instancePairs = ImmutableSet.of(pair1, pair2, pair3, pair4);
    verify(performanceTablebuilder).build(instancePairs);

    assertNotNull(actualProblem);
    assertNotNull(actualProblem.getTitle());
    assertEquals(singleStudyAnalysis.getTitle(), actualProblem.getTitle());
    assertNotNull(actualProblem.getAlternatives());
    assertNotNull(actualProblem.getCriteria());

    Map<URI, CriterionEntry> actualCriteria = actualProblem.getCriteria();
    assertTrue(actualCriteria.keySet().contains(variableUri));
    assertTrue(actualCriteria.keySet().contains(secondOutcomeUri));
  }

  @Test
  public void testGetNmaProblem() throws URISyntaxException, SQLException, IOException, ReadValueException, ResourceDoesNotExistException, InvalidTypeForDoseCheckException {

    // analysis
    NetworkMetaAnalysis networkMetaAnalysis = new NetworkMetaAnalysis(analysisId, project.getId(), "nma title", outcome);
    when(analysisRepository.get(networkMetaAnalysis.getId())).thenReturn(networkMetaAnalysis);

    //include interventions: fluox and sertra
    InterventionInclusion fluoxInclusion = new InterventionInclusion(networkMetaAnalysis.getId(), fluoxIntervention.getId());
    InterventionInclusion sertraInclusion = new InterventionInclusion(networkMetaAnalysis.getId(), sertraIntervention.getId());
    HashSet<InterventionInclusion> interventionInclusions = new HashSet<>(Arrays.asList(fluoxInclusion, sertraInclusion));
    networkMetaAnalysis.updateIncludedInterventions(interventionInclusions);

    // covariates
    Integer includedCovariateId = -1;
    Integer excludedCovariateId= -2;
    String includedCovariateDefinitionKey = CovariateOption.ALLOCATION_RANDOMIZED.toString();
    Covariate includedCovariate = new Covariate(includedCovariateId, project.getId(), "isRandomised", "mot",
            includedCovariateDefinitionKey, CovariateOptionType.STUDY_CHARACTERISTIC);
    Covariate notIncludedCovariate = new Covariate(excludedCovariateId, project.getId(), "age", "mot", "ageUri", CovariateOptionType.POPULATION_CHARACTERISTIC);
    Collection<Covariate> allProjectCovariates = Arrays.asList(includedCovariate, notIncludedCovariate);
    when(covariateRepository.findByProject(project.getId())).thenReturn(allProjectCovariates);

    Set<CovariateInclusion> covariateInclusions = new HashSet<>(Collections.singletonList(new CovariateInclusion(networkMetaAnalysis.getId(), includedCovariateId)));
    networkMetaAnalysis.updateIncludedCovariates(covariateInclusions);

    // trial data DaanetAl study
    URI daanEtAlUri = URI.create("DaanEtAlUri");
    URI daanEtAlFluoxInstance = URI.create("daanEtAlFluoxInstance");
    URI daanEtAlFluoxArmUri = URI.create("daanEtAlFluoxArm");
    int daanEtAlFluoxSampleSize = 20;
    int daanEtAlFluoxRate = 30;
    URI variableUri = outcome.getSemanticOutcomeUri();
    URI variableConceptUri = outcome.getSemanticOutcomeUri();
    Measurement daanEtAlFluoxMeasurement = new Measurement(daanEtAlUri, variableUri, variableConceptUri, daanEtAlFluoxArmUri,
            daanEtAlFluoxSampleSize, daanEtAlFluoxRate, null, null);
    AbstractSemanticIntervention simpleSemanticFluoxIntervention = new SimpleSemanticIntervention(daanEtAlFluoxInstance, fluoxConceptUri);

    TrialDataArm daanEtAlFluoxArm = new TrialDataArm(daanEtAlFluoxArmUri, "daanEtAlFluoxArm", daanEtAlFluoxInstance,
            simpleSemanticFluoxIntervention);
    daanEtAlFluoxArm.addMeasurement(daanEtAlFluoxMeasurement);

    URI daanEtAlSertraInstance = URI.create("daanEtAlSertraInstance");
    URI daanEtAlSertraArmUri = URI.create("daanEtAlSertraArm");
    int daanEtAlSertraSampleSize = 40;
    int daanEtAlSertraRate = 5;
    Measurement daanEtAlSertraMeasurement = new Measurement(daanEtAlUri, variableUri, variableConceptUri, daanEtAlSertraArmUri,
            daanEtAlSertraSampleSize, daanEtAlSertraRate, null, null);
    AbstractSemanticIntervention simpleSemanticSertraIntervention = new SimpleSemanticIntervention(daanEtAlSertraInstance, sertraConceptUri);

    TrialDataArm daanEtAlSertraArm = new TrialDataArm(daanEtAlSertraArmUri, "daanEtAlSertraArm", daanEtAlSertraInstance,
            simpleSemanticSertraIntervention);
    daanEtAlSertraArm.addMeasurement(daanEtAlSertraMeasurement);

    URI daanEtAlExcludedArmUri = URI.create("excludeme");
    Measurement daanEtAlExcludedMeasurement = new Measurement(daanEtAlUri, variableUri, variableConceptUri, daanEtAlExcludedArmUri,
            daanEtAlSertraSampleSize, daanEtAlSertraRate, null, null);
    TrialDataArm excludedArm = new TrialDataArm(daanEtAlSertraArmUri, "excludedArm", daanEtAlSertraInstance,
            simpleSemanticSertraIntervention);
    excludedArm.addMeasurement(daanEtAlExcludedMeasurement);

    // exclude arms
    Set<ArmExclusion> excludedArms = new HashSet<>(Collections.singletonList(new ArmExclusion(networkMetaAnalysis.getId(), daanEtAlExcludedArmUri)));
    networkMetaAnalysis.updateArmExclusions(excludedArms);

    // add matching result to arms
    daanEtAlFluoxArm.setMatchedProjectInterventionIds(new HashSet<>(Collections.singletonList(fluoxIntervention.getId())));
    daanEtAlSertraArm.setMatchedProjectInterventionIds(new HashSet<>(Collections.singletonList(sertraIntervention.getId())));
    List<TrialDataArm> daanEtAlArms = Arrays.asList(daanEtAlFluoxArm, daanEtAlSertraArm, excludedArm);
    TrialDataStudy daanEtAl = new TrialDataStudy(daanEtAlUri, "Daan et al", daanEtAlArms);
    Double randomisedValue = 30d;
    daanEtAl.addCovariateValue(new CovariateStudyValue(daanEtAlUri, includedCovariate.getDefinitionKey(), randomisedValue));
    List<TrialDataStudy> trialDataStudies = Collections.singletonList(daanEtAl);
    when(analysisService.buildEvidenceTable(project.getId(), networkMetaAnalysis.getId())).thenReturn(trialDataStudies);

    final AbstractProblem problem = problemService.getProblem(project.getId(), networkMetaAnalysis.getId());

    assertNotNull(problem);
    assertTrue(problem instanceof NetworkMetaAnalysisProblem);
    NetworkMetaAnalysisProblem networkProblem = (NetworkMetaAnalysisProblem) problem;
    assertEquals(interventionInclusions.size(), networkProblem.getTreatments().size());

    List<AbstractNetworkMetaAnalysisProblemEntry> entries = networkProblem.getEntries();
    assertNotNull(entries);
    assertEquals(2, entries.size());
    RateNetworkMetaAnalysisProblemEntry fluoxEntry = (RateNetworkMetaAnalysisProblemEntry) entries.get(0);
    assertEquals(fluoxIntervention.getId(), fluoxEntry.getTreatment());
    RateNetworkMetaAnalysisProblemEntry sertraEntry = (RateNetworkMetaAnalysisProblemEntry) entries.get(1);
    assertEquals(sertraIntervention.getId(), sertraEntry.getTreatment());

    TreatmentEntry fluoxTreatmentEntry = new TreatmentEntry(fluoxIntervention.getId(), fluoxIntervention.getName());
    TreatmentEntry sertraTreatmentEntry = new TreatmentEntry(sertraIntervention.getId(), sertraIntervention.getName());
    List<TreatmentEntry> expectedTreatments = Arrays.asList(fluoxTreatmentEntry, sertraTreatmentEntry);
    assertEquals(expectedTreatments, networkProblem.getTreatments());

    Map<String, Map<String, Double>> studyLevelCovariates = networkProblem.getStudyLevelCovariates();
    assertEquals(covariateInclusions.size(), studyLevelCovariates.size());
    assertEquals(daanEtAl.getName(), studyLevelCovariates.keySet().toArray()[0]);
    Map<String, Double> covariateEntry = (Map<String, Double>) studyLevelCovariates.values().toArray()[0];
    assertEquals(includedCovariate.getName(), covariateEntry.keySet().toArray()[0]);


    verify(projectRepository).get(project.getId());
    verify(analysisRepository).get(networkMetaAnalysis.getId());
    verify(interventionRepository).query(project.getId());
    verify(covariateRepository).findByProject(project.getId());
  }

  @Test
  public void testGetMetaBRProblem() throws ResourceDoesNotExistException, SQLException, IOException, URISyntaxException, InvalidModelException, ReadValueException, InvalidTypeForDoseCheckException {

    String version = "version 1";
    Integer projectId = 1;
    Integer analysisId = 2;
    String title = "title";

    Project project = mock(Project.class);
    when(project.getId()).thenReturn(projectId);
    when(project.getNamespaceUid()).thenReturn(namespaceUid);
    when(project.getDatasetVersion()).thenReturn(version);

    Set<InterventionInclusion> includedAlternatives = new HashSet<>(3);
    SimpleIntervention intervention1 = new SimpleIntervention(11, projectId, "fluox", "", new SemanticInterventionUriAndName(URI.create("uri1"), "fluoxS"));
    SimpleIntervention intervention2 = new SimpleIntervention(12, projectId, "parox", "", new SemanticInterventionUriAndName(URI.create("uri2"), "paroxS"));
    SimpleIntervention intervention3 = new SimpleIntervention(13, projectId, "sertr", "", new SemanticInterventionUriAndName(URI.create("uri3"), "sertrS"));
    includedAlternatives.addAll(Arrays.asList(new InterventionInclusion(analysisId, 11), new InterventionInclusion(analysisId, 12), new InterventionInclusion(analysisId, 13)));

    SimpleIntervention intervention4 = new SimpleIntervention(14, projectId, "foo", "", new SemanticInterventionUriAndName(URI.create("uri4"), "fooS"));
    List<AbstractIntervention> interventions = Arrays.asList(intervention1, intervention2, intervention3, intervention4);

    PataviTask pataviTask1 = new PataviTask(41, "gemtc", "problem");
    PataviTask pataviTask2 = new PataviTask(42, "gemtc", "problem");
    List<PataviTask> pataviTasks = Arrays.asList(pataviTask1, pataviTask2);

    Model model1 = new Model.ModelBuilder(analysisId, "model 1")
            .id(71)
            .modelType(Model.NETWORK_MODEL_TYPE)
            .taskId(pataviTask1.getId())
            .link(Model.LINK_IDENTITY)
            .build();
    Model model2 = new Model.ModelBuilder(analysisId, "model 2")
            .id(72)
            .modelType(Model.NETWORK_MODEL_TYPE)
            .taskId(pataviTask2.getId())
            .link(Model.LINK_CLOGLOG)
            .build();
    List<Model> models = Arrays.asList(model1, model2);

    Outcome outcome1 = new Outcome(21, projectId, "ham", "", new SemanticVariable(URI.create("outUri1"), "hamS"));
    Outcome outcome2 = new Outcome(22, projectId, "headache", "", new SemanticVariable(URI.create("outUri2"), "headacheS"));
    List<Outcome> outcomes = Arrays.asList(outcome1, outcome2);


    MetaBenefitRiskAnalysis analysis = new MetaBenefitRiskAnalysis(analysisId, projectId, title, includedAlternatives);
    Integer nma1Id = 31;
    Integer nma2Id = 32;
    String baseline1JsonString = "{\n" +
            "\"scale\": \"log odds\",\n" +
            "\"mu\": 4,\n" +
            "\"sigma\": 6,\n" +
            "\"name\": \"fluox\"\n" +
            "}";
    String baseline2JsonString ="{\n" +
            "\"scale\": \"log odds\",\n" +
            "\"mu\": 4,\n" +
            "\"sigma\": 6,\n" +
            "\"name\": \"fluox\"\n" +
            "}";;

    MbrOutcomeInclusion inclusion1 = new MbrOutcomeInclusion(analysisId, outcome1.getId(), nma1Id, model1.getId());
    inclusion1.setBaseline(baseline1JsonString);
    MbrOutcomeInclusion inclusion2 = new MbrOutcomeInclusion(analysisId, outcome2.getId(), nma2Id, model2.getId());
    inclusion2.setBaseline(baseline2JsonString);
    List<MbrOutcomeInclusion> outcomeInclusions = Arrays.asList(inclusion1, inclusion2);
    analysis.setMbrOutcomeInclusions(outcomeInclusions);

    ObjectMapper om = new ObjectMapper();
    String results1 = "{\n" +
            "  \"multivariateSummary\": {\n" +
            "    \"11\": {\n" +
            "      \"mu\": {\n" +
            "        \"d.11.12\": 0.55302,\n" +
            "        \"d.11.13\": 0.46622\n" +
            "      },\n" +
            "      \"sigma\": {\n" +
            "        \"d.11.12\": {\n" +
            "          \"d.11.12\": 74.346,\n" +
            "          \"d.11.13\": 1.9648\n" +
            "        },\n" +
            "        \"d.11.13\": {\n" +
            "          \"d.11.12\": 1.9648,\n" +
            "          \"d.11.13\": 74.837\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}\n";

    Map<Integer, JsonNode> results = new HashMap<>();
    JsonNode task1Results = om.readTree(results1);
    JsonNode task2Results = om.readTree(results1);
    results.put(pataviTask1.getId(), task1Results);
    results.put(pataviTask2.getId(), task2Results);

    List<Integer> modelIds = Arrays.asList(model2.getId(), model1.getId());
    List<Integer> outcomeIds = Arrays.asList(outcome2.getId(), outcome1.getId());
    when(projectRepository.get(projectId)).thenReturn(project);
    when(modelRepository.get(modelIds)).thenReturn(models);
    when(outcomeRepository.get(projectId, outcomeIds)).thenReturn(outcomes);
    when(analysisRepository.get(analysisId)).thenReturn(analysis);
    List<Integer> taskIds = Arrays.asList(model1.getTaskId(), model2.getTaskId());
    when(pataviTaskRepository.findByIds(taskIds)).thenReturn(pataviTasks);
    when(pataviTaskRepository.getResults(taskIds)).thenReturn(results);
    when(interventionRepository.query(projectId)).thenReturn(interventions);

    MetaBenefitRiskProblem problem = (MetaBenefitRiskProblem) problemService.getProblem(projectId, analysisId);

    verify(projectRepository).get(projectId);
    verify(modelRepository).get(modelIds);
    verify(outcomeRepository).get(projectId, outcomeIds);
    verify(analysisRepository).get(analysisId);
    verify(pataviTaskRepository).findByIds(taskIds);
    verify(pataviTaskRepository).getResults(taskIds);
    verify(interventionRepository).query(projectId);

    assertEquals(3, problem.getAlternatives().size());
    assertEquals(2, problem.getCriteria().size());
    assertEquals(2, problem.getPerformanceTable().size());
    assertEquals("relative-cloglog-normal", problem.getPerformanceTable().get(0).getPerformance().getType());
    assertEquals("relative-normal", problem.getPerformanceTable().get(1).getPerformance().getType());
    List<List<Double>> expectedDataHeadache = Arrays.asList(Arrays.asList(0.0, 0.0, 0.0), Arrays.asList(0.0, 74.346, 1.9648), Arrays.asList(0.0, 1.9648, 74.837));
    assertEquals(expectedDataHeadache ,problem.getPerformanceTable().get(0).getPerformance().getParameters().getRelative().getCov().getData());
    List<List<Double>> expectedDataHam = Arrays.asList(Arrays.asList(0.0, 0.0, 0.0), Arrays.asList(0.0, 74.346, 1.9648), Arrays.asList(0.0, 1.9648, 74.837));
    assertEquals(expectedDataHam ,problem.getPerformanceTable().get(1).getPerformance().getParameters().getRelative().getCov().getData());
  }

}
