package domain;

import java.util.List;

public interface Rule {
    String name();

    String htmlDocumentation();

    String type();

    String defaultSeverity();

    List<String> tags();

    String cleanCodeAttribute();

    String scope();

    Remediation remediation();

    String title();

    List<Parameter> parameters();

    List<String> compatibleLanguages();

    List<String> qualityProfiles();
}
