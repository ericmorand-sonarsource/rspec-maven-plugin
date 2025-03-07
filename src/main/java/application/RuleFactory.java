package application;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import domain.Parameter;
import domain.Remediation;
import domain.Rule;

import java.util.ArrayList;
import java.util.List;

public class RuleFactory {
    static Rule create(String name, String htmlDocumentation, JsonObject manifest) {
        var defaultQualityProfiles = manifest.get("defaultQualityProfiles");
        var tags = manifest.get("tags");
        var code = manifest.get("code");
        var cleanCodeAttribute = code != null ? code.getAsJsonObject().get("attribute").getAsString() : "CONVENTIONAL";

        var rawScope = manifest.get("scope").getAsString();
        var rawParameters = manifest.get("parameters");

        List<Parameter> parameters = new ArrayList();

        if (rawParameters != null) {
            rawParameters.getAsJsonArray().asList().forEach(rawValue -> {
                // todo: add a factory
                var value = rawValue.getAsJsonObject();

                parameters.add(new Parameter() {
                    public String name() {
                        return value.get("name").getAsString();
                    }

                    public String description() {
                        return value.get("description").getAsString();
                    }

                    public String type() {
                        return value.get("type").getAsString();
                    }

                    public String defaultValue() {
                        return value.get("defaultValue").getAsString();
                    }
                });
            });
        }

        return new Rule() {
            public String name() {
                return name;
            }

            public String htmlDocumentation() {
                return htmlDocumentation;
            }

            public String type() {
                return manifest.get("type").getAsString();
            }

            public List<String> defaultQualityProfiles() {
                return defaultQualityProfiles != null ? defaultQualityProfiles.getAsJsonArray().asList().stream().map(JsonElement::getAsString).toList()
                        : List.of();
            }

            public String defaultSeverity() {
                return manifest.get("defaultSeverity").getAsString().toUpperCase();
            }

            public List<String> tags() {
                return tags != null ? tags.getAsJsonArray().asList().stream().map(JsonElement::getAsString).toList() : List.of();
            }

            public String cleanCodeAttribute() {
                return cleanCodeAttribute;
            }

            public String scope() {
                return rawScope.equals("Tests") ? "TEST" : rawScope.toUpperCase();
            }

            public Remediation remediation() {
                var rawRemediation = manifest.get("remediation");

                if (rawRemediation != null) {
                    var remediationAsJsonObject = rawRemediation.getAsJsonObject();
                    var rawCost = remediationAsJsonObject.get("constantCost");

                    if (rawCost != null) {
                        return new Remediation() {
                            public String function() {
                                return "foo";
                            }

                            public String cost() {
                                return rawCost.getAsString();
                            }
                        };
                    }
                }

                return null;
            }

            public String title() {
                return manifest.get("title").getAsString();
            }

            public List<Parameter> parameters() {
                return parameters;
            }
        };
    }
}
