package biz.icecat.icedatav2.utils.containers.overrider;

public interface PropertiesOverrider {

    boolean isApplicable(Object propertiesBean);

    void override(Object propertiesBean);
}
