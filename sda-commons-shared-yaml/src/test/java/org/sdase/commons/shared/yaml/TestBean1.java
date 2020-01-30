package org.sdase.commons.shared.yaml;

public class TestBean1 {

  TestBean2 bean;

  TestBean1() {
    super();
  }

  TestBean1(TestBean2 bean) {
    this.bean = bean;
  }

  public TestBean2 getBean() {
    return bean;
  }

  public void setBean(TestBean2 bean) {
    this.bean = bean;
  }
}
