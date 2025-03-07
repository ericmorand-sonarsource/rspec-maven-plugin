package domain;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.text.StringEscapeUtils;

public class RegistrarsGenerator {
    private final Logger logger;
    private final RuleRepository ruleRepository;
    private final FileSystem fileSystem;

    public RegistrarsGenerator(Logger logger, RuleRepository ruleRepository, FileSystem fileSystem) {
        this.logger = logger;
        this.ruleRepository = ruleRepository;
        this.fileSystem = fileSystem;
    }

    public void execute(
            String packageName,
            List<String> languageKeys,
            String targetDirectory,
            String profileName
    ) throws Exception {
        for (var languageKey : languageKeys) {
            var languageTargetPath = this.fileSystem.resolve(targetDirectory, languageKey);

            logger.log(
                    String.format(
                            "Generating %s rule classes into %s",
                            languageKey,
                            languageTargetPath
                    )
            );

            var rules = ruleRepository.getRulesByLanguage(languageKey);

            // generate the repository factory
            var entryPointBuilder = new StringBuilder();

            var className = String.format("%sRepositoryRegistrar", languageKey);

            entryPointBuilder.append(String.format("""
                    package %s;
                    import org.sonar.api.batch.rule.Severity;
                    import org.sonar.api.rules.CleanCodeAttribute;
                    import org.sonar.api.server.rule.RulesDefinition;
                    import org.sonar.api.server.rule.RuleParamType;
                    import org.sonar.api.server.debt.DebtRemediationFunction;
                    import org.sonar.api.server.debt.internal.DefaultDebtRemediationFunction;
                    import org.sonar.api.rules.RuleType;
                    import org.sonar.api.rule.RuleScope;
                    """, packageName));
            entryPointBuilder.append(String.format("public class %s implements RulesDefinition {\n", className));
            entryPointBuilder.append("  public void define(Context context) {\n");
            entryPointBuilder.append(String.format("""
                              NewRepository repository = context
                                .createRepository("%s", "%s")
                                .setName("SonarAnalyzer");
                            
                            """,
                    languageKey,
                    "js" /* languageKey */
            ));

            for (var rule : rules) {
                var tags = rule.tags().stream().map(tag -> String.format("\"%s\"", tag));

                entryPointBuilder.append(String.format("""
                                  var %s = repository.createRule("%s")
                                    .setName("%s")
                                    .setType(RuleType.%s)
                                    .setSeverity(Severity.%s.toString())
                                    .setTags(%s)
                                    .setCleanCodeAttribute(CleanCodeAttribute.%s)
                                    .setScope(RuleScope.%s)
                                    .setHtmlDescription("%s");
                                
                                """,
                        rule.name(),
                        rule.name(),
                        StringEscapeUtils.escapeJava(rule.title()),
                        rule.type(),
                        rule.defaultSeverity(),
                        tags.collect(Collectors.joining(",")),
                        rule.cleanCodeAttribute(),
                        rule.scope(),
                        StringEscapeUtils.escapeJava(rule.htmlDocumentation())
                ));

                if (rule.remediation() != null) {
                    entryPointBuilder.append(String.format("""
                              %s.setDebtRemediationFunction(new DefaultDebtRemediationFunction(DebtRemediationFunction.Type.LINEAR, "%s", ""));
                            
                            """,
                            rule.name(),
                            rule.remediation().cost()
                    ));
                }

                for (var parameter : rule.parameters()) {
                    entryPointBuilder.append(String.format("""
                                      %s.createParam("%s")
                                        .setName("%s")
                                        .setDescription("%s")
                                        .setDefaultValue("%s")
                                        .setType(RuleParamType.%s);
                                    
                                    """,
                            rule.name(),
                            parameter.name(),
                            parameter.name(),
                            StringEscapeUtils.escapeJava(parameter.description()),
                            StringEscapeUtils.escapeJava(parameter.defaultValue()),
                            parameter.type()
                    ));
                }
            }
            entryPointBuilder.append("    repository.done();\n");

            entryPointBuilder.append("  }\n");
            entryPointBuilder.append("};\n");

            var entryPointFileName = this.fileSystem.resolve(targetDirectory, className + ".java");

            this.fileSystem.write(entryPointFileName, entryPointBuilder.toString());

            // generate the profile definition class
            var profileDefinitionClassName = String.format("%sProfileRegistrar", languageKey);
            var profileDefinitionBuilder = new StringBuilder();

            profileDefinitionBuilder.append(String.format("""
                    package %s;
                    import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
                    """, packageName));

            profileDefinitionBuilder.append(String.format("public class %s implements BuiltInQualityProfilesDefinition {\n", profileDefinitionClassName));
            profileDefinitionBuilder.append("public void define(Context context) {\n");

            // todo: "js" needs to be an argument...but what is it?
            profileDefinitionBuilder.append(String.format("""
                    var newProfile = context.createBuiltInQualityProfile(
                          "%s",
                          "js"
                        );
                    """, profileName));

            for (var rule : rules) {
                //if (rule.defaultQualityProfiles().contains(profileName)) {
                profileDefinitionBuilder.append(String.format("newProfile.activateRule(\"%s\", \"%s\");\n", languageKey, rule.name()));
                //}
            }

            profileDefinitionBuilder.append("newProfile.done();\n");
            profileDefinitionBuilder.append("  }\n");
            profileDefinitionBuilder.append("};\n");

            var profileDefinitionFileName = this.fileSystem.resolve(targetDirectory, profileDefinitionClassName + ".java");

            this.fileSystem.write(profileDefinitionFileName, profileDefinitionBuilder.toString());
        }
    }
}
