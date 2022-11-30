package org.sdase.commons.server.spring.data.mongo;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ConfigOverride.randomPorts;
import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.mongo.testing.MongoDbClassExtension;
import org.sdase.commons.server.spring.data.mongo.example.MyConfiguration;
import org.sdase.commons.server.spring.data.mongo.example.model.Cat;
import org.sdase.commons.server.spring.data.mongo.example.model.Dog;
import org.sdase.commons.server.spring.data.mongo.example.model.Person;
import org.sdase.commons.server.spring.data.mongo.example.model.PetOwner;
import org.sdase.commons.server.spring.data.mongo.example.model.PhoneNumber;
import org.sdase.commons.server.spring.data.mongo.example.repository.PersonRepository;
import org.sdase.commons.server.spring.data.mongo.example.repository.PetOwnerRepository;
import org.springframework.data.mongodb.core.MongoOperations;

class SpringDataMongoBundleClassDiscriminatorIT {

  @RegisterExtension
  @Order(0)
  static final MongoDbClassExtension mongo = MongoDbClassExtension.builder().build();

  @RegisterExtension
  @Order(1)
  static final DropwizardAppExtension<MyConfiguration> DW_WITH =
      new DropwizardAppExtension<>(
          WithClassDiscriminatorApp.class,
          null,
          randomPorts(),
          config("springDataMongo.connectionString", mongo::getConnectionString));

  @RegisterExtension
  @Order(2)
  static final DropwizardAppExtension<MyConfiguration> DW_WITHOUT =
      new DropwizardAppExtension<>(
          WithoutClassDiscriminatorApp.class,
          null,
          randomPorts(),
          config("springDataMongo.connectionString", mongo::getConnectionString));

  @Test
  void shouldSavePersonWithClassProperty() {
    WithClassDiscriminatorApp app = DW_WITH.getApplication();
    MongoOperations mongoOperations = app.getMongoOperations();
    var savedPerson = mongoOperations.save(createPerson());

    var foundPerson = mongoOperations.findById(savedPerson.getId(), Person.class);
    assertThat(foundPerson).isNotNull();
    assertThat(foundPerson.getEntityClass()).isNotNull();
  }

  @Test
  void shouldSavePersonWithRepositoryWithClassProperty() {
    WithClassDiscriminatorApp app = DW_WITH.getApplication();
    var repository = app.getPersonRepository();
    var savedPerson = repository.save(createPerson());

    var foundPerson = repository.findById(savedPerson.getId());
    assertThat(foundPerson).isPresent();
    assertThat(foundPerson.get().getEntityClass()).isNotNull();
  }

  @Test
  void shouldSavePersonWithoutClassProperty() {
    WithoutClassDiscriminatorApp app = DW_WITHOUT.getApplication();
    MongoOperations mongoOperations = app.getMongoOperations();
    var savedPerson = mongoOperations.save(createPerson());

    var foundPerson = mongoOperations.findById(savedPerson.getId(), Person.class);
    assertThat(foundPerson).isNotNull();
    assertThat(foundPerson.getEntityClass()).isNull();
  }

  @Test
  void shouldSavePersonWithRepositoryWithoutClassProperty() {
    WithoutClassDiscriminatorApp app = DW_WITHOUT.getApplication();
    var repository = app.getPersonRepository();
    var savedPerson = repository.save(createPerson());

    var foundPerson = repository.findById(savedPerson.getId());
    assertThat(foundPerson).isPresent();
    assertThat(foundPerson.get().getEntityClass()).isNull();
  }

  @Test
  void shouldSaveDogPetOwnerWithClassProperty() {
    WithClassDiscriminatorApp app = DW_WITH.getApplication();
    MongoOperations mongoOperations = app.getMongoOperations();
    var savedOwner = mongoOperations.save(createDogPetOwner());

    var foundOwner = mongoOperations.findById(savedOwner.getId(), PetOwner.class);

    assertThat(foundOwner).isNotNull();
    assertThat(foundOwner.getPet()).isNotNull();
    assertThat(foundOwner.getPet()).isInstanceOf(Dog.class);
    assertThat(foundOwner.getEntityClass()).isNotNull();
    assertThat(foundOwner.getPet().getEntityClass())
        .isEqualTo("org.sdase.commons.server.spring.data.mongo.example.model.Dog");
  }

