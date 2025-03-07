package domain;

import java.util.List;

public interface Rule {
    String name();

    String htmlDocumentation();

    String type();

    List<String> defaultQualityProfiles();

    String defaultSeverity();

    List<String> tags();

    String cleanCodeAttribute();

    String scope();

    Remediation remediation();

    String title();

    List<Parameter> parameters();
}
