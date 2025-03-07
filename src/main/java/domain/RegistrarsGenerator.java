package domain;

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

    private StringBuilder createRepositoryRegistrarBuilder(String repositoryKey, String languageKey, String packageName) {
        var result = new StringBuilder();

        var className = String.format("%sRepositoryRegistrar", languageKey);

        result.append(String.format("""
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
        result.append(String.format("public class %s implements RulesDefinition {\n", className));

        return result;
    }

    public void execute(
            String packageName,
            String languageKey,
            String compatibleLanguageKey,
            String repositoryKey,
            String targetDirectory,
            String profileName
    ) throws Exception {
        logger.log(
                String.format(
                        "Fetching rules for language %s",
                        languageKey
                )
        );

        var rules = ruleRepository.getRulesByLanguage(languageKey).stream().filter(rule -> rule.compatibleLanguages().contains(compatibleLanguageKey)).toList();

        // generate the repository factory
        logger.log(
                String.format(
                        "Generating the \"%s\" repository factory for language \"%s\" to %s",
                        repositoryKey,
                        compatibleLanguageKey,
                        targetDirectory
                )
        );

        var entryPointBuilder = new StringBuilder();

        var className = String.format("%sRepositoryRegistrar", repositoryKey);

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

        for (var rule : rules) {
            var tags = rule.tags().stream().map(tag -> String.format("\"%s\"", tag));

            entryPointBuilder.append(String.format("private void register%s(NewRepository repository) {\n", rule.name()));

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

            entryPointBuilder.append("}\n");
        }

        entryPointBuilder.append(String.format("""
                            public void define(Context context) {
                                NewRepository repository = context
                                    .createRepository("%s", "%s")
                                    .setName("SonarAnalyzer");
                        
                        """,
                repositoryKey,
                compatibleLanguageKey
        ));

        for (var rule : rules) {
            entryPointBuilder.append(String.format("""
                        this.register%s(repository);
                    """, rule.name()));
        }

        entryPointBuilder.append("""
                    repository.done();
                """);

        entryPointBuilder.append("  }\n");
        entryPointBuilder.append("};\n");

        var entryPointFileName = this.fileSystem.resolve(targetDirectory, className + ".java");

        this.fileSystem.write(entryPointFileName, entryPointBuilder.toString());

        // generate the profile definition class
        logger.log(
                String.format(
                        "Generating the \"%s\" profile definition for language \"%s\" to %s",
                        profileName,
                        compatibleLanguageKey,
                        targetDirectory
                )
        );

        var profileDefinitionClassName = String.format("%sProfileRegistrar", repositoryKey);
        var profileDefinitionBuilder = new StringBuilder();

        profileDefinitionBuilder.append(String.format("""
                package %s;
                import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
                """, packageName));

        profileDefinitionBuilder.append(String.format("public class %s implements BuiltInQualityProfilesDefinition {\n", profileDefinitionClassName));
        profileDefinitionBuilder.append("public void define(Context context) {\n");

        profileDefinitionBuilder.append(String.format("""
                var newProfile = context.createBuiltInQualityProfile(
                      "%s",
                      "%s"
                    );
                """, profileName, compatibleLanguageKey));

        for (var rule : rules) {
            if (rule.qualityProfiles().contains(profileName)) {
                profileDefinitionBuilder.append(String.format("newProfile.activateRule(\"%s\", \"%s\");\n", repositoryKey, rule.name()));
            }
        }

        profileDefinitionBuilder.append("newProfile.done();\n");
        profileDefinitionBuilder.append("  }\n");
        profileDefinitionBuilder.append("};\n");

        var profileDefinitionFileName = this.fileSystem.resolve(targetDirectory, profileDefinitionClassName + ".java");

        this.fileSystem.write(profileDefinitionFileName, profileDefinitionBuilder.toString());
    }
}
