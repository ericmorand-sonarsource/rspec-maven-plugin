package application;

import com.sonarsource.ruleapi.github.GitHubRuleMaker;
import domain.*;

import domain.Exception;
import infrastructure.JVMHost;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "generate-registrars")
public class GenerateRegistrarsMojo extends AbstractMojo {
    @Parameter(property = "rspec.languageKey", required = true)
    private String languageKey;

    @Parameter(property = "rspec.targetDirectory", required = true)
    private String targetDirectory;

    @Parameter(property = "rspec.branchName", defaultValue = "master")
    private String branchName;

    @Parameter(property = "rspec.profileName", defaultValue = "Sonar way")
    private String profileName;

    @Parameter(property = "rspec.packageName", required = true)
    private String packageName;

    // todo: should probably not be required and default to languageKey
    @Parameter(property = "rspec.compatibleLanguageKey", required = true)
    private String compatibleLanguageKey;

    // todo: should probably not be required and default to languageKey
    @Parameter(property = "rspec.repositoryKey", required = true)
    private String repositoryKey;

    @Override
    public void execute() throws MojoExecutionException {
        var host = new JVMHost();
        var logger = this.getLog();

        /**
         * Unfortunately, GitHubRuleMaker clones the repository during its creation.
         * Thus, we need to log as soon as we create it.
         */
        logger.info(String.format("Cloning repository branch %s", branchName));

        var gitHubRuleMaker = GitHubRuleMaker.create(branchName);

        var generator = new RegistrarsGenerator(
                logger::info,
                new RuleRepository(gitHubRuleMaker),
                new FileSystem(host)
        );

        try {
            generator.execute(this.packageName, this.languageKey, this.compatibleLanguageKey, this.repositoryKey, this.targetDirectory, this.profileName);
        } catch (Exception e) {
            throw new MojoExecutionException(e);
        }
    }
}
