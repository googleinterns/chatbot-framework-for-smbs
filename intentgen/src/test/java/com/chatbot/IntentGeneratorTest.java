package com.chatbot;

import static org.junit.Assert.assertTrue;

import com.google.cloud.dialogflow.v2.Intent.TrainingPhrase;
import com.google.cloud.dialogflow.v2.Intent.TrainingPhrase.Part;

import org.junit.Test;

public class IntentGeneratorTest 
{
  @Test
  public void testTrainingPhraseDecodingWithNoEntity() {
    String trainingStringWithNoEntities = "This is a training phrase with no entities";
    TrainingPhrase trainingPhrase = IntentGenerator.buildTrainingPhraseFromEncodedString(
        trainingStringWithNoEntities);
    assertTrue(trainingPhrase.getPartsCount() == 1 
        && trainingPhrase.getParts(0).getText().equals(trainingStringWithNoEntities));
  }

  @Test
  public void testTrainingPhraseDecodingWithEntity() {
    String trainingStringWithEntity = "He lives in |@sys.country;country;Peru|";
    TrainingPhrase trainingPhrase = IntentGenerator.buildTrainingPhraseFromEncodedString(
        trainingStringWithEntity);
    String textPart = trainingPhrase.getParts(0).getText();
    Part EntityPart = trainingPhrase.getParts(1);
    String entity = EntityPart.getEntityType().toString();
    String alias = EntityPart.getAlias();
    String text = EntityPart.getText();
    assertTrue( trainingPhrase.getPartsCount() == 2
        && textPart.equals("He lives in ")
        && entity.equals("@sys.country")
        && alias.equals("country")
        && text.equals("Peru"));
  }
}