  @Test
  void shouldSaveDogPetOwnerWithRepositoryWithClassProperty() {
    WithClassDiscriminatorApp app = DW_WITH.getApplication();
    var repository = app.getPetOwnerRepository();
    var savedOwner = repository.save(createDogPetOwner());

    var foundOwner = repository.findById(savedOwner.getId());

    assertThat(foundOwner).isPresent();
    assertThat(foundOwner.get().getPet()).isNotNull();
    assertThat(foundOwner.get().getPet()).isInstanceOf(Dog.class);
    assertThat(foundOwner.get().getEntityClass()).isNotNull();
    assertThat(foundOwner.get().getPet().getEntityClass())
        .isEqualTo("org.sdase.commons.server.spring.data.mongo.example.model.Dog");
  }

  @Test
  void shouldSaveCatPetOwnerWithClassProperty() {
    WithClassDiscriminatorApp app = DW_WITH.getApplication();
    MongoOperations mongoOperations = app.getMongoOperations();
    var savedOwner = mongoOperations.save(createCatPetOwner());

    var foundOwner = mongoOperations.findById(savedOwner.getId(), PetOwner.class);

    assertThat(foundOwner).isNotNull();
    assertThat(foundOwner.getEntityClass()).isNotNull();
    assertThat(foundOwner.getPet()).isNotNull();
    assertThat(foundOwner.getPet()).isInstanceOf(Cat.class);
    assertThat(foundOwner.getPet().getEntityClass())
        .isEqualTo("org.sdase.commons.server.spring.data.mongo.example.model.Cat");
  }

  @Test
  void shouldSaveCatPetOwnerWithRepositoryWithClassProperty() {
    WithClassDiscriminatorApp app = DW_WITH.getApplication();
    var repository = app.getPetOwnerRepository();
    var savedOwner = repository.save(createCatPetOwner());

    var foundOwner = repository.findById(savedOwner.getId());

    assertThat(foundOwner).isPresent();
    assertThat(foundOwner.get().getPet()).isNotNull();
    assertThat(foundOwner.get().getPet()).isInstanceOf(Cat.class);
    assertThat(foundOwner.get().getEntityClass()).isNotNull();
    assertThat(foundOwner.get().getPet().getEntityClass())
        .isEqualTo("org.sdase.commons.server.spring.data.mongo.example.model.Cat");
  }

  @Test
  void shouldSaveDogPetOwnerWithoutClassProperty() {
    WithoutClassDiscriminatorApp app = DW_WITHOUT.getApplication();
    MongoOperations mongoOperations = app.getMongoOperations();
    var savedOwner = mongoOperations.save(createDogPetOwner());

    var foundOwner = mongoOperations.findById(savedOwner.getId(), PetOwner.class);

    assertThat(foundOwner).isNotNull();
    assertThat(foundOwner.getEntityClass()).isNull();
    assertThat(foundOwner.getPet()).isNotNull();
    assertThat(foundOwner.getPet()).isInstanceOf(Dog.class);
    assertThat(foundOwner.getPet().getEntityClass()).isEqualTo("dog");
  }

  @Test
  void shouldSaveCatPetOwnerWithoutClassProperty() {
    WithoutClassDiscriminatorApp app = DW_WITHOUT.getApplication();
    MongoOperations mongoOperations = app.getMongoOperations();
    var savedOwner = mongoOperations.save(createCatPetOwner());

    var foundOwner = mongoOperations.findById(savedOwner.getId(), PetOwner.class);

    assertThat(foundOwner).isNotNull();
    assertThat(foundOwner.getEntityClass()).isNull();
    assertThat(foundOwner.getPet()).isNotNull();
    assertThat(foundOwner.getPet()).isInstanceOf(Cat.class);
    assertThat(foundOwner.getPet().getEntityClass()).isEqualTo("cat");
  }

  @Test
  void shouldSaveDogPetOwnerWithRepositoryWithoutClassProperty() {
    WithoutClassDiscriminatorApp app = DW_WITHOUT.getApplication();
    var repository = app.getPetOwnerRepository();
    var savedOwner = repository.save(createDogPetOwner());

    Optional<PetOwner> foundPetOwner = repository.findById(savedOwner.getId());
    assertThat(foundPetOwner).isPresent();
    assertThat(foundPetOwner.get().getEntityClass()).isNull();
    assertThat(foundPetOwner.get().getPet()).isInstanceOf(Dog.class);
    assertThat(foundPetOwner.get().getPet().getEntityClass()).isEqualTo("dog");
  }

