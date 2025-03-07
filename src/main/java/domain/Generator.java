package domain;

import com.google.gson.Gson;

import java.util.List;

public class Generator {
    private final Logger logger;
    private final RuleRepository ruleRepository;
    private final FileSystem fileSystem;

    public Generator(Logger logger, RuleRepository ruleRepository, FileSystem fileSystem) {
        this.logger = logger;
        this.ruleRepository = ruleRepository;
        this.fileSystem = fileSystem;
    }

    public void execute(
            List<String> languageKeys,
            String targetDirectory
    ) throws Exception {
        for (var languageKey : languageKeys) {
            var languageTargetPath = this.fileSystem.resolve(targetDirectory, languageKey);

            logger.log(
                    String.format(
                            "Generating %s rule data into %s",
                            languageKey,
                            languageTargetPath
                    )
            );

            var rules = ruleRepository.getRulesByLanguage(languageKey);
            var serializer = new Gson();

            for (var rule : rules) {
                var documentationFileName = rule.name() + ".html";
                var documentationFile = this.fileSystem.resolve(languageTargetPath, documentationFileName);

                this.fileSystem.write(documentationFile, rule.htmlDocumentation());

                var manifestFileName = rule.name() + ".json";
                var manifestFile = this.fileSystem.resolve(languageTargetPath, manifestFileName);

                this.fileSystem.write(manifestFile, serializer.toJson(rule));
            }
        }
    }
}
