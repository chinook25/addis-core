package org.drugis.addis.trialverse.service.impl;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.apache.commons.lang3.tuple.Pair;
import org.drugis.addis.trialverse.model.MeasurementMoment;
import org.drugis.addis.trialverse.model.trialdata.*;
import org.drugis.addis.trialverse.service.QueryResultMappingService;
import org.drugis.trialverse.util.Namespaces;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.*;

import static org.drugis.addis.trialverse.TrialverseUtilService.readValue;

/**
 * Created by connor on 8-4-16.
 */
@Service
public class QueryResultMappingServiceImpl implements QueryResultMappingService {

  private final static String SIMPLE_INTERVENTION_TYPE = "http://trials.drugis.org/ontology#SimpleDrugTreatment";
  private final static String FIXED_INTERVENTION_TYPE = "http://trials.drugis.org/ontology#FixedDoseDrugTreatment";
  private final static String TITRATED_INTERVENTION_TYPE = "http://trials.drugis.org/ontology#TitratedDoseDrugTreatment";

  @Override
  public Map<URI, TrialDataStudy> mapResultRowsToTrialDataStudy(JSONArray bindings) throws ReadValueException {
    Map<URI, TrialDataStudy> trialDataStudies = new HashMap<>();
    Map<URI, TrialDataArm> armCache = new HashMap<>();

    Set<Pair<URI, URI>> seenArmTreatmentCombinations = new HashSet<>();

    for (Object binding : bindings) {
      JSONObject row = new JSONObject((LinkedHashMap) binding);
      URI studyUri = readValue(row, "graph");
      TrialDataStudy trialDataStudy = trialDataStudies.get(studyUri);
      if (trialDataStudy == null) {
        String studyName = readValue(row, "studyName");
        trialDataStudy = new TrialDataStudy(studyUri, studyName, new ArrayList<>());
        URI defaultMeasurementMoment = readValue(row, "defaultMeasurementMoment");
        if (defaultMeasurementMoment != null) {
          trialDataStudy.setDefaultMeasurementMoment(defaultMeasurementMoment);
        }
        trialDataStudies.put(studyUri, trialDataStudy);
      }
      URI measurementMoment = readValue(row, "measurementMoment");
      if (measurementMoment != null) {
        trialDataStudy.addMeasurementMoment(new MeasurementMoment(readValue(row, "measurementMoment"), readValue(row, "measurementMomentLabel")));
      }

      URI drugInstance = readValue(row, "drugInstance");
      AbstractSemanticIntervention abstractSemanticIntervention = readSemanticIntervention(row, drugInstance);

      URI armUri = readValue(row, "arm");
      TrialDataArm trialDataArm = armCache.get(armUri);
      if (trialDataArm == null) {
        String armLabel = readValue(row, "armLabel");
        trialDataArm = new TrialDataArm(armUri, armLabel);
        armCache.put(armUri, trialDataArm);
        trialDataStudy.getArms().add(trialDataArm);
      }

      Pair<URI, URI> armPlusTreatment = Pair.of(armUri, readValue(row, "treatmentNode"));
      Boolean isPrimaryEpochTreatment = readValue(row, "isPrimaryEpoch");
      if (isPrimaryEpochTreatment && !seenArmTreatmentCombinations.contains(armPlusTreatment)) {
        seenArmTreatmentCombinations.add(armPlusTreatment);
        trialDataArm.addSemanticIntervention(abstractSemanticIntervention);
      }
      if (measurementMoment != null) {
        Measurement measurement = readMeasurement(row, studyUri, armUri);
        trialDataArm.addMeasurement(measurementMoment, measurement);
      }
    }
    return trialDataStudies;
  }

