package i5.las2peer.services.codeGenerationService.models.application;

import java.io.Serializable;

import i5.cae.simpleModel.SimpleModel;

public class Application {

  public Application(SimpleModel model, Serializable[] serializedModel) {
    for (int i = 0; i < serializedModel.length; i++)
      System.out.println(((SimpleModel) serializedModel[i]).getName());
  }

}
