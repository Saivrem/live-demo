package biz.icecat.icedatav2.test_containers.overrider;

public interface PropertiesOverrider {

    boolean isApplicable(Object propertiesBean);

    void override(Object propertiesBean);
}