  private Measurement readMeasurement(JSONObject row, URI studyUri, URI armUri) throws ReadValueException {
    // absolute
    Double mean = getValueFor(row, "mean");
    Double stdDev = getValueFor(row, "stdDev");
    Double stdErr = getValueFor(row, "stdErr");
    Integer count = getValueFor(row, "count");
    Integer sampleSize = getValueFor(row, "sampleSize");
    Double exposure = getValueFor(row, "exposure");
    String survivalTimeScale = getValueFor(row, "survivalTimeScale");

    //contrast
    Double confidenceIntervalWidth = getValueFor(row, "confidenceIntervalWidth");
    Double confidenceIntervalUpperBound = getValueFor(row, "confidenceIntervalUpperBound");
    Double confidenceIntervalLowerBound = getValueFor(row, "confidenceIntervalLowerBound");
    Double meanDifference = getValueFor(row, "meanDifference");
    Double standardizedMeanDifference = getValueFor(row, "standardizedMeanDifference");
    Double oddsRatio = getValueFor(row, "oddsRatio");
    Double riskRatio = getValueFor(row, "riskRatio");
    Double hazardRatio = getValueFor(row, "hazardRatio");
    Boolean isLog = getValueFor(row, "isLog");
    URI referenceArm = getValueFor(row, "referenceArm");

    Double referenceStdErr = getValueFor(row, "referenceStandardError");

    URI variableUri = readValue(row, "outcomeInstance");
    URI variableConceptUri = readValue(row, "outcomeTypeUri");
    URI measurementTypeUri = readValue(row, "measurementType");
    return new MeasurementBuilder(studyUri, variableUri, variableConceptUri, armUri, measurementTypeUri)
            .setSampleSize(sampleSize)
            .setCount(count)
            .setStdDev(stdDev)
            .setStdErr(stdErr)
            .setMean(mean)
            .setExposure(exposure)
            .setSurvivalTimeScale(survivalTimeScale)
            .setConfidenceIntervalWidth(confidenceIntervalWidth)
            .setConfidenceIntervalLowerBound(confidenceIntervalLowerBound)
            .setConfidenceIntervalUpperBound(confidenceIntervalUpperBound)
            .setMeanDifference(meanDifference)
            .setStandardizedMeanDifference(standardizedMeanDifference)
            .setOddsRatio(oddsRatio)
            .setRiskRatio(riskRatio)
            .setHazardRatio(hazardRatio)
            .setLog(isLog)
            .setReferenceArm(referenceArm)
            .setReferenceStdErr(referenceStdErr)
            .build();
  }

  private <T> T getValueFor(JSONObject row, String valueName) throws ReadValueException {
    if (row.containsKey(valueName)) {
      return readValue(row, valueName);
    }
    return null;
  }

  @Override
  public CovariateStudyValue mapResultToCovariateStudyValue(JSONObject row) throws ReadValueException {
    URI studyUri = readValue(row, "graph");
    String populationCharacteristicCovariateKey = Namespaces.CONCEPT_NAMESPACE + readValue(row, "populationCharacteristicCovariateKey");
    Double value = row.containsKey("value") ? readValue(row, "value") : null;
    return new CovariateStudyValue(studyUri, populationCharacteristicCovariateKey, value);
  }

  private AbstractSemanticIntervention readSemanticIntervention(JSONObject row, URI drugInstance) throws ReadValueException {
    URI drugConcept = readValue(row, "drug");
    AbstractSemanticIntervention abstractSemanticIntervention;
    Object treatmentType = readValue(row, "treatmentType");
    if (treatmentType == null) {
      throw new RuntimeException("missing treatment type in trial data");
    }
    switch (treatmentType.toString()) {
      case FIXED_INTERVENTION_TYPE:
        Double fixedDoseValue = readValue(row, "fixedDoseValue");
        String fixedDoseDosingPeriodicity = readValue(row, "fixedDoseDosingPeriodicity");
        URI fixedDoseUnitConceptUri = readValue(row, "fixedDoseUnitConcept");
        String fixedDoseUnitLabel = readValue(row, "fixedDoseUnitLabel");
        Double fixedDoseUnitMultiplier = readValue(row, "fixedDoseUnitMultiplier");
        Dose fixedDose = new Dose(fixedDoseValue, fixedDoseDosingPeriodicity, fixedDoseUnitConceptUri, fixedDoseUnitLabel,
                fixedDoseUnitMultiplier);
        abstractSemanticIntervention = new FixedSemanticIntervention(drugInstance, drugConcept, fixedDose);
        break;
      case TITRATED_INTERVENTION_TYPE:
        Double maxDoseValue = readValue(row, "maxDoseValue");
        String maxDoseDosingPeriodicity = readValue(row, "maxDoseDosingPeriodicity");
        URI maxDoseUnitConceptUri = readValue(row, "maxDoseUnitConcept");
        String maxDoseUnitLabel = readValue(row, "maxDoseUnitLabel");
        Double maxDoseUnitMultiplier = readValue(row, "maxDoseUnitMultiplier");
        Dose maxDose = new Dose(maxDoseValue, maxDoseDosingPeriodicity, maxDoseUnitConceptUri, maxDoseUnitLabel,
                maxDoseUnitMultiplier);
        Double minDoseValue = readValue(row, "minDoseValue");
        String minDoseDosingPeriodicity = readValue(row, "minDoseDosingPeriodicity");
        URI minDoseUnitConceptUri = readValue(row, "minDoseUnitConcept");
        String minDoseUnitLabel = readValue(row, "minDoseUnitLabel");
        Double minDoseUnitMultiplier = readValue(row, "minDoseUnitMultiplier");
        Dose mixDose = new Dose(minDoseValue, minDoseDosingPeriodicity, minDoseUnitConceptUri, minDoseUnitLabel,
                minDoseUnitMultiplier);
        abstractSemanticIntervention = new TitratedSemanticIntervention(drugInstance, drugConcept, mixDose, maxDose);
        break;
      default:
        abstractSemanticIntervention = new SimpleSemanticIntervention(drugInstance, drugConcept);
    }
    return abstractSemanticIntervention;
  }
}
