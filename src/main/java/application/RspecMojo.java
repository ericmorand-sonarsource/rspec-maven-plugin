package application;

import com.sonarsource.ruleapi.github.GitHubRuleMaker;
import domain.*;
import domain.Exception;
import infrastructure.JVMHost;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.HashMap;
import java.util.List;

@Mojo(name = "rspec")
public class RspecMojo extends AbstractMojo {
    @Parameter(property = "rspec.languageKeys", required = true)
    private List<String> languageKeys;

    @Parameter(property = "rspec.targetDirectory", required = true)
    private String targetDirectory;

    @Parameter(property = "rspec.branchName", defaultValue = "master")
    private String branchName;

    @Override
    public void execute() throws MojoExecutionException {
        var host = new JVMHost();
        var logger = this.getLog();

        /**
         * Unfortunately, GitHubRuleMaker fetches the repository during its creation.
         * Thus, we need to log as soon as we create it.
         */
        logger.info(String.format("Fetching rule data from branch %s", branchName));

        var gitHubRuleMaker = GitHubRuleMaker.create(branchName);

        var generator = new Generator(
                logger::info,
                languageKey -> {
                    var rules = gitHubRuleMaker.getRulesByLanguage(languageKey, new HashMap<>(), file -> false);

                    return rules.stream().map(rule -> {
                        var metadata = rule.getMetadata();

                        return RuleFactory.create(
                                rule.getKey(),
                                rule.getDescription(),
                                metadata
                        );
                    }).toList();
                },
                new FileSystem() {
                    public String resolve(String first, String... more) {
                        return host.resolve(first, more);
                    }

                    public void write(String filePath, String content) throws Exception {
                        try {
                            host.write(filePath, content);
                        } catch (IOException e) {
                            throw new Exception();
                        }
                    }
                }
        );

        try {
            generator.execute(this.languageKeys, this.targetDirectory);
        } catch (Exception e) {
            throw new MojoExecutionException(e);
        }
    }
}
