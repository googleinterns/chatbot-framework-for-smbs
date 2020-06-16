package com.chatbot;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import com.google.cloud.dialogflow.v2.Intent.TrainingPhrase;
import com.google.cloud.dialogflow.v2.Intent.TrainingPhrase.Part;
import org.junit.Test;

public class IntentGeneratorTest {
  @Test
  public void testTrainingPhraseDecodingWithNoEntity() {
    String trainingStringWithNoEntities = "This is a training phrase with no entities";
    TrainingPhrase trainingPhrase = IntentGenerator.buildTrainingPhraseFromEncodedString(trainingStringWithNoEntities);
    // since there are no entities there should be only one part in this training
    // phrase
    assertEquals(trainingPhrase.getPartsCount(), 1);
    assertEquals(trainingPhrase.getParts(0).getText(), trainingStringWithNoEntities);
  }

  @Test
  public void testTrainingPhraseDecodingWithEntity() {
    String trainingStringWithEntity = "He lives in |@sys.country;country;Peru|";
    TrainingPhrase trainingPhrase = IntentGenerator.buildTrainingPhraseFromEncodedString(trainingStringWithEntity);
    String textPart = trainingPhrase.getParts(0).getText();
    Part EntityPart = trainingPhrase.getParts(1);
    String entity = EntityPart.getEntityType().toString();
    String alias = EntityPart.getAlias();
    String text = EntityPart.getText();
    assertEquals(trainingPhrase.getPartsCount(), 2);
    assertEquals(textPart, "He lives in ");
    assertEquals(entity, "@sys.country");
    assertEquals(alias, "country");
    assertEquals(text, "Peru");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testIncorrectEntityEncodgin() throws IllegalArgumentException {
    String trainingStringWithEntity = "He lives in |@sys.country;country;Peru;Argentina|";
    IntentGenerator.buildTrainingPhraseFromEncodedString(trainingStringWithEntity);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmptyFile() throws IllegalArgumentException, NullPointerException, IOException {
    IntentGenerator.getIntentsInUseCase("emptyFile.txt");
  }

  @Test(expected = java.lang.NullPointerException.class)
  public void testFileDoesNotExists() throws IllegalArgumentException, NullPointerException, IOException {
    IntentGenerator.getIntentsInUseCase("Foo.txt");
  }

  @Test(expected = IOException.class)
  public void testIncorrectProtoFile() throws IllegalArgumentException, NullPointerException, IOException {
    IntentGenerator.getIntentsInUseCase("invalidProtoFile.prototxt");
  }
}