  @Test
  void shouldSaveCatPetOwnerWithRepositoryWithoutClassProperty() {
    WithoutClassDiscriminatorApp app = DW_WITHOUT.getApplication();
    var repository = app.getPetOwnerRepository();
    var savedOwner = repository.save(createCatPetOwner());

    var foundPetOwner = repository.findById(savedOwner.getId());
    assertThat(foundPetOwner).isPresent();
    assertThat(foundPetOwner.get().getEntityClass()).isNull();
    assertThat(foundPetOwner.get().getPet()).isInstanceOf(Cat.class);
    assertThat(foundPetOwner.get().getPet().getEntityClass()).isEqualTo("cat");
  }

  private PetOwner createDogPetOwner() {
    var petOwner = new PetOwner();
    petOwner.setPet(createDog());
    return petOwner;
  }

  private PetOwner createCatPetOwner() {
    var petOwner = new PetOwner();
    petOwner.setPet(createCat());
    return petOwner;
  }

  private Dog createDog() {
    Dog dog = new Dog();
    dog.setName("Brutus");
    dog.setBreed("Doberman");
    dog.setWeight(25.4D);
    dog.setColor("Black");
    return dog;
  }

  private Cat createCat() {
    Cat cat = new Cat();
    cat.setName("Brutus");
    cat.setAge(2);
    cat.setWeight(25.4D);
    cat.setColor("Black");
    return cat;
  }

  private Person createPerson() {
    PhoneNumber phoneNumber = new PhoneNumber().setNumber("+49123456789");
    return new Person()
        .setAge(18)
        .setName("Robert")
        .setBirthday(LocalDate.now().minusYears(44))
        .setPhoneNumber(phoneNumber);
  }

  public static class WithClassDiscriminatorApp extends Application<MyConfiguration> {

    private final SpringDataMongoBundle<MyConfiguration> springDataMongoBundle =
        SpringDataMongoBundle.builder()
            .withConfigurationProvider(MyConfiguration::getSpringDataMongo)
            .withEntities(Dog.class, Cat.class)
            .build();

    private PersonRepository personRepository;
    private PetOwnerRepository petOwnerRepository;

    @Override
    public void initialize(Bootstrap<MyConfiguration> bootstrap) {
      bootstrap.addBundle(springDataMongoBundle);
    }

    @Override
    public void run(MyConfiguration configuration, Environment environment) {
      this.personRepository = springDataMongoBundle.createRepository(PersonRepository.class);
      this.petOwnerRepository = springDataMongoBundle.createRepository(PetOwnerRepository.class);
    }

    public MongoOperations getMongoOperations() {
      return springDataMongoBundle.getMongoOperations();
    }

    public PersonRepository getPersonRepository() {
      return personRepository;
    }

    public PetOwnerRepository getPetOwnerRepository() {
      return this.petOwnerRepository;
    }
  }

  public static class WithoutClassDiscriminatorApp extends Application<MyConfiguration> {
    private final SpringDataMongoBundle<MyConfiguration> springDataMongoBundle =
        SpringDataMongoBundle.builder()
            .withConfigurationProvider(MyConfiguration::getSpringDataMongo)
            .withEntities(Dog.class, Cat.class)
            .withoutClassDiscriminator(
                new String[] {"org.sdase.commons.server.spring.data.mongo.example.model"})
            .build();

    private PersonRepository personRepository;
    private PetOwnerRepository petOwnerRepository;

    @Override
    public void initialize(Bootstrap<MyConfiguration> bootstrap) {
      bootstrap.addBundle(springDataMongoBundle);
    }

    @Override
    public void run(MyConfiguration configuration, Environment environment) {
      this.personRepository = springDataMongoBundle.createRepository(PersonRepository.class);
      this.petOwnerRepository = springDataMongoBundle.createRepository(PetOwnerRepository.class);
    }

    public MongoOperations getMongoOperations() {
      return springDataMongoBundle.getMongoOperations();
    }

    public PersonRepository getPersonRepository() {
      return personRepository;
    }

    public PetOwnerRepository getPetOwnerRepository() {
      return this.petOwnerRepository;
    }
  }
}
