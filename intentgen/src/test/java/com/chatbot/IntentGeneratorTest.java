package com.chatbot;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import com.google.cloud.dialogflow.v2.Intent;
import com.google.cloud.dialogflow.v2.ProjectAgentName;
import com.google.cloud.dialogflow.v2.Intent.TrainingPhrase;
import com.google.cloud.dialogflow.v2.Intent.TrainingPhrase.Part;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class IntentGeneratorTest {

  @Mock
  IntentsClientWrapper intentsClient;

  @Before
  public void setUp() throws IOException {
    final Intent intentA = Intent.newBuilder()
      .setName("123")
      .setDisplayName("intentA")
      .build();
    final Intent intentB = Intent.newBuilder()
      .setName("456")
      .setDisplayName("intentB")
      .build();
    when(intentsClient.getIntentsList(any(ProjectAgentName.class)))
        .thenReturn(new ArrayList<Intent>(Arrays.asList(intentA, intentB)));
  }

  @Test
  public void generateIntents_updateIntent() throws IllegalArgumentException, NullPointerException,
      IOException {
    IntentGenerator.generateIntents("updateIntents.prototxt", ProjectAgentName.of("123"),
        intentsClient);
    verify(intentsClient, times(2)).updateIntent(any(Intent.class), anyString());
  }

  @Test
  public void generateIntents_createIntent() throws IllegalArgumentException, NullPointerException,
      IOException {
    IntentGenerator.generateIntents("createIntents.prototxt", ProjectAgentName.of("123"),
        intentsClient);
    verify(intentsClient, times(2)).createIntent(any(ProjectAgentName.class), any(Intent.class));
  }

  @Test
  public void generateIntents_createAndUpdate() throws IllegalArgumentException,
      NullPointerException, IOException {
    IntentGenerator.generateIntents("createAndUpdateIntents.prototxt", ProjectAgentName.of("123"),
        intentsClient);
    verify(intentsClient, times(2)).createIntent(any(ProjectAgentName.class), any(Intent.class));
    verify(intentsClient, times(2)).updateIntent(any(Intent.class), anyString());
  }

  @Test
  public void buildTrainingPhraseFromEncodedString_noEntity() {
    final String trainingStringWithNoEntities = "This is a training phrase with no entities";
    final TrainingPhrase trainingPhrase =
        IntentGenerator.buildTrainingPhraseFromEncodedString(trainingStringWithNoEntities);
    // since there are no entities there should be only one part in this training
    // phrase
    assertEquals(trainingPhrase.getPartsCount(), 1);
    assertEquals(trainingPhrase.getParts(0).getText(), trainingStringWithNoEntities);
  }

  @Test
  public void buildTrainingPhraseFromEncodedString_withEntity() {
    final String trainingStringWithEntity = "He lives in |@sys.country;country;Peru|";
    final TrainingPhrase trainingPhrase =
        IntentGenerator.buildTrainingPhraseFromEncodedString(trainingStringWithEntity);
    final String textPart = trainingPhrase.getParts(0).getText();
    final Part EntityPart = trainingPhrase.getParts(1);
    final String entity = EntityPart.getEntityType().toString();
    final String alias = EntityPart.getAlias();
    final String text = EntityPart.getText();
    assertEquals(trainingPhrase.getPartsCount(), 2);
    assertEquals(textPart, "He lives in ");
    assertEquals(entity, "@sys.country");
    assertEquals(alias, "country");
    assertEquals(text, "Peru");
  }

  @Test(expected = IllegalArgumentException.class)
  public void buildTrainingPhraseFromEncodedString_invalidEntityEncoding()
      throws IllegalArgumentException {
    final String trainingStringWithEntity = "He lives in |@sys.country;country;Peru;Argentina|";
    IntentGenerator.buildTrainingPhraseFromEncodedString(trainingStringWithEntity);
  }

  @Test(expected = IllegalArgumentException.class)
  public void getIntentsInUseCase_emptyFile() throws IllegalArgumentException,
      NullPointerException, IOException {
    IntentGenerator.getIntentsInUseCase("emptyFile.txt");
  }

  @Test(expected = java.lang.NullPointerException.class)
  public void getIntentsInUseCase_nonExistentFile() throws IllegalArgumentException,
      NullPointerException, IOException {
    IntentGenerator.getIntentsInUseCase("Foo.txt");
  }

  @Test(expected = IOException.class)
  public void getIntentsInUseCase_invalidProtoFile() throws IllegalArgumentException,
      NullPointerException, IOException {
    IntentGenerator.getIntentsInUseCase("invalidProtoFile.prototxt");
  }
}
