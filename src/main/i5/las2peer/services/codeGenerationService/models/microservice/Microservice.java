package i5.las2peer.services.codeGenerationService.models.microservice;

import i5.cae.simpleModel.SimpleModel;

public class Microservice {
  private String name;
  private String repository;
  private String address;

  public Microservice(SimpleModel model) {
    this.name = model.getName();
  }

}
